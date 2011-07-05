/**
 * Copyright (c) 2011, Thilo Planz. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package v7cr.vaadin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;

import v7cr.v7db.SchemaDefinition;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Container.Ordered;
import com.vaadin.data.util.AbstractContainer;
import com.vaadin.data.util.BeanContainer;

/**
 * A Vaadin Container backed by a MongoDB collection.
 * 
 * <p>
 * It is "partially lazy-loading": Upon construction, it loads all documents'
 * _id fields (so it probably does not work with capped collections) into an
 * ordered in-memory collection. The rest of the document gets loaded on demand
 * and in pages.
 * 
 * <p>
 * Every document becomes a Vaadin Item. Since MongoDB is schema-free, there is
 * no database metadata to get the propertyIds for these items. If you do not
 * specify any, the first page of documents is loaded and all top-level
 * non-object field names found there are added as propertyIds. The types of
 * these properties are set to the first value found.
 * 
 * <p>
 * You can add nested properties ("a.x") manually, just like with the
 * {@link BeanContainer}.
 * 
 * 
 * @author Thilo Planz, based on the Vaadin SQLContainer addon
 * 
 */

public class DBCollectionContainer extends AbstractContainer implements
		Ordered, Indexed {

	private final List<Object> _ids;

	private final DBCollection collection;

	/** Container properties = column names, data types and statuses */
	private final List<String> propertyIds = new ArrayList<String>();
	private final Map<String, Class<?>> propertyTypes = new HashMap<String, Class<?>>();
	private final Map<String, Boolean> propertyReadOnly = new HashMap<String, Boolean>();
	private final Map<String, Boolean> propertyNullable = new HashMap<String, Boolean>();

	/** Page length = number of items contained in one page */
	private final int pageLength = DEFAULT_PAGE_LENGTH;
	public static final int DEFAULT_PAGE_LENGTH = 100;

	/** Starting row number of the currently fetched page */
	private int currentOffset = -1;

	/** Item and index caches */
	private final Map<Integer, Object> itemIndexes = new HashMap<Integer, Object>();
	private final Map<Object, BSONItem> cachedItems = new HashMap<Object, BSONItem>();

	public DBCollectionContainer(DBCollection collection, DBCursor cursor) {
		this.collection = collection;
		_ids = initIds(cursor);
		initPropertyIds();
	}

	private List<Object> initIds(DBCursor cursor) {
		List<DBObject> x = cursor.toArray();
		List<Object> _ids = new ArrayList<Object>(x.size());
		for (DBObject o : x) {
			_ids.add(o.get("_id"));
		}
		return _ids;
	}

	private void initPropertyIds() {
		if (propertyIds.isEmpty()) {
			getPage();

			for (BSONItem b : cachedItems.values())
				for (String s : b.getBSONObject().keySet())
					if (!propertyIds.contains(s))
						if (!(b.getBSONObject().get(s) instanceof BSONObject)) {
							propertyIds.add(s);
							propertyTypes.put(s, b.getBSONObject().get(s)
									.getClass());
						}
		}

	}

	private void initPropertyIds(SchemaDefinition schema) {
		if (schema != null)
			for (String s : schema.getFieldNames())
				if (!propertyIds.contains(s)) {
					propertyIds.add(s);
					propertyTypes.put(s, String.class);
				}

		initPropertyIds();

	}

	public DBCollectionContainer(DBCollection collection, String sortBy,
			boolean ascending) {
		this(null, collection, null, sortBy, ascending);
	}

	public DBCollectionContainer(SchemaDefinition schema,
			DBCollection collection, DBObject filter, String sortBy,
			boolean ascending) {
		this.collection = collection;
		DBObject _null = new BasicDBObject();
		if (filter == null)
			filter = _null;
		DBCursor cursor = collection.find(filter, _null);
		if (sortBy != null) {
			cursor = cursor.sort(new BasicDBObject(sortBy, ascending ? 1 : -1));
		}
		_ids = initIds(cursor);
		initPropertyIds(schema);
	}

	private void getPage() {

		if (_ids.isEmpty())
			return;

		int fromIndex;
		if (currentOffset < 0) {
			fromIndex = 0;
		} else {
			cachedItems.clear();
			itemIndexes.clear();
			fromIndex = currentOffset + pageLength;
		}

		int toIndex = fromIndex + pageLength;
		if (toIndex >= _ids.size()) {
			toIndex = _ids.size() - 1;
		}
		// TODO: is there a way to do bulk loading?
		int idx = fromIndex;
		for (Object id : _ids.subList(fromIndex, toIndex)) {
			// TODO: only load the fields we need
			cachedItems.put(id, new BSONItem(collection.findOne(id)));
			itemIndexes.put(idx++, id);
		}
		currentOffset = fromIndex;
	}

	public boolean addContainerProperty(Object propertyId, Class<?> type,
			Object defaultValue) throws UnsupportedOperationException {

		if (propertyIds.contains(propertyId))
			return false;

		if (!(propertyId instanceof String))
			return false;

		String pid = (String) propertyId;
		propertyIds.add(pid);
		propertyTypes.put(pid, type);
		return true;

	}

	public Object addItem() throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	public Item addItem(Object itemId) throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean containsId(Object itemId) {
		return _ids.contains(itemId);
	}

	public Property getContainerProperty(Object itemId, Object propertyId) {
		Item item = getItem(itemId);
		if (item == null) {
			return null;
		}
		return item.getItemProperty(propertyId);
	}

	public Collection<?> getContainerPropertyIds() {
		return Collections.unmodifiableCollection(propertyIds);
	}

	public Item getItem(Object itemId) {
		BSONItem cached = cachedItems.get(itemId);
		if (cached != null)
			return cached;

		BSONObject b = collection.findOne(itemId);
		if (b == null)
			return null;

		BSONItem i = new BSONItem(b);
		cachedItems.put(itemId, i);

		return i;
	}

	public Collection<?> getItemIds() {
		return Collections.unmodifiableCollection(_ids);
	}

	public Class<?> getType(Object propertyId) {
		return propertyTypes.get(propertyId);
	}

	public boolean removeAllItems() throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean removeContainerProperty(Object propertyId)
			throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean removeItem(Object itemId)
			throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	public int size() {
		return _ids.size();
	}

	public Object addItemAfter(Object previousItemId)
			throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	public Item addItemAfter(Object previousItemId, Object newItemId)
			throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object firstItemId() {
		return _ids.get(0);
	}

	public boolean isFirstId(Object itemId) {
		if (!_ids.get(0).equals(itemId))
			return false;
		return true;
	}

	public boolean isLastId(Object itemId) {
		int idx = _ids.size() - 1;
		if (!_ids.get(idx).equals(itemId))
			return false;
		return true;
	}

	public Object lastItemId() {
		int idx = _ids.size() - 1;
		return _ids.get(idx);
	}

	public Object nextItemId(Object itemId) {
		// TODO: linear search, not so nice
		boolean theNext = false;
		for (Object id : _ids) {
			if (theNext)
				return id;
			if (itemId.equals(id))
				theNext = true;
		}
		return null;
	}

	public Object prevItemId(Object itemId) {
		// TODO: linear search, not so nice
		Object prev = null;
		for (Object id : _ids) {
			if (itemId.equals(id))
				return prev;
			prev = id;
		}
		return null;
	}

	public Object addItemAt(int index) throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	public Item addItemAt(int index, Object newItemId)
			throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getIdByIndex(int index) {
		return _ids.get(index);
	}

	public int indexOfId(Object itemId) {
		return _ids.indexOf(itemId);
	}

}
