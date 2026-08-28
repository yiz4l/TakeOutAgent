-- 用户主动维护的权威健康画像。模型推断的习惯不得覆盖该表。
CREATE TABLE user_health_profile (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  height_cm decimal(5,2) DEFAULT NULL,
  weight_kg decimal(6,2) DEFAULT NULL,
  target_type varchar(32) NOT NULL DEFAULT 'MAINTAIN',
  target_weight_kg decimal(6,2) DEFAULT NULL,
  target_duration_weeks int DEFAULT NULL,
  activity_level varchar(32) DEFAULT NULL,
  diet_preferences json DEFAULT NULL,
  disliked_foods json DEFAULT NULL,
  allergies json DEFAULT NULL,
  profile_revision bigint NOT NULL DEFAULT 1,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_health_profile_user (user_id),
  CONSTRAINT ck_health_profile_height CHECK (height_cm IS NULL OR height_cm BETWEEN 50 AND 260),
  CONSTRAINT ck_health_profile_weight CHECK (weight_kg IS NULL OR weight_kg BETWEEN 10 AND 500),
  CONSTRAINT ck_health_profile_target CHECK (target_type IN ('MUSCLE_GAIN', 'FAT_LOSS', 'MAINTAIN')),
  CONSTRAINT ck_health_profile_duration CHECK (target_duration_weeks IS NULL OR target_duration_weeks BETWEEN 1 AND 260)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户主动填写的健康画像';

-- 最近14/30天趋势计算使用结构化每日汇总，避免把原始明细交给模型计算。
CREATE TABLE nutrition_daily_state (
  user_id bigint NOT NULL,
  record_date date NOT NULL,
  revision bigint NOT NULL DEFAULT 0,
  calories decimal(10,2) NOT NULL DEFAULT 0,
  protein_g decimal(10,2) NOT NULL DEFAULT 0,
  fat_g decimal(10,2) NOT NULL DEFAULT 0,
  carbohydrate_g decimal(10,2) NOT NULL DEFAULT 0,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, record_date),
  KEY idx_nutrition_daily_user_date (user_id, record_date),
  CONSTRAINT ck_nutrition_daily_revision CHECK (revision >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户每日营养数值及输入版本';

-- 周期总结在MySQL中作为事实来源，同时由Agent写入Qdrant用于语义召回。
CREATE TABLE health_period_summary (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  period_type varchar(20) NOT NULL,
  period_start date NOT NULL,
  period_end date NOT NULL,
  source_revision varchar(512) NOT NULL,
  profile_revision bigint NOT NULL DEFAULT 0,
  summary text NOT NULL,
  nutrient_trends json NOT NULL,
  source_summary_ids json DEFAULT NULL,
  model_version varchar(128) NOT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_period_summary (user_id, period_type, period_start, period_end, source_revision, model_version),
  KEY idx_period_summary_user_end (user_id, period_end),
  CONSTRAINT ck_period_summary_type CHECK (period_type IN ('BIWEEKLY', 'MONTHLY')),
  CONSTRAINT ck_period_summary_dates CHECK (period_start <= period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='两周和月度饮食总结';

-- 让同一任务队列表达日分析和周期总结任务。
ALTER TABLE health_analysis_task
  ADD COLUMN task_type varchar(32) NOT NULL DEFAULT 'DAILY_ANALYSIS' AFTER request_id,
  ADD COLUMN period_start date DEFAULT NULL AFTER analysis_date,
  ADD COLUMN period_end date DEFAULT NULL AFTER period_start,
  ADD COLUMN profile_revision bigint NOT NULL DEFAULT 0 AFTER input_revision,
  ADD COLUMN period_summary_id bigint DEFAULT NULL AFTER health_analysis_id,
  ADD KEY idx_analysis_task_type_status (task_type, status, create_time),
  ADD CONSTRAINT ck_analysis_task_type CHECK (
    task_type IN ('DAILY_ANALYSIS', 'BIWEEKLY_SUMMARY', 'MONTHLY_SUMMARY')
  );
