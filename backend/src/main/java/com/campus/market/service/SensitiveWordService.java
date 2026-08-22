package com.campus.market.service;

/**
 * 敏感词过滤服务
 *
 * 功能：
 * - containsSensitive: 判断文本是否包含敏感词
 * - filter: 将文本中的敏感词替换为 ***
 * - refresh: 重新加载词库（供定时任务调用）
 */
public interface SensitiveWordService {

    /**
     * 判断文本是否包含敏感词
     */
    boolean containsSensitive(String text);

    /**
     * 过滤敏感词，替换为 ***
     */
    String filter(String text);

    /**
     * 重新加载词库
     */
    void refresh();
}
