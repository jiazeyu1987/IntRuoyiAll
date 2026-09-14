START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_route_key_repair;
CREATE TEMPORARY TABLE tmp_route_key_repair (
  route_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY,
  route_id BIGINT NOT NULL,
  current_key_route_process_id BIGINT NOT NULL,
  terminal_sort INT NOT NULL
);

INSERT INTO tmp_route_key_repair(route_code, route_id, current_key_route_process_id, terminal_sort) VALUES
  ('ROUTE-XLSX-00001', 900025, 926807, 23),
  ('ROUTE-XLSX-00002', 900026, 926657, 26);

UPDATE mes_pro_route_process rp
JOIN tmp_route_key_repair t ON t.route_id = rp.route_id
SET rp.key_flag = CASE WHEN rp.id = t.current_key_route_process_id THEN b'1' ELSE b'0' END,
    rp.updater = 'codex',
    rp.update_time = NOW()
WHERE rp.tenant_id = 1
  AND rp.deleted = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_route_key_snapshot_path;
CREATE TEMPORARY TABLE tmp_route_key_snapshot_path AS
SELECT v.id AS version_id,
       r.code AS route_code,
       jt.ord - 1 AS json_index,
       jt.routeProcessId AS snapshot_route_process_id,
       jt.sort AS snapshot_sort
FROM mes_pro_route r
JOIN tmp_route_key_repair t ON t.route_id = r.id AND t.route_code = r.code
JOIN mes_pro_route_version v
  ON v.route_id = r.id
 AND v.tenant_id = r.tenant_id
 AND v.deleted = 0
 AND v.lifecycle_status IN ('ACTIVE', 'DRAFT')
JOIN JSON_TABLE(v.route_snapshot_json, '$.configSnapshots.flowGraph.nodes[*]'
  COLUMNS (
    ord FOR ORDINALITY,
    routeProcessId BIGINT PATH '$.routeProcessId',
    sort INT PATH '$.sort'
  )
) jt
WHERE r.tenant_id = 1
  AND r.deleted = 0
  AND jt.sort = t.terminal_sort;

UPDATE mes_pro_route_version v
JOIN tmp_route_key_snapshot_path p ON p.version_id = v.id
SET v.route_snapshot_json = JSON_SET(
      v.route_snapshot_json,
      CONCAT('$.configSnapshots.flowGraph.nodes[', p.json_index, '].keyFlag'),
      true
    ),
    v.updater = 'codex',
    v.update_time = NOW()
WHERE v.tenant_id = 1
  AND v.deleted = 0;

COMMIT;

SELECT 'route_process' AS scope_name, r.code,
       COUNT(*) AS process_count,
       SUM(CASE WHEN rp.key_flag = b'1' THEN 1 ELSE 0 END) AS key_count,
       GROUP_CONCAT(CASE WHEN rp.key_flag = b'1' THEN CONCAT(rp.sort, ':', p.code, ':', rp.id) END ORDER BY rp.sort SEPARATOR ',') AS key_process
FROM mes_pro_route r
JOIN mes_pro_route_process rp ON rp.route_id = r.id AND rp.tenant_id = r.tenant_id AND rp.deleted = 0
JOIN mes_pro_process p ON p.id = rp.process_id AND p.tenant_id = rp.tenant_id AND p.deleted = 0
WHERE r.tenant_id = 1
  AND r.deleted = 0
  AND r.code IN ('ROUTE-XLSX-00001', 'ROUTE-XLSX-00002')
GROUP BY r.code
UNION ALL
SELECT CONCAT('snapshot_', v.lifecycle_status) AS scope_name, r.code,
       COUNT(*) AS process_count,
       SUM(CASE WHEN jt.keyFlag = 'true' THEN 1 ELSE 0 END) AS key_count,
       GROUP_CONCAT(CASE WHEN jt.keyFlag = 'true' THEN CONCAT(jt.sort, ':', jt.routeProcessId, ':', JSON_TYPE(JSON_EXTRACT(v.route_snapshot_json, CONCAT('$.configSnapshots.flowGraph.nodes[', jt.ord - 1, '].keyFlag')))) END ORDER BY jt.sort SEPARATOR ',') AS key_process
FROM mes_pro_route r
JOIN mes_pro_route_version v ON v.route_id = r.id AND v.tenant_id = r.tenant_id AND v.deleted = 0 AND v.lifecycle_status IN ('ACTIVE', 'DRAFT')
JOIN JSON_TABLE(v.route_snapshot_json, '$.configSnapshots.flowGraph.nodes[*]'
  COLUMNS (
    ord FOR ORDINALITY,
    routeProcessId BIGINT PATH '$.routeProcessId',
    sort INT PATH '$.sort',
    keyFlag VARCHAR(8) PATH '$.keyFlag'
  )
) jt
WHERE r.tenant_id = 1
  AND r.deleted = 0
  AND r.code IN ('ROUTE-XLSX-00001', 'ROUTE-XLSX-00002')
GROUP BY r.code, v.id, v.lifecycle_status
ORDER BY code, scope_name;
