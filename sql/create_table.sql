-- 创建库
CREATE DATABASE IF NOT EXISTS lin_picture;

-- 切换库
USE lin_picture;

-- 用户表
CREATE TABLE IF NOT EXISTS user
(
    id            bigint AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    user_account  varchar(256)                           NOT NULL COMMENT '账号',
    user_password varchar(512)                           NOT NULL COMMENT '密码',
    user_name     varchar(256)                           NULL COMMENT '用户昵称',
    user_avatar   varchar(1024)                          NULL COMMENT '用户头像',
    user_profile  varchar(512)                           NULL COMMENT '用户简介',
    user_role     varchar(256) DEFAULT 'user'            NOT NULL COMMENT '用户角色：user/admin',
    create_time   datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    edit_time     datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    update_time   datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete     tinyint      DEFAULT 0                 NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_userAccount (user_account),
    INDEX idx_userName (user_name)
) COMMENT '用户' COLLATE = utf8mb4_unicode_ci;

-- 图片表
CREATE TABLE IF NOT EXISTS picture
(
    id           bigint AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    url          varchar(512)                       NOT NULL COMMENT '图片 url',
    name         varchar(128)                       NOT NULL COMMENT '图片名称',
    introduction varchar(512)                       NULL COMMENT '简介',
    category     varchar(64)                        NULL COMMENT '分类',
    tags         varchar(512)                       NULL COMMENT '标签（JSON 数组）',
    pic_size     bigint                             NULL COMMENT '图片体积',
    pic_width    int                                NULL COMMENT '图片宽度',
    pic_height   int                                NULL COMMENT '图片高度',
    pic_scale    double                             NULL COMMENT '图片宽高比例',
    pic_format   varchar(32)                        NULL COMMENT '图片格式',
    user_id      bigint                             NOT NULL COMMENT '创建用户 id',
    create_time  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    edit_time    datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    update_time  datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete    tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_user_id (user_id)            -- 提升基于用户 ID 的查询性能
) COMMENT '图片' COLLATE = utf8mb4_unicode_ci;

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN review_status  INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN review_message VARCHAR(512)  NULL COMMENT '审核信息',
    ADD COLUMN reviewer_id    BIGINT        NULL COMMENT '审核人 ID',
    ADD COLUMN review_time    DATETIME      NULL COMMENT '审核时间';

-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_review_status ON picture (review_status);

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN thumbnail_url varchar(512) NULL COMMENT '缩略图 url';

-- 空间表
CREATE TABLE IF NOT EXISTS space
(
    id          bigint AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    space_name  varchar(128)                       NULL COMMENT '空间名称',
    space_level int      DEFAULT 0                 NULL COMMENT '空间级别：0-普通版 1-专业版 2-旗舰版',
    max_size    bigint   DEFAULT 0                 NULL COMMENT '空间图片的最大总大小',
    max_count   bigint   DEFAULT 0                 NULL COMMENT '空间图片的最大数量',
    total_size  bigint   DEFAULT 0                 NULL COMMENT '当前空间下图片的总大小',
    total_count bigint   DEFAULT 0                 NULL COMMENT '当前空间下的图片数量',
    user_id     bigint                             NOT NULL COMMENT '创建用户 id',
    create_time datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    edit_time   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    update_time datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete   tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    -- 索引设计
    INDEX idx_userId (user_id),        -- 提升基于用户的查询效率
    INDEX idx_spaceName (space_name),  -- 提升基于空间名称的查询效率
    INDEX idx_spaceLevel (space_level) -- 提升按空间级别查询的效率
) COMMENT '空间' COLLATE = utf8mb4_unicode_ci;

-- 添加新列
ALTER TABLE picture
    ADD COLUMN space_id bigint NULL COMMENT '空间 id（为空表示公共空间）';

-- 创建索引
CREATE INDEX idx_space_id ON picture (space_id);

-- 添加新列
ALTER TABLE picture
    ADD COLUMN pic_color varchar(16) NULL COMMENT '图片主色调';

-- 支持空间类型，添加新列
ALTER TABLE space
    ADD COLUMN space_type int DEFAULT 0 NOT NULL COMMENT '空间类型：0-私有 1-团队';

CREATE INDEX idx_space_type ON space (space_type);

-- 空间成员表
CREATE TABLE IF NOT EXISTS space_user
(
    id          bigint AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    space_id    bigint                                 NOT NULL COMMENT '空间 id',
    user_id     bigint                                 NOT NULL COMMENT '用户 id',
    space_role  varchar(128) DEFAULT 'viewer'          NULL COMMENT '空间角色：viewer/editor/admin',
    create_time datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- 索引设计
    UNIQUE KEY uk_space_id_user_id (space_id, user_id), -- 唯一索引，用户在一个空间中只能有一个角色
    INDEX idx_space_id (space_id),                      -- 提升按空间查询的性能
    INDEX idx_user_id (user_id)                         -- 提升按用户查询的性能
) COMMENT '空间用户关联' COLLATE = utf8mb4_unicode_ci;

-- 调整注册方式为邮箱注册，加入邮箱列
ALTER TABLE user
    ADD COLUMN email varchar(255) NOT NULL COMMENT '邮箱';
-- 再添加唯一索引
ALTER TABLE user
    ADD UNIQUE INDEX idx_email (email);


-- 图片标签分类表
CREATE TABLE IF NOT EXISTS picture_tag_category
(
    id       bigint AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    type     tinyint(1)  NOT NULL COMMENT '类型(1标签2分类)',
    name     varchar(20) NOT NULL COMMENT '名称',
    sort_num int         NULL COMMENT '排序号'
)
    COMMENT '图片标签分类表' COLLATE = utf8mb4_unicode_ci;