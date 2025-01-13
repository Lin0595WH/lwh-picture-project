package com.lwh.pictureproject.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.exception.ThrowUtils;
import com.lwh.pictureproject.manager.CosManager;
import com.lwh.pictureproject.manager.upload.FilePictureUpload;
import com.lwh.pictureproject.manager.upload.PictureUploadTemplate;
import com.lwh.pictureproject.manager.upload.UrlPictureUpload;
import com.lwh.pictureproject.mapper.PictureMapper;
import com.lwh.pictureproject.model.dto.file.UploadPictureResult;
import com.lwh.pictureproject.model.dto.picture.PictureQueryRequest;
import com.lwh.pictureproject.model.dto.picture.PictureReviewRequest;
import com.lwh.pictureproject.model.dto.picture.PictureUploadByBatchRequest;
import com.lwh.pictureproject.model.dto.picture.PictureUploadRequest;
import com.lwh.pictureproject.model.entity.Picture;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.enums.PictureReviewStatusEnum;
import com.lwh.pictureproject.model.vo.PictureVO;
import com.lwh.pictureproject.model.vo.UserVO;
import com.lwh.pictureproject.service.PictureService;
import com.lwh.pictureproject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Lin
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2024-12-21 23:58:47
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    private final UserService userService;

    private final FilePictureUpload filePictureUpload;

    private final UrlPictureUpload urlPictureUpload;
    private final CosManager cosManager;

    /**
     * @param picture 图片
     * @description: 校验图片
     * @author: Lin
     * @date: 2024/12/24 22:14
     * @return: void
     **/
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(ObjUtil.isNull(picture), ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        // 如果传递了才校验
        ThrowUtils.throwIf(StrUtil.isNotBlank(url) && url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        ThrowUtils.throwIf(StrUtil.isNotBlank(introduction) && introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");

    }

    /**
     * @param inputSource          文件输入源
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser            当前登录用户
     * @description: 上传图片
     * @author: Lin
     * @date: 2024/12/24 21:27
     * @return: com.lwh.pictureproject.model.vo.PictureVO
     **/
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Long userId = loginUser.getId();
        boolean isAdmin = userService.isAdmin(loginUser);
        // 判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新，判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            Integer oldReviewStatus = oldPicture.getReviewStatus();
            // 不是管理员，且不是自己的图片，不能操作
            boolean noAuth = !Objects.equals(oldPicture.getUserId(), userId) && !isAdmin;
            ThrowUtils.throwIf(noAuth, ErrorCode.NO_AUTH_ERROR, "无权限操作该图片");
            ThrowUtils.throwIf(oldReviewStatus == PictureReviewStatusEnum.REVIEWING.getValue(), ErrorCode.OPERATION_ERROR, "图片正在审核中，请勿重复操作");
        }
        // 上传图片，得到图片信息
        // 按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/%s", userId);
        // 根据输入源判断上传方式，是文件还是url
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        // 2025.1.2 请求参数新增图片名称，如果有，那么用这个，否则用文件名
        String picName = (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName()))
                ? pictureUploadRequest.getPicName() : uploadPictureResult.getPicName();
        Picture picture = Picture.builder()
                .url(uploadPictureResult.getUrl())
                .name(picName)
                .picSize(uploadPictureResult.getPicSize())
                .picWidth(uploadPictureResult.getPicWidth())
                .picHeight(uploadPictureResult.getPicHeight())
                .picScale(uploadPictureResult.getPicScale())
                .picFormat(uploadPictureResult.getPicFormat())
                .userId(userId)
                // 拿不到缩略图就拿原图
                .thumbnailUrl(StrUtil.isNotBlank(uploadPictureResult.getThumbnailUrl())
                        ? uploadPictureResult.getThumbnailUrl() : uploadPictureResult.getUrl())
                .build();
        // 2024.12.27 加入审核部分的判断
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        // 如果 pictureId 不为空，表示更新，否则是新增
        boolean isUpdate = pictureId != null;
        if (isUpdate) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
        // 如果是更新的话，要把对象存储中，旧文件给删除
        if (isUpdate) {
            String oldUrl = this.getById(pictureId).getUrl();
            cosManager.deleteObject(oldUrl);
        }
        return PictureVO.objToVo(picture);
    }

    /**
     * @param picture 图片
     * @param request 请求
     * @description: 获取图片包装类（单条）
     * @author: Lin
     * @date: 2024/12/24 22:06
     * @return: com.lwh.pictureproject.model.vo.PictureVO
     **/
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * @param picturePage 图片分页对象
     * @param request     请求
     * @description: 获取图片包装类（分页）
     * @author: Lin
     * @date: 2024/12/24 22:08
     * @return: com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.lwh.pictureproject.model.vo.PictureVO>
     **/
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * @param pictureQueryRequest 图片查询请求
     * @description: 获取查询对象
     * @author: Lin
     * @date: 2024/12/24 22:00
     * @return: com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.lwh.pictureproject.model.entity.Picture>
     **/
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            // and (name like "%xxx%" or introduction like "%xxx%")
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "user_id", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "pic_format", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "review_message", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "pic_width", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "pic_height", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "pic_size", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "pic_scale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "review_status", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewer_id", reviewerId);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            /* and (tag like "%\"Java\"%" and like "%\"Python\"%") */
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser            当前登录用户
     * @description: 图片审核
     * @author: Lin
     * @date: 2024/12/27 22:25
     * @return: void
     **/
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 校验
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 开始审核操作
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        Integer oldReviewStatus = oldPicture.getReviewStatus();
        ThrowUtils.throwIf(Objects.equals(reviewStatus, oldReviewStatus), ErrorCode.PARAMS_ERROR, "请勿重复审核");
        // 构造更新图片的更新对象，只赋值需要的更新的字段信息
        Picture updatePicture = Picture.builder().id(id).reviewStatus(reviewStatus).reviewMessage(reviewMessage).reviewTime(new Date()).reviewerId(loginUser.getId()).build();
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "审核失败");
    }

    /**
     * @param picture   需要补充审核参数的图片对象
     * @param loginUser 当前登录用户
     * @description: 加了审核参数后，多个操作地方需补充审核参数，故抽成方法来统一填充图片的审核参数
     * @author: Lin
     * @date: 2024/12/27 23:08
     * @return: void
     **/
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        boolean isAdmin = userService.isAdmin(loginUser);
        if (isAdmin) {
            // 管理员审核
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员审核通过");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        } else {
            // 非管理员,上传或者编辑，都是需要审核的
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * @param pictureUploadByBatchRequest 批量上传图片请求
     * @param loginUser                   当前登录用户
     * @description: 批量抓取和创建图片
     * @author: Lin
     * @date: 2025/1/2 20:33
     * @return: java.lang.Integer 创建成功的图片数量
     **/
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "搜索词不能为空");
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 名称前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (div == null || ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        // 遍历元素，依次处理上传图片
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片的地址，防止转义或者和对象存储冲突的问题
            // codefather.cn?yupi=dog，应该只保留 codefather.cn
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    /**
     * @param picture 要清理的图片对象
     * @description: 清理图片文件
     * @author: Lin
     * @date: 2025/1/7 21:48
     * @return: void
     **/
    @Async
    @Override
    public void clearPictureFile(Picture picture) {
        // 校验
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        String fileUrl = picture.getUrl();
        // 找不到图片路径，也就不清理了
        if (StrUtil.isBlank(fileUrl)) {
            return;
        }
        // 该图片地址的使用次数
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, fileUrl)
                .count();
        // 如果 count > 1，说明该图片地址被其他图片引用，不进行清理
        if (count > 1) {
            return;
        }
        // 删除图片文件
        cosManager.deleteObject(fileUrl);
        // 删除缩略图
        String thumbnailUrl = picture.getThumbnailUrl();
        // 这里要有缩略图，且缩略图不等于原图才去删，因为之前可能会把原图当作缩略图用，那前面就已经删除了
        if (StrUtil.isNotBlank(thumbnailUrl) && !fileUrl.equals(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }
}




