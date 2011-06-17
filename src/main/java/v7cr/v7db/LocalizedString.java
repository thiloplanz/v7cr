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

package v7cr.v7db;

/**
 * A string intended for user interface elements. It can be localized for
 * different languages.
 * 
 * 
 */

public class LocalizedString {

	private final String fixedString;

	/**
	 * create a "localized" String that is actually just fixed (to the same
	 * String for all locales)
	 * 
	 * @param fixed
	 *            the single String to be used for all locales
	 */
	public LocalizedString(String fixed) {
		fixedString = fixed;
	}

	@Override
	public String toString() {
		return fixedString;
	}

}
