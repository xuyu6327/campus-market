-- ============================================================
-- 校园二手交易平台 - 数据库初始化脚本 v1.0
-- 对应产品功能文档 v11.0 / 技术设计文档 v3.0
-- 执行方式：Navicat 中新建查询，全选执行
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `campus_market`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `campus_market`;

-- 删除旧表（开发环境，按依赖反序删除）
DROP TABLE IF EXISTS `daily_statistics`;
DROP TABLE IF EXISTS `credit_log`;
DROP TABLE IF EXISTS `sensitive_word`;
DROP TABLE IF EXISTS `report`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `evaluation`;
DROP TABLE IF EXISTS `user_browse_history`;
DROP TABLE IF EXISTS `user_favorite`;
DROP TABLE IF EXISTS `trade_order`;
DROP TABLE IF EXISTS `goods_info`;
DROP TABLE IF EXISTS `goods_category`;
DROP TABLE IF EXISTS `sys_user`;

-- ============================================================
-- 1. sys_user 用户表
-- ============================================================
CREATE TABLE `sys_user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `openid`        VARCHAR(64)  DEFAULT NULL COMMENT '微信openid（小程序登录用）',
    `nickname`      VARCHAR(50)  NOT NULL COMMENT '昵称',
    `avatar`        VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `phone`         VARCHAR(200) DEFAULT NULL COMMENT '手机号（AES-GCM加密存储）',
    `phone_hash`    VARCHAR(64)  DEFAULT NULL COMMENT '手机号HMAC-SHA256盲索引（用于等值查询）',
    `password`      VARCHAR(100) DEFAULT NULL COMMENT '密码（BCrypt加密）',
    `student_id`    VARCHAR(20)  DEFAULT NULL COMMENT '学号',
    `real_name`     VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    `qq`            VARCHAR(20)  DEFAULT NULL COMMENT 'QQ号',
    `wechat`        VARCHAR(50)  DEFAULT NULL COMMENT '微信号',
    `credit_score`  INT          NOT NULL DEFAULT 100 COMMENT '信用分（初始100）',
    `role`          TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0普通用户 1管理员',
    `cancel_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '注销状态：0正常 1待注销',
    `cancel_at`     DATETIME     DEFAULT NULL COMMENT '申请注销时间（7天后执行）',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态：0禁用 1正常',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    UNIQUE KEY `uk_phone_hash` (`phone_hash`),
    UNIQUE KEY `uk_student_id` (`student_id`),
    KEY `idx_credit_score` (`credit_score`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. goods_category 商品分类表（两级分类）
-- ============================================================
CREATE TABLE `goods_category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类ID（0为一级分类）',
    `name`        VARCHAR(50)  NOT NULL COMMENT '分类名称',
    `icon`        VARCHAR(200) DEFAULT NULL COMMENT '分类图标URL',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序（越小越靠前）',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ============================================================
-- 3. goods_info 商品信息表
-- ============================================================
CREATE TABLE `goods_info` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `seller_id`       BIGINT        NOT NULL COMMENT '卖家用户ID',
    `title`           VARCHAR(100)  NOT NULL COMMENT '商品标题',
    `description`     TEXT          COMMENT '商品描述',
    `category_id`     BIGINT        NOT NULL COMMENT '分类ID',
    `price`           DECIMAL(10,2) NOT NULL COMMENT '售价',
    `original_price`  DECIMAL(10,2) DEFAULT NULL COMMENT '原价（展示用）',
    `goods_condition` TINYINT       NOT NULL DEFAULT 3 COMMENT '成色：1全新 2几乎全新 3轻微使用痕迹 4明显使用痕迹 5严重使用痕迹',
    `images`          TEXT          COMMENT '图片URL列表（JSON数组格式）',
    `trade_location`  VARCHAR(200)  DEFAULT NULL COMMENT '交易地点（如：三餐厅门口）',
    `contact_method`  TINYINT       NOT NULL DEFAULT 1 COMMENT '联系方式类型：1手机 2QQ 3微信',
    `contact_qq`      VARCHAR(20)   DEFAULT NULL COMMENT '联系QQ号',
    `contact_wechat`  VARCHAR(50)   DEFAULT NULL COMMENT '联系微信号',
    `contact_phone`   VARCHAR(200)  DEFAULT NULL COMMENT '联系手机号（AES-GCM加密存储）',
    `status`          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0下架 1在售 2预订中 3已售出 4待审核',
    `takedown_by`     TINYINT       NOT NULL DEFAULT 0 COMMENT '下架方式：0自行下架/正常 1管理员强制下架',
    `takedown_reason` VARCHAR(255)  DEFAULT NULL COMMENT '强制下架原因（审核驳回原因）',
    `view_count`      INT           NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `favorite_count`  INT           NOT NULL DEFAULT 0 COMMENT '收藏次数',
    `last_relisted_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近上架时间（30天自动下架按此字段计算）',
    `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version`         INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_last_relisted_at` (`last_relisted_at`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息表';

-- ============================================================
-- 4. trade_order 交易订单表
-- ============================================================
CREATE TABLE `trade_order` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`       VARCHAR(32)  NOT NULL COMMENT '订单编号（唯一）',
    `goods_id`       BIGINT       NOT NULL COMMENT '商品ID',
    `buyer_id`       BIGINT       NOT NULL COMMENT '买家用户ID',
    `seller_id`      BIGINT       NOT NULL COMMENT '卖家用户ID',
    `buyer_phone`    VARCHAR(200) DEFAULT NULL COMMENT '买家手机号快照（AES-GCM加密，买家选填，预订时冻结）',
    `buyer_qq`       VARCHAR(20)  DEFAULT NULL COMMENT '买家QQ号快照（买家选填）',
    `buyer_wechat`   VARCHAR(50)  DEFAULT NULL COMMENT '买家微信号快照（买家选填）',
    `seller_qq`      VARCHAR(20)  DEFAULT NULL COMMENT '卖家QQ号快照（预订时冻结，防止卖家修改后历史联系方式丢失）',
    `seller_wechat`  VARCHAR(50)  DEFAULT NULL COMMENT '卖家微信号快照',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '订单状态：0待交易 1已完成 2买家取消 3卖家取消 4超时自动取消',
    `contact_fail_at` DATETIME    DEFAULT NULL COMMENT '买家提交"联系不上卖家"的时间（24h后卖家仍未响应则自动取消）',
    `trade_time`     DATETIME     DEFAULT NULL COMMENT '实际交易完成时间',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预订时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_status` (`status`),
    KEY `idx_contact_fail_at` (`contact_fail_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易订单表';

-- ============================================================
-- 5. user_favorite 用户收藏表
-- ============================================================
CREATE TABLE `user_favorite` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `goods_id`    BIGINT   NOT NULL COMMENT '商品ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_goods` (`user_id`, `goods_id`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- ============================================================
-- 6. user_browse_history 浏览记录表
-- ============================================================
CREATE TABLE `user_browse_history` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `goods_id`    BIGINT   NOT NULL COMMENT '商品ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_goods` (`user_id`, `goods_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览记录表';

-- ============================================================
-- 7. evaluation 评价表
-- ============================================================
CREATE TABLE `evaluation` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id`      BIGINT      NOT NULL COMMENT '订单ID',
    `goods_id`      BIGINT      NOT NULL COMMENT '商品ID',
    `evaluator_id`  BIGINT      NOT NULL COMMENT '评价人ID',
    `evaluatee_id`  BIGINT      NOT NULL COMMENT '被评价人ID',
    `evaluator_role` TINYINT    NOT NULL COMMENT '评价人角色：1买家评卖家 2卖家评买家',
    `score`         TINYINT     NOT NULL COMMENT '评分：1-5星',
    `content`       VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    `is_anonymous`  TINYINT     NOT NULL DEFAULT 1 COMMENT '是否匿名：0实名 1匿名（双盲展示）',
    `status`        TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0隐藏 1正常 2申诉中 3申诉后隐藏',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_evaluator` (`order_id`, `evaluator_id`),
    KEY `idx_evaluatee_id` (`evaluatee_id`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- ============================================================
-- 8. notification 通知表
-- ============================================================
CREATE TABLE `notification` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT       NOT NULL COMMENT '接收用户ID',
    `type`        TINYINT      NOT NULL COMMENT '通知类型：1预订提醒 2取消提醒 3交易完成 4评价提醒 5联系不上提醒 6卖家已联系 7系统通知',
    `title`       VARCHAR(100) NOT NULL COMMENT '通知标题',
    `content`     VARCHAR(500) NOT NULL COMMENT '通知内容',
    `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    `related_id`  BIGINT       DEFAULT NULL COMMENT '关联业务ID（如订单ID、商品ID）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id_read` (`user_id`, `is_read`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- ============================================================
-- 9. report 举报表
-- ============================================================
CREATE TABLE `report` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `reporter_id`   BIGINT       NOT NULL COMMENT '举报人ID',
    `target_type`   TINYINT      NOT NULL COMMENT '举报对象类型：1用户 2商品',
    `target_id`     BIGINT       NOT NULL COMMENT '举报对象ID',
    `reason`        VARCHAR(200) NOT NULL COMMENT '举报理由（分类）',
    `description`   TEXT         COMMENT '详细描述',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理 1警告 2下架商品 3封禁账号 4驳回',
    `handler_id`    BIGINT       DEFAULT NULL COMMENT '处理人ID（管理员）',
    `handle_result` VARCHAR(500) DEFAULT NULL COMMENT '处理结果说明',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    `handle_time`   DATETIME     DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_reporter_id` (`reporter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';

-- ============================================================
-- 10. sensitive_word 敏感词表
-- ============================================================
CREATE TABLE `sensitive_word` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `word`        VARCHAR(100) NOT NULL COMMENT '敏感词',
    `category`    VARCHAR(50)  DEFAULT NULL COMMENT '分类（如：政治、广告、违禁品）',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

-- ============================================================
-- 11. credit_log 信用分变更日志表
-- 注意：此表只 INSERT，不 UPDATE / DELETE（审计要求）
-- ============================================================
CREATE TABLE `credit_log` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`          BIGINT       NOT NULL COMMENT '用户ID',
    `before_score`     INT          NOT NULL COMMENT '变更前信用分',
    `after_score`      INT          NOT NULL COMMENT '变更后信用分',
    `change_value`     INT          NOT NULL COMMENT '变更分值（正数加分，负数扣分）',
    `reason`           VARCHAR(200) NOT NULL COMMENT '变更原因',
    `related_order_id` BIGINT       DEFAULT NULL COMMENT '关联订单ID',
    `operator_id`      BIGINT       NOT NULL DEFAULT 0 COMMENT '操作人ID（0=系统自动）',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用分变更日志表（只增不改不删）';

-- ============================================================
-- 12. daily_statistics 每日统计聚合表（V1.0可选）
-- ============================================================
CREATE TABLE `daily_statistics` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stat_date`      DATE     NOT NULL COMMENT '统计日期',
    `new_users`      INT      NOT NULL DEFAULT 0 COMMENT '新增用户数',
    `new_goods`      INT      NOT NULL DEFAULT 0 COMMENT '新增商品数',
    `traded_goods`   INT      NOT NULL DEFAULT 0 COMMENT '成交商品数',
    `cancelled_orders` INT    NOT NULL DEFAULT 0 COMMENT '取消订单数',
    `total_users`    INT      NOT NULL DEFAULT 0 COMMENT '累计用户数',
    `total_goods`    INT      NOT NULL DEFAULT 0 COMMENT '累计商品数',
    `total_trades`   INT      NOT NULL DEFAULT 0 COMMENT '累计成交数',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计聚合表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 商品分类（两级）
INSERT INTO `goods_category` (`parent_id`, `name`, `sort_order`) VALUES
-- 一级分类
(0, '教材书籍', 1),
(0, '数码电子', 2),
(0, '生活用品', 3),
(0, '服装鞋包', 4),
(0, '运动器材', 5),
(0, '其他', 6);
-- 假设上面6条分别得到ID 1~6，以下二级分类引用对应的parent_id
INSERT INTO `goods_category` (`parent_id`, `name`, `sort_order`) VALUES
-- 教材书籍(1)
(1, '公共课教材', 1),
(1, '专业课教材', 2),
(1, '考研资料', 3),
(1, '考公资料', 4),
(1, '四六级/考证', 5),
(1, '课外读物', 6),
(1, '其他书籍', 7),
-- 数码电子(2)
(2, '手机', 1),
(2, '电脑/笔记本', 2),
(2, '平板', 3),
(2, '耳机/音箱', 4),
(2, '智能穿戴', 5),
(2, '配件/线材', 6),
(2, '其他数码', 7),
-- 生活用品(3)
(3, '台灯/照明', 1),
(3, '收纳/置物', 2),
(3, '床上用品', 3),
(3, '小家电', 4),
(3, '洗护/日用品', 5),
(3, '其他生活', 6),
-- 服装鞋包(4)
(4, '男装', 1),
(4, '女装', 2),
(4, '鞋子', 3),
(4, '箱包', 4),
(4, '配饰', 5),
-- 运动器材(5)
(5, '球类/球拍', 1),
(5, '健身器材', 2),
(5, '自行车/代步', 3),
(5, '户外装备', 4),
(5, '其他运动', 5),
-- 其他(6)
(6, '其他闲置', 1);

-- 敏感词（基础词库，后续可通过后台管理补充）
INSERT INTO `sensitive_word` (`word`, `category`) VALUES
('违禁品', '违禁品'),
('代写论文', '学术违规'),
('代考', '学术违规'),
('作弊', '学术违规'),
('枪支', '违法'),
('刀具', '违法'),
('假证', '违法'),
('微信号', '广告引流'),
('加微信', '广告引流'),
('QQ群', '广告引流');

-- 管理员账号（密码: admin123，BCrypt加密）
-- 注意：生产环境请修改密码
INSERT INTO `sys_user` (`nickname`, `password`, `role`, `credit_score`, `student_id`, `real_name`) VALUES
('系统管理员', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7pCu6v2XyFDtC4q.jeeGm/TEZyj3C6', 1, 100, 'admin', '管理员');

-- ============================================================
-- 13. im_conversation 私聊会话表
-- ============================================================
CREATE TABLE `im_conversation` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `goods_id`      BIGINT       DEFAULT NULL COMMENT '关联商品ID（可空，从商品页发起时有）',
    `user_a_id`     BIGINT       NOT NULL COMMENT '会话发起方用户ID',
    `user_b_id`     BIGINT       NOT NULL COMMENT '会话接收方用户ID',
    `last_message`  VARCHAR(500) DEFAULT NULL COMMENT '最后一条消息内容（列表预览）',
    `last_time`     DATETIME     DEFAULT NULL COMMENT '最后消息时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_a` (`user_a_id`),
    KEY `idx_user_b` (`user_b_id`),
    KEY `idx_goods` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊会话表';

-- ============================================================
-- 14. im_message 私聊消息表
-- ============================================================
CREATE TABLE `im_message` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` BIGINT       NOT NULL COMMENT '会话ID',
    `sender_id`       BIGINT       NOT NULL COMMENT '发送方用户ID',
    `content`         VARCHAR(500) NOT NULL COMMENT '消息内容（敏感词过滤后存储）',
    `is_read`         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_conv` (`conversation_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊消息表';

-- ============================================================
-- 15. goods_daily_view 商品浏览日统计表
-- ============================================================
CREATE TABLE `goods_daily_view` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `goods_id`   BIGINT   NOT NULL COMMENT '商品ID',
    `stat_date`  DATE     NOT NULL COMMENT '统计日期',
    `view_count` INT      NOT NULL DEFAULT 0 COMMENT '当日浏览量',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_goods_date` (`goods_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品浏览日统计表（热门排序时间衰减用）';

-- ============================================================
-- 验证
-- ============================================================
SELECT '========== 建表完成 ==========' AS result;
SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'campus_market';
