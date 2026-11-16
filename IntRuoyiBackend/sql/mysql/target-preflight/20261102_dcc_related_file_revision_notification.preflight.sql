-- release-target-preflight: migrationId=20261102_dcc_related_file_revision_notification; allowedEnvironments=test,backup,prod
-- Reject existing conflicting data; absent seed rows are created by the migration.
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM system_notify_template WHERE BINARY code=BINARY 'dcc_related_file_revision' AND deleted=0)>1) THEN 'TARGET_PREFLIGHT_BLOCKED:20261102_dcc_related_file_revision_notification:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM system_notify_template WHERE BINARY code=BINARY 'dcc_related_file_revision' AND deleted=0 AND
 (status<>0 OR type<>2 OR NOT (BINARY nickname <=> BINARY 'DCC系统')
 OR NOT (BINARY content <=> BINARY '您编制的文件 {relatedFileNumber} 所关联的文件 {fileNumber} 已完成升版至 {versionNo}，请核对关联影响。')
 OR NOT (BINARY params <=> BINARY '["fileNumber","versionNo","relatedFileNumber","sourceControlledFileId","revisedControlledFileId","detailUrl"]')))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261102_dcc_related_file_revision_notification:CONDITION_2'
 ELSE 'TARGET_PREFLIGHT_PASS:20261102_dcc_related_file_revision_notification' END;
