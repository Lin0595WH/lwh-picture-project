package com.lwh.pictureproject.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwh.pictureproject.constant.UserConstant;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.exception.ThrowUtils;
import com.lwh.pictureproject.manager.auth.StpKit;
import com.lwh.pictureproject.manager.captcha.CaptchaManager;
import com.lwh.pictureproject.mapper.UserMapper;
import com.lwh.pictureproject.model.dto.user.UserLoginRequest;
import com.lwh.pictureproject.model.dto.user.UserQueryRequest;
import com.lwh.pictureproject.model.dto.user.UserRegisterRequest;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.enums.UserRoleEnum;
import com.lwh.pictureproject.model.vo.LoginUserVO;
import com.lwh.pictureproject.model.vo.UserVO;
import com.lwh.pictureproject.service.UserService;
import lombok.RequiredArgsConstructor;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // 正则表达式：仅允许中英文、数字
    private static final String VALID_PATTERN = "^[a-zA-Z0-9\u4e00-\u9fa5]+$";

    private final CaptchaManager captchaManager;

    /**
     * @param userRegisterRequest 用户注册请求类
     * @description: 用户注册
     * @author: Lin
     * @date: 2024/12/18 21:27
     * @return: long 新用户 id
     **/
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        // 用户昵称
        String userName = userRegisterRequest.getUserName();
        // 用户账号
        String userAccount = userRegisterRequest.getUserAccount();
        // 密码
        String userPassword = userRegisterRequest.getUserPassword();
        // 确认密码
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 邮箱
        String email = userRegisterRequest.getEmail();
        // 验证码
        String captcha = userRegisterRequest.getCaptcha();
        // 校验
        this.userRegisterVerify(userName, userAccount, userPassword, checkPassword, email, captcha);
        // 校验验证码
        ThrowUtils.throwIf(!captchaManager.verifyCode(email, captcha), ErrorCode.PARAMS_ERROR, "验证码错误！");
        // 2.校验用户账号是否重复
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号重复！");
        // 校验邮箱是否重复
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, email);
        count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "该邮箱已被注册使用！");
        // 3.加密密码
        String encryptPassword = this.getEncryptPassword(userPassword);
        // 4.插入数据
        User user = User.builder()
                .userName(userName)
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userRole(UserRoleEnum.USER.getValue())
                .email(email)
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
        ThrowUtils.throwIf(CharSequenceUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "参数为空！");
        ThrowUtils.throwIf(userAccount.length() < 4 || userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "账号/密码错误！");
        // 2.查询用户是否存在
        User user = this.getOne(new QueryWrapper<User>().eq("user_account", userAccount), false);
        //不告诉用户到底是 账号不存在 还是 密码错误，降低泄露风险
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或密码错误！");
        // 英文存比较省空间
        log.error("User login failed: Current login user does not exist!");
        // 3.校验密码
        String encryptPassword = this.getEncryptPassword(userPassword);
        //不告诉用户到底是 账号不存在 还是 密码错误，降低泄露风险
        ThrowUtils.throwIf(!encryptPassword.equals(user.getUserPassword()), ErrorCode.PARAMS_ERROR, "用户不存在或密码错误！");
        // 英文存比较省空间
        log.error("User login failed: Incorrect password！");
        // 4.保存用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 2025.9.16 使用sa-token 校验团队空间的成员权限，所以这里同时要往StpKit的SPACE的session中保存用户信息
        // 注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
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
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空！");
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        // 2.开始构造查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(CharSequenceUtil.isNotBlank(userRole), "user_role", userRole);
        queryWrapper.like(CharSequenceUtil.isNotBlank(userAccount), "user_account", userAccount);
        queryWrapper.like(CharSequenceUtil.isNotBlank(userName), "user_name", userName);
        queryWrapper.like(CharSequenceUtil.isNotBlank(userProfile), "user_profile", userProfile);
        queryWrapper.orderBy(CharSequenceUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * @param loginUser 当前登录用户
     * @description: 是否为管理员
     * @author: Lin
     * @date: 2024/12/24 21:57
     * @return: boolean
     **/
    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
    }

    /**
     * 校验字符串是否合法（无特殊字符）
     *
     * @param str 待校验字符串
     * @return true：含特殊字符；false：合法（仅中英文、数字）
     */
    private boolean illegalStr(String str) {
        if (str == null || str.trim().isEmpty()) {
            return true;
        }
        return !ReUtil.isMatch(VALID_PATTERN, str);
    }

    /**
     * 注册前的校验
     *
     * @param userName      用户昵称
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @param email         邮箱
     * @param captcha       验证码
     */
    private void userRegisterVerify(String userName, String userAccount, String userPassword,
                                    String checkPassword, String email, String captcha) {
        ThrowUtils.throwIf(CharSequenceUtil.hasBlank(userName, userAccount, userPassword, checkPassword, email, captcha),
                ErrorCode.PARAMS_ERROR, "参数为空!");
        ThrowUtils.throwIf(this.illegalStr(userName) || this.illegalStr(userAccount),
                ErrorCode.PARAMS_ERROR, "昵称或账号中包含特殊字符！");
        ThrowUtils.throwIf(userName.length() < 2,
                ErrorCode.PARAMS_ERROR, "用户昵称过短！");
        ThrowUtils.throwIf(userAccount.length() < 4,
                ErrorCode.PARAMS_ERROR, "账号长度小于4！");
        ThrowUtils.throwIf(!Validator.isEmail(email), ErrorCode.PARAMS_ERROR, "邮箱格式错误！");
        ThrowUtils.throwIf(userAccount.contains(" ") || userPassword.contains(" ") || checkPassword.contains(" "),
                ErrorCode.PARAMS_ERROR, "用户账号/密码/确认密码不能包含空格！");
        ThrowUtils.throwIf(userPassword.length() < 8 || checkPassword.length() < 8,
                ErrorCode.PARAMS_ERROR, "用户密码过短!");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致!");
    }


}




