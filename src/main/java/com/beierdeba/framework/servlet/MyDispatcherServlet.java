package com.beierdeba.framework.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.beierdeba.framework.annotation.MyAutowired;
import com.beierdeba.framework.annotation.MyController;
import com.beierdeba.framework.annotation.MyRequestMapping;
import com.beierdeba.framework.annotation.MyRequestParam;
import com.beierdeba.framework.annotation.MyService;

/**
 * @author Administrator
 * @since 2020/9/3 10:32
 */
public class MyDispatcherServlet extends HttpServlet {

    private final static String CONTEXT_CONFIG_LOCATION = "config";
    private final static String SCAN_PACKAGE_PROPERTY = "scan-package";

    private Properties contextConfig = new Properties();

    private List<String> classNameList = new ArrayList<>();

    private Map<String, Object> iocMap = new ConcurrentHashMap<>();

    private Map<String, Method> handlerMapping = new ConcurrentHashMap<>();

    private Map<String, Object> handlerController = new ConcurrentHashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception Detail:\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1、加载配置文件
        myLoadConfig(config.getInitParameter(CONTEXT_CONFIG_LOCATION));
        // 2、扫描相关的类
        myScanner(contextConfig.getProperty(SCAN_PACKAGE_PROPERTY));
        // 3、初始化 IOC 容器
        myInstance();
        // 4、依赖注入
        myAutowired();
        // 5、初始化 HandlerMapping
        myHandlerMapping();

        doTestPrintData();
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        if (handlerMapping.isEmpty()) {
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Content-Type", "text/html;charset=UTF-8");
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().write("404 页面未找到");
            return;
        }

        String url = req.getRequestURI();

        System.out.println("[doDispatch] getRequestURI:" + url);

        String contextPath = req.getContextPath();

        System.out.println("[doDispatch] getContextPath:" + url);

        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");

        System.out.println("[doDispatch] request url-->" + url);

        if (!this.handlerMapping.containsKey(url)) {
            try {
                resp.setCharacterEncoding("UTF-8");
                resp.setHeader("Content-Type", "text/html;charset=UTF-8");
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().write("404 NOT FOUND!!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Method method = this.handlerMapping.get(url);

        System.out.println("[doDispatch] method-->" + method);

        Object[] args = hand(req, resp, method);

        method.invoke(handlerController.get(url), args);

        System.out.println("[doDispatch] method.invoke put {" + args + "}.");
    }

    private static Object[] hand(HttpServletRequest req, HttpServletResponse resp, Method method) {

        Class<?>[] parameterTypes = method.getParameterTypes();

        Object[] args = new Object[parameterTypes.length];

        int argsCount = 0;
        int index = 0;
        for (Class<?> parameterType : parameterTypes) {

            String simpleName = parameterType.getSimpleName();
            System.out.println("[hand] parameterType.getSimpleName:" + simpleName);

            if (ServletRequest.class.isAssignableFrom(parameterType)) {
                args[argsCount++] = req;
            } else if (ServletResponse.class.isAssignableFrom(parameterType)) {
                args[argsCount++] = resp;
            } else {
                Annotation[] annotation = method.getParameterAnnotations()[index];
                if (annotation.length > 0) {
                    for (Annotation annotation1 : annotation) {
                        if (MyRequestParam.class.isAssignableFrom(annotation1.getClass())) {
                            MyRequestParam requestParam = (MyRequestParam)annotation1;
                            System.out.println("[hand] requestParam.value:" + requestParam.value());
                            if ("String".equals(simpleName)) {
                                args[argsCount++] = req.getParameter(requestParam.value());
                            } else if ("Integer".equals(simpleName)) {
                                args[argsCount++] = Integer.valueOf(req.getParameter(requestParam.value()));
                            }
                        }
                    }
                }
            }
            index++;

        }
        return args;
    }

    private void doTestPrintData() {

        System.out.println("contextConfig.propertyNames()-->" + contextConfig.propertyNames());

        System.out.println("[classNameList]-->");
        for (String str : classNameList) {
            System.out.println(str);
        }

        System.out.println("[iocMap]-->");
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            System.out.println(entry);
        }

        System.out.println("[handlerMapping]-->");
        for (Map.Entry<String, Method> entry : handlerMapping.entrySet()) {
            System.out.println(entry);
        }

        System.out.println("[handlerController]-->");
        for (Map.Entry<String, Object> entry : handlerController.entrySet()) {
            System.out.println(entry);
        }

        System.out.println("====启动成功====");

    }

    private void myHandlerMapping() {

        if (iocMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }
            MyController myController = clazz.getAnnotation(MyController.class);
            String beanName = myController.value();

            String baseUrl = "";

            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping myRequestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = myRequestMapping.value();
            }

            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }

