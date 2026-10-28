/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : mlp_login

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 28/10/2026 09:49:17
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_category
-- ----------------------------
DROP TABLE IF EXISTS `t_category`;
CREATE TABLE `t_category`  (
  `id` bigint NOT NULL COMMENT '主键（手工指定，方便脚本里写关联）',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父分类 id，0=一级分类',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名',
  `level` tinyint NOT NULL DEFAULT 1 COMMENT '层级：1/2/3…',
  `icon` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '图标 emoji',
  `color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '#b79cf0' COMMENT '主题色',
  `intro` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '一句话说明',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序（小的在前）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_t_category_parent`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '小马图鉴分类表（分级，自关联）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_category
-- ----------------------------
INSERT INTO `t_category` VALUES (1, 0, '种族', 1, '🦄', '#b79cf0', '按小马的种族划分，决定他们与生俱来的能力', 1);
INSERT INTO `t_category` VALUES (2, 0, '元素', 1, '💫', '#f7e08a', '谐律精华元素，代表小马的品格与天赋', 2);
INSERT INTO `t_category` VALUES (3, 0, '栖息地', 1, '🏡', '#a7e3c9', '小马们常年生活的城邦与地区', 3);
INSERT INTO `t_category` VALUES (11, 1, '陆马', 2, '🐴', '#f0a06a', '没有角也没有翅膀，靠体力与种植维生', 1);
INSERT INTO `t_category` VALUES (12, 1, '飞马', 2, '🐦', '#8fc7f0', '能站在云上、还负责管理天气', 2);
INSERT INTO `t_category` VALUES (13, 1, '独角兽', 2, '✨', '#b79cf0', '额头有角，能用角施放魔法', 3);
INSERT INTO `t_category` VALUES (14, 1, '天角兽', 2, '👑', '#f7c94c', '同时拥有角和翅膀，通常出身王族', 4);
INSERT INTO `t_category` VALUES (21, 2, '欢笑', 2, '🎈', '#f7a8c4', '用笑容和派对赶走所有坏心情', 1);
INSERT INTO `t_category` VALUES (22, 2, '忠诚', 2, '⚡', '#8fc7f0', '答应过的事，无论多难都会做到', 2);
INSERT INTO `t_category` VALUES (23, 2, '善良', 2, '🦋', '#f7e08a', '对所有生命都温柔以待', 3);
INSERT INTO `t_category` VALUES (24, 2, '慷慨', 2, '💎', '#a8d8f7', '愿意把最好的东西让给别人', 4);
INSERT INTO `t_category` VALUES (25, 2, '诚实', 2, '🍎', '#f0a06a', '从不说谎，一诺千金', 5);
INSERT INTO `t_category` VALUES (26, 2, '魔法', 2, '🌟', '#b79cf0', '友谊本身就是最强大的魔法', 6);
INSERT INTO `t_category` VALUES (31, 3, '小马谷', 2, '🏡', '#a7e3c9', '和平宁静的小镇，主角团的家', 1);
INSERT INTO `t_category` VALUES (32, 3, '坎特洛特', 2, '🏰', '#f7e08a', '王室所在的白色山城，小马国首都', 2);
INSERT INTO `t_category` VALUES (33, 3, '云中城', 2, '☁️', '#8fc7f0', '建在云朵上的飞马之城', 3);
INSERT INTO `t_category` VALUES (34, 3, '水晶帝国', 2, '💠', '#a8d8f7', '北方冰雪中的水晶之城', 4);

-- ----------------------------
-- Table structure for t_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `t_chat_message`;
CREATE TABLE `t_chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_id` bigint NOT NULL COMMENT '发送者 id',
  `to_id` bigint NOT NULL COMMENT '接收者 id',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `read_flag` tinyint NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pair_time`(`from_id` ASC, `to_id` ASC, `id` ASC) USING BTREE,
  INDEX `idx_to_read`(`to_id` ASC, `read_flag` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '好友聊天消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_chat_message
-- ----------------------------

-- ----------------------------
-- Table structure for t_friend
-- ----------------------------
DROP TABLE IF EXISTS `t_friend`;
CREATE TABLE `t_friend`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '申请人 id',
  `friend_id` bigint NOT NULL COMMENT '接收人 id',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0=申请中 1=已通过',
  `relation_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '',
  `apply_msg` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '申请留言',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pair`(`user_id` ASC, `friend_id` ASC) USING BTREE,
  INDEX `idx_friend_status`(`friend_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '好友关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_friend
-- ----------------------------

-- ----------------------------
-- Table structure for t_message
-- ----------------------------
DROP TABLE IF EXISTS `t_message`;
CREATE TABLE `t_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BOARD 留言板 / PM 私信 / NOTIFY 系统通知',
  `from_id` bigint NULL DEFAULT NULL COMMENT '发送者 id（系统通知可为空）',
  `from_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者名字（展示用）',
  `to_id` bigint NULL DEFAULT NULL COMMENT '接收者 id；-1 表示全员广播；留言板为 NULL',
  `to_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '接收者名字（展示用）',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `read_flag` tinyint NOT NULL DEFAULT 0 COMMENT '0 未读 1 已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_t_message_type`(`type` ASC) USING BTREE,
  INDEX `idx_t_message_to`(`to_id` ASC) USING BTREE,
  INDEX `idx_t_message_from`(`from_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '小马王国消息中心（留言板 / 私信 / 系统通知）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_message
-- ----------------------------
INSERT INTO `t_message` VALUES (1, 'NOTIFY', NULL, '小马王国', -1, NULL, '欢迎来到小马王国！点开左上角 💬 消息中心，里面有留言板、私信和系统通知～', 0, '2026-10-28 08:28:53');
INSERT INTO `t_message` VALUES (2, 'NOTIFY', NULL, '小马王国', -1, NULL, '每天来首页抽一只「今日幸运小马」吧 🎲，看看今天谁陪你冒险！', 0, '2026-10-28 08:28:53');
INSERT INTO `t_message` VALUES (3, 'NOTIFY', NULL, '小马王国', 1, NULL, '欢迎来到小马王国，巴啦啦！记得去首页抽你的「今日幸运小马」🎲', 1, '2026-10-28 08:34:08');
INSERT INTO `t_message` VALUES (4, 'BOARD', 1, '巴啦啦', NULL, NULL, '11', 0, '2026-10-28 08:34:54');

-- ----------------------------
-- Table structure for t_pet
-- ----------------------------
DROP TABLE IF EXISTS `t_pet`;
CREATE TABLE `t_pet`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `kind` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '',
  `level` int NULL DEFAULT 1,
  `exp` int NULL DEFAULT 0,
  `hunger` int NULL DEFAULT 50,
  `mood` int NULL DEFAULT 80,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_pet
-- ----------------------------

-- ----------------------------
-- Table structure for t_point_log
-- ----------------------------
DROP TABLE IF EXISTS `t_point_log`;
CREATE TABLE `t_point_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `amount` int NOT NULL,
  `biz` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `remark` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_point_log
-- ----------------------------

-- ----------------------------
-- Table structure for t_pony
-- ----------------------------
DROP TABLE IF EXISTS `t_pony`;
CREATE TABLE `t_pony`  (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '中文名',
  `alias` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '英文名/别名',
  `mark` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'star' COMMENT '可爱标志图案 key',
  `emoji` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '⭐' COMMENT '图案缺失时的 emoji',
  `color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '#b79cf0' COMMENT '招牌色',
  `rarity` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '常见' COMMENT '稀有度：常见/稀有/传说',
  `intro` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '简介',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '被查看次数',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_t_pony_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '小马图鉴条目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_pony
-- ----------------------------
INSERT INTO `t_pony` VALUES (1, '紫悦', 'Twilight Sparkle', 'twilight', '📖', '#b79cf0', '传说', '坎特洛特的学霸小马，从独角兽一路成长为天角兽公主，掌管友谊魔法。', 0, 1, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (2, '碧琪', 'Pinkie Pie', 'pinkie', '🎈', '#f7a8c4', '稀有', '小马谷的派对担当，随时能从鬃毛里掏出派对道具，笑声能传遍整个镇子。', 0, 2, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (3, '云宝', 'Rainbow Dash', 'dash', '⚡', '#8fc7f0', '稀有', '云中城最耀眼的飞行家，彩虹音爆的创造者，梦想加入闪电飞马队。', 0, 3, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (4, '苹果嘉儿', 'Applejack', 'applejack', '🍎', '#f0a06a', '稀有', '甜苹果园的顶梁柱，戴着牛仔帽，说到做到、从不说谎。', 0, 4, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (5, '柔柔', 'Fluttershy', 'fluttershy', '🦋', '#a7e3c9', '稀有', '住在森林小屋的温柔飞马，说话轻声细语，却能让所有小动物听话。', 0, 5, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (6, '珍奇', 'Rarity', 'rarity', '💎', '#f7e08a', '稀有', '旋转木马时装店的主人，坎特洛特的时尚风向标，最爱宝石。', 0, 6, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (7, '宇宙公主', 'Princess Celestia', 'star', '☀️', '#f7c94c', '传说', '小马国最高统治者，升起太阳、掌管白昼，紫悦的老师。', 0, 7, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (8, '露娜公主', 'Princess Luna', 'star', '🌙', '#8f7ae8', '传说', '掌管月亮与梦境的公主，曾被黑暗侵蚀成为梦魇之月，后回归王座。', 0, 8, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (9, '韵律公主', 'Princess Cadance', 'star', '💗', '#f7a8c4', '传说', '水晶帝国的公主，掌管爱情与守护，紫悦小时候的保姆。', 0, 9, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (10, '闪耀盔甲', 'Shining Armor', 'star', '🛡️', '#8fc7f0', '稀有', '皇家护卫队队长、水晶帝国守护者，紫悦的哥哥。', 0, 10, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (11, '星光熠熠', 'Starlight Glimmer', 'star', '⭐', '#b79cf0', '稀有', '曾试图抹掉所有小马的可爱标志，后来成为友谊学校的校长。', 0, 11, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (12, '小苹花', 'Apple Bloom', 'star', '🌼', '#f0a06a', '常见', '苹果嘉儿的妹妹，可爱标志童子军的发起人。', 0, 12, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (13, '甜心宝宝', 'Sweetie Belle', 'star', '🎀', '#f7a8c4', '常见', '珍奇的妹妹，歌声最好听的小独角兽。', 0, 13, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (14, '飞板璐', 'Scootaloo', 'star', '🛴', '#8fc7f0', '常见', '云宝的头号粉丝，骑着滑板车到处飞驰的小飞马。', 0, 14, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (15, '大麦克', 'Big McIntosh', 'star', '🐴', '#f0a06a', '常见', '苹果家的长子，高大沉默，口头禅是「嗯哼」和「不啦」。', 0, 15, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (16, '芝士三明治', 'Cheese Sandwich', 'star', '🥪', '#f7e08a', '常见', '四处旅行的派对小马，碧琪的搭档，最擅长手风琴。', 0, 16, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (17, '纸杯蛋糕夫人', 'Mrs. Cup Cake', 'star', '🧁', '#f5c08a', '常见', '小马谷纸杯蛋糕店的老板，烤的纸杯蛋糕连碧琪都抢着要。', 0, 17, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (18, '马斯卡彭', 'Marzipan Mascarpone', 'star', '🍬', '#c9a0e8', '稀有', '坎特洛特的糖果艺术家，能把糖丝做成礼服。', 0, 18, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (19, '软糖', 'Bon Bon', 'star', '🍭', '#f7a8c4', '常见', '手工糖果小贩，招牌酸角软糖，嘴里总念叨「才不是爱吃糖」。', 0, 19, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (20, '苏里·普罗玛', 'Suri Polomare', 'star', '🎩', '#b8a0d8', '稀有', '从坎特洛特来的皮草设计师，珍奇的竞争对手，嘴甜心硬。', 0, 20, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (21, '霍伊蒂·托蒂', 'Hoity Toity', 'star', '👓', '#a8b8c8', '稀有', '时尚评论家，一句「太寻常了」能让设计师失眠一整周。', 0, 21, '2026-10-28 08:28:53');
INSERT INTO `t_pony` VALUES (22, '可可·潘玛尔', 'Coco Pommel', 'star', '🧣', '#d8a8b8', '常见', '惜字如金的裁缝，缝制的外套走线精致，一针一线都讲究。', 0, 22, '2026-10-28 08:28:53');

-- ----------------------------
-- Table structure for t_pony_category
-- ----------------------------
DROP TABLE IF EXISTS `t_pony_category`;
CREATE TABLE `t_pony_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pony_id` bigint NOT NULL COMMENT 't_pony.id',
  `category_id` bigint NOT NULL COMMENT 't_category.id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pony_category`(`pony_id` ASC, `category_id` ASC) USING BTREE,
  INDEX `idx_pc_category`(`category_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 55 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '小马条目与分类的关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_pony_category
-- ----------------------------
INSERT INTO `t_pony_category` VALUES (1, 1, 13);
INSERT INTO `t_pony_category` VALUES (2, 1, 14);
INSERT INTO `t_pony_category` VALUES (3, 1, 26);
INSERT INTO `t_pony_category` VALUES (4, 1, 32);
INSERT INTO `t_pony_category` VALUES (5, 2, 11);
INSERT INTO `t_pony_category` VALUES (6, 2, 21);
INSERT INTO `t_pony_category` VALUES (7, 2, 31);
INSERT INTO `t_pony_category` VALUES (8, 3, 12);
INSERT INTO `t_pony_category` VALUES (9, 3, 22);
INSERT INTO `t_pony_category` VALUES (10, 3, 33);
INSERT INTO `t_pony_category` VALUES (11, 4, 11);
INSERT INTO `t_pony_category` VALUES (12, 4, 25);
INSERT INTO `t_pony_category` VALUES (13, 4, 31);
INSERT INTO `t_pony_category` VALUES (14, 5, 12);
INSERT INTO `t_pony_category` VALUES (15, 5, 23);
INSERT INTO `t_pony_category` VALUES (16, 5, 31);
INSERT INTO `t_pony_category` VALUES (17, 6, 13);
INSERT INTO `t_pony_category` VALUES (18, 6, 24);
INSERT INTO `t_pony_category` VALUES (19, 6, 32);
INSERT INTO `t_pony_category` VALUES (20, 7, 14);
INSERT INTO `t_pony_category` VALUES (21, 7, 32);
INSERT INTO `t_pony_category` VALUES (22, 8, 14);
INSERT INTO `t_pony_category` VALUES (23, 8, 32);
INSERT INTO `t_pony_category` VALUES (24, 9, 14);
INSERT INTO `t_pony_category` VALUES (25, 9, 34);
INSERT INTO `t_pony_category` VALUES (26, 10, 13);
INSERT INTO `t_pony_category` VALUES (27, 10, 34);
INSERT INTO `t_pony_category` VALUES (28, 11, 13);
INSERT INTO `t_pony_category` VALUES (29, 11, 31);
INSERT INTO `t_pony_category` VALUES (30, 12, 11);
INSERT INTO `t_pony_category` VALUES (31, 12, 31);
INSERT INTO `t_pony_category` VALUES (32, 13, 13);
INSERT INTO `t_pony_category` VALUES (33, 13, 31);
INSERT INTO `t_pony_category` VALUES (34, 14, 12);
INSERT INTO `t_pony_category` VALUES (35, 14, 31);
INSERT INTO `t_pony_category` VALUES (36, 15, 11);
INSERT INTO `t_pony_category` VALUES (37, 15, 31);
INSERT INTO `t_pony_category` VALUES (38, 16, 11);
INSERT INTO `t_pony_category` VALUES (39, 16, 21);
INSERT INTO `t_pony_category` VALUES (40, 16, 31);
INSERT INTO `t_pony_category` VALUES (41, 17, 11);
INSERT INTO `t_pony_category` VALUES (42, 17, 21);
INSERT INTO `t_pony_category` VALUES (43, 17, 31);
INSERT INTO `t_pony_category` VALUES (44, 18, 11);
INSERT INTO `t_pony_category` VALUES (45, 18, 24);
INSERT INTO `t_pony_category` VALUES (46, 18, 32);
INSERT INTO `t_pony_category` VALUES (47, 19, 11);
INSERT INTO `t_pony_category` VALUES (48, 19, 31);
INSERT INTO `t_pony_category` VALUES (49, 20, 11);
INSERT INTO `t_pony_category` VALUES (50, 20, 32);
INSERT INTO `t_pony_category` VALUES (51, 21, 11);
INSERT INTO `t_pony_category` VALUES (52, 21, 32);
INSERT INTO `t_pony_category` VALUES (53, 22, 11);
INSERT INTO `t_pony_category` VALUES (54, 22, 31);

-- ----------------------------
-- Table structure for t_pony_tag
-- ----------------------------
DROP TABLE IF EXISTS `t_pony_tag`;
CREATE TABLE `t_pony_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pony_id` bigint NOT NULL COMMENT 't_pony.id',
  `tag` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关键词',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pony_tag`(`pony_id` ASC, `tag` ASC) USING BTREE,
  INDEX `idx_tag`(`tag` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 115 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '小马条目关键词标签' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_pony_tag
-- ----------------------------
INSERT INTO `t_pony_tag` VALUES (5, 1, '公主');
INSERT INTO `t_pony_tag` VALUES (3, 1, '学霸');
INSERT INTO `t_pony_tag` VALUES (4, 1, '星光');
INSERT INTO `t_pony_tag` VALUES (2, 1, '读书');
INSERT INTO `t_pony_tag` VALUES (1, 1, '魔法');
INSERT INTO `t_pony_tag` VALUES (11, 2, '搞怪');
INSERT INTO `t_pony_tag` VALUES (7, 2, '欢笑');
INSERT INTO `t_pony_tag` VALUES (10, 2, '气球');
INSERT INTO `t_pony_tag` VALUES (6, 2, '派对');
INSERT INTO `t_pony_tag` VALUES (9, 2, '甜品');
INSERT INTO `t_pony_tag` VALUES (8, 2, '甜点');
INSERT INTO `t_pony_tag` VALUES (14, 3, '彩虹');
INSERT INTO `t_pony_tag` VALUES (16, 3, '忠诚');
INSERT INTO `t_pony_tag` VALUES (13, 3, '竞速');
INSERT INTO `t_pony_tag` VALUES (15, 3, '闪电');
INSERT INTO `t_pony_tag` VALUES (12, 3, '飞行');
INSERT INTO `t_pony_tag` VALUES (18, 4, '农场');
INSERT INTO `t_pony_tag` VALUES (21, 4, '力气大');
INSERT INTO `t_pony_tag` VALUES (20, 4, '牛仔');
INSERT INTO `t_pony_tag` VALUES (17, 4, '苹果');
INSERT INTO `t_pony_tag` VALUES (19, 4, '诚实');
INSERT INTO `t_pony_tag` VALUES (26, 5, '害羞');
INSERT INTO `t_pony_tag` VALUES (23, 5, '小动物');
INSERT INTO `t_pony_tag` VALUES (25, 5, '森林');
INSERT INTO `t_pony_tag` VALUES (22, 5, '温柔');
INSERT INTO `t_pony_tag` VALUES (24, 5, '蝴蝶');
INSERT INTO `t_pony_tag` VALUES (30, 6, '优雅');
INSERT INTO `t_pony_tag` VALUES (28, 6, '宝石');
INSERT INTO `t_pony_tag` VALUES (31, 6, '慷慨');
INSERT INTO `t_pony_tag` VALUES (27, 6, '时尚');
INSERT INTO `t_pony_tag` VALUES (29, 6, '设计');
INSERT INTO `t_pony_tag` VALUES (32, 7, '太阳');
INSERT INTO `t_pony_tag` VALUES (36, 7, '温柔');
INSERT INTO `t_pony_tag` VALUES (34, 7, '王族');
INSERT INTO `t_pony_tag` VALUES (33, 7, '白昼');
INSERT INTO `t_pony_tag` VALUES (35, 7, '老师');
INSERT INTO `t_pony_tag` VALUES (38, 8, '夜晚');
INSERT INTO `t_pony_tag` VALUES (37, 8, '月亮');
INSERT INTO `t_pony_tag` VALUES (39, 8, '梦境');
INSERT INTO `t_pony_tag` VALUES (41, 8, '梦魇之月');
INSERT INTO `t_pony_tag` VALUES (40, 8, '王族');
INSERT INTO `t_pony_tag` VALUES (45, 9, '守护');
INSERT INTO `t_pony_tag` VALUES (43, 9, '水晶');
INSERT INTO `t_pony_tag` VALUES (46, 9, '温柔');
INSERT INTO `t_pony_tag` VALUES (42, 9, '爱情');
INSERT INTO `t_pony_tag` VALUES (44, 9, '王族');
INSERT INTO `t_pony_tag` VALUES (50, 10, '哥哥');
INSERT INTO `t_pony_tag` VALUES (47, 10, '守卫');
INSERT INTO `t_pony_tag` VALUES (49, 10, '王族');
INSERT INTO `t_pony_tag` VALUES (48, 10, '盾牌');
INSERT INTO `t_pony_tag` VALUES (51, 10, '队长');
INSERT INTO `t_pony_tag` VALUES (53, 11, '学校');
INSERT INTO `t_pony_tag` VALUES (54, 11, '平等');
INSERT INTO `t_pony_tag` VALUES (55, 11, '校长');
INSERT INTO `t_pony_tag` VALUES (56, 11, '独角兽');
INSERT INTO `t_pony_tag` VALUES (52, 11, '魔法');
INSERT INTO `t_pony_tag` VALUES (59, 12, '可爱标志');
INSERT INTO `t_pony_tag` VALUES (60, 12, '妹妹');
INSERT INTO `t_pony_tag` VALUES (58, 12, '童子军');
INSERT INTO `t_pony_tag` VALUES (57, 12, '苹果');
INSERT INTO `t_pony_tag` VALUES (61, 12, '蝴蝶结');
INSERT INTO `t_pony_tag` VALUES (65, 13, '可爱标志');
INSERT INTO `t_pony_tag` VALUES (63, 13, '唱歌');
INSERT INTO `t_pony_tag` VALUES (66, 13, '妹妹');
INSERT INTO `t_pony_tag` VALUES (62, 13, '歌喉');
INSERT INTO `t_pony_tag` VALUES (64, 13, '童子军');
INSERT INTO `t_pony_tag` VALUES (69, 14, '可爱标志');
INSERT INTO `t_pony_tag` VALUES (67, 14, '滑板车');
INSERT INTO `t_pony_tag` VALUES (68, 14, '童子军');
INSERT INTO `t_pony_tag` VALUES (70, 14, '粉丝');
INSERT INTO `t_pony_tag` VALUES (71, 14, '翅膀');
INSERT INTO `t_pony_tag` VALUES (73, 15, '农场');
INSERT INTO `t_pony_tag` VALUES (76, 15, '力气大');
INSERT INTO `t_pony_tag` VALUES (75, 15, '哥哥');
INSERT INTO `t_pony_tag` VALUES (74, 15, '沉默');
INSERT INTO `t_pony_tag` VALUES (72, 15, '苹果');
INSERT INTO `t_pony_tag` VALUES (78, 16, '手风琴');
INSERT INTO `t_pony_tag` VALUES (79, 16, '旅行');
INSERT INTO `t_pony_tag` VALUES (80, 16, '欢笑');
INSERT INTO `t_pony_tag` VALUES (77, 16, '派对');
INSERT INTO `t_pony_tag` VALUES (81, 16, '美食');
INSERT INTO `t_pony_tag` VALUES (87, 17, '小马谷');
INSERT INTO `t_pony_tag` VALUES (86, 17, '派对');
INSERT INTO `t_pony_tag` VALUES (84, 17, '烘焙');
INSERT INTO `t_pony_tag` VALUES (83, 17, '甜品');
INSERT INTO `t_pony_tag` VALUES (82, 17, '甜点');
INSERT INTO `t_pony_tag` VALUES (85, 17, '纸杯蛋糕');
INSERT INTO `t_pony_tag` VALUES (93, 18, '坎特洛特');
INSERT INTO `t_pony_tag` VALUES (89, 18, '甜品');
INSERT INTO `t_pony_tag` VALUES (88, 18, '甜点');
INSERT INTO `t_pony_tag` VALUES (92, 18, '精致');
INSERT INTO `t_pony_tag` VALUES (90, 18, '糖果');
INSERT INTO `t_pony_tag` VALUES (91, 18, '艺术');
INSERT INTO `t_pony_tag` VALUES (98, 19, '小马谷');
INSERT INTO `t_pony_tag` VALUES (99, 19, '手工');
INSERT INTO `t_pony_tag` VALUES (97, 19, '甜品');
INSERT INTO `t_pony_tag` VALUES (96, 19, '甜点');
INSERT INTO `t_pony_tag` VALUES (94, 19, '糖果');
INSERT INTO `t_pony_tag` VALUES (95, 19, '软糖');
INSERT INTO `t_pony_tag` VALUES (104, 20, '坎特洛特');
INSERT INTO `t_pony_tag` VALUES (100, 20, '时尚');
INSERT INTO `t_pony_tag` VALUES (101, 20, '皮草');
INSERT INTO `t_pony_tag` VALUES (103, 20, '竞争对手');
INSERT INTO `t_pony_tag` VALUES (102, 20, '设计');
INSERT INTO `t_pony_tag` VALUES (108, 21, '坎特洛特');
INSERT INTO `t_pony_tag` VALUES (107, 21, '墨镜');
INSERT INTO `t_pony_tag` VALUES (109, 21, '挑剔');
INSERT INTO `t_pony_tag` VALUES (105, 21, '时尚');
INSERT INTO `t_pony_tag` VALUES (106, 21, '评论');
INSERT INTO `t_pony_tag` VALUES (112, 22, '外套');
INSERT INTO `t_pony_tag` VALUES (113, 22, '小马谷');
INSERT INTO `t_pony_tag` VALUES (114, 22, '手巧');
INSERT INTO `t_pony_tag` VALUES (110, 22, '时尚');
INSERT INTO `t_pony_tag` VALUES (111, 22, '裁缝');

-- ----------------------------
-- Table structure for t_search_word
-- ----------------------------
DROP TABLE IF EXISTS `t_search_word`;
CREATE TABLE `t_search_word`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `word` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '词条',
  `hit_count` int NOT NULL DEFAULT 0 COMMENT '被搜索次数',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '人工置顶权重',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_word`(`word` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '首页热搜词' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_search_word
-- ----------------------------
INSERT INTO `t_search_word` VALUES (1, '魔法', 26, 9);
INSERT INTO `t_search_word` VALUES (2, '飞行', 22, 8);
INSERT INTO `t_search_word` VALUES (3, '派对', 19, 7);
INSERT INTO `t_search_word` VALUES (4, '苹果', 16, 6);
INSERT INTO `t_search_word` VALUES (5, '公主', 13, 5);
INSERT INTO `t_search_word` VALUES (6, '宝石', 10, 4);
INSERT INTO `t_search_word` VALUES (7, '可爱标志', 8, 3);
INSERT INTO `t_search_word` VALUES (8, '云中城', 5, 2);
INSERT INTO `t_search_word` VALUES (9, '苹果嘉儿', 3, 1);
INSERT INTO `t_search_word` VALUES (10, '天角兽', 1, 0);

-- ----------------------------
-- Table structure for t_task_progress
-- ----------------------------
DROP TABLE IF EXISTS `t_task_progress`;
CREATE TABLE `t_task_progress`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `day` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `task_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `progress` int NULL DEFAULT 0,
  `claimed` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_day_task`(`user_id` ASC, `day` ASC, `task_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_task_progress
-- ----------------------------
INSERT INTO `t_task_progress` VALUES (1, 1, '2026-10-28', 'daily_login', 1, 0);

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '小马名字，登录账号',
  `avatar` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '',
  `points` int NULL DEFAULT 0,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SHA-256(salt + 明文) 的小写 hex',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `permission` tinyint NOT NULL DEFAULT 1 COMMENT '1=鏅??鐢ㄦ埛 0=鏅??绠＄悊鍛?2=瓒呯骇绠＄悊鍛',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_user_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '小马王国用户表（没有邮箱、没有手机号）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES (1, '巴啦啦', '', 0, '6a8bddf97b11a3b4b914fc8e7ca2d2d5a58e03dcbbfd7a381801e37cd8c71cf9', '2026-10-28 08:34:08', 1);
INSERT INTO `t_user` VALUES (2, 'admin', '', 0, 'Admin@1234', '2026-10-28 09:39:48', 2);

-- ----------------------------
-- Table structure for t_user_item
-- ----------------------------
DROP TABLE IF EXISTS `t_user_item`;
CREATE TABLE `t_user_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `item_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `count` int NULL DEFAULT 0,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_item`(`user_id` ASC, `item_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_user_item
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
