-- ============================================================
--  星糖谷（mlp-login-vue）星糖大厅表
--  大厅公共聊天消息（所有用户可见）
--  这个脚本可以重复执行，不会把已有消息删掉
--  执行方式（cmd 里）：
--    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 < sql\mlp_hall.sql
-- ============================================================

USE `mlp_login`;

CREATE TABLE IF NOT EXISTS `t_hall_message` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NULL                   COMMENT '发送者 id（系统公告可为空）',
    `name`        VARCHAR(50)  NULL                   COMMENT '发送者名字（展示用）',
    `avatar`      VARCHAR(16)  NULL                   COMMENT '发送者头像 emoji',
    `content`     VARCHAR(500) NOT NULL               COMMENT '消息内容',
    `msg_type`    VARCHAR(10)  NOT NULL DEFAULT 'text' COMMENT 'text 文本 / system 系统提示',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_t_hall_message_time` (`create_time`),
    KEY `idx_t_hall_message_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '星糖谷大厅公共聊天';

-- 幂等种子：大厅欢迎语（重复执行不会重复插入）
INSERT INTO `t_hall_message` (`user_id`, `name`, `avatar`, `content`, `msg_type`, `create_time`)
SELECT NULL, '星糖谷', '🏠',
       '欢迎来到星糖大厅！这里所有小马都能聊天，也可以发起游戏邀请，喊上朋友一起玩～',
       'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_hall_message` WHERE `content` LIKE '欢迎来到星糖大厅%');

-- 自检
SELECT COUNT(*) AS t_hall_message_rows FROM `t_hall_message`;