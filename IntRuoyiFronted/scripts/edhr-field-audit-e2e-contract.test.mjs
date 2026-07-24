import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const e2eRelativePath = 'tests/e2e/edhr-field-audit-real-flow.e2e.js'
const e2ePath = path.join(root, e2eRelativePath)
const packagePath = path.join(root, 'package.json')

const readText = (absolutePath) => fs.readFileSync(absolutePath, 'utf8')

test('BDD: 字段审计真实路径 E2E 文件与 package scripts 必须存在', () => {
  assert.ok(fs.existsSync(e2ePath), `必须新增 ${e2eRelativePath}`)

  const packageJson = JSON.parse(readText(packagePath))
  assert.equal(
    packageJson.scripts?.['e2e:edhr:field-audit:check'],
    'node --check tests/e2e/edhr-field-audit-real-flow.e2e.js',
    'package.json 必须提供字段审计真实 E2E 语法检查脚本。'
  )
  assert.equal(
    packageJson.scripts?.['e2e:edhr:field-audit'],
    'node tests/e2e/edhr-field-audit-real-flow.e2e.js',
    'package.json 必须提供字段审计真实 E2E 执行脚本。'
  )
})

test('BDD: 字段审计真实路径 E2E 必须登录测试租户并打开列表路由', () => {
  const source = readText(e2ePath)

  for (const token of [
    'EDHR_FIELD_AUDIT_BASE_URL',
    'http://localhost:8081',
    'EDHR_FIELD_AUDIT_TENANT',
    '测试租户',
    'EDHR_FIELD_AUDIT_USERNAME',
    'aoteman',
    'EDHR_FIELD_AUDIT_PASSWORD',
    'EDHR_FIELD_AUDIT_EXECUTION_ID',
    "'40'"
  ]) {
    assert.match(source, new RegExp(token.replaceAll('/', '\\/')), `真实 E2E 必须包含默认配置 ${token}`)
  }

  assert.match(source, /form\.login-form[\s\S]*请输入用户名[\s\S]*请输入密码[\s\S]*登录/s, 'E2E 必须通过真实登录表单登录。')
  assert.match(source, /input\.el-select__input:visible[\s\S]*config\.tenant/s, 'E2E 必须选择真实测试租户。')
  assert.doesNotMatch(source, /admin123|DEFAULT_PASSWORD/, '真实测试密码不得写入脚本默认值。')
  assert.match(
    source,
    /\/mes\/pro\/feedback\/edhr-field-audit\?executionId=|buildFieldAuditListUrl/s,
    'E2E 必须打开 /mes/pro/feedback/edhr-field-audit?executionId=...。'
  )
})

test('BDD: 字段审计真实路径 E2E 必须覆盖列表、详情、校验链和导出 API', () => {
  const source = readText(e2ePath)

  for (const endpoint of [
    '/mes/pro/batch-record-execution/field-audit/page',
    '/mes/pro/batch-record-execution/field-audit/detail',
    '/mes/pro/batch-record-execution/field-audit/verify-chain',
    '/mes/pro/batch-record-execution/field-audit/export'
  ]) {
    assert.match(source, new RegExp(endpoint.replaceAll('/', '\\/')), `E2E 必须等待真实接口 ${endpoint}`)
  }

  assert.match(source, /(getByRole\('button',\s*\{\s*name:\s*\/\^详情\$\/\s*\}|clickVisibleButton\(listRow,\s*\/\^详情\$\/)/, 'E2E 必须点击列表“详情”。')
  assert.match(source, /waitForURL[\s\S]*(\/mes\/pro\/feedback\/edhr-field-audit\/detail|FIELD_AUDIT_DETAIL_ROUTE)/s, 'E2E 必须等待进入详情页。')
  assert.match(source, /校验当前筛选结果|校验链/, 'E2E 必须通过页面按钮触发字段审计链校验。')
  assert.match(source, /导出审计链/, 'E2E 必须通过页面按钮触发字段审计链导出。')
})

test('BDD: 字段审计真实路径 E2E 必须断言关键审计证据且 fail fast', () => {
  const source = readText(e2ePath)

  for (const token of [
    'hashVerification',
    'fileName',
    'content',
    'contentType',
    'sha256',
    'recordCount',
    'fieldPath',
    'oldValueJson',
    'newValueJson',
    'reasonText',
    'actorName',
    'signatureId',
    'auditHash'
  ]) {
    assert.match(source, new RegExp(token), `E2E 必须断言关键字段 ${token}`)
  }

  assert.match(source, /hashVerification\.status[\s\S]*VALID/, '字段审计链校验非 VALID 必须失败暴露。')
  assert.match(
    source,
    /assertFieldAuditRowUiVisible[\s\S]*oldValueDisplay[\s\S]*oldValueJson[\s\S]*newValueDisplay[\s\S]*newValueJson/s,
    'E2E 必须断言列表/详情页面正文实际展示旧值和新值。'
  )
  assert.match(
    source,
    /openFieldAuditList[\s\S]*assertFieldAuditRowUiVisible\(bodyText,\s*row,\s*'字段审计列表目标行'\)/s,
    'E2E 必须在列表页正文断言 old/new 等关键审计字段可见。'
  )
  assert.match(
    source,
    /for \(const item of detailData\.items\)[\s\S]*assertFieldAuditRowUiVisible\(bodyText,\s*item,\s*'字段审计详情 items'\)/s,
    'E2E 必须在详情页正文逐项断言 items 关键审计内容可见。'
  )
  assert.match(
    source,
    /assertFieldAuditRowUiVisible[\s\S]*fieldPath[\s\S]*fieldKey[\s\S]*reasonText[\s\S]*reasonCategory[\s\S]*actorName[\s\S]*signatureId[\s\S]*auditHash/s,
    'E2E 详情 items UI 断言必须覆盖 fieldPath/fieldKey、reason、actorName、signatureId/auditHash。'
  )
  assert.match(source, /设置 EDHR_FIELD_AUDIT_EXECUTION_ID|真实字段审计数据/, '缺少真实字段审计数据时必须提示设置真实 executionId。')
  assert.doesNotMatch(source, /\bmock\b|page\.route\(|fulfill\(|default-success|默认成功/i, 'E2E 不得 mock、拦截接口或默认成功。')
})

test('BDD: 字段审计定位执行记录必须使用真实列表行 executionCode 且空文本 fail fast', () => {
  const source = readText(e2ePath)

  assert.match(
    source,
    /async function openExecutionFromFieldAuditList\(page,\s*config,\s*row\)/,
    '定位执行记录 helper 必须接收字段审计列表真实 row，不能依赖未初始化的 config.executionCode。'
  )
  assert.match(
    source,
    /filter\(\{\s*hasText:\s*row\.executionCode\s*\}\)/,
    '定位执行记录必须使用列表接口返回的 row.executionCode 选择目标行。'
  )
  assert.match(
    source,
    /waitForText\(page,\s*row\.executionCode,\s*`执行详情页未展示执行编号 \$\{row\.executionCode\}`\)/,
    '定位后必须断言详情页展示同一 row.executionCode。'
  )
  assert.match(
    source,
    /openExecutionFromFieldAuditList\(page,\s*config,\s*listResult\.row\)/,
    '真实流程必须把 openFieldAuditList 返回的 row 传给定位执行记录步骤。'
  )
  assert.match(
    source,
    /if \(!textOrPattern\)[\s\S]*expected text\/pattern is missing/,
    'waitForText 必须对空文本或空正则 fail fast，避免 Playwright 内部 undefined selector 异常。'
  )
})
