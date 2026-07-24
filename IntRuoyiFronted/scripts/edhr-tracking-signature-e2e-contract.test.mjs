import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const e2eRelativePath = 'tests/e2e/edhr-tracking-signature-real-flow.e2e.js'
const e2ePath = path.join(root, e2eRelativePath)
const packagePath = path.join(root, 'package.json')

const readText = (absolutePath) => fs.readFileSync(absolutePath, 'utf8')
const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

test('BDD: 追踪与签名独立真实 E2E 文件与 package scripts 必须存在', () => {
  assert.ok(fs.existsSync(e2ePath), `必须新增 ${e2eRelativePath}`)

  const packageJson = JSON.parse(readText(packagePath))
  assert.equal(
    packageJson.scripts?.['e2e:edhr:tracking-signature:check'],
    'node --check tests/e2e/edhr-tracking-signature-real-flow.e2e.js',
    'package.json 必须提供追踪/签名真实 E2E 语法检查脚本。'
  )
  assert.equal(
    packageJson.scripts?.['e2e:edhr:tracking-signature'],
    'node tests/e2e/edhr-tracking-signature-real-flow.e2e.js',
    'package.json 必须提供追踪/签名真实 E2E 执行脚本。'
  )
})

test('BDD: 追踪与签名真实 E2E 必须声明环境前置并保护测试租户', () => {
  const source = readText(e2ePath)

  for (const token of [
    'EDHR_TRACKING_SIGNATURE_BASE_URL',
    'EDHR_TRACKING_SIGNATURE_TENANT',
    'EDHR_TRACKING_SIGNATURE_USERNAME',
    'EDHR_TRACKING_SIGNATURE_PASSWORD',
    'EDHR_TRACKING_SIGNATURE_EXECUTION_ID',
    'EDHR_TRACKING_SIGNATURE_EXECUTION_CODE',
    'EDHR_TRACKING_SIGNATURE_BATCH_CODE',
    'http://localhost:8081',
    '测试租户',
    'aoteman',
    'FORBIDDEN_LIVE_TENANTS'
  ]) {
    assert.match(source, new RegExp(escapeRegExp(token)), `真实 E2E 必须包含配置或保护 ${token}`)
  }

  assert.match(source, /form\.login-form[\s\S]*请输入用户名[\s\S]*请输入密码[\s\S]*登录/s, 'E2E 必须通过真实登录表单登录。')
  assert.match(source, /input\.el-select__input:visible[\s\S]*config\.tenant/s, 'E2E 必须选择测试租户。')
  assert.match(source, /config\.tenant\s*!==\s*DEFAULT_TENANT/, 'E2E 必须拒绝非测试租户。')
  assert.match(source, /EDHR_TRACKING_SIGNATURE_PASSWORD[\s\S]*测试租户密码必须/, '缺少真实密码时必须 fail fast。')
  const forbiddenPasswordPattern = new RegExp(
    `${'admin'}${'123'}|${'DEFAULT'}_${'PASSWORD'}|password:\\s*['"][^'"]+['"]`
  )
  assert.doesNotMatch(source, forbiddenPasswordPattern, '真实测试密码不得写入脚本默认值。')
})

