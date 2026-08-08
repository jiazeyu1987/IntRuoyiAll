const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../../../..')
const moduleRoot = path.resolve(__dirname, '../../..')
const readRepo = (relativePath) => fs.readFileSync(path.resolve(repoRoot, relativePath), 'utf8')
const readModule = (relativePath) => fs.readFileSync(path.resolve(moduleRoot, relativePath), 'utf8')

const migration = readRepo('sql/mysql/20260808_mes_active_order_release_application.sql')
const doSource = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/team/MesProcessPoolActiveOrderReleaseApplicationDO.java'
)
const mapperSource = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderReleaseApplicationMapper.java'
)

assert(
  migration.includes('release-migration:') &&
    migration.includes('dependsOn=20260802_mes_process_pool_active_order_process_snapshot'),
  'Migration must declare release metadata and dependency on active-order process snapshots.'
)

assert(
  migration.includes('CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_active_order_release_application`') &&
    migration.includes('`application_status` varchar(32) NOT NULL') &&
    migration.includes('`source_snapshot_hash` varchar(128) NOT NULL') &&
    migration.includes('`request_idempotency_key` varchar(128) NOT NULL') &&
    migration.includes('`business_idempotency_key` varchar(255) NOT NULL') &&
    migration.includes('`blocker_snapshot_json` json DEFAULT NULL') &&
    migration.includes('`dossier_summary_json` json DEFAULT NULL'),
  'Application table must persist status, source snapshot, idempotency keys, blockers, and dossier summary.'
)

assert(
  migration.includes('uk_mes_pp_active_order_release_request') &&
    migration.includes('uk_mes_pp_active_order_release_business') &&
    migration.includes('idx_mes_pp_active_order_release_status') &&
    migration.includes('idx_mes_pp_active_order_release_transaction'),
  'Application table must define request/business uniqueness and status/transaction indexes.'
)

assert(
  migration.includes('mes:pro-process-pool-team-leader:release-apply') &&
    migration.includes('活跃订单申请放行'),
  'Migration must register the production leader release application permission.'
)

assert(
  doSource.includes('@TableName("mes_pro_process_pool_active_order_release_application")') &&
    doSource.includes('extends TenantBaseDO') &&
    doSource.includes('private String applicationStatus;') &&
    doSource.includes('private String sourceSnapshotHash;') &&
    doSource.includes('private String requestIdempotencyKey;') &&
    doSource.includes('private String businessIdempotencyKey;'),
  'Application DO must map the new tenant-aware table and idempotency/status fields.'
)

assert(
  mapperSource.includes('selectLatestByActiveOrderIds') &&
    mapperSource.includes('selectByRequestIdempotencyKey') &&
    mapperSource.includes('selectByBusinessIdempotencyKey') &&
    mapperSource.includes('orderByDesc'),
  'Application mapper must support list status decoration and idempotent lookup.'
)

console.log('PASS: MES active-order release application schema static contract')
