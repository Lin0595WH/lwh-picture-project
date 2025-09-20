package com.lwh.pictureproject.manager.websocket.disruptor;


import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author Lin
 * @Date 2025/9/20 21:28
 * @Descriptions 图片编辑事件 Disruptor 配置
 */
@Configuration
@RequiredArgsConstructor
public class PictureEditEventDisruptorConfig {

    private final PictureEditEventWorkHandler pictureEditEventWorkHandler;

    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() {
        // 定义 ringBuffer 的大小
        int bufferSize = 1024 * 256;
        // 创建 disruptor
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                PictureEditEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("pictureEditEventDisruptor").build()
        );
        // 设置消费者
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        // 启动 disruptor
        disruptor.start();
        return disruptor;
    }
}
