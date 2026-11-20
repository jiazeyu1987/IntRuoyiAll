-- release-target-preflight: migrationId=20260715_mes_edhr_notify_template_garbled_repair; allowedEnvironments=test,backup,prod
WITH messages AS (
  SELECT template_code,JSON_VALID(template_params) AS valid_json,
    IF(JSON_VALID(template_params),template_params,JSON_OBJECT()) AS params,
    (COALESCE(LOCATE('??',template_nickname),0)>0 OR COALESCE(LOCATE('??',template_content),0)>0) AS affected
  FROM system_notify_message WHERE deleted=b'0'
  AND template_code IN ('MES_EDHR_ARCHIVE_TASK_ASSIGNED','MES_EDHR_WORK_TASK_OVERDUE')
)
SELECT CASE WHEN
(SELECT COUNT(*) FROM system_notify_template WHERE deleted=b'0'
   AND code IN ('MES_EDHR_ARCHIVE_TASK_ASSIGNED','MES_EDHR_WORK_TASK_OVERDUE'))=2
  AND (SELECT COUNT(DISTINCT code) FROM system_notify_template WHERE deleted=b'0'
   AND code IN ('MES_EDHR_ARCHIVE_TASK_ASSIGNED','MES_EDHR_WORK_TASK_OVERDUE'))=2
  AND NOT EXISTS (
    SELECT 1 FROM messages WHERE affected AND (
      NOT valid_json OR COALESCE(JSON_TYPE(JSON_EXTRACT(params,'$.workOrderCode')),'NULL')<>'STRING'
      OR COALESCE(JSON_TYPE(JSON_EXTRACT(params,'$.batchCode')),'NULL')<>'STRING'
      OR COALESCE(JSON_TYPE(JSON_EXTRACT(params,'$.actionUrl')),'NULL')<>'STRING'
      OR LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.workOrderCode')))>0
      OR LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.batchCode')))>0
      OR LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.actionUrl')))>0
      OR (template_code='MES_EDHR_WORK_TASK_OVERDUE' AND (
        COALESCE(JSON_TYPE(JSON_EXTRACT(params,'$.processName')),'NULL')<>'STRING'
        OR COALESCE(JSON_TYPE(JSON_EXTRACT(params,'$.dueTime')),'NULL')<>'STRING'
        OR LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.processName')))>0
        OR LOCATE('??',JSON_UNQUOTE(JSON_EXTRACT(params,'$.dueTime')))>0))
    )
  )
THEN 'TARGET_PREFLIGHT_PASS:20260715_mes_edhr_notify_template_garbled_repair'
ELSE 'TARGET_PREFLIGHT_BLOCKED:review-required-before-data-migration' END;
