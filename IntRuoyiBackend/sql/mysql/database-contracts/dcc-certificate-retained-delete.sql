-- Current application deletion/retained-evidence contract, checked after migrations on every release.
SELECT CASE WHEN
  EXISTS (SELECT 1 FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='trg_dcc_reg_cert_master_immutable_bu' AND SHA2(REGEXP_REPLACE(ACTION_STATEMENT,'[[:space:]`]+',''),256)='eaa0df4bc4292ebd45e863bda911ee2510e1d0815d125f4593a792253c6698be')
  AND EXISTS (SELECT 1 FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='trg_dcc_reg_cert_snapshot_immutable_bu' AND SHA2(REGEXP_REPLACE(ACTION_STATEMENT,'[[:space:]`]+',''),256)='56488eee0fcf5e42a03bc5271e08a42affbbb86eb3c529ca0ebc99c14b4c4a97')
  AND EXISTS (SELECT 1 FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='trg_dcc_reg_cert_master_immutable_bd' AND SHA2(REGEXP_REPLACE(ACTION_STATEMENT,'[[:space:]`]+',''),256)='bf847cbde9f9d64be698024eff6056108335c6a9a140e03313c4764923d2f5d9')
  AND EXISTS (SELECT 1 FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='trg_dcc_reg_cert_audit_immutable_bu' AND SHA2(REGEXP_REPLACE(ACTION_STATEMENT,'[[:space:]`]+',''),256)='50ab50e0ba3ce2f4eb28ad806696409543bbc01b86fa04e81bf028684fec23d7')
  AND EXISTS (SELECT 1 FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='trg_dcc_reg_cert_audit_immutable_bd' AND SHA2(REGEXP_REPLACE(ACTION_STATEMENT,'[[:space:]`]+',''),256)='836322dc9deb361a0f8d7239d3cc2142c128d983d6c3b3df2b6b50365d8f4914')
  AND (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_registration_certificate_audit' AND INDEX_NAME='uk_dcc_reg_cert_audit_event' AND NON_UNIQUE=0)='tenant_id,event_key'
THEN 'DATABASE_CONTRACT_PASS:dcc-certificate-retained-delete'
ELSE 'DATABASE_CONTRACT_FAIL:dcc-certificate-retained-delete' END;
