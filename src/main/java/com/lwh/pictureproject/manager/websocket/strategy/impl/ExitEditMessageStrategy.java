package com.lwh.pictureproject.manager.websocket.strategy.impl;


import com.lwh.pictureproject.manager.websocket.model.PictureEditRequestMessage;
import com.lwh.pictureproject.manager.websocket.model.PictureEditResponseMessage;
import com.lwh.pictureproject.manager.websocket.model.enums.PictureEditMessageTypeEnum;
import com.lwh.pictureproject.manager.websocket.strategy.PictureEditMessageStrategy;
import com.lwh.pictureproject.manager.websocket.util.PictureEditBroadcaster;
import com.lwh.pictureproject.manager.websocket.util.PictureEditingStatusManager;
import com.lwh.pictureproject.model.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * @Author Lin
 * @Date 2025/9/20 22:40
 * @Descriptions 退出编辑图片状态
 */
@Component
@RequiredArgsConstructor
public class ExitEditMessageStrategy implements PictureEditMessageStrategy {

    private final PictureEditBroadcaster pictureEditBroadcaster;

    private final PictureEditingStatusManager pictureEditingStatusManager;

    @Override
    public String getMessageType() {
        return PictureEditMessageTypeEnum.EXIT_EDIT.getValue();
    }

    @Override
    public void handle(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, UserVO userVO, Long pictureId) throws IOException {
        if (userVO.getId() == null || pictureId == null) {
            return;
        }
        // 当前正在编辑图片的用户id
        Long editingUserId = pictureEditingStatusManager.getEditingUser(pictureId);
        if (editingUserId != null && editingUserId.equals(userVO.getId())) {
            pictureEditingStatusManager.removeEditingUser(pictureId);
            // 构造响应，发送退出编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("用户 %s 退出编辑图片", userVO.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userVO);
            pictureEditBroadcaster.broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }
}
