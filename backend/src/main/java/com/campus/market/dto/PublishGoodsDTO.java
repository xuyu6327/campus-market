package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 发布商品 DTO
 * 对应接口：POST /goods
 *
 * 验证规则：
 * - 标题：1-100字符
 * - 价格：> 0，最多两位小数
 * - 分类ID：必填
 * - 成色：1-5
 * - 图片：至少1张，最多9张
 * - 描述：最多500字符
 */
@Data
public class PublishGoodsDTO {

    /** 商品标题 */
    @NotBlank(message = "商品标题不能为空")
    @Size(max = 100, message = "商品标题最多100字符")
    private String title;

    /** 商品描述 */
    @Size(max = 500, message = "商品描述最多500字符")
    private String description;

    /** 分类ID */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /** 售价 */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    /** 原价（展示用，可选） */
    @DecimalMin(value = "0.01", message = "原价必须大于0")
    private BigDecimal originalPrice;

    /** 成色：1全新 2几乎全新 3轻微使用痕迹 4明显使用痕迹 5严重使用痕迹 6故障品 */
    @NotNull(message = "成色不能为空")
    @Min(value = 1, message = "成色值无效")
    @Max(value = 6, message = "成色值无效")
    private Integer goodsCondition;

    /** 图片URL列表 */
    @NotEmpty(message = "至少上传1张图片")
    @Size(max = 9, message = "最多上传9张图片")
    private List<String> images;

    /** 交易地点 */
    @Size(max = 200, message = "交易地点最多200字符")
    private String tradeLocation;

    /** 联系方式类型：1手机 2QQ 3微信 */
    @NotNull(message = "联系方式类型不能为空")
    @Min(value = 1, message = "联系方式类型无效")
    @Max(value = 3, message = "联系方式类型无效")
    private Integer contactMethod;

    /** 联系QQ号（contact_method=2时必填） */
    private String contactQq;

    /** 联系微信号（contact_method=3时必填） */
    private String contactWechat;

    /** 联系手机号（contact_method=1时必填，明文传入，后端加密存储） */
    private String contactPhone;
}
