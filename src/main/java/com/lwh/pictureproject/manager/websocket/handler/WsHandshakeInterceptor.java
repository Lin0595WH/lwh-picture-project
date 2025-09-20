package com.lwh.pictureproject.manager.websocket.handler;


import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.lwh.pictureproject.manager.auth.SpaceUserAuthManager;
import com.lwh.pictureproject.manager.auth.model.SpaceUserPermissionConstant;
import com.lwh.pictureproject.model.entity.Picture;
import com.lwh.pictureproject.model.entity.Space;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.enums.SpaceTypeEnum;
import com.lwh.pictureproject.service.PictureService;
import com.lwh.pictureproject.service.SpaceService;
import com.lwh.pictureproject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @Author Lin
 * @Date 2025/9/19 21:10
 * @Descriptions WebSocket 拦截器，建立连接前要先进行权限校验
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    private final UserService userService;

    private final PictureService pictureService;

    private final SpaceService spaceService;

    private final SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 建立连接前要先校验
     *
     * @param request    请求
     * @param response   响应
     * @param wsHandler  拦截器
     * @param attributes 给 WebSocketSession 会话设置属性
     * @throws Exception 异常
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 从请求中获取参数
            String pictureId = httpServletRequest.getParameter("pictureId");
            if (CharSequenceUtil.isBlank(pictureId)) {
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            // 获取当前登录用户
            User loginUser = userService.getLoginUser(httpServletRequest);
            if (ObjectUtil.isEmpty(loginUser)) {
                log.error("用户未登录，拒绝握手");
                return false;
            }
            // 校验用户是否有编辑当前图片的权限
            Picture picture = pictureService.getById(pictureId);
            if (ObjectUtil.isEmpty(picture)) {
                log.error("图片不存在，拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            if (spaceId == null) {
                log.error("图片归属SpaceId为空，拒绝握手");
                return false;
            }
            Space space = spaceService.getById(spaceId);
            if (ObjectUtil.isEmpty(space)) {
                log.error("图片所在空间不存在，拒绝握手");
                return false;
            }
            if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                log.error("图片所在空间不是团队空间，拒绝握手");
                return false;
            }
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("用户没有编辑图片的权限，拒绝握手");
                return false;
            }
            // 设置用户登录信息等属性到 WebSocket 会话中
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            // 记得转换为 Long 类型
            attributes.put("pictureId", Long.valueOf(pictureId));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}
