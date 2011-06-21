/*
 * Copyright 1999-2010 Luca Garulli (l.garulli--at--orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.core.metadata.schema;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.serialization.OBase64Utils;

/**
 * Generic representation of a type.<br/>
 * allowAssignmentFrom accepts any class, but Array.class means that the type accepts generic Arrays.
 * 
 * @author Luca Garulli
 * 
 */
public enum OType {
	BOOLEAN("Boolean", 0, false, true, 1, new Class<?>[] { Boolean.class, Boolean.TYPE }, new Class<?>[] { Boolean.class }) {
	},
	INTEGER("Integer", 1, false, true, 4, new Class<?>[] { Integer.class, Integer.TYPE }, new Class<?>[] { Number.class }) {
	},
	SHORT("Short", 2, false, true, 2, new Class<?>[] { Short.class, Short.TYPE }, new Class<?>[] { Number.class }) {
	},
	LONG("Long", 3, false, true, 8, new Class<?>[] { Long.class, Long.TYPE }, new Class<?>[] { Number.class }) {
	},
	FLOAT("Float", 4, false, true, 4, new Class<?>[] { Float.class, Float.TYPE }, new Class<?>[] { Number.class }) {
	},
	DOUBLE("Double", 5, false, true, 8, new Class<?>[] { Double.class, Double.TYPE }, new Class<?>[] { Number.class }) {
	},
	DATETIME("Datetime", 6, false, true, 8, new Class<?>[] { Date.class }, new Class<?>[] { Date.class, Long.class }) {
	},
	STRING("String", 7, false, false, 8, new Class<?>[] { String.class }, new Class<?>[] { String.class }) {
	},
	BINARY("Binary", 8, false, false, 8, new Class<?>[] { Array.class }, new Class<?>[] { Array.class }) {
	},
	EMBEDDED("Embedded", 9, true, false, 8, new Class<?>[] { Object.class }, new Class<?>[] { Object.class }) {
	},
	EMBEDDEDLIST("EmbeddedList", 10, true, false, 8, new Class<?>[] { List.class }, new Class<?>[] { Collection.class, Array.class }) {
	},
	EMBEDDEDSET("EmbeddedSet", 11, true, false, 8, new Class<?>[] { Set.class }, new Class<?>[] { Collection.class, Array.class }) {
	},
	EMBEDDEDMAP("EmbeddedMap", 12, true, false, 8, new Class<?>[] { Map.class }, new Class<?>[] { Map.class }) {
	},
	LINK("Link", 13, true, true, 8, new Class<?>[] { Object.class, ORecordId.class }, new Class<?>[] { ORecord.class, ORID.class }) {
	},
	LINKLIST("LinkList", 14, true, false, 8, new Class<?>[] { List.class }, new Class<?>[] { Collection.class, Array.class }) {
	},
	LINKSET("LinkSet", 15, true, false, 8, new Class<?>[] { Set.class }, new Class<?>[] { Collection.class, Array.class }) {
	},
	LINKMAP("LinkMap", 16, true, false, 8, new Class<?>[] { Map.class }, new Class<?>[] { Map.class }) {
	},
	BYTE("Byte", 17, false, true, 1, new Class<?>[] { Byte.class, Byte.TYPE }, new Class<?>[] { Number.class, Character.class }) {
	},
	TRANSIENT("Transient", 18, false, true, 0, new Class<?>[] {}, new Class<?>[] {}) {
	},
	DATE("Date", 19, false, true, 8, new Class<?>[] { Date.class }, new Class<?>[] { Date.class, Long.class }) {
	};

	protected static final OType[]	TYPES	= new OType[] { BOOLEAN, BYTE, INTEGER, SHORT, LONG, FLOAT, DOUBLE, DATE, DATETIME, STRING,
			BINARY, EMBEDDED, EMBEDDEDLIST, EMBEDDEDSET, EMBEDDEDMAP, LINK, LINKLIST, LINKSET, LINKMAP, TRANSIENT };

	protected String								name;
	protected int										id;
	protected boolean								complex;
	protected boolean								fixedSize;
	protected int										size;
	protected Class<?>[]						javaTypes;
	protected Class<?>[]						allowAssignmentFrom;

	private OType(final String iName, final int iId, final boolean iComplex, final boolean iFixedSize, final int iSize,
			final Class<?>[] iJavaTypes, final Class<?>[] iAllowAssignmentBy) {
		name = iName;
		id = iId;
		complex = iComplex;
		fixedSize = iFixedSize;
		size = iSize;
		javaTypes = iJavaTypes;
		allowAssignmentFrom = iAllowAssignmentBy;
	}