test('BDD: 追踪与签名真实 E2E 不得使用 mock、fallback 或拦截接口', () => {
  const source = readText(e2ePath)

  assert.doesNotMatch(source, /\bmock\b|fallback|page\.route\(|fulfill\(|default-success|默认成功/i, 'E2E 不得使用 mock/fallback、接口拦截或默认成功路径。')
  assert.match(source, /loadPlaywright[\s\S]*Missing Playwright runtime[\s\S]*throw blocked/s, '缺少 Playwright runtime 必须阻塞退出。')
  assert.match(source, /process\.exitCode\s*=\s*1/g, 'BLOCKED/FAIL 必须退出非零。')
})

test('BDD: 追踪真实 E2E 必须覆盖路由、tracking-page 和 tracking-timeline', () => {
  const source = readText(e2ePath)

  for (const token of [
    "'40'",
    'BRE202605280518101280040',
    'EDHR-BATCH-122-E2E-APPROVE-GATE05280525',
    '/mes/pro/feedback/edhr-tracking',
    '/mes/pro/feedback/edhr-execution/detail',
    '/mes/pro/batch-record-execution/tracking-page',
    '/mes/pro/batch-record-execution/tracking-timeline'
  ]) {
    assert.match(source, new RegExp(escapeRegExp(token)), `真实 E2E 必须包含追踪门禁令牌 ${token}`)
  }

  assert.match(
    source,
    /buildTrackingUrl[\s\S]*searchParams\.set\('executionCode',\s*config\.executionCode\)/s,
    'E2E 必须以 executionCode 打开追踪页。'
  )
  assert.match(source, /clickVisibleButton\(targetRow,\s*\/\^查看\$\/,/, 'E2E 必须点击追踪目标行“查看”。')
  assert.match(
    source,
    /waitForURL[\s\S]*EXECUTION_DETAIL_ROUTE[\s\S]*searchParams\.get\('id'\)\s*===\s*String\(config\.executionId\)/s,
    'E2E 必须等待进入目标执行详情。'
  )
  assert.match(source, /TRACKING_EVENT_TYPES[\s\S]*SUBMIT[\s\S]*APPROVE[\s\S]*ARCHIVE_SEAL/s, '时间线必须断言提交、审批或归档封存事件。')
})

test('BDD: 签名真实 E2E 必须覆盖 signature-page 和 actionType 筛选', () => {
  const source = readText(e2ePath)

  for (const token of [
    '/mes/pro/feedback/edhr-signatures',
    '/mes/pro/batch-record-execution/signature-page',
    'SIGNATURE_ACTION_PRIORITY',
    'ARCHIVE_SEAL',
    'APPROVE',
    'SUBMIT',
    'actionType',
    'PASSWORD',
    'passwordVerified',
    'meaningText',
    'signedAt'
  ]) {
    assert.match(source, new RegExp(escapeRegExp(token)), `真实 E2E 必须覆盖签名门禁令牌 ${token}`)
  }

  assert.match(
    source,
    /buildSignatureUrl[\s\S]*searchParams\.set\('executionId',\s*config\.executionId\)/s,
    'E2E 必须以 executionId 打开签名页。'
  )
  assert.match(source, /selectSignatureAction[\s\S]*selectedAction/s, 'E2E 必须通过页面动作筛选器选择真实 actionType。')
  assert.match(
    source,
    /SIGNATURE_PAGE_ENDPOINT[\s\S]*executionId:\s*config\.executionId,\s*actionType:\s*selectedAction/s,
    'E2E 必须等待 URL 参数包含 actionType 的 signature-page 响应。'
  )
  assert.match(source, /every\(\(row\)\s*=>\s*row\.actionType\s*===\s*selectedAction\)/, 'E2E 必须断言目标响应 rows 全部匹配所选动作。')
})

test('BDD: 追踪与签名真实 E2E 必须写入证据 markdown 并记录 BLOCKED/FAIL', () => {
  const source = readText(e2ePath)

  for (const token of [
    'writeEvidenceMarkdown',
    'real-e2e-evidence.md',
    'test-results',
    'edhr-tracking-signature',
    'BLOCKED',
    'FAIL',
    'GREEN: `pnpm e2e:edhr:tracking-signature` -> PASS'
  ]) {
    assert.match(source, new RegExp(escapeRegExp(token)), `真实 E2E 必须写入证据或状态 ${token}`)
  }

  assert.match(
    source,
    /BLOCKED:[\s\S]*pnpm e2e:edhr:tracking-signature[\s\S]*FAIL/,
    '真实 E2E 必须记录 BLOCKED 命令失败证据。'
  )
  assert.match(
    source,
    /RED:[\s\S]*pnpm e2e:edhr:tracking-signature[\s\S]*FAIL/,
    '真实 E2E 必须记录 RED/FAIL 命令失败证据。'
  )
})
