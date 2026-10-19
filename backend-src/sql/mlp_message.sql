-- ============================================================
--  小马王国（mlp-login-vue）消息中心表
--  这个脚本可以重复执行，不会把已有消息删掉
--  执行方式（cmd 里）：
--    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 < sql\mlp_message.sql
-- ============================================================

USE `mlp_login`;

CREATE TABLE IF NOT EXISTS `t_message` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`        VARCHAR(10)  NOT NULL                COMMENT 'BOARD 留言板 / PM 私信 / NOTIFY 系统通知',
    `from_id`     BIGINT       NULL                   COMMENT '发送者 id（系统通知可为空）',
    `from_name`   VARCHAR(50)  NULL                   COMMENT '发送者名字（展示用）',
    `to_id`       BIGINT       NULL                   COMMENT '接收者 id；-1 表示全员广播；留言板为 NULL',
    `to_name`     VARCHAR(50)  NULL                   COMMENT '接收者名字（展示用）',
    `content`     VARCHAR(500) NOT NULL               COMMENT '消息内容',
    `read_flag`   TINYINT      NOT NULL DEFAULT 0     COMMENT '0 未读 1 已读',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_t_message_type` (`type`),
    KEY `idx_t_message_to`   (`to_id`),
    KEY `idx_t_message_from` (`from_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '小马王国消息中心（留言板 / 私信 / 系统通知）';

-- 幂等种子：全员广播的系统公告（to_id = -1），重复执行不会重复插入
INSERT INTO `t_message` (`type`, `from_name`, `to_id`, `content`, `read_flag`, `create_time`)
SELECT 'NOTIFY', '小马王国', -1, '欢迎来到小马王国！点开左上角 💬 消息中心，里面有留言板、私信和系统通知～', 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_message` WHERE `type` = 'NOTIFY' AND `content` LIKE '欢迎来到小马王国%');

INSERT INTO `t_message` (`type`, `from_name`, `to_id`, `content`, `read_flag`, `create_time`)
SELECT 'NOTIFY', '小马王国', -1, '每天来首页抽一只「今日幸运小马」吧 🎲，看看今天谁陪你冒险！', 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_message` WHERE `type` = 'NOTIFY' AND `content` LIKE '每天来首页抽%');

-- 自检
SELECT COUNT(*) AS t_message_rows FROM `t_message`;
