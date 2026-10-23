-- MySQL dump 10.13  Distrib 8.0.39, for Win64 (x86_64)
--
-- Host: localhost    Database: mlp_login
-- ------------------------------------------------------
-- Server version	8.0.39

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `t_category`
--

DROP TABLE IF EXISTS `t_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_category` (
  `id` bigint NOT NULL COMMENT '主键（手工指定，方便脚本里写关联）',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父分类 id，0=一级分类',
  `name` varchar(32) NOT NULL COMMENT '分类名',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '层级：1/2/3…',
  `icon` varchar(8) NOT NULL DEFAULT '' COMMENT '图标 emoji',
  `color` varchar(16) NOT NULL DEFAULT '#b79cf0' COMMENT '主题色',
  `intro` varchar(120) NOT NULL DEFAULT '' COMMENT '一句话说明',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '排序（小的在前）',
  PRIMARY KEY (`id`),
  KEY `idx_t_category_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小马图鉴分类表（分级，自关联）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_category`
--

LOCK TABLES `t_category` WRITE;
/*!40000 ALTER TABLE `t_category` DISABLE KEYS */;
INSERT INTO `t_category` VALUES (1,0,'种族',1,'🦄','#b79cf0','按小马的种族划分，决定他们与生俱来的能力',1),(2,0,'元素',1,'💫','#f7e08a','谐律精华元素，代表小马的品格与天赋',2),(3,0,'栖息地',1,'🏡','#a7e3c9','小马们常年生活的城邦与地区',3),(11,1,'陆马',2,'🐴','#f0a06a','没有角也没有翅膀，靠体力与种植维生',1),(12,1,'飞马',2,'🐦','#8fc7f0','能站在云上、还负责管理天气',2),(13,1,'独角兽',2,'✨','#b79cf0','额头有角，能用角施放魔法',3),(14,1,'天角兽',2,'👑','#f7c94c','同时拥有角和翅膀，通常出身王族',4),(21,2,'欢笑',2,'🎈','#f7a8c4','用笑容和派对赶走所有坏心情',1),(22,2,'忠诚',2,'⚡','#8fc7f0','答应过的事，无论多难都会做到',2),(23,2,'善良',2,'🦋','#f7e08a','对所有生命都温柔以待',3),(24,2,'慷慨',2,'💎','#a8d8f7','愿意把最好的东西让给别人',4),(25,2,'诚实',2,'🍎','#f0a06a','从不说谎，一诺千金',5),(26,2,'魔法',2,'🌟','#b79cf0','友谊本身就是最强大的魔法',6),(31,3,'小马谷',2,'🏡','#a7e3c9','和平宁静的小镇，主角团的家',1),(32,3,'坎特洛特',2,'🏰','#f7e08a','王室所在的白色山城，小马国首都',2),(33,3,'云中城',2,'☁️','#8fc7f0','建在云朵上的飞马之城',3),(34,3,'水晶帝国',2,'💠','#a8d8f7','北方冰雪中的水晶之城',4);
/*!40000 ALTER TABLE `t_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_chat_message`
--

DROP TABLE IF EXISTS `t_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_id` bigint NOT NULL COMMENT '发送者 id',
  `to_id` bigint NOT NULL COMMENT '接收者 id',
  `content` varchar(500) NOT NULL COMMENT '消息内容',
  `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '0=未读 1=已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pair_time` (`from_id`,`to_id`,`id`),
  KEY `idx_to_read` (`to_id`,`read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友聊天消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_chat_message`
--

LOCK TABLES `t_chat_message` WRITE;
/*!40000 ALTER TABLE `t_chat_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_chat_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_friend`
--

DROP TABLE IF EXISTS `t_friend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_friend` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '申请人 id',
  `friend_id` bigint NOT NULL COMMENT '接收人 id',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=申请中 1=已通过',
  `relation_type` varchar(16) DEFAULT '',
  `apply_msg` varchar(100) NOT NULL DEFAULT '' COMMENT '申请留言',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pair` (`user_id`,`friend_id`),
  KEY `idx_friend_status` (`friend_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_friend`
--

LOCK TABLES `t_friend` WRITE;
/*!40000 ALTER TABLE `t_friend` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_friend` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_message`
--

DROP TABLE IF EXISTS `t_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` varchar(10) NOT NULL COMMENT 'BOARD 留言板 / PM 私信 / NOTIFY 系统通知',
  `from_id` bigint DEFAULT NULL COMMENT '发送者 id（系统通知可为空）',
  `from_name` varchar(50) DEFAULT NULL COMMENT '发送者名字（展示用）',
  `to_id` bigint DEFAULT NULL COMMENT '接收者 id；-1 表示全员广播；留言板为 NULL',
  `to_name` varchar(50) DEFAULT NULL COMMENT '接收者名字（展示用）',
  `content` varchar(500) NOT NULL COMMENT '消息内容',
  `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '0 未读 1 已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_t_message_type` (`type`),
  KEY `idx_t_message_to` (`to_id`),
  KEY `idx_t_message_from` (`from_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小马王国消息中心（留言板 / 私信 / 系统通知）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_message`
--

LOCK TABLES `t_message` WRITE;
/*!40000 ALTER TABLE `t_message` DISABLE KEYS */;
INSERT INTO `t_message` VALUES (1,'NOTIFY',NULL,'小马王国',-1,NULL,'欢迎来到小马王国！点开左上角 💬 消息中心，里面有留言板、私信和系统通知～',0,'2026-10-23 15:33:26'),(2,'NOTIFY',NULL,'小马王国',-1,NULL,'每天来首页抽一只「今日幸运小马」吧 🎲，看看今天谁陪你冒险！',0,'2026-10-23 15:33:26'),(4,'NOTIFY',NULL,'小马王国',2,NULL,'欢迎来到小马王国，浏览器测试马！记得去首页抽你的「今日幸运小马」🎲',0,'2026-10-23 16:22:50');
/*!40000 ALTER TABLE `t_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_pet`
--

DROP TABLE IF EXISTS `t_pet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_pet` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `kind` varchar(32) NOT NULL,
  `name` varchar(32) DEFAULT '',
  `level` int DEFAULT '1',
  `exp` int DEFAULT '0',
  `hunger` int DEFAULT '50',
  `mood` int DEFAULT '80',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_pet`
--

LOCK TABLES `t_pet` WRITE;
/*!40000 ALTER TABLE `t_pet` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_pet` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_point_log`
--

DROP TABLE IF EXISTS `t_point_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_point_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `amount` int NOT NULL,
  `biz` varchar(32) NOT NULL DEFAULT '',
  `remark` varchar(128) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_point_log`
--

LOCK TABLES `t_point_log` WRITE;
/*!40000 ALTER TABLE `t_point_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_point_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_pony`
--

DROP TABLE IF EXISTS `t_pony`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_pony` (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(32) NOT NULL COMMENT '中文名',
  `alias` varchar(64) NOT NULL DEFAULT '' COMMENT '英文名/别名',
  `mark` varchar(24) NOT NULL DEFAULT 'star' COMMENT '可爱标志图案 key',
  `emoji` varchar(8) NOT NULL DEFAULT '⭐' COMMENT '图案缺失时的 emoji',
  `color` varchar(16) NOT NULL DEFAULT '#b79cf0' COMMENT '招牌色',
  `rarity` varchar(8) NOT NULL DEFAULT '常见' COMMENT '稀有度：常见/稀有/传说',
  `intro` varchar(200) NOT NULL DEFAULT '' COMMENT '简介',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '被查看次数',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_t_pony_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小马图鉴条目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_pony`
--

LOCK TABLES `t_pony` WRITE;
/*!40000 ALTER TABLE `t_pony` DISABLE KEYS */;
INSERT INTO `t_pony` VALUES (1,'紫悦','Twilight Sparkle','twilight','📖','#b79cf0','传说','坎特洛特的学霸小马，从独角兽一路成长为天角兽公主，掌管友谊魔法。',0,1,'2026-10-23 15:37:44'),(2,'碧琪','Pinkie Pie','pinkie','🎈','#f7a8c4','稀有','小马谷的派对担当，随时能从鬃毛里掏出派对道具，笑声能传遍整个镇子。',0,2,'2026-10-23 15:37:44'),(3,'云宝','Rainbow Dash','dash','⚡','#8fc7f0','稀有','云中城最耀眼的飞行家，彩虹音爆的创造者，梦想加入闪电飞马队。',0,3,'2026-10-23 15:37:44'),(4,'苹果嘉儿','Applejack','applejack','🍎','#f0a06a','稀有','甜苹果园的顶梁柱，戴着牛仔帽，说到做到、从不说谎。',0,4,'2026-10-23 15:37:44'),(5,'柔柔','Fluttershy','fluttershy','🦋','#a7e3c9','稀有','住在森林小屋的温柔飞马，说话轻声细语，却能让所有小动物听话。',0,5,'2026-10-23 15:37:44'),(6,'珍奇','Rarity','rarity','💎','#f7e08a','稀有','旋转木马时装店的主人，坎特洛特的时尚风向标，最爱宝石。',0,6,'2026-10-23 15:37:44'),(7,'宇宙公主','Princess Celestia','star','☀️','#f7c94c','传说','小马国最高统治者，升起太阳、掌管白昼，紫悦的老师。',0,7,'2026-10-23 15:37:44'),(8,'露娜公主','Princess Luna','star','🌙','#8f7ae8','传说','掌管月亮与梦境的公主，曾被黑暗侵蚀成为梦魇之月，后回归王座。',0,8,'2026-10-23 15:37:44'),(9,'韵律公主','Princess Cadance','star','💗','#f7a8c4','传说','水晶帝国的公主，掌管爱情与守护，紫悦小时候的保姆。',0,9,'2026-10-23 15:37:44'),(10,'闪耀盔甲','Shining Armor','star','🛡️','#8fc7f0','稀有','皇家护卫队队长、水晶帝国守护者，紫悦的哥哥。',0,10,'2026-10-23 15:37:44'),(11,'星光熠熠','Starlight Glimmer','star','⭐','#b79cf0','稀有','曾试图抹掉所有小马的可爱标志，后来成为友谊学校的校长。',0,11,'2026-10-23 15:37:44'),(12,'小苹花','Apple Bloom','star','🌼','#f0a06a','常见','苹果嘉儿的妹妹，可爱标志童子军的发起人。',0,12,'2026-10-23 15:37:44'),(13,'甜心宝宝','Sweetie Belle','star','🎀','#f7a8c4','常见','珍奇的妹妹，歌声最好听的小独角兽。',0,13,'2026-10-23 15:37:44'),(14,'飞板璐','Scootaloo','star','🛴','#8fc7f0','常见','云宝的头号粉丝，骑着滑板车到处飞驰的小飞马。',0,14,'2026-10-23 15:37:44'),(15,'大麦克','Big McIntosh','star','🐴','#f0a06a','常见','苹果家的长子，高大沉默，口头禅是「嗯哼」和「不啦」。',0,15,'2026-10-23 15:37:44'),(16,'芝士三明治','Cheese Sandwich','star','🥪','#f7e08a','常见','四处旅行的派对小马，碧琪的搭档，最擅长手风琴。',0,16,'2026-10-23 15:37:44'),(17,'纸杯蛋糕夫人','Mrs. Cup Cake','star','🧁','#f5c08a','常见','小马谷纸杯蛋糕店的老板，烤的纸杯蛋糕连碧琪都抢着要。',0,17,'2026-10-23 15:37:44'),(18,'马斯卡彭','Marzipan Mascarpone','star','🍬','#c9a0e8','稀有','坎特洛特的糖果艺术家，能把糖丝做成礼服。',0,18,'2026-10-23 15:37:44'),(19,'软糖','Bon Bon','star','🍭','#f7a8c4','常见','手工糖果小贩，招牌酸角软糖，嘴里总念叨「才不是爱吃糖」。',0,19,'2026-10-23 15:37:44'),(20,'苏里·普罗玛','Suri Polomare','star','🎩','#b8a0d8','稀有','从坎特洛特来的皮草设计师，珍奇的竞争对手，嘴甜心硬。',0,20,'2026-10-23 15:37:44'),(21,'霍伊蒂·托蒂','Hoity Toity','star','👓','#a8b8c8','稀有','时尚评论家，一句「太寻常了」能让设计师失眠一整周。',0,21,'2026-10-23 15:37:44'),(22,'可可·潘玛尔','Coco Pommel','star','🧣','#d8a8b8','常见','惜字如金的裁缝，缝制的外套走线精致，一针一线都讲究。',0,22,'2026-10-23 15:37:44');
/*!40000 ALTER TABLE `t_pony` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_pony_category`
--

DROP TABLE IF EXISTS `t_pony_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_pony_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pony_id` bigint NOT NULL COMMENT 't_pony.id',
  `category_id` bigint NOT NULL COMMENT 't_category.id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pony_category` (`pony_id`,`category_id`),
  KEY `idx_pc_category` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=109 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小马条目与分类的关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_pony_category`
--

LOCK TABLES `t_pony_category` WRITE;
/*!40000 ALTER TABLE `t_pony_category` DISABLE KEYS */;
INSERT INTO `t_pony_category` VALUES (55,1,13),(56,1,14),(57,1,26),(58,1,32),(59,2,11),(60,2,21),(61,2,31),(62,3,12),(63,3,22),(64,3,33),(65,4,11),(66,4,25),(67,4,31),(68,5,12),(69,5,23),(70,5,31),(71,6,13),(72,6,24),(73,6,32),(74,7,14),(75,7,32),(76,8,14),(77,8,32),(78,9,14),(79,9,34),(80,10,13),(81,10,34),(82,11,13),(83,11,31),(84,12,11),(85,12,31),(86,13,13),(87,13,31),(88,14,12),(89,14,31),(90,15,11),(91,15,31),(92,16,11),(93,16,21),(94,16,31),(95,17,11),(96,17,21),(97,17,31),(98,18,11),(99,18,24),(100,18,32),(101,19,11),(102,19,31),(103,20,11),(104,20,32),(105,21,11),(106,21,32),(107,22,11),(108,22,31);
/*!40000 ALTER TABLE `t_pony_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_pony_tag`
--

DROP TABLE IF EXISTS `t_pony_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_pony_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pony_id` bigint NOT NULL COMMENT 't_pony.id',
  `tag` varchar(24) NOT NULL COMMENT '关键词',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pony_tag` (`pony_id`,`tag`),
  KEY `idx_tag` (`tag`)
) ENGINE=InnoDB AUTO_INCREMENT=229 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小马条目关键词标签';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_pony_tag`
--

LOCK TABLES `t_pony_tag` WRITE;
/*!40000 ALTER TABLE `t_pony_tag` DISABLE KEYS */;
INSERT INTO `t_pony_tag` VALUES (119,1,'公主'),(117,1,'学霸'),(118,1,'星光'),(116,1,'读书'),(115,1,'魔法'),(125,2,'搞怪'),(121,2,'欢笑'),(124,2,'气球'),(120,2,'派对'),(123,2,'甜品'),(122,2,'甜点'),(128,3,'彩虹'),(130,3,'忠诚'),(127,3,'竞速'),(129,3,'闪电'),(126,3,'飞行'),(132,4,'农场'),(135,4,'力气大'),(134,4,'牛仔'),(131,4,'苹果'),(133,4,'诚实'),(140,5,'害羞'),(137,5,'小动物'),(139,5,'森林'),(136,5,'温柔'),(138,5,'蝴蝶'),(144,6,'优雅'),(142,6,'宝石'),(145,6,'慷慨'),(141,6,'时尚'),(143,6,'设计'),(146,7,'太阳'),(150,7,'温柔'),(148,7,'王族'),(147,7,'白昼'),(149,7,'老师'),(152,8,'夜晚'),(151,8,'月亮'),(153,8,'梦境'),(155,8,'梦魇之月'),(154,8,'王族'),(159,9,'守护'),(157,9,'水晶'),(160,9,'温柔'),(156,9,'爱情'),(158,9,'王族'),(164,10,'哥哥'),(161,10,'守卫'),(163,10,'王族'),(162,10,'盾牌'),(165,10,'队长'),(167,11,'学校'),(168,11,'平等'),(169,11,'校长'),(170,11,'独角兽'),(166,11,'魔法'),(173,12,'可爱标志'),(174,12,'妹妹'),(172,12,'童子军'),(171,12,'苹果'),(175,12,'蝴蝶结'),(179,13,'可爱标志'),(177,13,'唱歌'),(180,13,'妹妹'),(176,13,'歌喉'),(178,13,'童子军'),(183,14,'可爱标志'),(181,14,'滑板车'),(182,14,'童子军'),(184,14,'粉丝'),(185,14,'翅膀'),(187,15,'农场'),(190,15,'力气大'),(189,15,'哥哥'),(188,15,'沉默'),(186,15,'苹果'),(192,16,'手风琴'),(193,16,'旅行'),(194,16,'欢笑'),(191,16,'派对'),(195,16,'美食'),(201,17,'小马谷'),(200,17,'派对'),(198,17,'烘焙'),(197,17,'甜品'),(196,17,'甜点'),(199,17,'纸杯蛋糕'),(207,18,'坎特洛特'),(203,18,'甜品'),(202,18,'甜点'),(206,18,'精致'),(204,18,'糖果'),(205,18,'艺术'),(212,19,'小马谷'),(213,19,'手工'),(211,19,'甜品'),(210,19,'甜点'),(208,19,'糖果'),(209,19,'软糖'),(218,20,'坎特洛特'),(214,20,'时尚'),(215,20,'皮草'),(217,20,'竞争对手'),(216,20,'设计'),(222,21,'坎特洛特'),(221,21,'墨镜'),(223,21,'挑剔'),(219,21,'时尚'),(220,21,'评论'),(226,22,'外套'),(227,22,'小马谷'),(228,22,'手巧'),(224,22,'时尚'),(225,22,'裁缝');
/*!40000 ALTER TABLE `t_pony_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_search_word`
--

DROP TABLE IF EXISTS `t_search_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_search_word` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `word` varchar(32) NOT NULL COMMENT '词条',
  `hit_count` int NOT NULL DEFAULT '0' COMMENT '被搜索次数',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '人工置顶权重',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页热搜词';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_search_word`
--

LOCK TABLES `t_search_word` WRITE;
/*!40000 ALTER TABLE `t_search_word` DISABLE KEYS */;
INSERT INTO `t_search_word` VALUES (1,'魔法',26,9),(2,'飞行',22,8),(3,'派对',19,7),(4,'苹果',16,6),(5,'公主',13,5),(6,'宝石',10,4),(7,'可爱标志',8,3),(8,'云中城',5,2),(9,'苹果嘉儿',3,1),(10,'天角兽',1,0);
/*!40000 ALTER TABLE `t_search_word` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_task_progress`
--

DROP TABLE IF EXISTS `t_task_progress`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_task_progress` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `day` varchar(10) NOT NULL,
  `task_id` varchar(32) NOT NULL,
  `progress` int DEFAULT '0',
  `claimed` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_day_task` (`user_id`,`day`,`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_task_progress`
--

LOCK TABLES `t_task_progress` WRITE;
/*!40000 ALTER TABLE `t_task_progress` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_task_progress` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_user`
--

DROP TABLE IF EXISTS `t_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(20) NOT NULL COMMENT '小马名字，登录账号',
  `avatar` varchar(16) DEFAULT '',
  `points` int DEFAULT '0',
  `password` varchar(128) NOT NULL COMMENT 'SHA-256(salt + 明文) 的小写 hex',
  `salt` varchar(16) NOT NULL COMMENT '每个用户随机 16 位 hex',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_t_user_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小马王国用户表（没有邮箱、没有手机号）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_user`
--

LOCK TABLES `t_user` WRITE;
/*!40000 ALTER TABLE `t_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_user_item`
--

DROP TABLE IF EXISTS `t_user_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `item_id` varchar(32) NOT NULL,
  `count` int DEFAULT '0',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_item` (`user_id`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_user_item`
--

LOCK TABLES `t_user_item` WRITE;
/*!40000 ALTER TABLE `t_user_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_user_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'mlp_login'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed
