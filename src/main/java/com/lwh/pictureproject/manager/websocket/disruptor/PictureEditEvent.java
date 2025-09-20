package com.lwh.pictureproject.manager.websocket.disruptor;


import com.lwh.pictureproject.manager.websocket.model.PictureEditRequestMessage;
import com.lwh.pictureproject.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * @Author Lin
 * @Date 2025/9/20 21:22
 * @Descriptions 图片编辑事件
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
