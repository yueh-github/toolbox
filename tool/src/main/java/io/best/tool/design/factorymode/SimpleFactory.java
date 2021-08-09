package io.best.tool.design.factorymode;

public class SimpleFactory {

    public static Coffer createInstance(String type) {
        if ("americano".equals(type)) {
            return new Americano();
        } else if ("cappuccino".equals(type)) {
            return new Cappuccino();
        } else if ("latte".equals(type)) {
            return new Latte();
        } else {
            throw new RuntimeException("type not find");
        }
    }
}
