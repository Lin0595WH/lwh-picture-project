package com.lwh.pictureproject.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwh.pictureproject.constant.UserConstant;
import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.exception.ThrowUtils;
import com.lwh.pictureproject.model.dto.user.UserLoginRequest;
import com.lwh.pictureproject.model.dto.user.UserQueryRequest;
import com.lwh.pictureproject.model.dto.user.UserRegisterRequest;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.enums.UserRoleEnum;
import com.lwh.pictureproject.model.vo.LoginUserVO;
import com.lwh.pictureproject.model.vo.UserVO;
import com.lwh.pictureproject.service.UserService;
import com.lwh.pictureproject.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Lin
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2024-12-18 20:48:00
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * @param userRegisterRequest 用户注册请求类
     * @description: 用户注册
     * @author: Lin
     * @date: 2024/12/18 21:27
     * @return: long 新用户 id
     **/
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        // 用户账号
        String userAccount = userRegisterRequest.getUserAccount();
        // 密码
        String userPassword = userRegisterRequest.getUserPassword();
        // 确认密码
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 1.校验
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR, "参数为空!");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号长度小于4！");
        ThrowUtils.throwIf(userPassword.length() < 8 || checkPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码过短!");
        ThrowUtils.throwIf(userAccount.contains(" ") || userPassword.contains(" ") || checkPassword.contains(" "),
                ErrorCode.PARAMS_ERROR, "用户账号/密码/确认密码不能包含空格！");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致!");
        // 2.校验用户账号是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userAccount);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号重复！");
        // 3.加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        // 4.插入数据
        User user = User.builder()
                .userName("默认名称")
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userRole(UserRoleEnum.USER.getValue())
                .build();
        boolean save = this.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "用户注册失败");
        return user.getId();
    }

    /**
     * @param userLoginRequest 用户登录请求类
     * @param request          用户登录后，将用户信息存储到 session
     * @description: 用户登录
     * @author: Lin
     * @date: 2024/12/18 22:21
     * @return: java.lang.String
     **/
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 1.校验
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR, "参数为空！");
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "参数为空！");
        ThrowUtils.throwIf(userAccount.length() < 4 || userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "账号/密码错误！");
        // 2.查询用户是否存在
        User user = this.getOne(new QueryWrapper<User>().eq("user_account", userAccount), false);
        //不告诉用户到底是 账号不存在 还是 密码错误，降低泄露风险
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或密码错误！");
        // 英文存比较省空间
        log.error("User login failed: Current login user does not exist!");
        // 3.校验密码
        String encryptPassword = getEncryptPassword(userPassword);
        //不告诉用户到底是 账号不存在 还是 密码错误，降低泄露风险
        ThrowUtils.throwIf(!encryptPassword.equals(user.getUserPassword()), ErrorCode.PARAMS_ERROR, "用户不存在或密码错误！");
        // 英文存比较省空间
        log.error("User login failed: Incorrect password！");
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * @param request 用户请求
     * @description: 获取当前登录用户
     * @author: Lin
     * @date: 2024/12/18 22:58
     * @return: com.lwh.pictureproject.model.entity.User
     **/
    @Override
    public User getLoginUser(HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        loginUser = this.getById(loginUser.getId());
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        return loginUser;
    }

    /**
     * @param user 用户实体
     * @description: 获取用户视图
     * @author: Lin
     * @date: 2024/12/19 20:59
     * @return: com.lwh.pictureproject.model.vo.UserVO
     **/
    @Override
    public UserVO getUserVO(User user) {
        return Optional.ofNullable(user).map(item -> BeanUtil.toBean(item, UserVO.class)).orElse(null);
    }

    /**
     * @param userList 用户列表
     * @description: 返回脱敏后的用户视图列表
     * @author: Lin
     * @date: 2024/12/19 21:08
     * @return: java.util.List<com.lwh.pictureproject.model.vo.UserVO>
     **/
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        return Optional.ofNullable(userList)
                .map(users -> users.stream()
                        .map(this::getUserVO)
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
    }

    /**
     * @param request 用户请求
     * @description: 用户注销功能
     * @author: Lin
     * @date: 2024/12/19 20:18
     * @return: boolean 是否注销成功
     **/
    @Override
    public boolean userLogout(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(user == null, ErrorCode.OPERATION_ERROR, "未登录！");
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * @param userPassword 用户输入的原始密码
     * @description: 获取加密后的密码
     * @author: Lin
     * @date: 2024/12/18 21:41
     * @return: java.lang.String
     **/
    @Override
    public String getEncryptPassword(String userPassword) {
        // 加盐，混淆密码
        final String SALT = "lin_wh:prcture:user";
        return DigestUtils.md5DigestAsHex((userPassword + SALT).getBytes());
    }

    /**
     * @param user 用户信息
     * @description: 获取脱敏的登录用户信息
     * @author: Lin
     * @date: 2024/12/18 22:46
     * @return: com.lwh.pictureproject.model.vo.LoginUserVO
     **/
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * @param userQueryRequest 用户查询请求类
     * @description: 封装查询条件
     * @author: Lin
     * @date: 2024/12/19 21:15
     * @return: com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.lwh.pictureproject.model.entity.User>
     **/
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        // 1.校验
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR,"请求参数为空！" );
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        int pageSize = userQueryRequest.getPageSize();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        // 2.开始构造查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "user_role", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "user_account", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "user_name", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "user_profile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }


}




