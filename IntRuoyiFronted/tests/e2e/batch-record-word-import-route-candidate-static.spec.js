const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const projectRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(projectRoot, relativePath), 'utf8')

const apiSource = read('src/api/mes/pro/batchrecordreport/index.ts')
const pageSource = read('src/views/mes/pro/batchrecordformlist/index.vue')

for (const field of [
  'currentRouteStatus?: number',
  'routeRestoreRequired?: boolean',
  'currentRouteCandidateVersionId?: number',
  'currentRouteCandidateVersionNo?: string',
  'currentRouteCandidateVersionStatus?: string'
]) {
  assert.ok(apiSource.includes(field), `预检响应必须包含候选版本字段：${field}`)
}

assert.match(
  apiSource,
  /if \(expectedRouteCandidateVersionId != null\) \{\s*data\.append\('expectedRouteCandidateVersionId', String\(expectedRouteCandidateVersionId\)\)/,
  'Word 导入必须仅在预检存在候选版本时提交候选版本 ID，防止空候选被序列化为字符串 null。'
)

assert.ok(
  pageSource.includes('更新现有') &&
    pageSource.includes('草稿') &&
    pageSource.includes('不会创建') &&
    pageSource.includes('待发布后生效'),
  '已有 DRAFT 候选时，页面必须明确提示更新现有草稿且不会创建下一版本。'
)

assert.ok(
  pageSource.includes('确认后将先恢复路线，再生成/更新候选版本') &&
    pageSource.includes('routeRestoreRequired'),
  '唯一禁用路线导入时必须提示会先恢复路线，再生成或更新候选版本。'
)

assert.ok(
  pageSource.includes("routeGovernanceStatus === 'DUPLICATE_BLOCKED'") &&
    pageSource.includes('所选 DCC 项目代码存在多条正式路线绑定') &&
    pageSource.includes('请先人工确定/清理唯一保留路线'),
  '存在重复 DCC 正式路线绑定时，页面必须阻止导入并提示先清理唯一保留路线。'
)

assert.ok(
  pageSource.includes("'PENDING_APPROVAL'") &&
    pageSource.includes("'READY_TO_PUBLISH'") &&
    pageSource.includes('撤回、取消或完成发布'),
  '待审批或待发布候选必须显示明确处理方式。'
)

assert.match(
  pageSource,
  /expectedRouteCandidateVersionId:\s*wordImportDialog\.preflight\?\.currentRouteCandidateVersionId/,
  '页面必须冻结预检候选版本 ID。'
)

assert.match(
  pageSource,
  /:disabled="isWordImportRouteCandidateLocked\(wordImportDialog\.preflight\)"/,
  '待审批或待发布候选存在时必须禁用工艺流程选择。'
)

assert.match(
  pageSource,
  /const routeFlowRebuildRequested = selection\.selectedOptions\.length > 0/,
  '前端必须用“工艺流程”勾选项单独判定是否按 Word 重建 flowGraph，不能复用批记录表单勾选值。'
)

assert.match(
  pageSource,
  /const batchRecordBindingCandidateRequested = Boolean\(\s*selection\.routeUpgradeRequired && rebuildBatchRecord && !routeFlowRebuildRequested\s*\)/,
  '仅导入批记录表单绑定时，可以生成绑定候选，但必须显式标识为非工艺流程重建。'
)

assert.ok(
  !pageSource.includes('selection.selectedOptions.length || rebuildBatchRecord'),
  '未勾选“工艺流程”时不得把批记录表单勾选值当作路线 flowGraph 重建触发条件。'
)

assert.ok(
  pageSource.includes('批记录表单绑定候选') &&
    pageSource.includes('沿用当前工艺流程节点和流程关系') &&
    pageSource.includes('不按 Word 重建工艺流程'),
  '仅批记录表单绑定候选的用户提示必须说明沿用 ACTIVE flowGraph，不按 Word 重建工艺流程。'
)

console.log('PASS: batch-record Word import route candidate governance static contract')
