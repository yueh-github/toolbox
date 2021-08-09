package io.best.tool.design.factorymode;

public class ChinaCoffeeFactory extends CoffeeFactory{
    @Override
    public Coffer[] createCoffer() {
        return new Coffer[]{new Latte(),new Cappuccino()};
    }
}