                MyRequestMapping myRequestMapping = method.getAnnotation(MyRequestMapping.class);

                String url = ("/" + baseUrl + "/" + myRequestMapping.value()).replaceAll("/+", "/");

                try {

                    handlerMapping.put(url, method);
                    System.out.println("[INIT-myHandlerMapping] handlerMapping put {" + url + "} - {" + method + "}.");

                    Object obj;
                    if ("".equals(beanName)) {
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }
                    if (iocMap.containsKey(beanName)) {
                        obj = iocMap.get(beanName);
                    } else {
                        obj = clazz.newInstance();
                    }
                    handlerController.put(url, obj);
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void myAutowired() {
        if (iocMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {

            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }

                System.out.println("[INIT-MyAutowired] Existence MyAutowired.");

                // 获取注解对应的类
                MyAutowired myAutowired = field.getAnnotation(MyAutowired.class);
                String beanName = myAutowired.value().trim();

                if ("".equals(beanName)) {
                    System.out.println("[INIT-MyAutowired] MyAutowired.value() is null");
                    beanName = field.getType().getName();
                    System.out.println("[INIT-MyAutowired] MyAutowired beanName is " + field.getType().getName());
                }

                field.setAccessible(true);

                try {
                    field.set(entry.getValue(), iocMap.get(beanName));

                    System.out.println(
                        "[INIT-MyAutowired] field set {" + entry.getValue() + "} - {" + iocMap.get(beanName) + "}.");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void myInstance() {

        if (classNameList.isEmpty()) {
            return;
        }

        try {

            for (String className : classNameList) {

                Class<?> clazz = Class.forName(className);
                String beanName;

                if (clazz.isAnnotationPresent(MyController.class)) {

                    MyController myController = clazz.getAnnotation(MyController.class);
                    beanName = myController.value();
                    if ("".equals(myController.value())) {
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();

                    iocMap.put(beanName, instance);
                    System.out.println("[INIT-myInstance] {" + beanName + "} has been saved in iocMap.");

                } else if (clazz.isAnnotationPresent(MyService.class)) {

                    MyService myService = clazz.getAnnotation(MyService.class);
                    beanName = myService.value();
                    if ("".equals(myService.value())) {
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }

                    Object instance = clazz.newInstance();
                    iocMap.put(beanName, instance);
                    System.out.println("[INIT-myInstance] {" + beanName + "} has been saved in iocMap.");

                    for (Class<?> i : clazz.getInterfaces()) {
                        if (iocMap.containsKey(i.getName())) {
                            throw new Exception("The Bean Name Is Exist.");
                        }

                        iocMap.put(i.getName(), instance);
                        System.out
                            .println("[INIT-myInstance] {" + i.getName() + "} interface has been saved in iocMap.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void myLoadConfig(String location) {

        // location = application.properties
        System.out.println("[INIT-myLoadConfig] location:" + location);

        // file:/D:/workplace/spring-study/target/classes/ + application.properties
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(location);

        try {
            contextConfig.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void myScanner(String scanPackage) {

        // scanPackage = com.beierdeba.demo
        System.out.println("[INIT-myScanner] scanPackage:" + scanPackage);

        // file:/D:/workplace/spring-study/target/classes/com/beierdeba/demo/
        URL resourcePath = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));

        if (resourcePath == null) {
            return;
        }

        System.out.println("[INIT-myScanner] resourcePath:" + resourcePath.toString());

        File classPath = new File(resourcePath.getFile());

        File[] files = classPath.listFiles();

        if (null == files || files.length == 0) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println("[INIT-myScanner] {" + file.getName() + "} is a directory.");
                myScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    System.out.println("[INIT-myScanner] {" + file.getName() + "} is not a class file.");
                    continue;
                }
                String className = (scanPackage + "." + file.getName()).replace(".class", "");
                classNameList.add(className);
                System.out.println("[INIT-myScanner] {" + className + "} has been saved in classNameList.");
            }
        }
    }

    // 利用ASCII码的差值
    private String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
}
