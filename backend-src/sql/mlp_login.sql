-- ============================================================
--  小马王国（mlp-login-vue）后端数据库
--  这个脚本可以重复执行，不会把已有账号删掉
--  执行方式（cmd 里）：
--    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 < sql\mlp_login.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `mlp_login`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `mlp_login`;

CREATE TABLE IF NOT EXISTS `t_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(20)  NOT NULL COMMENT '小马名字，登录账号',
    `password`    VARCHAR(128) NOT NULL COMMENT 'SHA-256(salt + 明文) 的小写 hex',
    `salt`        VARCHAR(16)  NOT NULL COMMENT '每个用户随机 16 位 hex',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_t_user_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '小马王国用户表（没有邮箱、没有手机号）';

-- 老库如果还留着 email 列，这里自动删掉（用户明确要求只用名字登录）
SET @has_email := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'mlp_login' AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'email'
);
SET @sql := IF(@has_email > 0,
    'ALTER TABLE `t_user` DROP COLUMN `email`',
    'SELECT ''t_user 没有 email 列，跳过'' AS note');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 说明：新注册的账号一律是 SHA-256(salt + 明文) 加盐哈希，库里不会出现明文。
-- 如果想手工造一个测试账号，salt 和 password 必须一起算，例如：
--   INSERT INTO t_user(name, salt, password)
--   VALUES ('碧琪', 'a1b2c3d4e5f60718', SHA2(CONCAT('a1b2c3d4e5f60718', 'abc12345'), 256));
-- 上面这种账号密码就是 abc12345（salt 是固定的 a1b2c3d4e5f60718）。

-- 自检
SELECT COUNT(*) AS t_user_rows FROM `t_user`;
