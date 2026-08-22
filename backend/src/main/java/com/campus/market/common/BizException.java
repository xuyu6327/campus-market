package com.campus.market.common;

import lombok.Getter;

/**
 * 业务异常
 * 用于在 Service 层抛出，由 GlobalExceptionHandler 统一捕获并包装为 Result 响应
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    /**
     * 业务异常（默认状态码 500）
     */
    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 业务异常（指定状态码）
     * @param code  状态码，如 400（参数错误）、403（权限不足）、404（资源不存在）
     * @param message 错误消息
     */
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
