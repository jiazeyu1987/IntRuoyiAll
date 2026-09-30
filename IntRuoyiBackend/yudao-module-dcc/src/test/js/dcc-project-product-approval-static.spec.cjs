const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..', '..')
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), 'utf8')

const service = read(
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'dcc',
  'service',
  'projectcode',
  'productcreate',
  'DccProjectProductCreateServiceImpl.java'
)
const writeService = read(
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'dcc',
  'service',
  'projectcode',
  'productcreate',
  'DccProjectProductCreateWriteService.java'
)
const stateService = read(
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'dcc',
  'service',
  'projectcode',
  'productcreate',
  'DccProjectProductCreateStateService.java'
)
const failureService = read(
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'dcc',
  'service',
  'projectcode',
  'productcreate',
  'DccProjectProductCreateFailureService.java'
)
const projectCodeService = read(
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'dcc',
  'service',
  'projectcode',
  'DccProjectCodeServiceImpl.java'
)
const productCatalogService = read(
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'dcc',
  'service',
  'productcatalog',
  'DccProductCatalogServiceImpl.java'
)
const migration = fs.readFileSync(
  path.join(root, '..', 'sql', 'mysql', '20260920_dcc_project_product_create_approval.sql'),
  'utf8'
)
const claimDo = read(
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'dcc',
  'dal',
  'dataobject',
  'projectcode',
  'DccProjectProductIdentityClaimDO.java'
)
const requestMapper = read(
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'dcc',
  'dal',
  'mysql',
  'projectcode',
  'DccProjectProductCreateRequestMapper.java'
)
const claimMapper = read(
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'dcc',
  'dal',
  'mysql',
  'projectcode',
  'DccProjectProductIdentityClaimMapper.java'
)

for (const token of [
  'DccProjectProductCreateStatusConstants.PENDING_REVIEW',
  'DccProjectProductCreateStatusConstants.PENDING_APPROVAL',
  'requireAdmin(operatorUserId)',
  '"admin".equals(user.getUsername())',
  'claimIdentity(request.getId(), IDENTITY_PROJECT_CODE, projectCode)',
  'claimIdentity(request.getId(), IDENTITY_PRODUCT_CODE, productCode)',
  'claimIdentity(request.getId(), IDENTITY_PRODUCT_NAME, productName)',
  'failureService.markWriteFailed',
  'throw ex'
]) {
  if (!service.includes(token)) {
    throw new Error(`联合新建服务缺少合同：${token}`)
  }
}

for (const token of [
  '@Transactional(rollbackFor = Exception.class)',
  'DccProjectProductCreateStatusConstants.WRITING',
  'DccProjectProductCreateStatusConstants.COMPLETED',
  'projectCodeMapper.insert(projectCode)',
  'productCatalogMapper.insert(productCatalog)',
  'relationMapper.insert(relation)',
  'claimedByOtherRequest(IDENTITY_PROJECT_CODE, projectCode, requestId)',
  'claimedByOtherRequest(IDENTITY_PRODUCT_CODE, productCode, requestId)',
  'claimedByOtherRequest(IDENTITY_PRODUCT_NAME, productName, requestId)'
]) {
  if (!writeService.includes(token)) {
    throw new Error(`正式写入服务缺少合同：${token}`)
  }
}

if (!stateService.includes('markApprovalDecision') || !stateService.includes('markRetryWriting')) {
  throw new Error('状态流转服务缺少批准或重试写入状态切换')
}

if (!failureService.includes('Propagation.REQUIRES_NEW') || !failureService.includes('WRITE_FAILED')) {
  throw new Error('写入失败标记必须使用独立事务并落 WRITE_FAILED')
}
if (!projectCodeService.includes('DCC_PROJECT_CODE_DIRECT_CREATE_NOT_ALLOWED')) {
  throw new Error('DCC 项目代码直接新增未禁用')
}
if (!productCatalogService.includes('DCC_PRODUCT_CATALOG_DIRECT_CREATE_NOT_ALLOWED')) {
  throw new Error('DCC 产品目录直接新增未禁用')
}
for (const ddl of [
  'CREATE TABLE IF NOT EXISTS dcc_project_product_create_request',
  'CREATE TABLE IF NOT EXISTS dcc_project_product_relation',
  'CREATE TABLE IF NOT EXISTS dcc_project_product_identity_claim',
  'id BIGINT NOT NULL AUTO_INCREMENT',
  'UNIQUE KEY uk_dcc_ppic_identity',
  'UNIQUE KEY uk_dcc_ppr_project',
  'UNIQUE KEY uk_dcc_ppr_product'
]) {
  if (!migration.includes(ddl)) {
    throw new Error(`迁移缺少：${ddl}`)
  }
}

if (!claimDo.includes('@TableName("dcc_project_product_identity_claim")')) {
  throw new Error('唯一身份占用 DO 未绑定到 dcc_project_product_identity_claim')
}
if (!requestMapper.includes('DccProjectProductCreateStatusConstants.WRITING')) {
  throw new Error('待办列表必须包含 WRITING，防止写入中状态从审批面板消失')
}
if (!claimMapper.includes('@Delete("DELETE FROM dcc_project_product_identity_claim WHERE request_id = #{requestId}")')) {
  throw new Error('驳回释放唯一身份占用必须物理删除，避免逻辑删除唯一键冲突')
}

console.log('DCC backend project-product approval static contract passed')
