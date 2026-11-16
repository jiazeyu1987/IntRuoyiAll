-- release-target-preflight: migrationId=20261030_dcc_application_withdrawn_notify; allowedEnvironments=test,backup,prod
-- Reject existing conflicting data; absent seed rows are created by the migration.
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM system_notify_template WHERE code='dcc_controlled_file_withdrawn')>1) THEN 'TARGET_PREFLIGHT_BLOCKED:20261030_dcc_application_withdrawn_notify:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM system_notify_template WHERE code='dcc_controlled_file_withdrawn' AND
 (deleted<>0 OR status<>0 OR type<>2 OR NOT (nickname <=> 'DCC系统')
 OR NOT (content <=> '受控文件流程《{processInstanceName}》已撤回，原因：{reason}。可在消息详情重新发起。')
 OR NOT (params <=> '["processInstanceName","reason","detailUrl"]')))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261030_dcc_application_withdrawn_notify:CONDITION_2'
 ELSE 'TARGET_PREFLIGHT_PASS:20261030_dcc_application_withdrawn_notify' END;
