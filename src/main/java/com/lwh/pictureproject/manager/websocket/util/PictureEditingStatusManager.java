package com.lwh.pictureproject.manager.websocket.util;


import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Lin
 * @Date 2025/9/20 22:30
 * @Descriptions 图片编辑状态管理器
 */
@Component
public class PictureEditingStatusManager {
    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    /**
     * 判断图片是否正在被编辑
     *
     * @param pictureId 图片 ID
     */
    public boolean isBeingEdited(Long pictureId) {
        return pictureEditingUsers.containsKey(pictureId);
    }

    /**
     * 获取图片正在被哪个用户编辑
     *
     * @param pictureId 图片 ID
     */
    public Long getEditingUser(Long pictureId) {
        return pictureEditingUsers.get(pictureId);
    }

    /**
     * 设置图片正在被哪个用户编辑
     *
     * @param pictureId 图片 ID
     * @param userId    用户 ID
     */
    public Long setEditingUser(Long pictureId, Long userId) {
        return pictureEditingUsers.putIfAbsent(pictureId, userId);
    }

    /**
     * 移除图片正在被哪个用户编辑
     *
     * @param pictureId 图片 ID
     */
    public void removeEditingUser(Long pictureId) {
        pictureEditingUsers.remove(pictureId);
    }
}
