const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vm = require('node:vm')
const root = path.resolve(__dirname, '../..')
const file = path.join(root, 'src/views/mes/pro/processpool/activeOrderReplenishmentConfirmation.ts')
assert.ok(fs.existsSync(file), '缺少完工无补料确认处理')
const source = fs.readFileSync(file, 'utf8')
const sandbox = { exports: {} }
vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, sandbox)
const apply = sandbox.exports.applyWithNoReplenishmentConfirmation
const missing = new Error('NO_REPLENISHMENT_CONFIRMATION_REQUIRED')
const message = error => error.message
;(async () => {
  let calls = [], confirms = 0
  const success = await apply(async flag => { calls.push(flag); return 'receipt' }, async () => { confirms++ }, message)
  assert.equal(success, 'receipt')
  assert.deepEqual(calls, [undefined])
  assert.equal(confirms, 0)
  calls = []
  const confirmed = await apply(async flag => { calls.push(flag); if (!flag) throw missing; return 'confirmed' }, async () => { confirms++ }, message)
  assert.equal(confirmed, 'confirmed')
  assert.deepEqual(calls, [undefined, true])
  calls = []
  const cancelled = await apply(async flag => { calls.push(flag); throw missing }, async () => { throw 'cancel' }, message)
  assert.equal(cancelled, undefined)
  assert.deepEqual(calls, [undefined])
  await assert.rejects(() => apply(async () => { throw new Error('服务失败') }, async () => { throw new Error('不应确认') }, message), /服务失败/)
  await assert.rejects(() => apply(async flag => { if (!flag) throw missing; throw new Error('重试失败') }, async () => {}, message), /重试失败/)
  const page = fs.readFileSync(path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
  assert.match(page, /applyWithNoReplenishmentConfirmation\(/)
  assert.match(page, /confirmNoReplenishmentInfo/)
  assert.match(page, /确认后将按无正式损耗完成订单/)
  const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/processpool/teamLeader.ts'), 'utf8')
  assert.match(api, /confirmNoReplenishmentInfo\?: boolean/)
  console.log('PASS: 完工无补料确认、取消、失败和请求合同')
})().catch(error => { console.error(error); process.exitCode = 1 })
