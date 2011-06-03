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

public class Cardinality {

	private final int min;

	private final Integer max;

	private final String string;

	static final Cardinality ONE = new Cardinality("1", 1, 1);

	static final Cardinality N = new Cardinality("N", 1, null);

	private Cardinality(String cardinality, int min, Integer max) {
		string = cardinality;
		this.min = min;
		this.max = max;
	}

	public boolean areMultipleAllowed() {
		return max == null || max > 1;
	}

	public boolean isAllowed(int count) {
		if (count < min)
			return false;
		if (max == null)
			return true;
		return max >= count;
	}

	public String toString() {
		return string;
	}

	static Cardinality getCardinality(String cardinality) {
		if (cardinality == null)
			return null;
		if ("N".equals(cardinality))
			return N;
		if ("1".equals(cardinality))
			return ONE;
		int dotdot = cardinality.indexOf("..");
		if (dotdot == -1) {
			int fixed = Integer.parseInt(cardinality);
			return new Cardinality(cardinality, fixed, fixed);
		}
		int min = Integer.parseInt(cardinality.substring(0, dotdot));
		String rest = cardinality.substring(dotdot + 2);
		if ("N".equals(rest)) {
			if (min == 1)
				return N;
			return new Cardinality(cardinality, min, null);
		}
		return new Cardinality(cardinality, min, Integer.parseInt(rest));
	}

}
