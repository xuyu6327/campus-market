package com.campus.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.market.entity.SensitiveWord;
import com.campus.market.mapper.SensitiveWordMapper;
import com.campus.market.service.SensitiveWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 敏感词过滤实现（DFA 算法）
 *
 * 数据结构：
 * - 用 Trie 树（字典树）存储敏感词，每个字符一个节点
 * - 匹配时逐字符扫描文本，命中节点且标记为词尾则命中敏感词
 *
 * 线程安全：
 * - 词库树用 volatile 引用，refresh 时构建新树整体替换
 * - 读操作（containsSensitive/filter）不需要加锁
 */
@Slf4j
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    /** DFA Trie 节点 */
    private static class TrieNode {
        boolean end; // 是否为词尾
        Map<Character, TrieNode> children = new HashMap<>();
    }

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    /** 词库树根节点（volatile 保证可见性） */
    private volatile TrieNode root = new TrieNode();

    /** 词库词条数 */
    private volatile int wordCount = 0;

    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * 每小时刷新词库（0分0秒）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void scheduledRefresh() {
        refresh();
    }

    @Override
    public synchronized void refresh() {
        try {
            List<SensitiveWord> words = sensitiveWordMapper.selectList(
                    new LambdaQueryWrapper<SensitiveWord>()
                            .eq(SensitiveWord::getStatus, 1)
            );

            TrieNode newRoot = new TrieNode();
            int count = 0;
            for (SensitiveWord sw : words) {
                if (sw.getWord() == null || sw.getWord().isEmpty()) {
                    continue;
                }
                insertWord(newRoot, sw.getWord());
                count++;
            }

            this.root = newRoot;
            this.wordCount = count;
            log.info("[敏感词] 词库刷新完成，共 {} 个敏感词", count);
        } catch (Exception e) {
            log.error("[敏感词] 词库刷新失败", e);
        }
    }

    @Override
    public boolean containsSensitive(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        TrieNode currentRoot = this.root;
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            TrieNode node = currentRoot;
            for (int j = i; j < chars.length; j++) {
                node = node.children.get(chars[j]);
                if (node == null) {
                    break;
                }
                if (node.end) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        TrieNode currentRoot = this.root;
        char[] chars = text.toCharArray();
        StringBuilder result = new StringBuilder(text);
        for (int i = 0; i < chars.length; i++) {
            TrieNode node = currentRoot;
            int matchLen = 0;
            for (int j = i; j < chars.length; j++) {
                node = node.children.get(chars[j]);
                if (node == null) {
                    break;
                }
                matchLen++;
                if (node.end) {
                    // 替换 [i, i+matchLen) 为 ***
                    for (int k = i; k < i + matchLen; k++) {
                        result.setCharAt(k, '*');
                    }
                    i = i + matchLen - 1;
                    break;
                }
            }
        }
        return result.toString();
    }

    /**
     * 插入敏感词到 Trie 树
     */
    private void insertWord(TrieNode rootNode, String word) {
        TrieNode node = rootNode;
        for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.end = true;
    }
}
