-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Disable base ERP realtime sync jobs.
-- JobStatusEnum.STOP = 2. Keep sync services, history, watermarks, and audit records intact.

UPDATE `infra_job`
SET `status` = 2,
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (
    `id` IN (5602, 5603, 5604, 5605)
    OR `handler_name` IN (
      'kingdeeProductItemSyncJob',
      'kingdeeStockSyncJob',
      'kingdeePurchaseOrderSyncJob',
      'kingdeeSaleOrderSyncJob'
    )
  );