	/**
	 * Return the type by ID.
	 * 
	 * @param iId
	 *          The id to search
	 * @return The type if any, otherwise null
	 */
	public static OType getById(final byte iId) {
		for (OType t : TYPES) {
			if (iId == t.id)
				return t;
		}
		return null;
	}

	/**
	 * Check if the value is assignable by the current type
	 * 
	 * @param iPropertyValue
	 *          Object to check
	 * @return true if it's assignable, otherwise false
	 */
	public boolean isAssignableFrom(final Object iPropertyValue) {
		if (iPropertyValue == null)
			return true;

		final Class<?> cls = iPropertyValue.getClass();

		for (int i = 0; i < allowAssignmentFrom.length; ++i) {
			if (allowAssignmentFrom[i].equals(Array.class) && cls.isArray())
				// SPECIAL CASE: GET ARRAYS
				return true;

			if (allowAssignmentFrom[i].isAssignableFrom(cls))
				return true;
		}
		return false;
	}

	/**
	 * Return the correspondent type by checking the "assignability" of the class received as parameter.
	 * 
	 * @param iClass
	 *          Class to check
	 * @return OType instance if found, otherwise null
	 */
	public static OType getTypeByClass(final Class<?> iClass) {
		if (iClass == null)
			return null;

		for (OType type : TYPES)
			for (int i = 0; i < type.javaTypes.length; ++i) {
				if (type.javaTypes[i] == iClass)
					return type;
			}

		return null;
	}

	/**
	 * Return the correspondent type by checking the "assignability" of the class received as parameter.
	 * 
	 * @param iClass
	 *          Class to check
	 * @return OType instance if found, otherwise null
	 */
	public static OType getTypeByAssignability(final Class<?> iClass) {
		if (iClass == null)
			return null;

		for (OType type : TYPES)
			for (int i = 0; i < type.allowAssignmentFrom.length; ++i) {
				if (type.allowAssignmentFrom[i].isAssignableFrom(iClass))
					return type;
			}

		return null;
	}

	public boolean isComplex() {
		return complex;
	}

	/**
	 * Convert the input object to an integer.
	 * 
	 * @param iValue
	 *          Any type supported
	 * @return The integer value if the conversion succeed, otherwise the IllegalArgumentException exception
	 */
	public int asInt(final Object iValue) {
		if (iValue instanceof Number)
			return ((Number) iValue).intValue();
		else if (iValue instanceof String)
			return Integer.valueOf((String) iValue);
		else if (iValue instanceof Boolean)
			return ((Boolean) iValue) ? 1 : 0;

		throw new IllegalArgumentException("Can't convert value " + iValue + " to int for the type: " + name);
	}

	/**
	 * Convert the input object to a long.
	 * 
	 * @param iValue
	 *          Any type supported
	 * @return The long value if the conversion succeed, otherwise the IllegalArgumentException exception
	 */
	public long asLong(final Object iValue) {
		if (iValue instanceof Number)
			return ((Number) iValue).longValue();
		else if (iValue instanceof String)
			return Long.valueOf((String) iValue);
		else if (iValue instanceof Boolean)
			return ((Boolean) iValue) ? 1 : 0;

		throw new IllegalArgumentException("Can't convert value " + iValue + " to long for the type: " + name);
	}

	/**
	 * Convert the input object to a float.
	 * 
	 * @param iValue
	 *          Any type supported
	 * @return The float value if the conversion succeed, otherwise the IllegalArgumentException exception
	 */
	public float asFloat(final Object iValue) {
		if (iValue instanceof Number)
			return ((Number) iValue).intValue();
		else if (iValue instanceof String)
			return Float.valueOf((String) iValue);

		throw new IllegalArgumentException("Can't convert value " + iValue + " to float for the type: " + name);
	}

	/**
	 * Convert the input object to a double.
	 * 
	 * @param iValue
	 *          Any type supported
	 * @return The double value if the conversion succeed, otherwise the IllegalArgumentException exception
	 */
	public double asDouble(final Object iValue) {
		if (iValue instanceof Number)
			return ((Number) iValue).doubleValue();
		else if (iValue instanceof String)
			return Double.valueOf((String) iValue);

		throw new IllegalArgumentException("Can't convert value " + iValue + " to double for the type: " + name);
	}

	/**
	 * Convert the input object to a string.
	 * 
	 * @param iValue
	 *          Any type supported
	 * @return The string if the conversion succeed, otherwise the IllegalArgumentException exception
	 */
	public String asString(final Object iValue) {
		return iValue.toString();
	}

