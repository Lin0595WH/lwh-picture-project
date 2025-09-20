package com.lwh.pictureproject.manager.websocket.disruptor;


import com.lmax.disruptor.WorkHandler;
import com.lwh.pictureproject.manager.websocket.model.PictureEditRequestMessage;
import com.lwh.pictureproject.manager.websocket.model.enums.PictureEditMessageTypeEnum;
import com.lwh.pictureproject.manager.websocket.strategy.PictureEditMessageStrategy;
import com.lwh.pictureproject.manager.websocket.strategy.PictureEditMessageStrategyFactory;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * @Author Lin
 * @Date 2025/9/20 21:23
 * @Descriptions 图片编辑事件处理器（消费者）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    private final UserService userService;

    private final PictureEditMessageStrategyFactory pictureEditMessageStrategyFactory;

    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();
        // 获取到消息类别
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        PictureEditMessageStrategy pictureEditMessageStrategy = pictureEditMessageStrategyFactory.getStrategy(pictureEditMessageTypeEnum.getValue());
        pictureEditMessageStrategy.handle(pictureEditRequestMessage, session, userService.getUserVO(user), pictureId);
    }
}
