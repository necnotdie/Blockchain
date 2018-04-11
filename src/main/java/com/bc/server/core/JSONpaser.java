package com.bc.server.core;

import com.bc.server.exception.JSONparseException;
import com.bc.server.exception.ParseExcetion;
import net.sf.ezmorph.bean.MorphDynaBean;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.lang.reflect.*;
import java.util.*;

public class JSONpaser {
    public static Object jsonToBean(String arg, BlockThread.EntryMethod entryMethod) throws ParseExcetion, InstantiationException, JSONparseException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.COLLECTION) {
            Collection collection = JSONArray.toCollection(JSONArray.fromObject(arg));
            return formateBean(collection, entryMethod);
        } else if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.MAP) {
            MorphDynaBean morphDynaBean = (MorphDynaBean) JSONObject.toBean(JSONObject.fromObject(arg));
            return formateBean(morphDynaBean, entryMethod);
        } else if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.ARRAY) {
            Object object = JSONArray.toArray(JSONArray.fromObject(arg));
            return formateBean(object, entryMethod);
        } else if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.CLASS) {
            MorphDynaBean morphDynaBean = (MorphDynaBean) JSONObject.toBean(JSONObject.fromObject(arg));
            return formateBean(morphDynaBean, entryMethod);
        } else if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.BASETYPE) {
            return parse(entryMethod.getTypeClass(), arg);
        }
        return null;
    }

    private static Object formateBean(Object object, BlockThread.EntryMethod entryMethod) throws IllegalAccessException, ParseExcetion, JSONparseException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.COLLECTION && object instanceof Collection) {
            Collection collection = (Collection) object;
            Collection collectionClone = (Collection) object.getClass().newInstance();
            BlockThread.Gereric gereric = entryMethod.getGereric();
            List<BlockThread.Gereric> gererics = gereric.getGerericList();
            for (Object item : collection) {
                BlockThread.EntryMethod itemEntryMethod = new BlockThread.EntryMethod(gererics.get(0).getGerericClass(), null, gererics.get(0));
                collectionClone.add(formateBean(item, itemEntryMethod));
            }
            return collectionClone;
        } else if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.MAP && object instanceof MorphDynaBean) {
            MorphDynaBean morphDynaBean = (MorphDynaBean) object;
            Map<String, Object> map = null;
            if (entryMethod.getTypeClass().isInterface()) {
                map = new HashMap<String, Object>();
            } else {
                map = (Map<String, Object>) entryMethod.getTypeClass().newInstance();
            }
            Map<String, Object> objectMap = getJsonBean(morphDynaBean);
            Set<Map.Entry<String, Object>> entrySet = objectMap.entrySet();
            List<BlockThread.Gereric> gererics = entryMethod.getGereric().getGerericList();
            for (Map.Entry<String, Object> kvEntry : entrySet) {
                map.put(kvEntry.getKey(), formateBean(kvEntry.getValue(), new BlockThread.EntryMethod(gererics.get(1).getGerericClass(), null, gererics.get(1))));
            }
            return map;
        } else if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.ARRAY && object.getClass().isArray()) {
            int length = Array.getLength(object);
            Class componentType = entryMethod.getTypeClass().getComponentType();
            Object result = Array.newInstance(componentType, length);
            for (int i = 0; i < length; i++) {
                Array.set(result, i, formateBean(Array.get(object, i), new BlockThread.EntryMethod(componentType, null, entryMethod.getGereric())));
            }
            return result;
        } else if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.CLASS && object instanceof MorphDynaBean) {
            MorphDynaBean morphDynaBean = (MorphDynaBean) object;
            Object methodObject = entryMethod.getTypeClass().newInstance();
            Field[] fields = entryMethod.getTypeClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                BlockThread.EntryMethod itemEntryMethod = BlockThread.getFieldAsm(field);
                field.set(methodObject, formateBean(morphDynaBean.get(field.getName()), itemEntryMethod));
            }
            return methodObject;
        } else if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.BASETYPE && ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.BASETYPE) {
            return parse(entryMethod.getTypeClass(), object);
        } else if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.CLASS && ClassTypeUtil.getObjectType(object) == ClassType.BASETYPE) {
            if (entryMethod.getTypeClass().isAssignableFrom(object.getClass())) {
                return object;
            } else {
                Constructor<?>[] constructors = entryMethod.getTypeClass().getConstructors();
                for (Constructor<?> constructor : constructors) {
                    Class[] types = constructor.getParameterTypes();
                    if (types.length == 1 && ClassTypeUtil.getType(types[0]) == ClassType.BASETYPE) {
                        Object trueObject = null;
                        try {
                            trueObject = parse(types[0], object);
                        } catch (Exception e) {
                            continue;
                        }
                        return constructor.newInstance(trueObject);
                    }
                }
                return null;
            }
        } else if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.CLASS && !(object instanceof MorphDynaBean) && entryMethod.getTypeClass().isAssignableFrom(object.getClass())) {
            return object;
        } else {
            throw new JSONparseException("JSON转bean失败！");
        }
    }

    private static Map<String, Object> getJsonBean(MorphDynaBean morphDynaBean) {
        Map<String, Object> beanmap = null;
        try {
            Field field = MorphDynaBean.class.getDeclaredField("dynaValues");
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            beanmap = (Map<String, Object>) field.get(morphDynaBean);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return beanmap;
    }

    private static Object parse(Class typeClass, Object source) throws ParseExcetion {
        String sourceStr = source.toString();
        if (String.class.isAssignableFrom(typeClass)) {
            return sourceStr;
        } else if (Byte.class.isAssignableFrom(typeClass) || byte.class.isAssignableFrom(typeClass)) {
            return Byte.parseByte(sourceStr);
        } else if (Short.class.isAssignableFrom(typeClass) || short.class.isAssignableFrom(typeClass)) {
            return Short.parseShort(sourceStr);
        } else if (Integer.class.isAssignableFrom(typeClass) || int.class.isAssignableFrom(typeClass)) {
            return Integer.parseInt(sourceStr);
        } else if (Character.class.isAssignableFrom(typeClass) || char.class.isAssignableFrom(typeClass)) {
            char[] chars = sourceStr.toCharArray();
            return chars[0];
        } else if (Long.class.isAssignableFrom(typeClass) || long.class.isAssignableFrom(typeClass)) {
            return Long.parseLong(sourceStr);
        } else if (Boolean.class.isAssignableFrom(typeClass) || boolean.class.isAssignableFrom(typeClass)) {
            return Boolean.parseBoolean(sourceStr);
        } else if (Float.class.isAssignableFrom(typeClass) || float.class.isAssignableFrom(typeClass)) {
            return Float.parseFloat(sourceStr);
        } else if (Double.class.isAssignableFrom(typeClass) || double.class.isAssignableFrom(typeClass)) {
            return Double.parseDouble(sourceStr);
        } else {
            throw new ParseExcetion("参数：" + sourceStr + "，强制转化为" + typeClass.getName() + "类型出错！");
        }
    }
}
