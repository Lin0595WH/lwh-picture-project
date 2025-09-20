package com.lwh.pictureproject.manager.websocket.strategy;


import com.lwh.pictureproject.manager.websocket.model.PictureEditRequestMessage;
import com.lwh.pictureproject.model.vo.UserVO;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * @Author Lin
 * @Date 2025/9/20 22:18
 * @Descriptions 图片编辑消息处理策略
 */
public interface PictureEditMessageStrategy {

    /**
     * 获取当前策略处理的消息类型
     *
     * @return 消息类型字符串（对应PictureEditMessageTypeEnum的值）
     */
    String getMessageType();

    /**
     * 处理图片编辑请求消息
     *
     * @param pictureEditRequestMessage 图片编辑请求消息
     * @param session                   WebSocket会话
     * @param userVO                    封装后的用户信息
     * @param pictureId                 图片ID
     **/
    void handle(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, UserVO userVO, Long pictureId) throws IOException;

}
