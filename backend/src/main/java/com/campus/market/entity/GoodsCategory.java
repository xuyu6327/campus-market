package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类实体类
 * 对应数据库表 goods_category
 *
 * 设计说明：
 * - 两级分类结构，parent_id=0 为一级分类
 * - 该表为基础数据表，不做逻辑删除，不使用乐观锁
 * - 分类数据量小，可直接全量缓存到 Redis（后续优化）
 */
@Data
@TableName("goods_category")
public class GoodsCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类ID（0为一级分类） */
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 分类图标URL */
    private String icon;

    /** 排序（越小越靠前） */
    private Integer sortOrder;

    /** 状态：0禁用 1启用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
