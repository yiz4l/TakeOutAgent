-- 2026-08-30 增量迁移
-- 前置条件：上一版迁移已经创建 task_type、period_start、period_end、
-- profile_revision、period_summary_id、idx_analysis_task_type_status
-- 和 ck_analysis_task_type。

-- Agent 会读取最近14天数据，因此任务需要保存该批输入的SHA-256指纹。
-- 原唯一键没有区分任务类型、画像版本和14天输入指纹，因此一并重建。
ALTER TABLE health_analysis_task
  ADD COLUMN source_revision varchar(64) NOT NULL DEFAULT '' AFTER profile_revision,
  DROP INDEX uk_analysis_input,
  ADD UNIQUE KEY uk_analysis_input (
    user_id,
    task_type,
    analysis_date,
    input_revision,
    profile_revision,
    source_revision,
    model_version
  );
