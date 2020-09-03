package com.beierdeba.test;

/**
 * @author Administrator
 * @since 2020/9/3 16:35
 */

public class Test {
    public static void main(String[] args) {
        System.out.println(Test.class.getResource(""));
        System.out.println(Test.class.getResource("/"));
        System.out.println(Test.class.getClassLoader().getResource(""));
        System.out.println(Test.class.getClassLoader().getResource("/"));
        //System.out.println(Test.class.getClassLoader().getResourceAsStream(""));
        //System.out.println(Test.class.getClassLoader().getResourceAsStream("/"));
        //System.out.println(Test.class.getResourceAsStream(""));
        //System.out.println(Test.class.getResourceAsStream("/"));

    }
}
