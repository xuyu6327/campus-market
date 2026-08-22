package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类 VO
 * 用于前端展示分类列表
 */
@Data
public class CategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类ID */
    private Long id;

    /** 父分类ID（0为一级分类） */
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 分类图标URL */
    private String icon;

    /** 排序值 */
    private Integer sortOrder;
}
