-- =====================================================
-- 小马王国 · 好友 + 聊天（mlp_login 库）
--   t_friend       好友关系（user_id=申请人, friend_id=接收人, status 0申请中 1已通过）
--   t_chat_message 一对一聊天消息
-- 执行：mysql -uroot -p123456 mlp_login < mlp_friend.sql
-- =====================================================

USE mlp_login;

CREATE TABLE IF NOT EXISTS `t_friend` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '申请人 id',
  `friend_id`   BIGINT       NOT NULL COMMENT '接收人 id',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0=申请中 1=已通过',
  `apply_msg`   VARCHAR(100) NOT NULL DEFAULT '' COMMENT '申请留言',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pair` (`user_id`, `friend_id`),
  KEY `idx_friend_status` (`friend_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

CREATE TABLE IF NOT EXISTS `t_chat_message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `from_id`     BIGINT       NOT NULL COMMENT '发送者 id',
  `to_id`       BIGINT       NOT NULL COMMENT '接收者 id',
  `content`     VARCHAR(500) NOT NULL COMMENT '消息内容',
  `read_flag`   TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pair_time` (`from_id`, `to_id`, `id`),
  KEY `idx_to_read` (`to_id`, `read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友聊天消息表';
