package com.lwh.pictureproject.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author Lin
 * @Date 2025/10/13 22:27
 * @Descriptions 自定义线程池
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "myAsyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程数为2
        executor.setCorePoolSize(2);
        // 设置最大线程数为4
        executor.setMaxPoolSize(4);
        // 设置任务等待队列容量为25
        executor.setQueueCapacity(25);
        // 设置线程名称前缀为"MyAsync-"
        executor.setThreadNamePrefix("MyAsync-");
        // 设置空闲线程存活时间为60秒
        executor.setKeepAliveSeconds(60);
        // 设置关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 设置等待终止时间为30秒
        executor.setAwaitTerminationSeconds(30);
        // 设置拒绝策略为调用者运行策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
