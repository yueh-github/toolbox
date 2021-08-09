package io.best.tool.design.factorymode;

public class Test {

    /**
     * 简单工厂实际不能算作一种设计模式，它引入了创建者的概念，将实例化的代码从应用代码中抽离，在创建者类的静态方法中只处理创建对象的细节，后续创建的实例如需改变，只需改造创建者类即可，
     * <p>
     * 但由于使用静态方法来获取对象，使其不能在运行期间通过不同方式去动态改变创建行为，因此存在一定局限性
     *
     * @param args
     */
    public static void main(String[] args) {
        Coffer latte = SimpleFactory.createInstance("latte");
        System.out.println(latte.getName());

        Coffer cappuccino = SimpleFactory.createInstance("cappuccino");
        System.out.println(cappuccino.getName());
    }
}
