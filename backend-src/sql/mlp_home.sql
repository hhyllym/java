-- ============================================================
--  小马王国 · 首页图鉴库（分级分类 + 条目 + 标签 + 热搜词）
--  依赖 mlp_login 库（先跑 sql\mlp_login.sql 建库）
--  本脚本可以重复执行：分类/条目是参考资料，每次重建；热搜词只补不删
--  执行方式（cmd / Git Bash 里）：
--    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 --default-character-set=utf8mb4 < sql\mlp_home.sql
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
--    mark ：CutieMark.vue 支持的手绘可爱标志（pinkie/twilight/dash/applejack/fluttershy/rarity），
--           没有对应图案的填 star，前端会改用 emoji 显示
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_pony` (
    `id`         BIGINT       NOT NULL COMMENT '主键',
    `name`       VARCHAR(32)  NOT NULL COMMENT '中文名',
    `alias`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '英文名/别名',
    `mark`       VARCHAR(24)  NOT NULL DEFAULT 'star' COMMENT '可爱标志图案 key',
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
-- 3) 条目 ↔ 分类（多对多）：一只小马可以同时属于「飞马 + 忠诚 + 云中城」
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
-- 4) 条目标签（关键词）：搜索时 "碧琪" 搜得到 "派对"，靠的就是这张表
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
 (2, 0, '元素',   1, '💫', '#f7e08a', '谐律精华元素，代表小马的品格与天赋',     2),
 (3, 0, '栖息地', 1, '🏡', '#a7e3c9', '小马们常年生活的城邦与地区',             3);

-- ---------- 二级分类：种族 ----------
INSERT INTO `t_category` (`id`,`parent_id`,`name`,`level`,`icon`,`color`,`intro`,`sort_no`) VALUES
 (11, 1, '陆马',   2, '🐴', '#f0a06a', '没有角也没有翅膀，靠体力与种植维生',   1),
 (12, 1, '飞马',   2, '🐦', '#8fc7f0', '能站在云上、还负责管理天气',           2),
 (13, 1, '独角兽', 2, '✨', '#b79cf0', '额头有角，能用角施放魔法',             3),
 (14, 1, '天角兽', 2, '👑', '#f7c94c', '同时拥有角和翅膀，通常出身王族',       4);

-- ---------- 二级分类：元素 ----------
INSERT INTO `t_category` (`id`,`parent_id`,`name`,`level`,`icon`,`color`,`intro`,`sort_no`) VALUES
 (21, 2, '欢笑', 2, '🎈', '#f7a8c4', '用笑容和派对赶走所有坏心情', 1),
 (22, 2, '忠诚', 2, '⚡', '#8fc7f0', '答应过的事，无论多难都会做到', 2),
 (23, 2, '善良', 2, '🦋', '#f7e08a', '对所有生命都温柔以待',       3),
 (24, 2, '慷慨', 2, '💎', '#a8d8f7', '愿意把最好的东西让给别人',   4),
 (25, 2, '诚实', 2, '🍎', '#f0a06a', '从不说谎，一诺千金',         5),
 (26, 2, '魔法', 2, '🌟', '#b79cf0', '友谊本身就是最强大的魔法',   6);

-- ---------- 二级分类：栖息地 ----------
INSERT INTO `t_category` (`id`,`parent_id`,`name`,`level`,`icon`,`color`,`intro`,`sort_no`) VALUES
 (31, 3, '小马谷',   2, '🏡', '#a7e3c9', '和平宁静的小镇，主角团的家',       1),
 (32, 3, '坎特洛特', 2, '🏰', '#f7e08a', '王室所在的白色山城，小马国首都',   2),
 (33, 3, '云中城',   2, '☁️', '#8fc7f0', '建在云朵上的飞马之城',             3),
 (34, 3, '水晶帝国', 2, '💠', '#a8d8f7', '北方冰雪中的水晶之城',             4);

-- ---------- 小马条目 ----------
INSERT INTO `t_pony` (`id`,`name`,`alias`,`mark`,`emoji`,`color`,`rarity`,`intro`,`view_count`,`sort_no`) VALUES
 (1,  '紫悦',     'Twilight Sparkle', 'twilight',   '📖', '#b79cf0', '传说', '坎特洛特的学霸小马，从独角兽一路成长为天角兽公主，掌管友谊魔法。', 0, 1),
 (2,  '碧琪',     'Pinkie Pie',       'pinkie',     '🎈', '#f7a8c4', '稀有', '小马谷的派对担当，随时能从鬃毛里掏出派对道具，笑声能传遍整个镇子。', 0, 2),
 (3,  '云宝',     'Rainbow Dash',     'dash',       '⚡', '#8fc7f0', '稀有', '云中城最耀眼的飞行家，彩虹音爆的创造者，梦想加入闪电飞马队。',     0, 3),
 (4,  '苹果嘉儿', 'Applejack',        'applejack',  '🍎', '#f0a06a', '稀有', '甜苹果园的顶梁柱，戴着牛仔帽，说到做到、从不说谎。',               0, 4),
 (5,  '柔柔',     'Fluttershy',       'fluttershy', '🦋', '#a7e3c9', '稀有', '住在森林小屋的温柔飞马，说话轻声细语，却能让所有小动物听话。',     0, 5),
 (6,  '珍奇',     'Rarity',           'rarity',     '💎', '#f7e08a', '稀有', '旋转木马时装店的主人，坎特洛特的时尚风向标，最爱宝石。',           0, 6),
 (7,  '宇宙公主', 'Princess Celestia','star',       '☀️', '#f7c94c', '传说', '小马国最高统治者，升起太阳、掌管白昼，紫悦的老师。',               0, 7),
 (8,  '露娜公主', 'Princess Luna',    'star',       '🌙', '#8f7ae8', '传说', '掌管月亮与梦境的公主，曾被黑暗侵蚀成为梦魇之月，后回归王座。',     0, 8),
 (9,  '韵律公主', 'Princess Cadance', 'star',       '💗', '#f7a8c4', '传说', '水晶帝国的公主，掌管爱情与守护，紫悦小时候的保姆。',               0, 9),
 (10, '闪耀盔甲', 'Shining Armor',    'star',       '🛡️', '#8fc7f0', '稀有', '皇家护卫队队长、水晶帝国守护者，紫悦的哥哥。',                     0, 10),
 (11, '星光熠熠', 'Starlight Glimmer','star',       '⭐', '#b79cf0', '稀有', '曾试图抹掉所有小马的可爱标志，后来成为友谊学校的校长。',           0, 11),
 (12, '小苹花',   'Apple Bloom',      'star',       '🌼', '#f0a06a', '常见', '苹果嘉儿的妹妹，可爱标志童子军的发起人。',                         0, 12),
 (13, '甜心宝宝', 'Sweetie Belle',    'star',       '🎀', '#f7a8c4', '常见', '珍奇的妹妹，歌声最好听的小独角兽。',                               0, 13),
 (14, '飞板璐',   'Scootaloo',        'star',       '🛴', '#8fc7f0', '常见', '云宝的头号粉丝，骑着滑板车到处飞驰的小飞马。',                     0, 14),
 (15, '大麦克',   'Big McIntosh',     'star',       '🐴', '#f0a06a', '常见', '苹果家的长子，高大沉默，口头禅是「嗯哼」和「不啦」。',             0, 15),
 (16, '芝士三明治','Cheese Sandwich', 'star',       '🥪', '#f7e08a', '常见', '四处旅行的派对小马，碧琪的搭档，最擅长手风琴。',                   0, 16);

-- ---------- 条目 ↔ 分类（分级分类搜索的关键：一只小马挂到多个分类节点上） ----------
INSERT INTO `t_pony_category` (`pony_id`,`category_id`) VALUES
 (1,13),(1,14),(1,26),(1,32),          -- 紫悦：独角兽/天角兽 · 魔法 · 坎特洛特
 (2,11),(2,21),(2,31),                  -- 碧琪：陆马 · 欢笑 · 小马谷
 (3,12),(3,22),(3,33),                  -- 云宝：飞马 · 忠诚 · 云中城
 (4,11),(4,25),(4,31),                  -- 苹果嘉儿：陆马 · 诚实 · 小马谷
 (5,12),(5,23),(5,31),                  -- 柔柔：飞马 · 善良 · 小马谷
 (6,13),(6,24),(6,32),                  -- 珍奇：独角兽 · 慷慨 · 坎特洛特
 (7,14),(7,32),                         -- 宇宙公主：天角兽 · 坎特洛特
 (8,14),(8,32),                         -- 露娜公主：天角兽 · 坎特洛特
 (9,14),(9,34),                         -- 韵律公主：天角兽 · 水晶帝国
 (10,13),(10,34),                       -- 闪耀盔甲：独角兽 · 水晶帝国
 (11,13),(11,31),                       -- 星光熠熠：独角兽 · 小马谷
 (12,11),(12,31),                       -- 小苹花：陆马 · 小马谷
 (13,13),(13,31),                       -- 甜心宝宝：独角兽 · 小马谷
 (14,12),(14,31),                       -- 飞板璐：飞马 · 小马谷
 (15,11),(15,31),                       -- 大麦克：陆马 · 小马谷
 (16,11),(16,21),(16,31);               -- 芝士三明治：陆马 · 欢笑 · 小马谷

-- ---------- 条目标签（关键词搜索命中这里） ----------
INSERT INTO `t_pony_tag` (`pony_id`,`tag`) VALUES
 (1,'魔法'),(1,'读书'),(1,'学霸'),(1,'星光'),(1,'公主'),
 (2,'派对'),(2,'欢笑'),(2,'甜点'),(2,'气球'),(2,'搞怪'),
 (3,'飞行'),(3,'竞速'),(3,'彩虹'),(3,'闪电'),(3,'忠诚'),
 (4,'苹果'),(4,'农场'),(4,'诚实'),(4,'牛仔'),(4,'力气大'),
 (5,'温柔'),(5,'小动物'),(5,'蝴蝶'),(5,'森林'),(5,'害羞'),
 (6,'时尚'),(6,'宝石'),(6,'设计'),(6,'优雅'),(6,'慷慨'),
 (7,'太阳'),(7,'白昼'),(7,'王族'),(7,'老师'),(7,'温柔'),
 (8,'月亮'),(8,'夜晚'),(8,'梦境'),(8,'王族'),(8,'梦魇之月'),
 (9,'爱情'),(9,'水晶'),(9,'王族'),(9,'守护'),(9,'温柔'),
 (10,'守卫'),(10,'盾牌'),(10,'王族'),(10,'哥哥'),(10,'队长'),
 (11,'魔法'),(11,'学校'),(11,'平等'),(11,'校长'),(11,'独角兽'),
 (12,'苹果'),(12,'童子军'),(12,'可爱标志'),(12,'妹妹'),(12,'蝴蝶结'),
 (13,'歌喉'),(13,'唱歌'),(13,'童子军'),(13,'可爱标志'),(13,'妹妹'),
 (14,'滑板车'),(14,'童子军'),(14,'可爱标志'),(14,'粉丝'),(14,'翅膀'),
 (15,'苹果'),(15,'农场'),(15,'沉默'),(15,'哥哥'),(15,'力气大'),
 (16,'派对'),(16,'手风琴'),(16,'旅行'),(16,'欢笑'),(16,'美食');

-- ---------- 热搜词（只补不删，重复执行不会把已有搜索次数清零） ----------
INSERT IGNORE INTO `t_search_word` (`word`,`hit_count`,`sort_no`) VALUES
 ('魔法', 26, 9), ('飞行', 22, 8), ('派对', 19, 7), ('苹果', 16, 6),
 ('公主', 13, 5), ('宝石', 10, 4), ('可爱标志', 8, 3), ('云中城', 5, 2),
 ('苹果嘉儿', 3, 1), ('天角兽', 1, 0);

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
