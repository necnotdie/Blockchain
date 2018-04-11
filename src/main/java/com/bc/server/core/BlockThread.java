package com.bc.server.core;

import com.bc.server.content.Summer;
import com.bc.server.exception.ParseExcetion;
import com.bc.server.manager.Request;
import com.bc.server.manager.Response;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import org.objectweb.asm.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.*;

/**
 * Created by Administrator on 2018/3/6.
 */
public class BlockThread extends Thread {
    private volatile Socket connection;

    public BlockThread(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        Response response = null;
        try {
            Request request = new Request(connection.getInputStream());
            response = new Response(connection.getOutputStream());
            //如果检测到是HTTP/1.0及以后的协议，按照规范，需要发送一个MIME首部
            if (request.getProtocol() != null && request.getProtocol().indexOf("HTTP") >= 0) {
                Summer.SummerEntry summerEntry = Summer.GetResponse(request.getRequestPath());
                if (summerEntry != null) {
                    String mimeType = summerEntry.getProduces();
                    Method method = summerEntry.getMethod();
                    Object object = summerEntry.getObject();
                    Map<String, EntryMethod> methodTypes = getMethodTypesAsm(method);
                    Set<Map.Entry<String, EntryMethod>> methodEntry = methodTypes.entrySet();
                    Object[] methodArgs = new Object[methodEntry.size()];
                    int index = 0;
                    for (Map.Entry<String, EntryMethod> entry : methodEntry) {
                        Object value = null;
                        String arg = request.getArg(entry.getKey());
                        if (arg != null) {
                            try {
                                try {
                                    value = getValue(arg, entry.getValue());
                                } catch (ParseExcetion parseExcetion) {
                                }
                                if (value == null) {
                                    value = JSONpaser.jsonToBean(arg, entry.getValue());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                value = null;
                            }
                        } else if (arg == null && request.getContentStr() != null) {
                            try {
                                try {
                                    value = getValue(request.getContentStr(), entry.getValue());
                                } catch (ParseExcetion parseExcetion) {
                                }
                                if (value == null) {
                                    value = JSONpaser.jsonToBean(request.getContentStr(), entry.getValue());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                value = null;
                            }
                        }
                        methodArgs[index++] = value;
                    }
                    Object contentObject = method.invoke(object, methodArgs);
                    String content = null;
                    if (ClassTypeUtil.getObjectType(contentObject) == ClassType.BASETYPE) {
                        content = contentObject.toString();
                    } else if (ClassTypeUtil.getObjectType(contentObject) == ClassType.ARRAY || ClassTypeUtil.getObjectType(contentObject) == ClassType.COLLECTION) {
                        content = JSONArray.fromObject(contentObject).toString();
                    } else {
                        content = JSONObject.fromObject(contentObject).toString();
                    }
                    response.write("200", mimeType, content);
                } else {
                    response.write("404", "text/html", "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.write("503", "text/html", "");
            } catch (IOException ioe) {
            }
        }
        finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public static Map<String, EntryMethod> getMethodTypesAsm(final Method method) {
        Map<String, EntryMethod> methodTypes = new LinkedHashMap<String, EntryMethod>();
        final String[] paramNames = new String[method.getParameterTypes().length];
        final String[] descs = new String[method.getParameterTypes().length];
        final String[] signatures = new String[method.getParameterTypes().length];
        final String n = method.getDeclaringClass().getName();
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassReader cr = null;
        try {
            cr = new ClassReader(n);
            cr.accept(new ClassVisitor(Opcodes.ASM4, cw) {
                @Override
                public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
                    final org.objectweb.asm.Type[] args = org.objectweb.asm.Type.getArgumentTypes(desc);
                    if (!name.equals(method.getName()) || !sameType(args, method.getParameterTypes())) {
                        return super.visitMethod(access, name, desc, signature, exceptions);
                    }
                    MethodVisitor v = cv.visitMethod(access, name, desc, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM4, v) {
                        @Override
                        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                            int i = index - 1;
                            if (Modifier.isStatic(method.getModifiers())) {
                                i = index;
                            }
                            if (i >= 0 && i < paramNames.length) {
                                paramNames[i] = name;
                                descs[i] = desc;
                                signatures[i] = signature;
//                                System.out.println("name=" + name);
//                                System.out.println("desc=" + desc);
//                                System.out.println("signature=" + signature);
                            }
                            super.visitLocalVariable(name, desc, signature, start, end, index);
                        }
                    };
                }
            }, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Class[] classes = method.getParameterTypes();
        Type[] types = method.getGenericParameterTypes();
        for (int i = 0; i < paramNames.length; i++) {
            methodTypes.put(paramNames[i], new EntryMethod(classes[i], types[i], new Gereric(signatures[i] == null ? descs[i] : signatures[i])));
        }
        return methodTypes;
    }

    public static EntryMethod getFieldAsm(final Field field) {
        final String n = field.getDeclaringClass().getName();
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassReader cr = null;
        final Map<String, String> map = new HashMap<String, String>();
        try {
            cr = new ClassReader(n);
            cr.accept(new ClassVisitor(Opcodes.ASM4, cw) {
                @Override
                public FieldVisitor visitField(int i, String s, String s1, String s2, Object o) {
                    if (field.getName().equals(s)) {
                        map.put("desc", s1);
                        map.put("signature", s2);
//                        System.out.println(i);
//                        System.out.println("s=" + s);
//                        System.out.println("s1=" + s1);
//                        System.out.println("s2=" + s2);
//                        System.out.println("o=" + o);
                    }
                    return super.visitField(i, s, s1, s2, o);
                }
            }, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(field.getType());
        return new EntryMethod(field.getType(), field.getGenericType(), new Gereric(map.get("signature") == null ? map.get("desc") : map.get("signature")));
    }

    private static boolean sameType(org.objectweb.asm.Type[] types, Class<?>[] clazzes) {
        if (types.length != clazzes.length) {
            return false;
        }

        for (int i = 0; i < types.length; i++) {
            if (!org.objectweb.asm.Type.getType(clazzes[i]).equals(types[i])) {
                return false;
            }
        }
        return true;
    }

   /* @Deprecated
    private Map<String, EntryMethod> getMethodTypesJavassit(Method method) {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc;
        Map<String, EntryMethod> methodTypes = new LinkedHashMap<String, EntryMethod>();
        try {
            cc = pool.get(method.getDeclaringClass().getName());
            CtMethod cm = cc.getDeclaredMethod(method.getName());
            MethodInfo info = cm.getMethodInfo();
            CodeAttribute codeAttribute = info.getCodeAttribute();
            LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
            int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
//            String[] names = new String[cm.getParameterTypes().length];
            Class[] classes = method.getParameterTypes();
            Type[] types = method.getGenericParameterTypes();
            for (int i = 0; i < attr.tableLength(); i++) {
                System.out.println(attr.variableName(i));
            }
            for (int i = 0; i < cm.getParameterTypes().length; i++) {
                methodTypes.put(attr.variableName(i + pos), new EntryMethod(classes[i], types[i]));
//                System.out.println(attr.variableName(i + pos));
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return methodTypes;
    }*/

    private Object getValue(String arg, EntryMethod entryMethod) throws ParseExcetion {
        Object value = null;
        try {
            if (String.class.isAssignableFrom(entryMethod.getTypeClass())) {
                value = arg;
            } else if (Byte.class.isAssignableFrom(entryMethod.getTypeClass()) || byte.class.isAssignableFrom(entryMethod.getTypeClass())) {
                value = Byte.parseByte(arg);
            } else if (Short.class.isAssignableFrom(entryMethod.getTypeClass()) || short.class.isAssignableFrom(entryMethod.getTypeClass())) {
                value = Short.parseShort(arg);
            } else if (Integer.class.isAssignableFrom(entryMethod.getTypeClass()) || int.class.isAssignableFrom(entryMethod.getTypeClass())) {
                value = Integer.parseInt(arg);
            } else if (Character.class.isAssignableFrom(entryMethod.getTypeClass()) || char.class.isAssignableFrom(entryMethod.getTypeClass())) {
                char[] chars = arg.toCharArray();
                if (chars.length > 1) {
                    throw new ParseExcetion(arg + "不是char类型！");
                } else {
                    value = chars[0];
                }
            } else if (Long.class.isAssignableFrom(entryMethod.getTypeClass()) || long.class.isAssignableFrom(entryMethod.getTypeClass())) {
                value = Long.parseLong(arg);
            } else if (Boolean.class.isAssignableFrom(entryMethod.getTypeClass()) || boolean.class.isAssignableFrom(entryMethod.getTypeClass())) {
                if ("true".equals(arg) || "false".equals(arg)) {
                    value = Boolean.parseBoolean(arg);
                } else {
                    throw new ParseExcetion(arg + "不是boolean类型！");
                }
            } else if (Float.class.isAssignableFrom(entryMethod.getTypeClass()) || float.class.isAssignableFrom(entryMethod.getTypeClass())) {
                value = Float.parseFloat(arg);
            } else if (Double.class.isAssignableFrom(entryMethod.getTypeClass()) || double.class.isAssignableFrom(entryMethod.getTypeClass())) {
                value = Double.parseDouble(arg);
            }
        } catch (Exception e) {
            System.err.println("参数：" + arg + "，强制转化为" + entryMethod.getTypeClass().getName() + "类型出错！");
            e.printStackTrace();
            throw new ParseExcetion("参数：" + arg + "，强制转化为" + entryMethod.getTypeClass().getName() + "类型出错！");
        }
        return value;
    }

    /**
     * 此方法无法应对复杂的泛型转化
     *
     * @param arg
     * @param entryMethod
     * @return
     */
    @Deprecated
    private Object parseJson(String arg, EntryMethod entryMethod) {
        Object value = null;
        if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.COLLECTION) {
            if (ParameterizedType.class.isAssignableFrom(entryMethod.getType().getClass())) {
                ParameterizedType parameterizedType = (ParameterizedType) entryMethod.getType();
                Class aClass = (Class) parameterizedType.getActualTypeArguments()[0];
                Map<String, Class> map = new HashMap<String, Class>();
                analyzeClass(aClass, map);
                if (map.entrySet().size() > 0) {
                    JsonConfig jsonConfig = new JsonConfig();
                    jsonConfig.setClassMap(map);
                    jsonConfig.setRootClass(aClass);
                    value = entryMethod.getTypeClass().cast(JSONArray.toCollection(JSONArray.fromObject(arg), jsonConfig));
                } else {
                    value = entryMethod.getTypeClass().cast(JSONArray.toCollection(JSONArray.fromObject(arg), aClass));
                }
            }
        } else if (ClassTypeUtil.getType(entryMethod.getTypeClass()) == ClassType.ARRAY) {

        } else {
            value = JSONObject.toBean(JSONObject.fromObject(arg), entryMethod.getTypeClass());
        }
        return value;
    }

    private void analyzeClass(Class rootClass, Map<String, Class> map) {
        Field[] fields = rootClass.getDeclaredFields();
        for (Field field : fields) {
            Class fieldClass = field.getClass();
            if (ClassTypeUtil.getType(fieldClass) == ClassType.COLLECTION || ClassTypeUtil.getType(fieldClass) == ClassType.MAP) {
                Type fieldType = fieldClass.getGenericSuperclass();
                if (fieldType instanceof ParameterizedType) {
                    Type[] types = ((ParameterizedType) fieldType).getActualTypeArguments();
                    for (Type type : types) {
                        Class typeClass = type.getClass();
                        if (ClassTypeUtil.getType(typeClass) == ClassType.CLASS) {
                            map.put(field.getName(), type.getClass());
                            analyzeClass(typeClass, map);
                        } else {

                        }

                    }
                }
            } else if (ClassTypeUtil.getType(fieldClass) == ClassType.ARRAY) {
                fieldClass.getComponentType();
            }
        }
    }

    public static class EntryMethod {
        private Class typeClass;
        private Type type;
        private Gereric gereric;

        public Class getTypeClass() {
            return this.typeClass;
        }

        public Type getType() {
            return this.type;
        }

        public Gereric getGereric() {
            return gereric;
        }

        public EntryMethod(Class typeClass, Type type, Gereric gereric) {
            this.typeClass = typeClass;
            this.type = type;
            this.gereric = gereric;
        }
    }

    public static class Gereric {
        private List<Gereric> gerericList = new ArrayList<Gereric>();
        private Class gerericClass;
        private boolean isArray = false;
        private int arrayCount = 0;

        public boolean isArray() {
            return this.isArray;
        }

        public List<Gereric> getGerericList() {
            return this.gerericList;
        }

        public Class getGerericClass() {
            if (isArray) {
                return Array.newInstance(gerericClass, new int[arrayCount]).getClass();
            } else {
                return gerericClass;
            }
        }

        Gereric(String gerericstr) {
            if (gerericstr != null) {
                char[] chars = gerericstr.toCharArray();
                boolean isClass = false;
                boolean isEnd = false;
                StringBuffer className = new StringBuffer();
                StringBuffer gerericBuffer = new StringBuffer();
                int gerericCount = 0;
                for (char c : chars) {
                    if (!isEnd && gerericCount == 0) {
                        if (isClass) {
                            if (c == '<') {
                                gerericCount++;
                            } else if (c == '>') {
                                gerericCount--;
                            } else if (c == ';') {
                                isEnd = true;
                            } else if (c == '/') {
                                className.append('.');
                            } else {
                                className.append(c);
                            }
                        } else {
                            if (c == '[') {
                                this.isArray = true;
                                arrayCount++;
                            } else if (c == 'L') {
                                isClass = true;
                            } else {
                                className.append(c);
                            }
                        }
                    } else {
                        if (isEnd && gerericCount == 1) {
                            isEnd = false;
                            gerericList.add(new Gereric(gerericBuffer.toString()));
                            gerericBuffer = new StringBuffer();
                        } else {
                            if (c == '<') {
                                gerericCount++;
                            } else if (c == '>') {
                                gerericCount--;
                            } else if (c == ';' && gerericCount == 1) {
                                isEnd = true;
                            }
                            gerericBuffer.append(c);
                        }
                    }
                }
                try {
                    System.out.println("classname==" + className.toString());
                    if ("Z".equals(className.toString())) {
                        gerericClass = boolean.class;
                    } else if ("C".equals(className.toString())) {
                        gerericClass = char.class;
                    } else if ("B".equals(className.toString())) {
                        gerericClass = byte.class;
                    } else if ("S".equals(className.toString())) {
                        gerericClass = short.class;
                    } else if ("I".equals(className.toString())) {
                        gerericClass = int.class;
                    } else if ("F".equals(className.toString())) {
                        gerericClass = float.class;
                    } else if ("J".equals(className.toString())) {
                        gerericClass = long.class;
                    } else if ("D".equals(className.toString())) {
                        gerericClass = double.class;
                    } else {
                        gerericClass = Class.forName(className.toString());
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
