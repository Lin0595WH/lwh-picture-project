package com.lwh.pictureproject.manager.websocket.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author Lin
 * @Date 2025/9/19 21:07
 * @Descriptions 图片编辑请求消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditRequestMessage {

    /**
     * 消息类型，例如 "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 执行的编辑动作（放大、缩小）
     */
    private String editAction;
}
