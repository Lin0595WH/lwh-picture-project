package com.lwh.pictureproject.manager.websocket.handler;


import cn.hutool.json.JSONUtil;
import com.lwh.pictureproject.manager.websocket.disruptor.PictureEditEventProducer;
import com.lwh.pictureproject.manager.websocket.model.PictureEditRequestMessage;
import com.lwh.pictureproject.manager.websocket.model.PictureEditResponseMessage;
import com.lwh.pictureproject.manager.websocket.model.enums.PictureEditMessageTypeEnum;
import com.lwh.pictureproject.manager.websocket.strategy.PictureEditMessageStrategy;
import com.lwh.pictureproject.manager.websocket.strategy.PictureEditMessageStrategyFactory;
import com.lwh.pictureproject.manager.websocket.util.PictureEditBroadcaster;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * @Author Lin
 * @Date 2025/9/19 21:22
 * @Descriptions 图片编辑 WebSocket 处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PictureEditHandler extends TextWebSocketHandler {

    private final UserService userService;

    private final PictureEditBroadcaster pictureEditBroadcaster;

    private final PictureEditEventProducer pictureEditEventProducer;

    private final PictureEditMessageStrategyFactory pictureEditMessageStrategyFactory;

    /**
     * 连接建立成功
     *
     * @param session 当前会话
     **/
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        // 保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureEditBroadcaster.addSession(pictureId, session);
        // 构造响应，发送加入编辑的消息通知
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        // 广播给所有用户
        pictureEditBroadcaster.broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 收到前端发送的消息，根据消息类别处理消息
     *
     * @param session 当前会话
     * @param message 消息内容
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        // 获取消息内容，将 JSON 转换为 PictureEditRequestMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        // 从 Session 属性中获取到公共参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 根据消息类型处理消息（生产消息到 Disruptor 环形队列中）
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }

    /**
     * 关闭连接
     *
     * @param session 当前会话
     * @param status  关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        // 从 Session 属性中获取到公共参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 移除当前用户的编辑状态
        //this.handleExitEditMessage(null, session, user, pictureId);
        PictureEditMessageStrategy exitStrategy = pictureEditMessageStrategyFactory.getStrategy(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
        exitStrategy.handle(null, session, userService.getUserVO(user), pictureId);
        // 删除会话
        pictureEditBroadcaster.removeSession(pictureId, session);
        // 通知其他用户，该用户已经离开编辑
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        pictureEditBroadcaster.broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

}
