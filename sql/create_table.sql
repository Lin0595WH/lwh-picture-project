-- 创建库
create database if not exists lin_picture;

-- 切换库
use lin_picture;

-- 用户表
create table if not exists user
(
    id            bigint auto_increment comment 'id' primary key,
    user_account  varchar(256)                           not null comment '账号',
    user_password varchar(512)                           not null comment '密码',
    user_name     varchar(256)                           null comment '用户昵称',
    user_avatar   varchar(1024)                          null comment '用户头像',
    user_profile  varchar(512)                           null comment '用户简介',
    user_role     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    create_time   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    edit_time     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    update_time   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (user_account),
    INDEX idx_userName (user_name)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                       null comment '标签（JSON 数组）',
    pic_size     bigint                             null comment '图片体积',
    pic_width    int                                null comment '图片宽度',
    pic_height   int                                null comment '图片高度',
    pic_scale    double                             null comment '图片宽高比例',
    pic_format   varchar(32)                        null comment '图片格式',
    user_id      bigint                             not null comment '创建用户 id',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    edit_time    datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (user_id)             -- 提升基于用户 ID 的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;


ALTER TABLE picture
    -- 添加新列
    ADD COLUMN review_status  INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN review_message VARCHAR(512)  NULL COMMENT '审核信息',
    ADD COLUMN reviewer_id    BIGINT        NULL COMMENT '审核人 ID',
    ADD COLUMN review_time    DATETIME      NULL COMMENT '审核时间';

-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON picture (review_status);

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN thumbnail_url varchar(512) NULL COMMENT '缩略图 url';