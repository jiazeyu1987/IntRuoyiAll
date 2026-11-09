const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const calls = []
const api = {}
const source = fs.readFileSync(path.resolve(__dirname, '../../src/api/dcc/controlledFile/projectProductRequests.ts'), 'utf8')
vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText, {
  exports: api, Error, require: id => {
    if (id === '@/config/axios') return { default: { post: async request => {
      calls.push(request)
      return { id: '123', status: request.url.endsWith('/reject') ? 'REJECTED' : request.url.includes('/review/') ? 'PENDING_APPROVAL' : 'COMPLETED' }
    } } }
    if (id.endsWith('/project-reviewer')) return { projectRequestContract: () => {} }
    if (id.endsWith('/project-product-role')) return {}
    throw new Error('Unexpected test import: ' + id)
  }
})
const center = fs.readFileSync(path.resolve(__dirname, '../../src/views/approval-center/index.vue'), 'utf8')
const moduleHandling = center.slice(center.indexOf('const canReviewInModule ='), center.indexOf('const canReviewAction ='))
const queryParams = { viewType: 'TODO' }
const canReviewInModule = vm.runInNewContext(ts.transpileModule(moduleHandling + '\ncanReviewInModule', { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText, {
  queryParams, bpmMissingPermissionReason: () => '', canOpenView: row => !!row.detailRoute, canProcessBpmInModule: () => false
})
for (const sourceTaskType of ['DCC_PROJECT_PRODUCT_REVIEW', 'DCC_PROJECT_PRODUCT_APPROVAL']) {
  const row = { moduleCode: 'DCC', sourceTaskType, availableActions: ['PROCESS_IN_MODULE'], detailRoute: '/mdm/product-catalog' }
  assert.equal(canReviewInModule(row), true)
  assert.equal(canReviewInModule({ ...row, businessDeleted: true }), false)
  assert.equal(canReviewInModule({ ...row, availableActions: [] }), false)
  assert.equal(canReviewInModule({ ...row, detailRoute: '' }), false)
  queryParams.viewType = 'DONE'; assert.equal(canReviewInModule(row), false); queryParams.viewType = 'TODO'
}
assert.equal(canReviewInModule({ moduleCode: 'DCC', sourceTaskType: 'UNRELATED', availableActions: ['PROCESS_IN_MODULE'], detailRoute: '/unrelated' }), false)
;(async () => {
  for (const action of [api.reviewDccProjectProductRequest, api.approveDccProjectProductRequest]) {
    for (const reason of ['', '   ', '同意']) {
      await action('123', { reason }, true)
      assert.equal(calls.at(-1).data.reason, reason.trim())
    }
    const before = calls.length
    for (const reason of ['', '   ', 'x'.repeat(501)]) await assert.rejects(() => action('123', { reason }, false))
    await assert.rejects(() => action('123', { reason: 'x'.repeat(501) }, true))
    assert.equal(calls.length, before, 'Invalid decisions must not send a request')
    await action('123', { reason: ' 必须说明驳回原因 ' }, false)
    assert.equal(calls.at(-1).data.reason, '必须说明驳回原因')
  }
  await assert.rejects(() => api.retryDccProjectProductRequestWrite('123', { reason: '' }))
  await api.retryDccProjectProductRequestWrite('123', { reason: ' 修复后重试 ' })
  assert.equal(calls.at(-1).data.reason, '修复后重试')
  console.log('PASS: project approval permits blank opinions; rejection, retry and length guards remain enforced')
})().catch(error => { console.error(error); process.exitCode = 1 })
