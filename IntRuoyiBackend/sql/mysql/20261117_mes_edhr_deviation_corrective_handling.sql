-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261028_mes_edhr_deviation_review_assignments; type=schema; riskLevel=medium
-- Additive only. Existing records retain their content and signatures; new correction
-- submissions use explicit state and evidence. No role or tenant package is broadened.
DROP PROCEDURE IF EXISTS ensure_edhr_corrective_handling;
DELIMITER $$
CREATE PROCEDURE ensure_edhr_corrective_handling()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
    AND table_name = 'mes_pro_edhr_deviation_handling' AND column_name = 'corrective_status') THEN
    ALTER TABLE mes_pro_edhr_deviation_handling
      ADD COLUMN corrective_status varchar(20) DEFAULT NULL COMMENT 'DRAFT/SUBMITTED/RETURNED/VERIFIED',
      ADD COLUMN investigation_submitted bit(1) NOT NULL DEFAULT b'0' COMMENT '调查是否已正式提交',
      ADD COLUMN corrective_result text DEFAULT NULL COMMENT '整改责任人提交的整改结果',
      ADD COLUMN corrective_evidence_urls_json json DEFAULT NULL COMMENT '整改证据链接';
  END IF;
END$$
DELIMITER ;
CALL ensure_edhr_corrective_handling();
DROP PROCEDURE IF EXISTS ensure_edhr_corrective_handling;
