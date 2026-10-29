-- release-target-preflight: migrationId=20260902_mes_active_order_version_upgrade_bpm_seed; allowedEnvironments=test,backup,prod
-- Principals must be unique/enabled; hard-coded Flowable IDs must be free for a new seed or form a complete matching existing graph. Refuse partial/orphan/custom-ID deployments.
WITH tenants AS (
 SELECT 1 tenant_id,'admin' username,'mes-active-order-version-upgrade-v1:1:active-order-admin' procdef_id,
 'mes-active-order-upgrade-deploy-tenant-1' deploy_id,'mes-active-order-upgrade-model-tenant-1' model_id,
 'mes-active-order-upgrade-model-src-tenant-1' source_id,'mes-active-order-upgrade-bpmn-tenant-1' bpmn_id
 UNION ALL SELECT 122,'aoteman','mes-active-order-version-upgrade-v1:1:active-order-test',
 'mes-active-order-upgrade-deploy-tenant-122','mes-active-order-upgrade-model-tenant-122',
 'mes-active-order-upgrade-model-src-tenant-122','mes-active-order-upgrade-bpmn-tenant-122'
)
SELECT CASE WHEN

  NOT EXISTS (SELECT 1 FROM tenants t WHERE
    (SELECT COUNT(*) FROM system_role r WHERE r.tenant_id=t.tenant_id AND r.code='mes_route_version_admin' AND r.deleted=b'0' AND r.status=0)<>1
    OR (SELECT COUNT(*) FROM system_users u WHERE u.tenant_id=t.tenant_id AND BINARY u.username=BINARY t.username AND u.deleted=b'0' AND u.status=0)<>1)
  AND NOT EXISTS (SELECT 1 FROM act_re_procdef d JOIN tenants t ON BINARY d.TENANT_ID_=BINARY CAST(t.tenant_id AS CHAR)
     WHERE d.KEY_='mes-active-order-version-upgrade-v1' AND (BINARY d.ID_<>BINARY t.procdef_id OR BINARY d.DEPLOYMENT_ID_<>BINARY t.deploy_id OR d.RESOURCE_NAME_<>'mes-active-order-version-upgrade-v1.bpmn'))
  AND NOT EXISTS (SELECT 1 FROM tenants t WHERE
     NOT EXISTS (SELECT 1 FROM act_re_procdef d WHERE d.KEY_='mes-active-order-version-upgrade-v1' AND BINARY d.TENANT_ID_=BINARY CAST(t.tenant_id AS CHAR))
     AND (EXISTS (SELECT 1 FROM act_re_procdef existing_id WHERE BINARY existing_id.ID_=BINARY t.procdef_id)
       OR EXISTS (SELECT 1 FROM act_re_deployment d WHERE BINARY d.ID_=BINARY t.deploy_id)
       OR EXISTS (SELECT 1 FROM act_re_model m WHERE BINARY m.ID_=BINARY t.model_id OR (m.KEY_='mes-active-order-version-upgrade-v1' AND BINARY m.TENANT_ID_=BINARY CAST(t.tenant_id AS CHAR)))
       OR EXISTS (SELECT 1 FROM act_ge_bytearray b WHERE b.ID_ IN (t.source_id,t.bpmn_id))
       OR EXISTS (SELECT 1 FROM bpm_process_definition_info i WHERE BINARY i.process_definition_id=BINARY t.procdef_id)))
  AND NOT EXISTS (SELECT 1 FROM tenants t JOIN act_re_procdef d ON BINARY d.ID_=BINARY t.procdef_id
     WHERE NOT EXISTS (SELECT 1 FROM act_re_deployment dep WHERE BINARY dep.ID_=BINARY t.deploy_id AND BINARY dep.TENANT_ID_=BINARY CAST(t.tenant_id AS CHAR))
       OR NOT EXISTS (SELECT 1 FROM act_re_model m JOIN act_ge_bytearray b ON b.ID_=m.EDITOR_SOURCE_VALUE_ID_
                      WHERE BINARY m.ID_=BINARY t.model_id AND m.KEY_='mes-active-order-version-upgrade-v1' AND BINARY m.TENANT_ID_=BINARY CAST(t.tenant_id AS CHAR)
                        AND BINARY m.DEPLOYMENT_ID_=BINARY t.deploy_id AND CONVERT(b.BYTES_ USING utf8mb4) LIKE '%id="mes-active-order-version-upgrade-v1"%')
       OR NOT EXISTS (SELECT 1 FROM act_ge_bytearray b WHERE BINARY b.ID_=BINARY t.bpmn_id AND BINARY b.DEPLOYMENT_ID_=BINARY t.deploy_id AND b.NAME_=d.RESOURCE_NAME_
                         AND CONVERT(b.BYTES_ USING utf8mb4) LIKE '%id="mes-active-order-version-upgrade-v1"%'))
  AND NOT EXISTS (SELECT 1 FROM tenants t JOIN bpm_process_definition_info i ON BINARY i.process_definition_id=BINARY t.procdef_id
     WHERE i.tenant_id<>t.tenant_id OR BINARY i.model_id<>BINARY t.model_id OR i.deleted<>b'0')
THEN 'TARGET_PREFLIGHT_PASS:20260902_mes_active_order_version_upgrade_bpm_seed' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260902_mes_active_order_version_upgrade_bpm_seed' END;
