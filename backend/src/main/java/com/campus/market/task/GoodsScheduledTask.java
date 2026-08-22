package com.campus.market.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.market.entity.GoodsInfo;
import com.campus.market.mapper.GoodsInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品定时任务
 *
 * 功能：
 * - 30天自动下架：每天凌晨2点扫描，将在售超过30天的商品自动下架
 *   按 last_relisted_at 字段计算（而非 create_time），确保重新上架后重新计时
 *
 * 技术要点：
 * - @Scheduled(cron) 基于 Spring 的定时任务，无需额外依赖
 * - @EnableScheduling 需要在启动类或配置类上标注（已在 CampusMarketApplication 上启用）
 */
@Slf4j
@Component
public class GoodsScheduledTask {

    @Autowired
    private GoodsInfoMapper goodsInfoMapper;

    @Value("${campus.market.goods.offline-days}")
    private Integer offlineDays;

    /**
     * 30天自动下架
     * cron: 每天凌晨2点执行
     * 0 秒 0 分 2 时 * * ?  （秒 分 时 日 月 周）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoTakedownExpiredGoods() {
        log.info("[定时任务] 开始执行30天自动下架，阈值={}天", offlineDays);

        // 1. 计算截止时间：当前时间 - 30天
        LocalDateTime deadline = LocalDateTime.now().minusDays(offlineDays);

        // 2. 查询需要下架的商品（在售 + 上架时间超过30天）
        List<GoodsInfo> expiredGoods = goodsInfoMapper.selectList(
                new LambdaQueryWrapper<GoodsInfo>()
                        .eq(GoodsInfo::getStatus, 1)  // 在售
                        .lt(GoodsInfo::getLastRelistedAt, deadline)
        );

        if (expiredGoods.isEmpty()) {
            log.info("[定时任务] 没有需要自动下架的商品");
            return;
        }

        log.info("[定时任务] 发现{}个商品需要自动下架", expiredGoods.size());

        // 3. 逐个下架（批量UPDATE，不走乐观锁，因为是系统自动操作无并发冲突）
        int count = 0;
        for (GoodsInfo goods : expiredGoods) {
            goodsInfoMapper.update(null,
                    new LambdaUpdateWrapper<GoodsInfo>()
                            .eq(GoodsInfo::getId, goods.getId())
                            .eq(GoodsInfo::getStatus, 1)  // 保险：只更新在售的
                            .set(GoodsInfo::getStatus, 0)  // 下架
            );
            count++;
            log.info("[定时任务] 商品自动下架: goodsId={}, title={}, lastRelistedAt={}",
                    goods.getId(), goods.getTitle(), goods.getLastRelistedAt());
        }

        log.info("[定时任务] 30天自动下架完成，共下架{}个商品", count);
    }
}
