package com.campus.market.dto;

import lombok.Data;

/**
 * 后台用户查询DTO
 * 支持按昵称/学号/真实姓名模糊搜索，按状态和角色筛选
 */
@Data
public class AdminUserQueryDTO {

    /** 搜索关键词（昵称/学号/真实姓名） */
    private String keyword;

    /** 账号状态：null全部 0禁用 1正常 */
    private Integer status;

    /** 角色：null全部 0普通用户 1管理员 */
    private Integer role;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;
}
