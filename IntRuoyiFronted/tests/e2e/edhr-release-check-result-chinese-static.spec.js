const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '..', '..')

const read = (relativePath) => {
  const fullPath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(fullPath), `${relativePath} must exist`)
  return fs.readFileSync(fullPath, 'utf8')
}

const presentation = read('src/views/mes/pro/edhr/shared/releaseCheckPresentation.ts')
const detail = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const releasePage = read('src/views/mes/pro/edhr-release/ReleasePage.vue')
const formTraceReleaseTab = read('src/views/mes/pro/edhr/form-trace/FormTraceReleaseTab.vue')
const api = read('src/api/mes/pro/edhr/release.ts')

for (const [code, label] of [
  ['PASS', '通过'],
  ['FAIL', '失败'],
  ['BLOCKER', '阻塞'],
  ['NOT_APPLICABLE', '不适用'],
  ['PRECHECK_REQUIRED', '待检']
]) {
  assert(
    presentation.includes(`${code}: '${label}'`),
    `放行检查结果中文映射必须包含 ${code} -> ${label}`
  )
}

for (const helperName of [
  'resolveReleaseCheckResultLabel',
  'resolveReleaseCheckResultTagType'
]) {
  assert(presentation.includes(`export const ${helperName}`), `必须导出共享方法 ${helperName}`)
  assert(detail.includes(helperName), `批次详情放行检查结果列必须使用 ${helperName}`)
  assert(releasePage.includes(helperName), `放行管理检查项结果列必须使用 ${helperName}`)
  assert(formTraceReleaseTab.includes(helperName), `表单追溯放行检查结果列必须使用 ${helperName}`)
}

assert(
  !detail.includes('<el-table-column label="结果" width="100" prop="checkResult" />'),
  '批次详情放行检查结果列不得直接绑定 checkResult 英文枚举'
)

assert(
  api.includes("'NOT_APPLICABLE'"),
  'EdhrReleaseCheckResult 类型必须声明 NOT_APPLICABLE，避免后端返回不适用结果时前端类型漂移'
)

console.log('PASS: eDHR release check result Chinese static contract')
