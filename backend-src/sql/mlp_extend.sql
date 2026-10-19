-- 小马王国扩展：积分 / 头像 / 绑定关系 / 任务 / 背包 / 宠物
-- 幂等：重复执行时 ALTER 的"列已存在"报错可忽略（--force 会继续）

USE mlp_login;

-- 用户扩展：头像 emoji + 积分余额
ALTER TABLE t_user ADD COLUMN avatar VARCHAR(16) DEFAULT '' AFTER name;
ALTER TABLE t_user ADD COLUMN points INT DEFAULT 0 AFTER avatar;

-- 好友关系扩展：绑定关系（闺蜜/兄弟/师徒/搭档/对手）
ALTER TABLE t_friend ADD COLUMN relation_type VARCHAR(16) DEFAULT '' AFTER status;

-- 积分流水：每一笔积分的来龙去脉
CREATE TABLE IF NOT EXISTS t_point_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  amount INT NOT NULL,
  biz VARCHAR(32) NOT NULL DEFAULT '',
  remark VARCHAR(128) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 每日任务进度：按 日期 记，每天清零
CREATE TABLE IF NOT EXISTS t_task_progress (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  day VARCHAR(10) NOT NULL,
  task_id VARCHAR(32) NOT NULL,
  progress INT DEFAULT 0,
  claimed TINYINT DEFAULT 0,
  UNIQUE KEY uk_user_day_task (user_id, day, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 背包：商店兑换来的物品
CREATE TABLE IF NOT EXISTS t_user_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  item_id VARCHAR(32) NOT NULL,
  count INT DEFAULT 0,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_item (user_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 宠物：一人一只，喂食升级
CREATE TABLE IF NOT EXISTS t_pet (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  kind VARCHAR(32) NOT NULL,
  name VARCHAR(32) DEFAULT '',
  level INT DEFAULT 1,
  exp INT DEFAULT 0,
  hunger INT DEFAULT 50,
  mood INT DEFAULT 80,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
