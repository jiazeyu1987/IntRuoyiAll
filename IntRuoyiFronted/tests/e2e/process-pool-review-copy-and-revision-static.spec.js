const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const files = {
  packageJson: 'package.json',
  remainingRouter: 'src/router/modules/remaining.ts',
  timelinePage: 'src/views/mes/pro/processpool/TimelinePage.vue',
  reviewCopyPage: 'src/views/mes/pro/processpool/ReviewCopyPage.vue',
  eventRevisionPage: 'src/views/mes/pro/processpool/EventRevisionPage.vue',
  reviewCopyApi: 'src/api/mes/pro/processpool/reviewCopy.ts',
  eventRevisionApi: 'src/api/mes/pro/processpool/eventRevision.ts',
  realE2e: 'tests/e2e/process-pool-review-copy-and-revision.spec.ts'
}

for (const [name, relativePath] of Object.entries(files)) {
  assert.ok(exists(relativePath), `${name} must exist for the F5/F6 write-path frontend contract.`)
}

const packageJson = JSON.parse(read(files.packageJson))
const remainingRouter = read(files.remainingRouter)
const timelinePage = read(files.timelinePage)
const reviewCopyPage = read(files.reviewCopyPage)
const eventRevisionPage = read(files.eventRevisionPage)
const realE2e = read(files.realE2e)

assert.equal(
  packageJson.scripts?.['test:e2e'],
  'playwright test',
  'package.json must provide a standard Playwright test:e2e command.'
)

for (const token of [
  "path: 'pro/process-pool/review-copy'",
  "component: () => import('@/views/mes/pro/processpool/ReviewCopyPage.vue')",
  "name: 'MesProProcessPoolReviewCopy'",
  "permission: ['mes:pro-process-pool-review-copy:generate-submit']",
  "path: 'pro/process-pool/event-revision'",
  "component: () => import('@/views/mes/pro/processpool/EventRevisionPage.vue')",
  "name: 'MesProProcessPoolEventRevision'",
  "permission: ['mes:pro-process-pool:event-revision:update']"
]) {
  assert.ok(remainingRouter.includes(token), `remaining router must include ${token}.`)
}

for (const token of [
  '审核副本处理',
  '生成并提交审核副本',
  '工序池提交事件ID',
  '审核人用户ID',
  '审核签名ID',
  '审核签名快照JSON',
  '字段上下限映射JSON',
  'generateSubmitProcessPoolReviewCopy',
  'parseJsonField',
  'JSON.parse',
  'resultReviewCopyId'
]) {
  assert.ok(reviewCopyPage.includes(token), `review copy page must include ${token}.`)
}
assert.doesNotMatch(reviewCopyPage, /mock|localStorage|sessionStorage|timeline/i, 'review copy page must not use mocks, browser storage fallback, or timeline writes.')

for (const token of [
  '原始记录修改',
  '修改并重新签名',
  '工序池提交事件ID',
  '修改人用户ID',
  '变更原因',
  '修改后payload JSON',
  '修改签名快照JSON',
  '字段变更JSON',
  'updateProcessPoolOriginalRecord',
  'parseJsonField',
  'JSON.parse',
  'resultRevisionId'
]) {
  assert.ok(eventRevisionPage.includes(token), `event revision page must include ${token}.`)
}
assert.doesNotMatch(eventRevisionPage, /mock|localStorage|sessionStorage|timeline/i, 'event revision page must not use mocks, browser storage fallback, or timeline writes.')

assert.doesNotMatch(
  timelinePage,
  /generateSubmitProcessPoolReviewCopy|updateProcessPoolOriginalRecord|ReviewCopyPage|EventRevisionPage/,
  'timeline page must remain a read-only traceability surface and must not host F5/F6 write actions.'
)

for (const token of [
  "from 'playwright/test'",
  '/mes/pro/process-pool/review-copy',
  '/mes/pro/process-pool/event-revision',
  '/mes/pro/process-pool/review-copy/generate-submit',
  '/mes/pro/process-pool/event-revision/update-original',
  '生成并提交审核副本',
  '修改并重新签名'
]) {
  assert.ok(realE2e.includes(token), `real E2E spec must include ${token}.`)
}
assert.doesNotMatch(realE2e, /mock|route\.fulfill|page\.route|localStorage|sessionStorage/, 'real E2E must use real frontend paths and must not mock network or storage state.')

console.log('PASS: process-pool review-copy and event-revision frontend write-path contract')
