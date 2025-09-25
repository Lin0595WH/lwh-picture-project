package com.lwh.pictureproject.manager.websocket.util;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.lwh.pictureproject.manager.websocket.model.PictureEditResponseMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Lin
 * @Date 2025/9/20 22:23
 * @Descriptions 广播消息
 */
@Component
public class PictureEditBroadcaster {

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 用户加入指定图片的会话集合
     *
     * @param pictureId 图片Id
     * @param session   用户会话
     **/
    public void addSession(Long pictureId, WebSocketSession session) {
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
    }

    /**
     * 用户退出指定图片的会话集合
     *
     * @param pictureId 图片Id
     * @param session   用户会话
     **/
    public void removeSession(Long pictureId, WebSocketSession session) {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
    }

    /**
     * @param pictureEditResponseMessage 要返回的图片编辑响应消息
     * @param specifySession             指定Session
     * @Descriptions 广播给指定会话
     * @Date 2025/9/19 21:28
     * @Author Lin
     */
    public void broadcastToPicture(PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession specifySession) throws IOException {
        if (ObjectUtil.isNotNull(specifySession)) {
            // 创建 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化：将 Long 类型转为 String，解决丢失精度问题（PictureEditResponseMessage : UserVO ：id ：long)
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
            objectMapper.registerModule(module);
            // 序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            if (specifySession.isOpen()) {
                specifySession.sendMessage(textMessage);
            }
        }
    }

    /**
     * @param pictureId                  图片Id
     * @param pictureEditResponseMessage 要返回的图片编辑响应消息
     * @param excludeSession             要排除的Session
     * @Descriptions 广播给该图片的所有用户（支持排除掉某个 Session）
     * @Date 2025/9/19 21:28
     * @Author Lin
     */
    public void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage,
                                   WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            // 创建 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化：将 Long 类型转为 String，解决丢失精度问题（PictureEditResponseMessage : UserVO ：id ：long)
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
            objectMapper.registerModule(module);
            // 序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                // 排除掉的 session 不发送
                if (excludeSession != null && session.equals(excludeSession)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * @param pictureId                  图片Id
     * @param pictureEditResponseMessage 要返回的图片编辑响应消息
     * @Descriptions 广播给该图片的所有用户
     * @Date 2025/9/19 21:31
     * @Author Lin
     */
    public void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage)
            throws IOException {
        this.broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }
}
