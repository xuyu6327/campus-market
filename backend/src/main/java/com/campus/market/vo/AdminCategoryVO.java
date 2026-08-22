package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台分类管理VO
 */
@Data
public class AdminCategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类ID */
    private Long id;

    /** 父分类ID（0为一级分类） */
    private Long parentId;

    /** 父分类名称（一级分类为null） */
    private String parentName;

    /** 分类名称 */
    private String name;

    /** 分类图标URL */
    private String icon;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：0禁用 1启用 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    /** 该分类下商品数量 */
    private Integer goodsCount;

    /** 创建时间 */
    private LocalDateTime createTime;
}