	public static boolean isSimpleType(final Object iObject) {
		if (iObject == null)
			return false;

		final Class<? extends Object> iType = iObject.getClass();

		if (iType.isPrimitive() || Number.class.isAssignableFrom(iType) || String.class.isAssignableFrom(iType)
				|| Boolean.class.isAssignableFrom(iType) || Date.class.isAssignableFrom(iType)
				|| (iType.isArray() && (iType.equals(byte[].class) || iType.equals(char[].class))))
			return true;
		return false;
	}

	/**
	 * Convert types between numbers based on the iTargetClass parameter.
	 * 
	 * @param iValue
	 *          Value to convert
	 * @param iTargetClass
	 *          Expected class
	 * @return The converted value or the original if no conversion was applied
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object convert(final Object iValue, final Class<?> iTargetClass) {
		if (iValue == null)
			return null;

		if (iValue.getClass().equals(iTargetClass))
			// SAME TYPE: DON'T CONVERT IT
			return iValue;

		if (iTargetClass.isAssignableFrom(iValue.getClass()))
			// COMPATIBLE TYPES: DON'T CONVERT IT
			return iValue;

		try {
			if (iTargetClass.isAssignableFrom(byte[].class)) {
				return OBase64Utils.decode(iValue.toString());
			} else if (iValue.getClass().isAssignableFrom(byte[].class)) {
				return iValue;
			} else if (iTargetClass.isEnum()) {
				if (iValue instanceof Number)
					return ((Class<Enum>) iTargetClass).getEnumConstants()[((Number) iValue).intValue()];
				return Enum.valueOf((Class<Enum>) iTargetClass, iValue.toString());
			} else if (iTargetClass.equals(Byte.TYPE) || iTargetClass.equals(Byte.class)) {
				if (iValue instanceof Byte)
					return iValue;
				else if (iValue instanceof String)
					return Byte.parseByte((String) iValue);
				else
					return ((Number) iValue).byteValue();

			} else if (iTargetClass.equals(Short.TYPE) || iTargetClass.equals(Short.class)) {
				if (iValue instanceof Short)
					return iValue;
				else if (iValue instanceof String)
					return Short.parseShort((String) iValue);
				else
					return ((Number) iValue).shortValue();

			} else if (iTargetClass.equals(Integer.TYPE) || iTargetClass.equals(Integer.class)) {
				if (iValue instanceof Integer)
					return iValue;
				else if (iValue instanceof String)
					return Integer.parseInt((String) iValue);
				else
					return ((Number) iValue).intValue();

			} else if (iTargetClass.equals(Long.TYPE) || iTargetClass.equals(Long.class)) {
				if (iValue instanceof Long)
					return iValue;
				else if (iValue instanceof String)
					return Long.parseLong((String) iValue);
				else
					return ((Number) iValue).longValue();

			} else if (iTargetClass.equals(Float.TYPE) || iTargetClass.equals(Float.class)) {
				if (iValue instanceof Float)
					return iValue;
				else if (iValue instanceof String)
					return Float.parseFloat((String) iValue);
				else
					return ((Number) iValue).floatValue();

			} else if (iTargetClass.equals(Double.TYPE) || iTargetClass.equals(Double.class)) {
				if (iValue instanceof Double)
					return iValue;
				else if (iValue instanceof String)
					return Double.parseDouble((String) iValue);
				else
					return ((Number) iValue).doubleValue();

			} else if (iTargetClass.equals(Boolean.TYPE) || iTargetClass.equals(Boolean.class)) {
				if (iValue instanceof Boolean)
					return ((Boolean) iValue).booleanValue();
				else if (iValue instanceof String)
					return Boolean.parseBoolean((String) iValue);
				else if (iValue instanceof Number)
					return ((Number) iValue).intValue() != 0;

			} else if (iValue instanceof Collection<?> && Set.class.isAssignableFrom(iTargetClass)) {
				final Set<Object> set = new HashSet<Object>();
				set.addAll((Collection<? extends Object>) iValue);
				return set;

			} else if (iTargetClass.equals(Date.class) && iValue instanceof Number) {
				return new Date(((Number) iValue).longValue());
			}
		} catch (Exception e) {
			OLogManager.instance().debug(OType.class, "Error in conversion of value '%s' to type '%s'", iValue, iTargetClass);
		}

		return null;
	}

	public Class<?> getDefaultJavaType() {
		return javaTypes.length > 0 ? javaTypes[0] : null;
	}

	public Class<?>[] getJavaTypes() {
		return javaTypes;
	}
}
