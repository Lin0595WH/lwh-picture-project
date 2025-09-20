package com.lwh.pictureproject.manager.websocket.strategy.impl;


import com.lwh.pictureproject.manager.websocket.model.PictureEditRequestMessage;
import com.lwh.pictureproject.manager.websocket.model.PictureEditResponseMessage;
import com.lwh.pictureproject.manager.websocket.model.enums.PictureEditActionEnum;
import com.lwh.pictureproject.manager.websocket.model.enums.PictureEditMessageTypeEnum;
import com.lwh.pictureproject.manager.websocket.strategy.PictureEditMessageStrategy;
import com.lwh.pictureproject.manager.websocket.util.PictureEditBroadcaster;
import com.lwh.pictureproject.manager.websocket.util.PictureEditingStatusManager;
import com.lwh.pictureproject.model.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * @Author Lin
 * @Date 2025/9/20 22:37
 * @Descriptions 执行编辑操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EditActionMessageStrategy implements PictureEditMessageStrategy {

    private final PictureEditBroadcaster pictureEditBroadcaster;

    private final PictureEditingStatusManager pictureEditingStatusManager;

    @Override
    public String getMessageType() {
        return PictureEditMessageTypeEnum.EDIT_ACTION.getValue();
    }

    @Override
    public void handle(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, UserVO userVO, Long pictureId) throws IOException {
        // 先看是否是合法的编辑动作
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            log.error("无效的编辑动作");
            return;
        }
        // 当前图片有人编辑，且编辑人等于当前登录用户，才可以执行编辑操作
        if (pictureEditingStatusManager.isBeingEdited(pictureId) && pictureEditingStatusManager.getEditingUser(pictureId).equals(userVO.getId())) {
            // 构造响应，发送编辑操作的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            pictureEditResponseMessage.setEditAction(pictureEditRequestMessage.getEditAction());
            pictureEditResponseMessage.setUser(userVO);
            String message = String.format("%s 执行 %s", userVO.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            // 广播给除了当前客户端之外的其他用户，否则会造成重复编辑
            pictureEditBroadcaster.broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }
}
