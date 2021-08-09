package io.best.tool.design.singleton;

public class SingletonTest {

    public static void main(String[] args) {
        Singleton singleton = Singleton.getInstance();

        System.out.println(singleton);
        Singleton singleton1 = Singleton.getInstance();

        System.out.println(singleton1);
    }
}
