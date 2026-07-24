const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const signaturePagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/SignaturePage.vue')
const routePath = path.resolve(process.cwd(), 'src/router/modules/remaining.ts')

const signaturePage = fs.readFileSync(signaturePagePath, 'utf8')
const routeSource = fs.readFileSync(routePath, 'utf8')

const assertIncludes = (source, token, message) => {
  assert.ok(source.includes(token), message)
}

const assertExcludes = (source, token, message) => {
  assert.ok(!source.includes(token), message)
}

for (const token of [
  'const router = useRouter()',
  'class="edhr-signature__execution-link"',
  '@click="openExecution(row)"',
  'const openExecution = async (row: EdhrSignatureSummaryVO)',
  "path: '/mes/pro/feedback/edhr-execution/form'",
  "viewMode: 'tracking'",
  'v-if="row.executionId"',
  'v-else class="edhr-signature-muted"'
]) {
  assertIncludes(signaturePage, token, `签名记录必须提供可直达对应表单的执行编号入口：${token}`)
}

assertExcludes(
  signaturePage,
  "path: '/mes/pro/feedback/edhr-execution/detail'",
  '签名记录执行编号不得继续跳转到执行详情摘要入口。'
)

for (const token of [
  "path: 'pro/feedback/edhr-execution/form'",
  "component: () => import('@/views/mes/pro/edhr/ExecutionPage.vue')",
  "title: 'eDHR执行表单'",
  "activeMenu: '/mes/pro/feedback/edhr-batch-execution'"
]) {
  assertIncludes(routeSource, token, `签名记录表单入口路由缺少契约：${token}`)
}

assertExcludes(
  signaturePage,
  '<el-table-column label="签名编号" prop="id" width="90" />',
  '签名编号不应继续作为默认主表列，应下沉到展开证据。'
)

const signatureEvidenceIndex = signaturePage.indexOf('edhr-signature-evidence')
const signatureIdIndex = signaturePage.indexOf('签名编号')
assert(
  signatureEvidenceIndex > -1 && signatureIdIndex > signatureEvidenceIndex,
  '签名编号必须保留在展开证据区，方便审计追溯。'
)

assert(
  !/mock|降级|静默跳过/.test(signaturePage),
  '执行详情入口优化不得引入 mock、降级或静默跳过。'
)

console.log('PASS: EDHR signature execution entry static contract')
