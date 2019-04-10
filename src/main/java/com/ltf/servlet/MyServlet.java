package com.ltf.servlet;

import com.ltf.annotaiton.*;
import com.ltf.strategy.ConvertStragegy;
import org.springframework.util.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MyServlet extends HttpServlet {

    /** 保存配置文件键值对 */
    private Properties properties = new Properties();
    /** 存储扫描的包的类*/
    private List<String> beanNames = new ArrayList<String>();
    /** 存储初始好了的bean对象  实则就是ioc*/
    private Map<String,Object> beans = new ConcurrentHashMap<String, Object>();
    private List<HanderMapping> handerMappings = new ArrayList<HanderMapping>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        doHander(req,resp);
    }

    @Override
    public void init(ServletConfig config) {
        // 加载配置
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        // 扫描相关类
        doScanner(properties.getProperty("scanPackage"));
        // 初始化bean
        doInstanceBean();
        // 依赖注入
        doAutowired();
        // 初始化MappingHandler
        doInitMappingHandler();
    }

    private void doInitMappingHandler() {
        if (beans.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object instance = entry.getValue();
            Class<?> aclass = instance.getClass();
            if (!aclass.isAnnotationPresent(MyController.class)) {
                continue;
            }
            String urlPrefix = "";
            if (aclass.isAnnotationPresent(MyMapping.class)) {
                urlPrefix = aclass.getAnnotation(MyMapping.class).value();
            }
            Method[] methods = aclass.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(MyMapping.class)) {
                    continue;
                }
                String url = method.getAnnotation(MyMapping.class).value();
                // 判断接口地址是否重复
                url = urlPrefix+url;
                checkUrlRepeat(url);
                handerMappings.add(new HanderMapping(url,instance,method));
            }
        }
    }

    private void checkUrlRepeat(String url) {
        for (HanderMapping handerMapping : handerMappings) {
            if (handerMapping.getUrl().equals(url)) {
                throw new RuntimeException("found the same controller url mapping");
            }
        }
    }

    /**
     * 依赖注入
     */
    private void doAutowired() {
        if (beans.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object instance = entry.getValue();
            Field[] declaredFields = instance.getClass().getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (declaredField.isAnnotationPresent(MyAutowried.class)) {
                    // 有MyAutowried注解的才注入值
                    MyAutowried myAutowried = declaredField.getAnnotation(MyAutowried.class);
                    if (StringUtils.isEmpty(myAutowried.value().trim())) {
                        Class<?> type = declaredField.getType();
                        Object o;
                        if (type.isInterface()) {
                            // 如果注入的是接口，则用类型去获取
                             o = beans.get(type.getName());
                        } else {
                             o = beans.get(firstToLower(type.getSimpleName()));
                        }
                        declaredField.setAccessible(true);
                        try {
                            declaredField.set(instance, o);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }

    }

    /**
     * 初始化beanNames中的类
     */
    private void doInstanceBean() {
        if (beanNames.isEmpty()) {
            return;
        }
        for (String beanName : beanNames) {
            try {
                Class<?> aClass = Class.forName(beanName);
                if (aClass.isAnnotationPresent(MyController.class)) {
                    MyController myController = aClass.getAnnotation(MyController.class);
                    String value = myController.value().trim();
                    String name;
                    if (StringUtils.isEmpty(value)) {
                        name = firstToLower(aClass.getSimpleName());
                    } else {
                        name = value;
                    }
                    beans.put(name,aClass.newInstance());
                } else if (aClass.isAnnotationPresent(MyService.class)) {
                    MyService myService = aClass.getAnnotation(MyService.class);
                    String value = myService.value().trim();
                    String name;
                    if (StringUtils.isEmpty(value)) {
                        name = firstToLower(aClass.getSimpleName());
                    } else {
                        name = value;
                    }
                    Object o = aClass.newInstance();
                    beans.put(name,o);
                    for (Class<?> anInterface : aClass.getInterfaces()) {
                        if (beans.containsKey(anInterface.getName())) {
                            throw new RuntimeException("find many type from beans");
                        }
                        beans.put(anInterface.getName(),o);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 名称首字母小写
     */
    private String firstToLower(String simpleName) {
        return simpleName.substring(0, 1).toLowerCase()+simpleName.substring(1);
    }

    /**
     * 扫描相关类
     * @param scanPackage 扫描的包路径
     */
    private void doScanner(String scanPackage) {
        // 将包.替换成文件路径/
        String path = scanPackage.replaceAll("\\.","/");
        String filePath = this.getClass().getResource("/" + path).getFile();
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("doScanner:not find file");
        }
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                doScanner(scanPackage+"."+f.getName());
            } else {
                if (f.getName().endsWith(".class")) {
                    beanNames.add(scanPackage+"."+f.getName().replace(".class",""));
                }
            }
        }

    }

    /**
     * 加载配置文件
     * @param path 配置文件路径
     */
    private void doLoadConfig(String path) {
        if (path == null || "".equals(path.trim())) {
            throw new IllegalArgumentException("not found init param config path");
        }
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(path);
        try {
            properties.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doHander(HttpServletRequest req, HttpServletResponse resp) {
        String requestURI = req.getRequestURI();
        HanderMapping handerMapping = getHandlerMapping(requestURI);
        if (handerMapping == null) {
            try {
                resp.getWriter().write("path not found");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Class<?>[] parameterTypes = handerMapping.parameterTypes;
        Object[] params = new Object[parameterTypes.length];
        // 获取请求参数
        Map<String,String[]> parameterMap = req.getParameterMap();
        for (Map.Entry<String,Integer> entry : handerMapping.map.entrySet()) {
            Object key = entry.getKey();
            if (parameterMap.containsKey(key)) {
                String[] o = parameterMap.get(key);
                String replace = Arrays.toString(o).replace("[", "").replace("]", "");
                params[entry.getValue()] = convertType(parameterTypes[entry.getValue()], replace);
            }
        }
        Method method = handerMapping.getMethod();
        try {
            Object invoke = method.invoke(handerMapping.controller, params);
            resp.getWriter().write((String) invoke);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 转换类型
     */
    private Object convertType(Class<?> parameterType, String o) {
        // 使用策略模式
       return ConvertStragegy.covertType(parameterType,o);
    }

    private HanderMapping getHandlerMapping(String requestURI) {
        if (StringUtils.isEmpty(requestURI)) {
            throw new RuntimeException("path not fond");
        }
        for (HanderMapping handerMapping : handerMappings) {
            if (handerMapping.getUrl().equals(requestURI)) {
                return handerMapping;
            }
        }
        return null;
    }

    private class HanderMapping{
        private String url;
        private Method method;
        private Object controller;
        /** 存储方法的参数类型 */
        private Class<?>[] parameterTypes;

        private Map<String,Integer> map;

        public HanderMapping(String url, Object controller,Method method) {
            this.url = url;
            this.method = method;
            this.controller = controller;
            parameterTypes = method.getParameterTypes();
            map = new HashMap<String, Integer>();
            putParamterNameAndIndex(method);
        }


        private void putParamterNameAndIndex(Method method) {
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                Annotation[] parameterAnnotation = parameterAnnotations[i];
                for (int j = 0; j < parameterAnnotation.length; j++) {
                    if (parameterAnnotation[j] instanceof MyRequestParam) {
                        map.put(((MyRequestParam)parameterAnnotation[j]).value(),i);
                    }
                }
            }

            // HttpServletRequest HttpServletResponse 也需要提取
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i< parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                if (parameterType == HttpServletRequest.class ||
                        parameterType == HttpServletResponse.class) {
                    map.put(parameterType.getName(),i);
                }
            }
        }

        /**
         * Gets the value of url.
         *
         * @return the value of url
         */
        public String getUrl() {
            return url;
        }

        /**
         * Sets the url.
         *
         * @param url url
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * Gets the value of method.
         *
         * @return the value of method
         */
        public Method getMethod() {
            return method;
        }

        /**
         * Sets the method.
         *
         * @param method method
         */
        public void setMethod(Method method) {
            this.method = method;
        }
    }
}
