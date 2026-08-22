package com.campus.market.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一API响应结果
 * 所有后端接口返回统一格式：{ "code": 0, "msg": "success", "data": ... }
 * 对应前端统一处理请求状态
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码：0 = 成功，非0 = 具体业务错误码 */
    private Integer code;

    /** 消息提示 */
    private String msg;

    /** 响应数据 */
    private T data;

    public Result() {
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ================== 成功响应 ==================

    public static <T> Result<T> success() {
        return new Result<>(0, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(0, msg, data);
    }

    // ================== 失败响应 ==================

    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

    // ================== 常用业务错误码 ==================

    /** 参数错误 */
    public static <T> Result<T> badRequest(String msg) {
        return new Result<>(400, msg, null);
    }

    /** 未授权 */
    public static <T> Result<T> unauthorized(String msg) {
        return new Result<>(401, msg, null);
    }

    /** 禁止访问 */
    public static <T> Result<T> forbidden(String msg) {
        return new Result<>(403, msg, null);
    }

    /** 资源不存在 */
    public static <T> Result<T> notFound(String msg) {
        return new Result<>(404, msg, null);
    }

    // ================== 快捷方法 ==================

    public boolean isSuccess() {
        return this.code != null && this.code == 0;
    }
}
