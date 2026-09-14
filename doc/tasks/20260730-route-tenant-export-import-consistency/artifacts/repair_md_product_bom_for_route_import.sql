START TRANSACTION;

UPDATE mes_md_product_bom
SET deleted = b'0', updater = 'codex', update_time = NOW()
WHERE tenant_id = 1
  AND deleted = b'1'
  AND id IN (1, 2, 3, 4, 5);
SET @source_restore_rows = ROW_COUNT();

INSERT INTO mes_md_product_bom (
  item_id, bom_item_id, quantity, status, remark,
  creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
  tp.id AS item_id,
  tbi.id AS bom_item_id,
  smb.quantity,
  smb.status,
  smb.remark,
  'codex' AS creator,
  NOW() AS create_time,
  'codex' AS updater,
  NOW() AS update_time,
  b'0' AS deleted,
  122 AS tenant_id
FROM (
  SELECT DISTINCT p.code AS product_code, bi.code AS bom_item_code
  FROM mes_pro_route_product_bom rb
  JOIN mes_pro_route r ON r.id = rb.route_id AND r.tenant_id = 1 AND r.deleted = 0
  JOIN mes_md_item p ON p.id = rb.product_id AND p.tenant_id = 1 AND p.deleted = 0
  JOIN mes_md_item bi ON bi.id = rb.item_id AND bi.tenant_id = 1 AND bi.deleted = 0
  WHERE rb.tenant_id = 1 AND rb.deleted = 0
) n
JOIN mes_md_item sp ON sp.tenant_id = 1 AND sp.deleted = 0 AND sp.code = n.product_code
JOIN mes_md_item sbi ON sbi.tenant_id = 1 AND sbi.deleted = 0 AND sbi.code = n.bom_item_code
JOIN mes_md_product_bom smb ON smb.tenant_id = 1 AND smb.deleted = 0 AND smb.item_id = sp.id AND smb.bom_item_id = sbi.id
JOIN mes_md_item tp ON tp.tenant_id = 122 AND tp.deleted = 0 AND tp.code = n.product_code
JOIN mes_md_item tbi ON tbi.tenant_id = 122 AND tbi.deleted = 0 AND tbi.code = n.bom_item_code
LEFT JOIN mes_md_product_bom tmb ON tmb.tenant_id = 122 AND tmb.deleted = 0 AND tmb.item_id = tp.id AND tmb.bom_item_id = tbi.id
WHERE tmb.id IS NULL;
SET @target_insert_rows = ROW_COUNT();

SELECT @source_restore_rows AS source_restore_rows, @target_insert_rows AS target_insert_rows;

COMMIT;
