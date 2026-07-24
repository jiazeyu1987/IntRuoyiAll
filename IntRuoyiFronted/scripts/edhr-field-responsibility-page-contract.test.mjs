import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))
const page = readText('src/views/mes/pro/edhr/FieldAuditPage.vue')
const executionPage = readText('src/views/mes/pro/edhr/ExecutionPage.vue')
const pkg = JSON.parse(readText('package.json'))

test('BDD: 当前责任汇总入口 -> 字段审计页必须提供责任视图且保留变更流水', () => {
  assert.equal(
    pkg.scripts?.['edhr:field-responsibility:page-contract'],
    'node scripts/edhr-field-responsibility-page-contract.test.mjs',
    'package.json 必须暴露字段责任页面合同测试'
  )
  for (const token of [
    '当前责任汇总',
    '变更流水',
    'activeView',
    '责任视图仅展示实际证据',
    'getEdhrFieldResponsibilitySummary',
    'getEdhrFieldResponsibilityHistory',
    'exportEdhrFieldResponsibility',
    'responsibility-summary',
    'responsibility-history',
    'responsibility-export'
  ]) {
    assert.match(page, new RegExp(token), `FieldAuditPage 必须包含 ${token}`)
  }
  assert.match(page, /getEdhrFieldAuditPage/, '现有字段审计流水查询必须保留')
  assert.match(page, /getEdhrFieldAuditDetail|edhr-field-audit\/detail/, '现有字段审计详情入口必须保留')
})

test('BDD: 管理员看谁填了什么 -> 责任表必须展示当前值、首次填写人、当前操作人和证据状态', () => {
  for (const token of [
    'currentValueDisplay',
    'firstHumanActorName',
    'firstHumanChangedAt',
    'currentValueActorName',
    'currentValueChangedAt',
    'valueOrigin',
    'evidenceStatus',
    'reasonCodes',
    'contextWarnings',
    'historyCount',
    'latestAuditItemId',
    '查看历史',
    '查看审计'
  ]) {
    assert.match(page, new RegExp(token), `责任汇总必须展示或使用 ${token}`)
  }
  assert.doesNotMatch(page, /creatorName|updaterName|createdBy|updatedBy/, '责任视图不得用创建人/更新人兜底实际填写人')
})

test('BDD: 责任查询 fail fast -> 缺少 executionId、无权限和导出异常必须显式暴露', () => {
  for (const token of [
    '缺少执行ID，无法加载字段责任汇总',
    '当前账号没有字段审计查询权限',
    '责任证明导出',
    '字段责任导出响应缺少 contentBase64',
    'mes:pro-batch-record-execution:field-audit-export'
  ]) {
    assert.match(page, new RegExp(token), `责任查询错误处理必须包含 ${token}`)
  }
  assert.doesNotMatch(page, /catch\s*\{\s*\}/, '不得空 catch 吞掉责任查询或导出错误')
})

test('BDD: 执行入口 -> 字段审计入口应能带入责任视图参数', () => {
  assert.match(page, /view.*responsibility|activeView/, 'FieldAuditPage 必须支持责任视图路由或状态')
  assert.doesNotMatch(executionPage, /responsibilityStatus|originType|warningCodes/, 'ExecutionPage 不得引入责任合同兼容别名')
})

test('BDD: 表单填写工作台 -> 待保存变更必须在当前工作台内填写原因后签名保存', () => {
  for (const token of [
    'edhr-fill-workspace__field-audit-reason',
    'fieldAuditReasonForm.reasonCategory',
    'EDHR_FIELD_CHANGE_REASON_OPTIONS',
    'fieldAuditReasonForm.reasonText',
    'canOpenFieldAuditSignatureDialog'
  ]) {
    assert.match(executionPage, new RegExp(token), `ExecutionPage 填写工作台必须包含 ${token}`)
  }
})

test('BDD: 字段责任真实 E2E -> 必须覆盖责任汇总、历史、导出和 admin 只读保护', () => {
  const e2ePath = 'tests/e2e/edhr-field-responsibility-real-flow.e2e.js'
  assert.equal(
    pkg.scripts?.['e2e:edhr:field-responsibility:check'],
    `node --check ${e2ePath}`,
    'package.json 必须提供字段责任真实 E2E 语法检查脚本'
  )
  assert.equal(
    pkg.scripts?.['e2e:edhr:field-responsibility'],
    `node ${e2ePath}`,
    'package.json 必须提供字段责任真实 E2E 执行脚本'
  )
  assert.equal(exists(e2ePath), true, '字段责任真实 E2E 文件必须存在')
  const e2e = readText(e2ePath)
  for (const token of [
    '/mes/pro/feedback/edhr-field-audit',
    'view=responsibility',
    '/mes/pro/batch-record-execution/field-audit/responsibility-summary',
    '/mes/pro/batch-record-execution/field-audit/responsibility-history',
    '/mes/pro/batch-record-execution/field-audit/responsibility-export',
    '测试租户',
    'aoteman',
    '芋道源码',
    'admin readonly',
    'writeRequests'
  ]) {
    assert.match(e2e, new RegExp(token.replaceAll('/', '\\/')), `真实 E2E 必须包含 ${token}`)
  }
  assert.match(e2e, /chromium\.launch|require\('playwright'\)/, '真实 E2E 必须使用 Playwright 打开真实页面')
  assert.doesNotMatch(e2e, /\bmock\b|page\.route\(|fulfill\(|default-success|默认成功/i, '真实 E2E 不得使用 mock、接口拦截或默认成功路径')
})
