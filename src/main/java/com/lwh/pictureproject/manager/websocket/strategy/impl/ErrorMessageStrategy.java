package com.lwh.pictureproject.manager.websocket.strategy.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.lwh.pictureproject.manager.websocket.model.PictureEditRequestMessage;
import com.lwh.pictureproject.manager.websocket.model.PictureEditResponseMessage;
import com.lwh.pictureproject.manager.websocket.model.enums.PictureEditMessageTypeEnum;
import com.lwh.pictureproject.manager.websocket.strategy.PictureEditMessageStrategy;
import com.lwh.pictureproject.model.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * @Author Lin
 * @Date 2025/9/20 22:41
 * @Descriptions 消息类型错误处理
 */
@Component
@RequiredArgsConstructor
public class ErrorMessageStrategy implements PictureEditMessageStrategy {

    @Override
    public String getMessageType() {
        return PictureEditMessageTypeEnum.ERROR.getValue();
    }

    @Override
    public void handle(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, UserVO userVO, Long pictureId) throws IOException {
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
        pictureEditResponseMessage.setMessage("消息类型错误");
        pictureEditResponseMessage.setUser(userVO);
        // 创建 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置序列化：将 Long 类型转为 String，解决丢失精度问题（PictureEditResponseMessage : UserVO ：id ：long)
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
        objectMapper.registerModule(module);
        // 序列化为 JSON 字符串
        String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
        session.sendMessage(new TextMessage(message));
    }
}
