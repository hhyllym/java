-- ============================================================
--  星糖谷 · 首页图鉴库（分级分类 + 条目 + 标签 + 热搜词）
--  角色名、元素、心印图案、地名与简介均为本项目原创设定，请勿再引入任何既有作品的专有内容。
--  依赖 mlp_login 库（先跑 sql\mlp_login.sql 建库）
--  本脚本可以重复执行：分类/条目是参考资料，每次重建；热搜词只补不删
--  同时用作「原创设定同步脚本」：执行后运行中的图鉴数据即与前端一致
--  执行方式（cmd / Git Bash 里）：
--    "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -uroot -p123456 --default-character-set=utf8mb4 < sql\mlp_home.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `mlp_login`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `mlp_login`;

-- ------------------------------------------------------------
-- 1) 分类表：自关联，parent_id=0 表示一级分类
--    现在用「一级=分类维度 / 二级=具体取值」两层，
--    但结构本身支持任意层级（后端用递归 CTE 取全部子孙分类）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_category` (
    `id`        BIGINT      NOT NULL COMMENT '主键（手工指定，方便脚本里写关联）',
    `parent_id` BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类 id，0=一级分类',
    `name`      VARCHAR(32) NOT NULL COMMENT '分类名',
    `level`     TINYINT     NOT NULL DEFAULT 1 COMMENT '层级：1/2/3…',
    `icon`      VARCHAR(8)  NOT NULL DEFAULT '' COMMENT '图标 emoji',
    `color`     VARCHAR(16) NOT NULL DEFAULT '#b79cf0' COMMENT '主题色',
    `intro`     VARCHAR(120) NOT NULL DEFAULT '' COMMENT '一句话说明',
    `sort_no`   INT         NOT NULL DEFAULT 0 COMMENT '排序（小的在前）',
    PRIMARY KEY (`id`),
    KEY `idx_t_category_parent` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '小马图鉴分类表（分级，自关联）';

-- ------------------------------------------------------------
-- 2) 小马条目表
--    mark ：CutieMark.vue 支持的手绘心印徽记（pinkie/twilight/dash/applejack/fluttershy/rarity），
--           没有对应图案的填 star，前端会改用 emoji 显示
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_pony` (
    `id`         BIGINT       NOT NULL COMMENT '主键',
    `name`       VARCHAR(32)  NOT NULL COMMENT '中文名',
    `alias`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '英文名/别名',
    `mark`       VARCHAR(24)  NOT NULL DEFAULT 'star' COMMENT '心印徽记图案 key',
    `emoji`      VARCHAR(8)   NOT NULL DEFAULT '⭐' COMMENT '图案缺失时的 emoji',
    `color`      VARCHAR(16)  NOT NULL DEFAULT '#b79cf0' COMMENT '招牌色',
    `rarity`     VARCHAR(8)   NOT NULL DEFAULT '常见' COMMENT '稀有度：常见/稀有/传说',
    `intro`      VARCHAR(200) NOT NULL DEFAULT '' COMMENT '简介',
    `view_count` INT          NOT NULL DEFAULT 0 COMMENT '被查看次数',
    `sort_no`    INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_t_pony_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '小马图鉴条目表';

-- ------------------------------------------------------------
-- 3) 条目 ↔ 分类（多对多）：一只小马可以同时属于「云翼驹 + 忠诚 + 流云城」
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_pony_category` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `pony_id`     BIGINT NOT NULL COMMENT 't_pony.id',
    `category_id` BIGINT NOT NULL COMMENT 't_category.id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pony_category` (`pony_id`, `category_id`),
    KEY `idx_pc_category` (`category_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '小马条目与分类的关联表';

-- ------------------------------------------------------------
-- 4) 条目标签（关键词）：搜索时 "泡芙" 搜得到 "派对"，靠的就是这张表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_pony_tag` (
    `id`      BIGINT      NOT NULL AUTO_INCREMENT,
    `pony_id` BIGINT      NOT NULL COMMENT 't_pony.id',
    `tag`     VARCHAR(24) NOT NULL COMMENT '关键词',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pony_tag` (`pony_id`, `tag`),
    KEY `idx_tag` (`tag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '小马条目关键词标签';

-- ------------------------------------------------------------
-- 5) 热搜词（首页展示 + 搜索时自动累加次数）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_search_word` (
    `id`        BIGINT      NOT NULL AUTO_INCREMENT,
    `word`      VARCHAR(32) NOT NULL COMMENT '词条',
    `hit_count` INT         NOT NULL DEFAULT 0 COMMENT '被搜索次数',
    `sort_no`   INT         NOT NULL DEFAULT 0 COMMENT '人工置顶权重',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '首页热搜词';

-- ============================================================
--  灌数据（先清后插，保证重复执行结果一致）
--  只清 id <= 999 的参考资料，不碰 t_user 里的注册账号
-- ============================================================
DELETE FROM `t_pony_category` WHERE `pony_id` <= 999;
DELETE FROM `t_pony_tag`      WHERE `pony_id` <= 999;
DELETE FROM `t_pony`          WHERE `id` <= 999;
DELETE FROM `t_category`      WHERE `id` <= 999;

-- ---------- 一级分类：分类维度 ----------
INSERT INTO `t_category` (`id`,`parent_id`,`name`,`level`,`icon`,`color`,`intro`,`sort_no`) VALUES
 (1, 0, '种族',   1, '🦄', '#b79cf0', '按小马的种族划分，决定他们与生俱来的能力', 1),
 (2, 0, '六色心印',   1, '💫', '#f7e08a', '六色心印：六种心意，代表伙伴的品格与天赋',     2),
 (3, 0, '栖息地', 1, '🏡', '#a7e3c9', '小马们常年生活的城邦与地区',             3);

-- ---------- 二级分类：种族 ----------
INSERT INTO `t_category` (`id`,`parent_id`,`name`,`level`,`icon`,`color`,`intro`,`sort_no`) VALUES
 (11, 1, '原野驹',   2, '🐴', '#f0a06a', '没有角也没有翅膀，靠体力与种植维生',   1),
 (12, 1, '云翼驹',   2, '🐦', '#8fc7f0', '能站在云上、还负责管理天气',           2),
 (13, 1, '星角驹', 2, '✨', '#b79cf0', '额头有角，能用角施放魔法',             3),
 (14, 1, '星翼驹', 2, '👑', '#f7c94c', '同时拥有角和翅膀，通常出身王族',       4);

-- ---------- 二级分类：六色心印 ----------
INSERT INTO `t_category` (`id`,`parent_id`,`name`,`level`,`icon`,`color`,`intro`,`sort_no`) VALUES
 (21, 2, '欢悦', 2, '🎈', '#f7a8c4', '用笑容和派对赶走所有坏心情', 1),
 (22, 2, '守护', 2, '⚡', '#8fc7f0', '答应过的事，无论多难都会做到', 2),
 (23, 2, '柔心', 2, '🦋', '#f7e08a', '对所有生命都温柔以待',       3),
 (24, 2, '分享', 2, '💎', '#a8d8f7', '愿意把最好的东西让给别人',   4),
 (25, 2, '真切', 2, '🍎', '#f0a06a', '从不说谎，一诺千金',         5),
 (26, 2, '星辉', 2, '🌟', '#b79cf0', '六种心意一起亮起时，就是最亮的光',   6);

-- ---------- 二级分类：栖息地 ----------
INSERT INTO `t_category` (`id`,`parent_id`,`name`,`level`,`icon`,`color`,`intro`,`sort_no`) VALUES
 (31, 3, '糖霜镇',   2, '🏡', '#a7e3c9', '和平宁静的小镇，主角团的家',       1),
 (32, 3, '云顶城', 2, '🏰', '#f7e08a', '王室所在的白色山城，星糖谷首都',   2),
 (33, 3, '流云城',   2, '☁️', '#8fc7f0', '建在云朵上的云翼驹之城',             3),
 (34, 3, '冰晶城', 2, '💠', '#a8d8f7', '北方冰雪中的水晶之城',             4);

-- ---------- 小马条目 ----------
INSERT INTO `t_pony` (`id`,`name`,`alias`,`mark`,`emoji`,`color`,`rarity`,`intro`,`view_count`,`sort_no`) VALUES
 (1,  '星澜',     'Lumina Gleam', 'twilight',   '📖', '#b79cf0', '传说', '星辉书阁的管理员，把复杂知识整理成清楚清单，六色心印就是她归纳出来的。', 0, 1),
 (2,  '泡芙',     'Puffette',       'pinkie',     '🎈', '#f7a8c4', '稀有', '糖霜镇的甜点师，随身带着彩纸和糖霜，走到哪儿哪儿就开起小派对。', 0, 2),
 (3,  '云驰',     'Zephyr Cloud',     'dash',       '⚡', '#8fc7f0', '稀有', '流云城的信使队长，一眨眼穿过整片云海，答应过的事从不迟到。',     0, 3),
 (4,  '金穗', 'Harvesta',        'applejack',  '🍎', '#f0a06a', '稀有', '金穗果园的园主，戴一顶草帽，说话算数，最见不得账目含糊。',               0, 4),
 (5,  '茸茸',     'Mossy Fluff',       'fluttershy', '🦋', '#a7e3c9', '稀有', '住在林边小屋的照料员，听得懂小动物的心思，连胆小的松鼠都愿意靠近。',     0, 5),
 (6,  '绮罗',     'Silkara',           'rarity',     '💎', '#f7e08a', '稀有', '缎带工坊的主理人，一匹缎带几颗玻璃珠就能让旧衣焕然一新，最舍得分给朋友。',           0, 6),
 (7,  '曦光院长', 'Matron Solia','star',       '☀️', '#f7c94c', '传说', '星辉学院的创办人，掌管白昼与开学典礼，星澜的老师。',               0, 7),
 (8,  '月汐院长', 'Matron Moona',    'star',       '🌙', '#8f7ae8', '传说', '守夜的导师，替大家看护梦境，曾一度迷失在暗夜里，后来回到学院。',     0, 8),
 (9,  '暖心导师', 'Mentor Aria', 'star',       '💗', '#f7a8c4', '传说', '冰晶城的守护者，擅长调解争执，星澜小时候由她照看。',               0, 9),
 (10, '亮盾卫长', 'Captain Brightshield',    'star',       '🛡️', '#8fc7f0', '稀有', '云顶卫队队长、冰晶城的守门人，星澜的兄长。',                     0, 10),
 (11, '星屑', 'Stardust','star',       '⭐', '#b79cf0', '稀有', '曾主张大家收起心印图案，后来成为星辉学院校长，学会尊重每个人的不同。',           0, 11),
 (12, '小穗芽',   'Sprout',      'star',       '🌼', '#f0a06a', '常见', '金穗的妹妹，心印小队的发起人，总在找自己的拿手本领。',                         0, 12),
 (13, '甜铃铛', 'Bellsy',    'star',       '🎀', '#f7a8c4', '常见', '绮罗的妹妹，歌声最甜的小星角驹。',                               0, 13),
 (14, '小滑车',   'Rollie',        'star',       '🛴', '#8fc7f0', '常见', '云驰的头号粉丝，踩着滑板车到处飞驰的小云翼驹。',                     0, 14),
 (15, '大麦叔',   'Uncle Barley',     'star',       '🐴', '#f0a06a', '常见', '果园家的长子，高大沉默，口头禅是「嗯哼」和「不啦」。',             0, 15),
 (16, '酪酪','Cheesie', 'star',       '🥪', '#f7e08a', '常见', '四处旅行的派对搭档，泡芙的老朋友，最擅长手风琴。',                   0, 16),
 (17, '奶油夫人','Madame Cream', 'star',       '🧁', '#f5c08a', '常见', '糖霜镇纸杯蛋糕店的老板，烤的杯糕连泡芙都抢着要。',             0, 17),
 (18, '糖丝师傅',  'Sugarline','star',    '🍬', '#c9a0e8', '稀有', '云顶城的糖果艺术家，能把糖丝拉成一件小礼服。',                         0, 18),
 (19, '糖豆',      'Jellybean',          'star',      '🍭', '#f7a8c4', '常见', '手工糖果小贩，招牌酸角糖豆，嘴里总念叨「才不是爱吃糖」。',         0, 19),
 (20, '薇拉·绸缎','Vela Silk',   'star',      '🎩', '#b8a0d8', '稀有', '从云顶城来的面料设计师，绮罗的竞争对手，嘴甜心硬。',             0, 20),
 (21, '托比先生','Mr. Toby',     'star',      '👓', '#a8b8c8', '稀有', '时尚评论家，一句「太寻常了」能让设计师失眠一整周。',               0, 21),
 (22, '缕缕', 'Threadie',     'star',      '🧣', '#d8a8b8', '常见', '惜字如金的裁缝，缝制的外套走线精致，一针一线都讲究。',             0, 22);

-- ---------- 条目 ↔ 分类（分级分类搜索的关键：一只小马挂到多个分类节点上） ----------
INSERT INTO `t_pony_category` (`pony_id`,`category_id`) VALUES
 (1,13),(1,14),(1,26),(1,32),          -- 星澜：星角驹/星翼驹 · 魔法 · 云顶城
 (2,11),(2,21),(2,31),                  -- 泡芙：原野驹 · 欢笑 · 糖霜镇
 (3,12),(3,22),(3,33),                  -- 云驰：云翼驹 · 忠诚 · 流云城
 (4,11),(4,25),(4,31),                  -- 金穗：原野驹 · 诚实 · 糖霜镇
 (5,12),(5,23),(5,31),                  -- 茸茸：云翼驹 · 善良 · 糖霜镇
 (6,13),(6,24),(6,32),                  -- 绮罗：星角驹 · 慷慨 · 云顶城
 (7,14),(7,32),                         -- 曦光院长：星翼驹 · 云顶城
 (8,14),(8,32),                         -- 月汐院长：星翼驹 · 云顶城
 (9,14),(9,34),                         -- 暖心导师：星翼驹 · 冰晶城
 (10,13),(10,34),                       -- 亮盾卫长：星角驹 · 冰晶城
 (11,13),(11,31),                       -- 星屑：星角驹 · 糖霜镇
 (12,11),(12,31),                       -- 小穗芽：原野驹 · 糖霜镇
 (13,13),(13,31),                       -- 甜铃铛：星角驹 · 糖霜镇
 (14,12),(14,31),                       -- 小滑车：云翼驹 · 糖霜镇
 (15,11),(15,31),                       -- 大麦叔：原野驹 · 糖霜镇
 (16,11),(16,21),(16,31),               -- 酪酪：原野驹 · 欢笑 · 糖霜镇
 (17,11),(17,21),(17,31),               -- 奶油夫人：原野驹 · 欢笑 · 糖霜镇
 (18,11),(18,24),(18,32),               -- 糖丝师傅：原野驹 · 慷慨 · 云顶城
 (19,11),(19,31),                       -- 糖豆：原野驹 · 糖霜镇
 (20,11),(20,32),                       -- 薇拉·绸缎：原野驹 · 云顶城
 (21,11),(21,32),                       -- 托比先生：原野驹 · 云顶城
 (22,11),(22,31);                       -- 缕缕：原野驹 · 糖霜镇

-- ---------- 条目标签（关键词搜索命中这里） ----------
INSERT INTO `t_pony_tag` (`pony_id`,`tag`) VALUES
 (1,'星辉'),(1,'读书'),(1,'学霸'),(1,'星光'),(1,'院长'),
 (2,'派对'),(2,'欢悦'),(2,'甜点'),(2,'甜品'),(2,'糖霜杯糕'),(2,'搞怪'),
 (3,'飞行'),(3,'竞速'),(3,'彩虹'),(3,'闪电'),(3,'守护'),
 (4,'苹果'),(4,'农场'),(4,'真切'),(4,'牛仔'),(4,'力气大'),
 (5,'温柔'),(5,'小动物'),(5,'嫩叶绒球'),(5,'森林'),(5,'害羞'),
 (6,'时尚'),(6,'宝石'),(6,'设计'),(6,'优雅'),(6,'分享'),
 (7,'太阳'),(7,'白昼'),(7,'王族'),(7,'老师'),(7,'温柔'),
 (8,'月亮'),(8,'夜晚'),(8,'梦境'),(8,'王族'),(8,'暗夜之影'),
 (9,'爱情'),(9,'水晶'),(9,'王族'),(9,'守护'),(9,'温柔'),
 (10,'守卫'),(10,'盾牌'),(10,'王族'),(10,'哥哥'),(10,'队长'),
 (11,'星辉'),(11,'学校'),(11,'平等'),(11,'校长'),(11,'星角驹'),
 (12,'苹果'),(12,'心印小队'),(12,'心印图案'),(12,'妹妹'),(12,'蝴蝶结'),
 (13,'歌喉'),(13,'唱歌'),(13,'心印小队'),(13,'心印图案'),(13,'妹妹'),
 (14,'滑板车'),(14,'心印小队'),(14,'心印图案'),(14,'粉丝'),(14,'翅膀'),
 (15,'苹果'),(15,'农场'),(15,'沉默'),(15,'哥哥'),(15,'力气大'),
 (16,'派对'),(16,'手风琴'),(16,'旅行'),(16,'欢悦'),(16,'美食'),
 (17,'甜点'),(17,'甜品'),(17,'烘焙'),(17,'纸杯蛋糕'),(17,'派对'),(17,'糖霜镇'),
 (18,'甜点'),(18,'甜品'),(18,'糖果'),(18,'艺术'),(18,'精致'),(18,'云顶城'),
 (19,'糖果'),(19,'糖豆'),(19,'甜点'),(19,'甜品'),(19,'糖霜镇'),(19,'手工'),
 (20,'时尚'),(20,'皮草'),(20,'设计'),(20,'竞争对手'),(20,'云顶城'),
 (21,'时尚'),(21,'评论'),(21,'墨镜'),(21,'云顶城'),(21,'挑剔'),
 (22,'时尚'),(22,'裁缝'),(22,'外套'),(22,'糖霜镇'),(22,'手巧');

-- ---------- 热搜词（只补不删，重复执行不会把已有搜索次数清零） ----------
INSERT IGNORE INTO `t_search_word` (`word`,`hit_count`,`sort_no`) VALUES
 ('星辉', 26, 9), ('飞行', 22, 8), ('派对', 19, 7), ('苹果', 16, 6),
 ('院长', 13, 5), ('宝石', 10, 4), ('心印图案', 8, 3), ('流云城', 5, 2),
 ('金穗', 3, 1), ('星翼驹', 1, 0);

-- 说明：旧版遗留的专有名词热搜词已在 2026-11-08 设定迁移时一次性清理完毕；
--       本脚本只补新词、不删用户搜索词，因此不会把旧词重新带回来。

-- ============================================================
--  自检
-- ============================================================
SELECT '分类数' AS 项, COUNT(*) AS 值 FROM `t_category`
UNION ALL SELECT '条目数',       COUNT(*) FROM `t_pony`
UNION ALL SELECT '分类关联数',   COUNT(*) FROM `t_pony_category`
UNION ALL SELECT '标签数',       COUNT(*) FROM `t_pony_tag`
UNION ALL SELECT '热搜词数',     COUNT(*) FROM `t_search_word`
UNION ALL SELECT '注册账号数',   COUNT(*) FROM `t_user`;

-- 分级数据抽查：每个一级分类下面有多少小马（含子分类）
WITH RECURSIVE `sub` AS (
    SELECT `id`, `id` AS `root_id` FROM `t_category` WHERE `parent_id` = 0
    UNION ALL
    SELECT c.`id`, s.`root_id` FROM `t_category` c JOIN `sub` s ON c.`parent_id` = s.`id`
)
SELECT p.`name` AS 一级分类, COUNT(DISTINCT pc.`pony_id`) AS 小马数
FROM `t_category` p
LEFT JOIN `sub` s ON s.`root_id` = p.`id`
LEFT JOIN `t_pony_category` pc ON pc.`category_id` = s.`id`
WHERE p.`parent_id` = 0
GROUP BY p.`id`, p.`name`
ORDER BY p.`id`;
