package com.beierdeba.demo.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.beierdeba.demo.service.IndexService;
import com.beierdeba.framework.annotation.MyAutowired;
import com.beierdeba.framework.annotation.MyController;
import com.beierdeba.framework.annotation.MyRequestMapping;
import com.beierdeba.framework.annotation.MyRequestParam;

/**
 * @author Administrator
 * @since 2020/9/3 15:18
 */
@MyController
@MyRequestMapping("/index")
public class IndexController {

    @MyAutowired
    private IndexService indexService;

    @MyRequestMapping("/arg1")
    public void arg1(HttpServletRequest request, HttpServletResponse response, //
        @MyRequestParam("name") String name) {
        try {
            response.getWriter().write(indexService.get(name));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @MyRequestMapping("/arg2")
    public void arg2(HttpServletRequest request, HttpServletResponse response, //
        @MyRequestParam("name") String name, @MyRequestParam("age") Integer age) {
        try {
            response.getWriter().write(indexService.get(name, age));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @MyRequestMapping("/arg3")
    public void arg3(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.getWriter().write("success!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
