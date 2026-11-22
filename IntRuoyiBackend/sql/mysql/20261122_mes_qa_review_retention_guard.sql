-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261028_mes_edhr_nonconformance_reviewer_dispatch; type=schema; riskLevel=medium
-- Preserve the source of QA decisions and handoff history. No historical row is reconstructed or removed.
-- Recovery/rollback: drop only the two named triggers after reviewing the retention impact.
SET NAMES utf8mb4;
DROP TRIGGER IF EXISTS trg_mes_ncr_retained_no_delete;
DROP TRIGGER IF EXISTS trg_mes_ncr_retained_identity;
DELIMITER $$
CREATE TRIGGER trg_mes_ncr_retained_no_delete
BEFORE DELETE ON mes_pro_edhr_nonconformance_review
FOR EACH ROW
BEGIN
  IF OLD.review_status = 'closed' OR EXISTS (
    SELECT 1 FROM mes_active_order_handoff_task h
    WHERE h.tenant_id = OLD.tenant_id AND h.source_type = 'NONCONFORMANCE_REVIEW'
      AND h.source_id = OLD.id
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'QA_REVIEW_RETAINED: decision or handoff source cannot be deleted';
  END IF;
END$$
CREATE TRIGGER trg_mes_ncr_retained_identity
BEFORE UPDATE ON mes_pro_edhr_nonconformance_review
FOR EACH ROW
BEGIN
  IF OLD.review_status = 'closed' AND NOT (NEW.review_status <=> OLD.review_status) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'QA_REVIEW_RETAINED: closed decision cannot be reopened';
  END IF;
  IF (NOT (NEW.id <=> OLD.id) OR NOT (NEW.tenant_id <=> OLD.tenant_id)
      OR NOT (NEW.deleted <=> OLD.deleted))
    AND (OLD.review_status = 'closed' OR EXISTS (
      SELECT 1 FROM mes_active_order_handoff_task h
      WHERE h.tenant_id = OLD.tenant_id AND h.source_type = 'NONCONFORMANCE_REVIEW'
        AND h.source_id = OLD.id
    )) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'QA_REVIEW_RETAINED: decision or handoff identity cannot be changed';
  END IF;
END$$
DELIMITER ;
