package com.bc.server.core;

import java.util.Collection;
import java.util.Map;

public final class ClassTypeUtil {
    private static boolean isBaseType(Class aClass) {
        if (String.class.isAssignableFrom(aClass)) {
            return true;
        } else if (Byte.class.isAssignableFrom(aClass) || byte.class.isAssignableFrom(aClass)) {
            return true;
        } else if (Short.class.isAssignableFrom(aClass) || short.class.isAssignableFrom(aClass)) {
            return true;
        } else if (Integer.class.isAssignableFrom(aClass) || int.class.isAssignableFrom(aClass)) {
            return true;
        } else if (Character.class.isAssignableFrom(aClass) || char.class.isAssignableFrom(aClass)) {
            return true;
        } else if (Long.class.isAssignableFrom(aClass) || long.class.isAssignableFrom(aClass)) {
            return true;
        } else if (Boolean.class.isAssignableFrom(aClass) || boolean.class.isAssignableFrom(aClass)) {
            return true;
        } else if (Float.class.isAssignableFrom(aClass) || float.class.isAssignableFrom(aClass)) {
            return true;
        } else if (Double.class.isAssignableFrom(aClass) || double.class.isAssignableFrom(aClass)) {
            return true;
        }
        return false;
    }

    public static boolean isMap(Class aClass) {
        if (Map.class.isAssignableFrom(aClass)) {
            return true;
        }
        return false;
    }

    public static boolean isCollection(Class aClass) {
        if (Collection.class.isAssignableFrom(aClass)) {
            return true;
        }
        return false;
    }

    public static boolean isArray(Class aClass) {
        if (aClass.isArray()) {
            return true;
        }
        return false;
    }

    public static ClassType getType(Class aClass) {
        if (isBaseType(aClass)) {
            return ClassType.BASETYPE;
        } else if (isMap(aClass)) {
            return ClassType.MAP;
        } else if (isCollection(aClass)) {
            return ClassType.COLLECTION;
        } else if (isArray(aClass)) {
            return ClassType.ARRAY;
        } else {
            return ClassType.CLASS;
        }
    }
    public static ClassType getObjectType(Object object) {
        if (isBaseType(object.getClass())) {
            return ClassType.BASETYPE;
        } else if (isMap(object.getClass())) {
            return ClassType.MAP;
        } else if (isCollection(object.getClass())) {
            return ClassType.COLLECTION;
        } else if (isArray(object.getClass())) {
            return ClassType.ARRAY;
        } else {
            return ClassType.CLASS;
        }
    }
}
