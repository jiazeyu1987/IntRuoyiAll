const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const projectRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(projectRoot, relativePath), 'utf8')

const apiSource = read('src/api/mes/pro/batchrecordreport/index.ts')
const pageSource = read('src/views/mes/pro/batchrecordformlist/index.vue')

for (const field of [
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

console.log('PASS: batch-record Word import route candidate governance static contract')
