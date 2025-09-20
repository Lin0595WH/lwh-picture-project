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
 * @Date 2025/9/20 22:21
 * @Descriptions 进入编辑图片状态
 */
@Component
@RequiredArgsConstructor
public class EnterEditMessageStrategy implements PictureEditMessageStrategy {

    private final PictureEditBroadcaster pictureEditBroadcaster;

    private final PictureEditingStatusManager pictureEditingStatusManager;

    @Override
    public String getMessageType() {
        return PictureEditMessageTypeEnum.ENTER_EDIT.getValue();
    }

    @Override
    public void handle(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, UserVO userVO, Long pictureId) throws IOException {
        if (userVO.getId() == null || pictureId == null) {
            return;
        }
        // 当前图片没人编辑，才可以进入编辑
        if (!pictureEditingStatusManager.isBeingEdited(pictureId)) {
            // 更新当前图片的编辑状态
            pictureEditingStatusManager.setEditingUser(pictureId, userVO.getId());
            // 构造响应，发送进入编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户 %s 开始编辑图片", userVO.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userVO);
            // 广播给所有用户
            pictureEditBroadcaster.broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }
}
