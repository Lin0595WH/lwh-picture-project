package com.lwh.pictureproject.manager.websocket.disruptor;


import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lwh.pictureproject.manager.websocket.model.PictureEditRequestMessage;
import com.lwh.pictureproject.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;

/**
 * @Author Lin
 * @Date 2025/9/20 21:33
 * @Descriptions 图片编辑事件生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PictureEditEventProducer {

    private final Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    /**
     * 发布事件
     *
     * @param pictureEditRequestMessage 图片编辑请求消息
     * @param session                   会话
     * @param user                      用户
     * @param pictureId                 图片Id
     */
    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();
        // 获取到可以放置事件的位置
        long next = ringBuffer.next();
        PictureEditEvent pictureEditEvent = ringBuffer.get(next);
        pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);
        pictureEditEvent.setSession(session);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setPictureId(pictureId);
        // 发布事件
        ringBuffer.publish(next);
    }

    /**
     * 优雅停机
     */
    @PreDestroy
    public void destroy() {
        pictureEditEventDisruptor.shutdown();
    }
}
