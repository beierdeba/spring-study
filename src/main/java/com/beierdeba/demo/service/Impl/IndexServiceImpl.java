package com.beierdeba.demo.service.Impl;

import com.beierdeba.demo.service.IndexService;
import com.beierdeba.framework.annotation.MyService;

/**
 * @author Administrator
 * @since 2020/9/3 15:21
 */
@MyService
public class IndexServiceImpl implements IndexService {

    @Override
    public String get(String name) {
        if (name == null || name.length() == 0) {
            return "nothing";
        }
        return String.format("my name is %s.", name);
    }

    @Override
    public String get(String name, Integer age) {
        if (name == null || name.length() == 0) {
            return "nothing";
        }
        if (age == null) {
            return "nothing";
        }
        return String.format("my name is %s., age is %d", name, age);
    }
}
