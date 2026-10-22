const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const moduleRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')
const migrationPath = path.join(moduleRoot, '../sql/mysql/20260924_mes_edhr_deviation_management.sql')

test('P1 deviation contract has tenant-scoped API, formal batch source, and locked release boundary', () => {
  const service = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationServiceImpl.java')
  const controller = read('src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrDeviationController.java')
  const mapper = read('src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrDeviationMapper.java')
  const idempotencyMapper = read('src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrDeviationCreateRequestMapper.java')
  const batchMapper = read('src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrBatchExecutionMapper.java')
  const sequenceMapper = read('src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrDeviationSequenceMapper.java')
  const numberGenerator = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationNumberGenerator.java')

  assert.match(controller, /mes:pro-edhr-deviation:create/)
  assert.match(service, /selectDeviationOptionsPage/)
  assert.match(service, /TenantContextHolder\.getRequiredTenantId\(\)/)
  assert.match(service, /selectByTenantIdAndIdForUpdate\(/)
  assert.match(service, /BATCH_MARKET_RELEASED/)
  assert.match(service, /IDEMPOTENCY_CONFLICT/)
  assert.match(batchMapper, /release_status\s*=\s*'RELEASED'/)
  assert.match(mapper, /FOR UPDATE/)
  assert.match(idempotencyMapper, /reserveOrLock/)
  assert.match(idempotencyMapper, /selectForUpdate/)
  assert.match(idempotencyMapper, /linkDeviation/)
  assert.match(sequenceMapper, /LAST_INSERT_ID\(`last_value`\s*\+\s*1\)/)
  assert.match(numberGenerator, /Asia\/Shanghai/)
})

test('P1 deviation migration preserves history and constrains formal batch, code, sequence and handling identity', () => {
  assert.ok(fs.existsSync(migrationPath), 'P1 migration must exist before policy validation')
  const migration = fs.readFileSync(migrationPath, 'utf8')
  assert.match(migration, /^-- release-migration: .*dependsOn=20260608_edhr_batch_execution_schema.*type=schema/m)
  assert.match(migration, /CREATE TABLE IF NOT EXISTS `mes_pro_edhr_deviation`/)
  assert.match(migration, /`batch_execution_id` bigint NOT NULL/)
  assert.match(migration, /UNIQUE KEY `uk_mes_edhr_deviation_code` \(`tenant_id`, `deviation_code`\)/)
  assert.match(migration, /UNIQUE KEY `uk_mes_edhr_deviation_create_idempotency` \(`tenant_id`, `create_idempotency_key`\)/)
  assert.match(migration, /CREATE TABLE IF NOT EXISTS `mes_pro_edhr_deviation_sequence`/)
  assert.match(migration, /PRIMARY KEY \(`tenant_id`, `year_month`\)/)
  assert.match(migration, /CREATE TABLE IF NOT EXISTS `mes_pro_edhr_deviation_create_request`/)
  assert.match(migration, /PRIMARY KEY \(`tenant_id`, `idempotency_key`\)/)
  assert.match(migration, /CREATE TABLE IF NOT EXISTS `mes_pro_edhr_deviation_handling`/)
  assert.match(migration, /UNIQUE KEY `uk_mes_edhr_deviation_handling` \(`tenant_id`, `deviation_id`\)/)
  assert.match(migration, /mes:pro-edhr-deviation:query/)
  assert.match(migration, /mes:pro-edhr-deviation:create/)
  assert.doesNotMatch(migration, /INSERT INTO `system_(role_menu|user_role)`/)
  assert.doesNotMatch(migration, /\bUPDATE\s+`?mes_pro_edhr_(batch_execution|release_transaction|nonconformance_review)`?/i)
})

test('P1 source and number APIs use stable formal batch identity and do not infer by order or batch label', () => {
  const service = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationServiceImpl.java')
  const req = read('src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProEdhrDeviationCreateReqVO.java')
  const sequenceMapper = read('src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrDeviationSequenceMapper.java')

  assert.match(req, /private\s+Long\s+batchExecutionId\s*;/)
  assert.match(service, /selectByTenantIdAndIdForUpdate\(\s*tenantId,\s*reqVO\.getBatchExecutionId\(\)\)/)
  assert.match(service, /selectDeviationOptions/)
  assert.match(sequenceMapper, /LAST_INSERT_ID\(1\)/)
  assert.match(sequenceMapper, /selectLastInsertedId/)
  assert.doesNotMatch(service, /reqVO\.get(?:ActiveOrderId|BatchCode)\(\)/)
})

test('monthly sequence mapper quotes MySQL YEAR_MONTH identifier on allocation', () => {
  const sequenceMapper = read('src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrDeviationSequenceMapper.java')

  assert.match(sequenceMapper, /\(tenant_id,\s*`year_month`,\s*`last_value`\)/,
    'MySQL treats the unquoted YEAR_MONTH and LAST_VALUE identifiers as reserved in this migration contract')
  assert.match(sequenceMapper, /`last_value`\s*=\s*LAST_INSERT_ID\(`last_value`\s*\+\s*1\)/,
    'the increment update must quote the LAST_VALUE column on both sides')
  assert.match(sequenceMapper, /VALUES \(#\{tenantId\}, #\{yearMonth\}, LAST_INSERT_ID\(1\)\)/,
    'the initial monthly sequence value must use the same Shanghai month as its unique key')
})

test('market release takes the batch lock before its transaction lock and blocks open deviations', () => {
  const release = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java')
  const entryStart = release.indexOf('public MesProEdhrReleaseRespVO finalizeRelease(')
  const entryEnd = release.indexOf('private void hydrateBatchExecutionIdFromReleaseTransaction(', entryStart)
  assert.ok(entryStart >= 0 && entryEnd > entryStart, 'release entry must expose its pre-finalizer path')
  const preFinalizer = release.slice(entryStart, entryEnd)
  assert.match(preFinalizer, /requireTransaction\(/,
    'pre-finalizer replay/hydration lookup must be non-locking')
  assert.doesNotMatch(preFinalizer, /requireTransactionForUpdate\(/,
    'pre-finalizer must not lock the release transaction before the batch lock')

  const start = release.indexOf('private MesProEdhrReleaseRespVO finalizeApproval(')
  const end = release.indexOf('private void closeBatchAfterFinalRelease(', start)
  assert.ok(start >= 0 && end > start, 'market release finalizer must remain an explicit service boundary')
  const finalizer = release.slice(start, end)
  const batchLock = finalizer.indexOf('selectByTenantIdAndIdForUpdate(')
  const transactionLock = finalizer.indexOf('requireTransactionForUpdate(')
  assert.ok(batchLock >= 0 && transactionLock > batchLock,
    'market release and deviation create must acquire the batch row before the release transaction row')
  assert.match(finalizer, /ensureNoOpenDeviationForMarketRelease\(/,
    'market release must reject an open deviation after acquiring the shared lock')
})

test('P1 initiation signs canonical content and appends GxP audit without credential hashing', () => {
  const service = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationServiceImpl.java')
  const signatureService = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java')
  const signatureAdapter = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesBatchRecordSignatureSubjectAdapter.java')
  const req = read('src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProEdhrDeviationCreateReqVO.java')
  const migration = fs.readFileSync(migrationPath, 'utf8')

  assert.match(service, /@GxpWriteOperation\(operationId\s*=\s*"edhr\.deviation\.create"\)/)
  assert.match(service, /recordDeviationInitiationSignature\(/)
  assert.match(service, /gxpAuditService\.append\(/)
  assert.match(service, /signatureRecordId\(String\.valueOf\(deviation\.getInitiatorSignatureId\(\)\)\)/)
  assert.match(signatureService, /ACTION_DEVIATION_INITIATION/)
  assert.match(signatureService, /ACTION_NONCONFORMANCE_REVIEW_CREATE/)
  assert.match(signatureService, /"EDHR_DEVIATION", deviationId/)
  assert.match(signatureAdapter, /ACTION_DEVIATION_INITIATION/)
  assert.match(signatureAdapter, /ACTION_NONCONFORMANCE_REVIEW_CREATE/)
  assert.match(req, /signaturePassword/)
  assert.doesNotMatch(service, /JSON\.toJSONString\(reqVO\)/,
    'password must not be included in the idempotency payload')
  assert.doesNotMatch(migration, /system_role_menu|system_user_role/)
  assert.match(migration, /edhr\.deviation\.create/,
    'the approved, role-owned GxP operation must be registered by migration')
  assert.match(migration, /ROLE_QA_QUALITY_OWNER/,
    'GxP policy ownership must be represented by a permission role')
})
