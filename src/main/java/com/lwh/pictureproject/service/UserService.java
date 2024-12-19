package com.lwh.pictureproject.service;

import cn.hutool.http.server.HttpServerRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lwh.pictureproject.model.dto.user.UserLoginRequest;
import com.lwh.pictureproject.model.dto.user.UserQueryRequest;
import com.lwh.pictureproject.model.dto.user.UserRegisterRequest;
import com.lwh.pictureproject.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lwh.pictureproject.model.vo.LoginUserVO;
import com.lwh.pictureproject.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Lin
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2024-12-18 20:48:00
*/
public interface UserService extends IService<User> {
    
    /**
     * 用户注册
     **/
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录请求
     **/
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获取当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获得脱敏后的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获得脱敏后的用户信息列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取加密后的密码
     **/
    String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏的登录用户信息
     **/
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
