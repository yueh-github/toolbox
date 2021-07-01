package io.best.tool.boot;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class Task {

    // 当前任务执行结束1秒后开启另一个任务
    @Scheduled(fixedDelay = 1000)
    public void fixedDelay() {
        System.out.println("fixedDelay："+new Date());
    }

    // 当前任务开始执行2秒后开启另一个定时任务
    @Scheduled(fixedRate = 2000)
    public void fixedRate() {
        System.out.println("fixedRate："+new Date());
    }

    // 当前任务开始执行2秒后开启另一个定时任务，并且首次延迟1秒执行
    @Scheduled(initialDelay = 1000, fixedRate = 2000)
    public void initialDelay() {
        System.out.println("initialDelay："+new Date());
    }

    // 使用crom 表达式，下面表示定时任务每分钟执行一次
    @Scheduled(cron = "0 * * * * ?")
    public void cron() {
        System.out.println("cron："+new Date());
    }
}
