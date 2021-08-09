package io.best.tool.design.factorymode;

public class AmericaCoffeeFactory extends CoffeeFactory{
    @Override
    public Coffer[] createCoffer() {
        return new Coffer[]{new Latte(),new Americano()};
    }
}
