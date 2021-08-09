package io.best.tool.design.factorymode;

public class FactoryMethodTest {

    static void print(Coffer[] c){
        for (Coffer coffer:c){
            System.out.println(coffer.getName());
        }
    }

    public static void main(String[] args) {

        CoffeeFactory coffeeFactory = new ChinaCoffeeFactory();
        print(coffeeFactory.createCoffer());

        CoffeeFactory coffeeFactory1 =  new AmericaCoffeeFactory();
        print(coffeeFactory1.createCoffer());
    }
}
