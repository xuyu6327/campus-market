package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 后台分类新增/编辑DTO
 */
@Data
public class AdminCategoryDTO {

    /** 父分类ID（0为一级分类） */
    @NotNull(message = "父分类ID不能为空")
    private Long parentId;

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /** 分类图标URL */
    private String icon;

    /** 排序（越小越靠前） */
    private Integer sortOrder;

    /** 状态：0禁用 1启用 */
    private Integer status;
}
