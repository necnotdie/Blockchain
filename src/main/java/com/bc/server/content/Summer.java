package com.bc.server.content;

import com.bc.annotation.RequestMapping;
import com.bc.annotation.RestController;
import com.bc.block.BlockAction;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;


public final class Summer {
    private static Summer summer;
    private List<String> classList = new ArrayList<String>();
    private Map<String, SummerEntry> SummerObject = new HashMap<String, SummerEntry>();

    private Summer() {
        GetResource();
        for (String className : classList) {
            Class<?> cls;
            try {
                System.out.println(className);
                cls = Class.forName(className, false, Summer.class.getClassLoader());
                if (cls.isAnnotationPresent(RestController.class)) {
                    Object object = cls.newInstance();
                    Method[] methods = cls.getDeclaredMethods();
                    for (Method method : methods) {
                        if (method.isAnnotationPresent(RequestMapping.class)) {
                            if (!method.isAccessible()) {
                                method.setAccessible(true);
                            }
                            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                            String value = requestMapping.value();
                            String produces = requestMapping.produces();
                            SummerObject.put(value, new SummerEntry(produces, method, object));
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    public static synchronized Summer newinstance() {
        if (summer == null) {
            summer = new Summer();
        }
        return summer;
    }

    private void GetResource() {
        URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        URL[] urls = classLoader.getURLs();
        URL libURL = null;
        try {
            libURL = new File(System.getProperty("user.dir") + "/lib").toURI().toURL();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (URL url : urls) {
            File file;
            String pack;
            if (!url.getPath().contains(libURL.getPath())) {
                try {
                    file = new File(url.toURI());
                    pack = file.toURI().toURL().getPath();
                    GetResource(file, pack, classList);
                } catch (Throwable e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private void GetResource(File file, String pack, List<String> list) throws Exception {
        if (file.isFile()) {
//            if (file.getName().endsWith(".jar")) {
//                JarFile jarFile = new JarFile(file);
//                Enumeration<JarEntry> enumeration = jarFile.entries();
//                while (enumeration.hasMoreElements()) {
//                    JarEntry jarEntry = (JarEntry) enumeration.nextElement();
//                    if (jarEntry.getName().endsWith(".class")) {
//                        list.add(jarEntry.getName().replaceAll("/", ".").replaceAll(".class", ""));
//                    }
//                }
//            } else
            if (file.getName().endsWith(".class")) {
                list.add(file.toURI().toURL().getPath().replaceAll(pack, "").replaceAll("/", ".").replaceAll(".class", ""));
            }
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File item : files) {
                GetResource(item, pack, list);
            }
        }
    }

    public static SummerEntry GetResponse(String value) {
        return summer.SummerObject.get(value);
    }

    public class SummerEntry {
        private String produces;
        private Method method;
        private Object object;

        public String getProduces() {
            return this.produces;
        }

        public Method getMethod() {
            return method;
        }

        public Object getObject() {
            return object;
        }

        public SummerEntry(String produces, Method method, Object object) {
            this.produces = produces;
            this.method = method;
            this.object = object;
        }
    }
}