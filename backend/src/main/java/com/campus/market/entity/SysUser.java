package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表 sys_user
 *
 * 关键设计：
 * - phone 字段存储 AES-GCM 密文，phone_hash 存储 HMAC-SHA256 盲索引
 * - password 字段存储 BCrypt 哈希
 * - role: 0=普通用户, 1=管理员
 * - credit_score: 初始 100，范围 0-200
 * - version: 乐观锁版本号，配合 @Version 注解
 * - deleted: 逻辑删除标记
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 微信openid（小程序登录用，Web端暂不使用） */
    private String openid;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号（AES-GCM加密存储） */
    private String phone;

    /** 手机号HMAC-SHA256盲索引（用于等值查询） */
    private String phoneHash;

    /** 密码（BCrypt加密） */
    private String password;

    /** 学号 */
    private String studentId;

    /** 真实姓名 */
    private String realName;

    /** QQ号 */
    private String qq;

    /** 微信号 */
    private String wechat;

    /** 信用分（初始100） */
    private Integer creditScore;

    /** 角色：0普通用户 1管理员 */
    private Integer role;

    /** 注销状态：0正常 1待注销 */
    private Integer cancelStatus;

    /** 申请注销时间（7天后执行） */
    private LocalDateTime cancelAt;

    /** 账号状态：0禁用 1正常 */
    private Integer status;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;

    /** 注册时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
