-- 二级分类补充（安全：NOT EXISTS 防止重复）
-- 在 Navicat 中运行此文件即可

INSERT INTO `goods_category` (`parent_id`, `name`, `sort_order`)
SELECT c.category_id, sub.name, sub.sort
FROM goods_category c
CROSS JOIN (
    SELECT '公共课教材' AS name, 1 AS sort UNION SELECT '专业课教材', 2 UNION
    SELECT '考研资料', 3 UNION SELECT '考公资料', 4 UNION
    SELECT '四六级/考证', 5 UNION SELECT '课外读物', 6 UNION SELECT '其他书籍', 7
) sub
WHERE c.name = '教材书籍' AND c.parent_id = 0
AND NOT EXISTS (SELECT 1 FROM goods_category WHERE parent_id = c.category_id AND name = sub.name);

INSERT INTO `goods_category` (`parent_id`, `name`, `sort_order`)
SELECT c.category_id, sub.name, sub.sort
FROM goods_category c
CROSS JOIN (
    SELECT '手机' AS name, 1 AS sort UNION SELECT '电脑/笔记本', 2 UNION
    SELECT '平板', 3 UNION SELECT '耳机/音箱', 4 UNION
    SELECT '智能穿戴', 5 UNION SELECT '配件/线材', 6 UNION SELECT '其他数码', 7
) sub
WHERE c.name = '数码电子' AND c.parent_id = 0
AND NOT EXISTS (SELECT 1 FROM goods_category WHERE parent_id = c.category_id AND name = sub.name);

INSERT INTO `goods_category` (`parent_id`, `name`, `sort_order`)
SELECT c.category_id, sub.name, sub.sort
FROM goods_category c
CROSS JOIN (
    SELECT '台灯/照明' AS name, 1 AS sort UNION SELECT '收纳/置物', 2 UNION
    SELECT '床上用品', 3 UNION SELECT '小家电', 4 UNION
    SELECT '洗护/日用品', 5 UNION SELECT '其他生活', 6
) sub
WHERE c.name = '生活用品' AND c.parent_id = 0
AND NOT EXISTS (SELECT 1 FROM goods_category WHERE parent_id = c.category_id AND name = sub.name);

INSERT INTO `goods_category` (`parent_id`, `name`, `sort_order`)
SELECT c.category_id, sub.name, sub.sort
FROM goods_category c
CROSS JOIN (
    SELECT '男装' AS name, 1 AS sort UNION SELECT '女装', 2 UNION
    SELECT '鞋子', 3 UNION SELECT '箱包', 4 UNION SELECT '配饰', 5
) sub
WHERE c.name = '服装鞋包' AND c.parent_id = 0
AND NOT EXISTS (SELECT 1 FROM goods_category WHERE parent_id = c.category_id AND name = sub.name);

INSERT INTO `goods_category` (`parent_id`, `name`, `sort_order`)
SELECT c.category_id, sub.name, sub.sort
FROM goods_category c
CROSS JOIN (
    SELECT '球类/球拍' AS name, 1 AS sort UNION SELECT '健身器材', 2 UNION
    SELECT '自行车/代步', 3 UNION SELECT '户外装备', 4 UNION SELECT '其他运动', 5
) sub
WHERE c.name = '运动器材' AND c.parent_id = 0
AND NOT EXISTS (SELECT 1 FROM goods_category WHERE parent_id = c.category_id AND name = sub.name);

INSERT INTO `goods_category` (`parent_id`, `name`, `sort_order`)
SELECT c.category_id, '其他闲置', 1
FROM goods_category c
WHERE c.name = '其他' AND c.parent_id = 0
AND NOT EXISTS (SELECT 1 FROM goods_category WHERE parent_id = c.category_id AND name = '其他闲置');
