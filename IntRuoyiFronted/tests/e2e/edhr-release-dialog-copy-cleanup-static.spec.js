const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '..', '..')

const read = (relativePath) => {
  const fullPath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(fullPath), `${relativePath} must exist`)
  return fs.readFileSync(fullPath, 'utf8')
}

const batchDetail = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const releasePage = read('src/views/mes/pro/edhr-release/ReleasePage.vue')
const formTraceReleaseTab = read('src/views/mes/pro/edhr/form-trace/FormTraceReleaseTab.vue')
const releaseEventListPane = read('src/views/mes/pro/edhr/components/ReleaseEventListPane.vue')
const formTraceChangeTab = read('src/views/mes/pro/edhr/form-trace/FormTraceChangeTab.vue')
const operationAuditListPane = read('src/views/mes/pro/edhr/components/OperationAuditListPane.vue')
const operationAuditPage = read('src/views/mes/pro/edhr/OperationAuditPage.vue')
const permissionMatrixPage = read('src/views/mes/pro/edhr/PermissionMatrixPage.vue')
const presentation = read('src/views/mes/pro/edhr/shared/releaseCheckPresentation.ts')

const sourceFiles = [
  ['批次详情', batchDetail],
  ['放行管理', releasePage],
  ['表单追溯放行页签', formTraceReleaseTab]
]

for (const [name, source] of sourceFiles) {
  assert(
    !source.includes('{{ row.checkCode }} / {{ row.checkCategory }}'),
    `${name} 不得展示 checkCode / checkCategory 内部路径组合`
  )
  assert(
    !source.includes('{{ row.sourceObjectType || \'--\' }}'),
    `${name} 不得直接展示 sourceObjectType 内部枚举`
  )
}

for (const forbiddenCopy of ['UX检查', 'UX 检查', '页面级 UX']) {
  assert(!batchDetail.includes(forbiddenCopy), `批次详情放行参数区不得展示英文缩写文案：${forbiddenCopy}`)
}

assert(
  !batchDetail.includes('<el-table-column label="事件" width="100" prop="eventType" />'),
  '批次详情追溯记录不得直接绑定 eventType 英文枚举'
)
assert(
  !batchDetail.includes("{{ row.fromStatus || '--' }} -> {{ row.toStatus || '--' }}"),
  '批次详情追溯记录不得直接展示 fromStatus/toStatus 英文枚举'
)

for (const helperName of [
  'resolveReleaseCheckCodeLabel',
  'resolveReleaseCheckCategoryLabel',
  'resolveReleaseCheckSourceObjectTypeLabel'
]) {
  assert(presentation.includes(`export const ${helperName}`), `共享展示层必须导出 ${helperName}`)
  assert(batchDetail.includes(helperName), `批次详情必须使用 ${helperName}`)
  assert(releasePage.includes(helperName), `放行管理必须使用 ${helperName}`)
  assert(formTraceReleaseTab.includes(helperName), `表单追溯放行页签必须使用 ${helperName}`)
}

for (const helperName of [
  'resolveReleaseEventLabel',
  'resolveReleaseStatusLabel'
]) {
  assert(presentation.includes(`export const ${helperName}`), `共享展示层必须导出 ${helperName}`)
  assert(releaseEventListPane.includes(helperName), `放行事件组件必须使用 ${helperName}`)
  assert(releasePage.includes(helperName), `放行管理必须使用 ${helperName}`)
  assert(formTraceReleaseTab.includes(helperName), `表单追溯放行页签必须使用 ${helperName}`)
}

assert(batchDetail.includes('ReleaseEventListPane'), '批次详情追溯记录必须复用放行事件组件展示事件')
assert(!releaseEventListPane.includes('|| eventType'), '放行事件组件不得在未知事件时直出 eventType 枚举')
assert(!releaseEventListPane.includes('|| status'), '放行事件组件不得在未知状态时直出状态枚举')

for (const forbiddenChangeCopy of ['eDHR 变更详情', 'Head Hash', '归档 Hash']) {
  assert(!formTraceChangeTab.includes(forbiddenChangeCopy), `变更记录弹框不得展示英文描述：${forbiddenChangeCopy}`)
}
assert(formTraceChangeTab.includes('电子批记录变更详情'), '变更详情弹框标题必须使用中文描述')
assert(formTraceChangeTab.includes('原链头哈希'), '变更证据必须使用中文哈希标签')
assert(!formTraceChangeTab.includes('labels[type] || type'), '变更类型未知时不得回显原始枚举')
assert(!formTraceChangeTab.includes('labels[status] || status'), '变更状态未知时不得回显原始枚举')

const operationAuditSurfaces = [
  ['操作审计弹框', operationAuditListPane],
  ['操作审计页面', operationAuditPage]
]

for (const [name, source] of operationAuditSurfaces) {
  for (const forbiddenAuditCopy of [
  '{{ row.objectType }} / {{ row.objectId }}',
  '{{ detail.objectType }} / {{ detail.objectId }}',
  'execution={{',
  '{{ row.operationType || \'--\' }}',
  '{{ row.resultStatus || \'--\' }}',
  '{{ row.permissionDecision || \'--\' }}',
  '对象级 eDHR 操作审计',
  'eDHR 操作审计'
  ]) {
    assert(!source.includes(forbiddenAuditCopy), `${name}不得展示原始技术描述：${forbiddenAuditCopy}`)
  }
}

for (const helperName of [
  'resolveOperationActionLabel',
  'resolveOperationAuditObjectTypeLabel',
  'resolveOperationTypeLabel',
  'resolveResultStatusLabel',
  'resolvePermissionDecisionLabel'
]) {
  assert(operationAuditListPane.includes(helperName), `操作审计弹框必须使用中文展示函数：${helperName}`)
  assert(operationAuditPage.includes(helperName), `操作审计页面必须使用中文展示函数：${helperName}`)
}

for (const helperName of [
  'resolveOperationAuditObjectTypeLabel',
  'resolveOperationTypeLabel',
  'resolveOperationActionLabel',
  'resolveOperationAuditResultStatusLabel',
  'resolveOperationAuditPermissionDecisionLabel'
]) {
  assert(presentation.includes(`export const ${helperName}`), `共享展示层必须导出操作审计中文展示函数：${helperName}`)
}

for (const forbiddenPermissionCopy of [
  "{{ result.objectType || '--' }} / {{ result.objectId || '--' }}",
  "{{ result.objectType || '--' }}",
  '{{ row.ability }}',
  "{{ row.decision || '未返回' }}"
]) {
  assert(!permissionMatrixPage.includes(forbiddenPermissionCopy), `权限评估结果不得展示原始技术值：${forbiddenPermissionCopy}`)
}
assert(permissionMatrixPage.includes('formatObjectSummary'), '权限评估结果必须用中文对象摘要展示评估对象')
assert(
  permissionMatrixPage.includes('resolveOperationAuditObjectTypeLabel'),
  '权限评估证据必须用中文对象类型展示函数'
)

for (const forbiddenRawValue of ['INVENTORY_CONSISTENCY / INVENTORY', 'SCRAP_RECORDED / SCRAP', 'REWORK_CLOSED / REWORK']) {
  assert(!batchDetail.includes(forbiddenRawValue), `批次详情不得包含内部路径示例：${forbiddenRawValue}`)
  assert(!releasePage.includes(forbiddenRawValue), `放行管理不得包含内部路径示例：${forbiddenRawValue}`)
  assert(!formTraceReleaseTab.includes(forbiddenRawValue), `表单追溯放行页签不得包含内部路径示例：${forbiddenRawValue}`)
}

console.log('PASS: eDHR release dialog copy cleanup static contract')
