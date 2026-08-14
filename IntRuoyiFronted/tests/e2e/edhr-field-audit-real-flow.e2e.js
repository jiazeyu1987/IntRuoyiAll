const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const crypto = require('node:crypto')

const TASK_ID = '20260528-edhr-field-audit-real-e2e-gate'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-field-audit')
const DEFAULT_EVIDENCE_FILE = path.resolve(
  process.cwd(),
  'doc',
  'tasks',
  TASK_ID,
  'real-e2e-evidence.md'
)

const REQUIRED_BASE_URL = 'http://localhost:8081'
const DEFAULT_TENANT = '测试租户'
const DEFAULT_USERNAME = 'aoteman'
const DEFAULT_EXECUTION_ID = '40'
const FORBIDDEN_LIVE_TENANTS = new Set(['芋道源码', 'yudao', 'prod', 'production'])

const FIELD_AUDIT_LIST_ROUTE = '/mes/pro/feedback/edhr-field-audit'
const FIELD_AUDIT_DETAIL_ROUTE = '/mes/pro/feedback/edhr-field-audit/detail'
const FIELD_AUDIT_PAGE_ENDPOINT = '/mes/pro/batch-record-execution/field-audit/page'
const FIELD_AUDIT_DETAIL_ENDPOINT = '/mes/pro/batch-record-execution/field-audit/detail'
const FIELD_AUDIT_VERIFY_ENDPOINT = '/mes/pro/batch-record-execution/field-audit/verify-chain'
const FIELD_AUDIT_EXPORT_ENDPOINT = '/mes/pro/batch-record-execution/field-audit/export'
const HASH_STATUS_LABELS = {
  VALID: '校验通过',
  CHAIN_BROKEN: '链断裂',
  SIGNATURE_MISMATCH: '签名不匹配',
  SOURCE_MISSING: '源数据缺失',
  CONCURRENCY_CONFLICT: '并发冲突'
}

const BDD_SCENARIOS = [
  'BDD: 字段审计列表可追溯 -> Given 测试租户存在真实字段审计执行记录 / When 用户登录并打开 `/mes/pro/feedback/edhr-field-audit?executionId=<id>` / Then 前端请求真实 `/field-audit/page` 并展示执行编号、字段路径、旧值、新值、原因、修改人、签名和 hash 状态。',
  'BDD: 字段审计详情可核验 -> Given 列表中存在可点击的真实审计行 / When 用户点击“详情” / Then 页面进入 `/mes/pro/feedback/edhr-field-audit/detail` 并展示 items 字段路径、旧值、新值、原因、修改人、签名或审计 hash 以及 hashVerification。',
  'BDD: 字段审计链可校验 -> Given 详情页已加载真实审计批次 / When 用户点击“校验链” / Then 前端调用真实 `/field-audit/verify-chain` 且返回的 hashVerification.status 必须为 VALID。',
  'BDD: 字段审计链可导出 -> Given 真实审计链可校验 / When 用户点击“导出审计链” / Then 前端调用真实 `/field-audit/export` 并返回 fileName、contentType、sha256、recordCount、hashVerification 与非空 content。',
  'BDD: 字段审计定位执行记录 -> Given 字段审计列表展示目标审计行 / When 用户点击“定位执行记录” / Then 前端进入 `/mes/pro/feedback/edhr-execution/detail?id=<executionId>` 并展示同一执行记录。'
]

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function envValue(key) {
  return (process.env[key] || '').trim()
}

function resolveEvidenceFile() {
  return process.env.EDHR_FIELD_AUDIT_EVIDENCE_FILE
    ? path.resolve(process.env.EDHR_FIELD_AUDIT_EVIDENCE_FILE)
    : DEFAULT_EVIDENCE_FILE
}

function collectConfig() {
  const config = {
    baseUrl: envValue('EDHR_FIELD_AUDIT_BASE_URL') || REQUIRED_BASE_URL,
    tenant: envValue('EDHR_FIELD_AUDIT_TENANT') || DEFAULT_TENANT,
    username: envValue('EDHR_FIELD_AUDIT_USERNAME') || DEFAULT_USERNAME,
    password: envValue('EDHR_FIELD_AUDIT_PASSWORD'),
    executionId: envValue('EDHR_FIELD_AUDIT_EXECUTION_ID') || DEFAULT_EXECUTION_ID,
    headed: envValue('EDHR_FIELD_AUDIT_HEADED') === '1',
    evidenceFile: resolveEvidenceFile()
  }

  const missing = collectInvalidConfig(config)
  if (missing.length > 0) {
    return {
      missing,
      evidenceFile: config.evidenceFile,
      invalidConfig: true
    }
  }

  return {
    ...config,
    missing: []
  }
}

function collectInvalidConfig(config) {
  const invalid = []

  if (config.baseUrl !== REQUIRED_BASE_URL) {
    invalid.push({
      key: 'EDHR_FIELD_AUDIT_BASE_URL',
      description: `真实前端入口必须固定为 ${REQUIRED_BASE_URL}。`
    })
  }

  if (FORBIDDEN_LIVE_TENANTS.has(config.tenant.toLowerCase()) || config.tenant.includes('芋道源码')) {
    invalid.push({
      key: 'EDHR_FIELD_AUDIT_TENANT',
      description: '当前值命中 live 租户保护名单；真实 E2E 只能使用测试租户。'
    })
  }

  if (!/^\d+$/.test(String(config.executionId))) {
    invalid.push({
      key: 'EDHR_FIELD_AUDIT_EXECUTION_ID',
      description: 'executionId 必须是真实数字型执行 ID；不能使用空值、编号或占位文本。'
    })
  }

  if (!config.password) {
    invalid.push({
      key: 'EDHR_FIELD_AUDIT_PASSWORD',
      description: '测试租户密码必须由当前进程环境或登录基线注入；不得写入脚本默认值或证据文件。'
    })
  }

  return invalid
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    const blocked = new Error(
      'Missing Playwright runtime. Run `pnpm install` in this workspace so package.json devDependency `playwright` is installed, then re-run `pnpm e2e:edhr:field-audit`.'
    )
    blocked.blocked = true
    throw blocked
  }
}

function serializeError(error) {
  if (!error) return undefined
  return {
    name: error.name || 'Error',
    message: error.message || String(error),
    stack: error.stack
  }
}

function writeJsonResult(result) {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(
    path.join(RESULT_DIR, 'result.json'),
    `${JSON.stringify(result, null, 2)}\n`,
    'utf8'
  )
}

function writeEvidenceMarkdown(result, evidenceFile) {
  ensureDir(path.dirname(evidenceFile))
  const lines = [
    '# eDHR 字段审计真实路径 E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- 生成时间：${new Date().toISOString()}`,
    `- 前端 worktree：${process.cwd()}`,
    `- 固定前端入口：\`${REQUIRED_BASE_URL}\``,
    '- 默认测试租户：`测试租户`',
    '- 默认账号名：`aoteman`；密码由 `EDHR_FIELD_AUDIT_PASSWORD` 或登录基线注入，不写入仓库证据。',
    '- 真实 E2E 复跑命令：`pnpm e2e:edhr:field-audit`',
    '- 静态语法检查命令：`pnpm e2e:edhr:field-audit:check`',
    '- 证据文件：默认写入本任务目录 `doc/tasks/20260528-edhr-field-audit-real-e2e-gate/real-e2e-evidence.md`，作为可提交任务证据。',
    '- 临时产物目录：`test-results/edhr-field-audit/`（截图、trace、result.json 与下载文件不提交）',
    `- 当前状态：${result.status}`,
    `- executionId：\`${result.executionId || DEFAULT_EXECUTION_ID}\``,
    ''
  ]

  lines.push('## BDD')
  lines.push('')
  for (const scenario of BDD_SCENARIOS) lines.push(`- ${scenario}`)
  lines.push('')

  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED')
    lines.push('')
    lines.push(`- BLOCKED: \`pnpm e2e:edhr:field-audit\` -> FAIL, ${result.reason}`)
    if (result.missing?.length) {
      lines.push(result.invalidConfig ? '- 不满足的真实 E2E 前置条件：' : '- 缺失环境变量：')
      for (const item of result.missing) {
        lines.push(`  - \`${item.key}\`：${item.description}`)
      }
    }
    lines.push('- 影响：无法通过真实页面登录测试租户、打开字段审计列表、进入详情、校验链或导出审计链；未使用模拟数据、API-only 或测试专用 UI。')
    lines.push('')
  }

  if (result.status === 'PASS') {
    lines.push('## GREEN')
    lines.push('')
    lines.push('- GREEN: `pnpm e2e:edhr:field-audit` -> PASS, 真实字段审计列表、详情、校验链和导出已完成。')
    for (const step of result.steps || []) {
      const detailEvidence = step.detail
        ? `, detail.executionId=${step.detail.executionId}, detail.executionCode=${step.detail.executionCode}, detail.hashVerification=${step.detail.hashVerification.status}, detail.items=${step.detail.items.length}`
        : ''
      const exportEvidence = step.export
        ? `, fileName=${step.export.fileName}, contentType=${step.export.contentType}, sha256=${step.export.sha256}, recordCount=${step.export.recordCount}, downloadedSha256=${step.export.downloadedSha256 || '--'}`
        : ''
      const verifyEvidence = step.verify
        ? `, verify.hashVerification=${step.verify.hashVerification.status}, verifiedCount=${step.verify.verifiedCount}, fieldAuditRevision=${step.verify.fieldAuditRevision}, fieldAuditHeadHash=${step.verify.fieldAuditHeadHash}, cellValuesHash=${step.verify.cellValuesHash}`
        : ''
      lines.push(`- ${step.name} -> PASS${step.screenshot ? `, screenshot: \`${step.screenshot}\`` : ''}${detailEvidence}${verifyEvidence}${exportEvidence}`)
    }
    lines.push(`- Trace: \`${result.trace}\``)
    lines.push('')
  }

  if (result.status === 'FAIL') {
    lines.push('## RED')
    lines.push('')
    lines.push(`- RED: \`pnpm e2e:edhr:field-audit\` -> FAIL, ${result.error?.message || '未知错误'}`)
    lines.push('- 影响：真实 UI E2E 未放行；不得提交为通过。')
    lines.push('')
  }

  fs.writeFileSync(evidenceFile, `${lines.join('\n')}\n`, 'utf8')
}

async function screenshot(page, name, steps) {
  ensureDir(RESULT_DIR)
  const fileName = `${String(steps.length + 1).padStart(2, '0')}-${name}.png`
  const filePath = path.join(RESULT_DIR, fileName)
  await page.screenshot({ path: filePath, fullPage: true })
  return filePath
}

async function visibleCount(locator) {
  const count = await locator.count()
  let visible = 0
  for (let index = 0; index < count; index += 1) {
    if (await locator.nth(index).isVisible()) visible += 1
  }
  return visible
}

async function firstVisible(locator, failureMessage) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(failureMessage)
}

async function clickVisibleButton(scope, namePattern, failureMessage) {
  const button = await firstVisible(scope.getByRole('button', { name: namePattern }), failureMessage)
  await button.click()
}

async function fillFirstVisible(locator, value, failureMessage) {
  const item = await firstVisible(locator, failureMessage)
  await item.fill(value)
}

async function waitForText(page, textOrPattern, failureMessage) {
  if (!textOrPattern) {
    throw new Error(`${failureMessage}: expected text/pattern is missing`)
  }
  const locator = page.getByText(textOrPattern).first()
  try {
    await locator.waitFor({ state: 'visible', timeout: 30000 })
  } catch (error) {
    throw new Error(`${failureMessage}: ${error.message}`)
  }
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const loginForm = page
    .locator('form.login-form')
    .filter({ has: page.getByPlaceholder('请输入用户名') })
    .filter({ hasText: '记住我' })
    .first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })

  const captchaInput = loginForm.locator('input[placeholder*="验证码"]').first()
  if ((await captchaInput.count()) > 0 && (await captchaInput.isVisible())) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E；请在测试租户关闭验证码后重跑。')
  }

  const tenantInput = loginForm.locator('input.el-select__input:visible').first()
  if ((await tenantInput.count()) === 0) {
    throw new Error('登录页缺少可见租户选择输入框，无法确认正在登录测试租户。')
  }
  await tenantInput.click()
  await page.keyboard.press('Control+A')
  await page.keyboard.type(config.tenant)
  await page.keyboard.press('Enter')
  await page.waitForTimeout(400)

  await fillFirstVisible(
    loginForm.locator('input[placeholder="请输入用户名"]'),
    config.username,
    '登录页缺少用户名输入框。'
  )
  await fillFirstVisible(
    loginForm.locator('input[placeholder="请输入密码"]'),
    config.password,
    '登录页缺少密码输入框。'
  )

  await clickVisibleButton(loginForm, /^登录$/, '登录页缺少登录按钮。')
  await page.waitForURL((url) => url.pathname === '/index', { timeout: 60000 })
}

function buildListUrl(config) {
  const url = new URL(config.baseUrl)
  url.pathname = FIELD_AUDIT_LIST_ROUTE
  url.searchParams.set('executionId', String(config.executionId))
  return url.toString()
}

function buildDetailUrl(config, row) {
  const url = new URL(config.baseUrl)
  url.pathname = FIELD_AUDIT_DETAIL_ROUTE
  url.searchParams.set('executionId', String(config.executionId))
  if (row.auditBatchId) {
    url.searchParams.set('auditBatchId', String(row.auditBatchId))
  }
  url.searchParams.set('auditItemId', String(row.id))
  return url.toString()
}

function parseBusinessData(body, label) {
  assert.ok(body && typeof body === 'object', `${label} 响应必须是对象。`)
  if (Object.prototype.hasOwnProperty.call(body, 'code')) {
    assert.ok(
      body.code === 0 || body.code === 200,
      `${label} 业务状态码应为 0 或 200，实际 ${body.code}：${body.msg || body.message || ''}`
    )
    assert.notEqual(body.data, undefined, `${label} CommonResult 缺少 data。`)
    return body.data
  }
  return body
}

async function parseJsonResponse(response, label) {
  assert.equal(response.status(), 200, `${label} HTTP 状态应为 200，实际 ${response.status()}，URL: ${response.url()}`)
  try {
    return parseBusinessData(await response.json(), label)
  } catch (error) {
    throw new Error(`${label} 响应不是 JSON：${error.message}`)
  }
}

function assertFieldAuditRow(row, label) {
  assert.ok(row && typeof row === 'object', `${label} 必须是对象。`)
  for (const field of [
    'id',
    'executionId',
    'executionCode',
    'fieldPath',
    'fieldKey',
    'oldValueJson',
    'newValueJson',
    'oldValueDisplay',
    'newValueDisplay',
    'reasonText',
    'actorName',
    'signatureId',
    'auditHash',
    'previousHash',
    'hashVerification'
  ]) {
    assert.ok(
      Object.prototype.hasOwnProperty.call(row, field),
      `${label} 缺少字段 ${field}。`
    )
  }
  assert.ok(row.hashVerification && typeof row.hashVerification === 'object', `${label} hashVerification 必须是对象。`)
}

function assertFieldAuditDetail(detail) {
  assert.ok(detail && typeof detail === 'object', '字段审计详情必须是对象。')
  assert.ok(detail.auditBatch && typeof detail.auditBatch === 'object', '字段审计详情必须包含 auditBatch。')
  assert.ok(Array.isArray(detail.items), '字段审计详情必须包含 items 数组。')
  assert.ok(detail.items.length > 0, '字段审计详情 items 必须至少有一条真实记录。')
  assert.ok(detail.signature && typeof detail.signature === 'object', '字段审计详情必须包含 signature。')
  assert.ok(detail.hashVerification && typeof detail.hashVerification === 'object', '字段审计详情必须包含 hashVerification。')
  for (const item of detail.items) {
    assertFieldAuditRow(item, '字段审计详情 items[]')
  }
  assert.equal(detail.hashVerification.status, 'VALID', `字段审计详情 hashVerification.status 必须为 VALID，实际 ${detail.hashVerification.status}`)
}

function assertFieldAuditVerify(verifyData) {
  assert.ok(verifyData && typeof verifyData === 'object', '字段审计校验响应必须是对象。')
  assert.ok(verifyData.hashVerification && typeof verifyData.hashVerification === 'object', '字段审计校验响应必须包含 hashVerification。')
  assert.equal(verifyData.hashVerification.status, 'VALID', `字段审计链校验必须返回 VALID，实际 ${verifyData.hashVerification.status}`)
  assert.ok(Number(verifyData.verifiedCount) > 0, '字段审计校验 verifiedCount 必须大于 0。')
  for (const field of ['fieldAuditRevision', 'fieldAuditHeadHash', 'cellValuesHash']) {
    assert.ok(
      Object.prototype.hasOwnProperty.call(verifyData, field) &&
        verifyData[field] !== undefined &&
        verifyData[field] !== null &&
        String(verifyData[field]).trim(),
      `字段审计校验响应缺少 ${field}。`
    )
  }
}

function decodeExportContent(exportPayload) {
  const { content } = exportPayload
  if (Array.isArray(content)) {
    if (!content.length) throw new Error('字段审计导出响应 content 为空。')
    return Uint8Array.from(content)
  }
  if (typeof content === 'string' && content.trim()) {
    const base64Content = content.includes(',') ? content.slice(content.indexOf(',') + 1) : content
    const binary = Buffer.from(base64Content, 'base64')
    if (!binary.length) throw new Error('字段审计导出响应 content 为空。')
    return Uint8Array.from(binary)
  }
  throw new Error('字段审计导出响应缺少 content。')
}

function sha256Bytes(bytes) {
  return crypto.createHash('sha256').update(Buffer.from(bytes)).digest('hex')
}

function assertFieldAuditExport(exportPayload) {
  assert.ok(exportPayload && typeof exportPayload === 'object', '字段审计导出响应必须是对象。')
  for (const field of ['fileName', 'contentType', 'sha256', 'recordCount', 'hashVerification', 'content']) {
    assert.ok(
      Object.prototype.hasOwnProperty.call(exportPayload, field),
      `字段审计导出响应缺少 ${field}。`
    )
  }
  assert.ok(String(exportPayload.fileName).trim(), '字段审计导出响应 fileName 不能为空。')
  assert.ok(String(exportPayload.contentType).trim(), '字段审计导出响应 contentType 不能为空。')
  assert.ok(String(exportPayload.sha256).trim(), '字段审计导出响应 sha256 不能为空。')
  assert.ok(Number(exportPayload.recordCount) > 0, '字段审计导出响应 recordCount 必须大于 0。')
  assert.ok(exportPayload.hashVerification && typeof exportPayload.hashVerification === 'object', '字段审计导出响应 hashVerification 必须是对象。')
  assert.equal(
    exportPayload.hashVerification.status,
    'VALID',
    `字段审计导出响应 hashVerification.status 必须为 VALID，实际 ${exportPayload.hashVerification.status}`
  )

  const bytes = decodeExportContent(exportPayload)
  const calculatedSha256 = sha256Bytes(bytes)
  assert.equal(
    calculatedSha256,
    String(exportPayload.sha256),
    '字段审计导出响应 sha256 必须与 content 计算结果一致。'
  )
  return bytes
}

function bodyIncludesHashStatus(bodyText, status) {
  return bodyText.includes(String(status)) || bodyText.includes(HASH_STATUS_LABELS[status] || '')
}

function visibleValueCandidates(record, fields) {
  return fields
    .map((field) => record[field])
    .filter((value) => value !== undefined && value !== null && String(value).trim())
    .map((value) => String(value).trim())
}

function assertBodyContainsAnyValue(bodyText, candidates, failureMessage) {
  assert.ok(candidates.length > 0, `${failureMessage}；API 响应缺少可断言候选值。`)
  assert.ok(
    candidates.some((value) => bodyText.includes(value)),
    `${failureMessage}；候选值=${candidates.join(' | ')}。`
  )
}

function assertFieldAuditRowUiVisible(bodyText, row, label) {
  assertBodyContainsAnyValue(
    bodyText,
    visibleValueCandidates(row, ['fieldPath', 'fieldKey']),
    `${label} 页面未展示 fieldPath 或 fieldKey`
  )
  assertBodyContainsAnyValue(
    bodyText,
    visibleValueCandidates(row, ['oldValueDisplay', 'oldValueJson']),
    `${label} 页面未展示 oldValueDisplay 或 oldValueJson`
  )
  assertBodyContainsAnyValue(
    bodyText,
    visibleValueCandidates(row, ['newValueDisplay', 'newValueJson']),
    `${label} 页面未展示 newValueDisplay 或 newValueJson`
  )
  assertBodyContainsAnyValue(
    bodyText,
    visibleValueCandidates(row, ['reasonText', 'reasonCategory']),
    `${label} 页面未展示 reasonText 或 reasonCategory`
  )
  assertBodyContainsAnyValue(
    bodyText,
    visibleValueCandidates(row, ['actorName']),
    `${label} 页面未展示 actorName`
  )
  assertBodyContainsAnyValue(
    bodyText,
    visibleValueCandidates(row, ['signatureId', 'auditHash']),
    `${label} 页面未展示 signatureId 或 auditHash`
  )
}

async function openFieldAuditList(page, config) {
  const pageResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'GET' &&
      response.url().includes(FIELD_AUDIT_PAGE_ENDPOINT) &&
      new URL(response.url()).searchParams.get('executionId') === String(config.executionId),
    { timeout: 60000 }
  )
  await page.goto(buildListUrl(config), { waitUntil: 'domcontentloaded', timeout: 60000 })
  await waitForText(page, /字段审计|hash 状态/, '未进入 eDHR 字段审计列表页')
  const response = await pageResponsePromise
  const data = await parseJsonResponse(response, '字段审计列表')
  const rows = Array.isArray(data.list) ? data.list : []
  if (!rows.length) {
    throw new Error(
      `字段审计列表未返回任何真实数据。请设置 EDHR_FIELD_AUDIT_EXECUTION_ID 为存在字段审计记录的真实 executionId；当前值=${config.executionId}。`
    )
  }
  const row = rows.find((item) => String(item.executionId) === String(config.executionId)) || rows[0]
  assertFieldAuditRow(row, '字段审计列表目标行')
  if (String(row.executionId) !== String(config.executionId)) {
    throw new Error(
      `字段审计列表未命中 executionId=${config.executionId} 的真实记录。请设置 EDHR_FIELD_AUDIT_EXECUTION_ID 为已有字段审计数据的真实 executionId。`
    )
  }
  const bodyText = (await page.locator('body').textContent({ timeout: 30000 })) || ''
  assert.ok(bodyText.includes(row.executionCode), `字段审计列表页面未展示执行编号 ${row.executionCode}。`)
  assertFieldAuditRowUiVisible(bodyText, row, '字段审计列表目标行')
  assert.ok(
    bodyIncludesHashStatus(bodyText, row.hashVerification.status),
    `字段审计列表页面未展示 hashVerification.status 或映射标签：${row.hashVerification.status}。`
  )
  return { response, data, row }
}

async function openFieldAuditDetailFromList(page, config, row) {
  const detailResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'GET' &&
      response.url().includes(FIELD_AUDIT_DETAIL_ENDPOINT) &&
      new URL(response.url()).searchParams.get('executionId') === String(config.executionId),
    { timeout: 60000 }
  )
  const listRow = page
    .locator('.edhr-field-audit .el-table__body .el-table__row')
    .filter({ hasText: row.executionCode })
    .first()
  await listRow.waitFor({ state: 'visible', timeout: 30000 })
  await clickVisibleButton(listRow, /^详情$/, '字段审计列表目标行缺少“详情”按钮。')
  await page.waitForURL(
    (url) =>
      url.pathname === FIELD_AUDIT_DETAIL_ROUTE &&
      url.searchParams.get('executionId') === String(config.executionId),
    { timeout: 60000 }
  )
  await waitForText(page, /字段审计详情/, '未进入 eDHR 字段审计详情页')
  const response = await detailResponsePromise
  const detailData = await parseJsonResponse(response, '字段审计详情')
  assertFieldAuditDetail(detailData)
  const bodyText = (await page.locator('body').textContent({ timeout: 30000 })) || ''
  assert.ok(bodyText.includes(String(detailData.auditBatch.id)), '字段审计详情页面未展示 auditBatch.id。')
  assert.ok(bodyText.includes(String(detailData.auditBatch.newHeadHash)), '字段审计详情页面未展示 newHeadHash。')
  assert.ok(bodyText.includes(String(detailData.signature.signatureId)), '字段审计详情页面未展示 signatureId。')
  for (const item of detailData.items) {
    assertFieldAuditRowUiVisible(bodyText, item, '字段审计详情 items')
  }
  assert.ok(
    bodyIncludesHashStatus(bodyText, detailData.hashVerification.status),
    `字段审计详情页面未展示 hashVerification.status 或映射标签：${detailData.hashVerification.status}。`
  )
  return { response, detailData }
}

async function verifyFieldAuditChain(page, config, detailData) {
  const verifyResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes(FIELD_AUDIT_VERIFY_ENDPOINT),
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /校验链|校验当前筛选结果/, '字段审计详情页缺少“校验链”或列表页缺少“校验当前筛选结果”按钮。')
  const response = await verifyResponsePromise
  const requestBody = response.request().postData() || ''
  assert.ok(
    requestBody.includes(String(config.executionId)),
    `字段审计校验请求未携带目标 executionId=${config.executionId}。实际请求体：${requestBody}`
  )
  const verifyData = await parseJsonResponse(response, '字段审计链校验')
  assertFieldAuditVerify(verifyData)
  assert.equal(
    String(verifyData.fieldAuditRevision),
    String(detailData.auditBatch.afterFieldAuditRevision),
    '字段审计校验返回的 fieldAuditRevision 与详情页 auditBatch.afterFieldAuditRevision 不一致。'
  )
  assert.equal(
    String(verifyData.fieldAuditHeadHash),
    String(detailData.auditBatch.newHeadHash),
    '字段审计校验返回的 fieldAuditHeadHash 与详情页 newHeadHash 不一致。'
  )
  assert.equal(
    String(verifyData.cellValuesHash),
    String(detailData.auditBatch.afterCellValuesHash),
    '字段审计校验返回的 cellValuesHash 与详情页 afterCellValuesHash 不一致。'
  )
  await waitForText(page, /校验通过|VALID/, '字段审计链校验后页面未展示 VALID/校验通过证据')
  return { response, verifyData }
}

async function exportFieldAuditChain(page, config) {
  const downloadPromise = page.waitForEvent('download', { timeout: 60000 })
  const exportResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'GET' &&
      response.url().includes(FIELD_AUDIT_EXPORT_ENDPOINT) &&
      new URL(response.url()).searchParams.get('executionId') === String(config.executionId),
    { timeout: 60000 }
  )
  await clickVisibleButton(page, /导出审计链/, '字段审计列表页缺少“导出审计链”按钮。')
  const response = await exportResponsePromise
  const exportData = await parseJsonResponse(response, '字段审计导出')
  const exportBytes = assertFieldAuditExport(exportData)
  const download = await downloadPromise
  const failure = await download.failure()
  assert.equal(failure, null, `字段审计导出下载失败：${failure}`)
  const downloadName = download.suggestedFilename()
  assert.ok(downloadName && downloadName.trim(), '字段审计导出缺少建议文件名。')
  const savedFilePath = path.join(RESULT_DIR, downloadName)
  await download.saveAs(savedFilePath)
  const downloadedSha256 = crypto.createHash('sha256').update(fs.readFileSync(savedFilePath)).digest('hex')
  assert.equal(
    downloadedSha256,
    String(exportData.sha256),
    '字段审计导出下载文件 sha256 必须等于导出响应 sha256。'
  )
  assert.equal(
    downloadedSha256,
    sha256Bytes(exportBytes),
    '字段审计导出下载文件 sha256 必须等于响应 content 计算结果。'
  )
  return { response, exportData, savedFilePath, downloadedSha256 }
}

async function openExecutionFromFieldAuditList(page, config, row) {
  const listRow = page
    .locator('.edhr-field-audit .el-table__body .el-table__row')
    .filter({ hasText: row.executionCode })
    .first()
  await listRow.waitFor({ state: 'visible', timeout: 30000 })
  await clickVisibleButton(listRow, /^定位执行记录$/, '字段审计列表目标行缺少“定位执行记录”按钮。')
  await page.waitForURL(
    (url) =>
      url.pathname === '/mes/pro/feedback/edhr-execution/detail' &&
      url.searchParams.get('id') === String(config.executionId),
    { timeout: 60000 }
  )
  await waitForText(page, 'eDHR 执行详情', '字段审计定位后未进入 eDHR 执行详情页')
  await waitForText(page, row.executionCode, `执行详情页未展示执行编号 ${row.executionCode}`)
}

async function runRealFlow(config) {
  const { chromium } = loadPlaywright()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
    acceptDownloads: true
  })
  const tracePath = path.join(RESULT_DIR, 'trace.zip')
  const page = await context.newPage()
  const steps = []

  await context.tracing.start({ screenshots: true, snapshots: true, sources: true })
  try {
    await login(page, config)
    const listResult = await openFieldAuditList(page, config)
    steps.push({
      name: '字段审计列表可追溯',
      screenshot: await screenshot(page, 'field-audit-list', steps)
    })

    const detailResult = await openFieldAuditDetailFromList(page, config, listResult.row)
    steps.push({
      name: '字段审计详情可核验',
      screenshot: await screenshot(page, 'field-audit-detail', steps),
      detail: detailResult.detailData
    })

    const verifyResult = await verifyFieldAuditChain(page, config, detailResult.detailData)
    steps.push({
      name: '字段审计链可校验',
      screenshot: await screenshot(page, 'field-audit-verified', steps),
      verify: verifyResult.verifyData
    })

    await clickVisibleButton(page, /^返回$/, '字段审计详情页缺少标准“返回”按钮。')
    await page.waitForURL(
      (url) => url.pathname === FIELD_AUDIT_LIST_ROUTE && url.searchParams.get('executionId') === String(config.executionId),
      { timeout: 60000 }
    )
    await waitForText(page, /字段审计|hash 状态/, '返回字段审计列表后未重新展示列表内容')

    const exportResult = await exportFieldAuditChain(page, config)
    steps.push({
      name: '字段审计链可导出',
      screenshot: await screenshot(page, 'field-audit-exported', steps),
      export: {
        ...exportResult.exportData,
        downloadedFilePath: exportResult.savedFilePath,
        downloadedSha256: exportResult.downloadedSha256
      }
    })

    await openExecutionFromFieldAuditList(page, config, listResult.row)
    steps.push({
      name: '字段审计定位执行记录',
      screenshot: await screenshot(page, 'field-audit-open-execution', steps)
    })

    await context.tracing.stop({ path: tracePath })
    await browser.close()
    return {
      status: 'PASS',
      steps,
      trace: tracePath,
      resultFile: path.join(RESULT_DIR, 'result.json'),
      executionId: String(config.executionId)
    }
  } catch (error) {
    try {
      await context.tracing.stop({ path: tracePath })
    } catch (traceError) {
      error.message = `${error.message}; trace 写入失败: ${
        traceError instanceof Error ? traceError.message : String(traceError)
      }`
    }
    await browser.close()
    throw Object.assign(error, { tracePath, steps, executionId: String(config.executionId) })
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const result = {
      status: 'BLOCKED',
      reason: config.invalidConfig
        ? '真实 E2E 前置条件不满足，不能使用 live 租户或无效 executionId。'
        : '缺少真实前端入口、测试租户、账号或真实字段审计执行记录，不能执行真实 UI E2E。',
      missing: config.missing,
      invalidConfig: config.invalidConfig === true,
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      evidenceFile: config.evidenceFile,
      executionId: String(config.executionId || DEFAULT_EXECUTION_ID)
    }
    writeJsonResult(result)
    writeEvidenceMarkdown(result, config.evidenceFile)
    console.error(`BLOCKED: ${result.reason}`)
    for (const item of config.missing) {
      console.error(`- ${item.key}: ${item.description}`)
    }
    process.exitCode = 1
    return
  }

  try {
    const result = await runRealFlow(config)
    result.generatedAt = new Date().toISOString()
    result.resultDir = RESULT_DIR
    result.evidenceFile = config.evidenceFile
    writeJsonResult(result)
    writeEvidenceMarkdown(result, config.evidenceFile)
    console.log(`PASS: eDHR field audit real E2E. Trace: ${result.trace}`)
  } catch (error) {
    const result = {
      status: error.blocked ? 'BLOCKED' : 'FAIL',
      reason: error.blocked ? error.message : undefined,
      generatedAt: new Date().toISOString(),
      resultDir: RESULT_DIR,
      evidenceFile: config.evidenceFile,
      executionId: String(config.executionId),
      steps: error.steps || [],
      trace: error.tracePath,
      error: serializeError(error),
      missing: error.blocked
        ? [
            {
              key: 'playwright',
              description: error.message
            }
          ]
        : undefined,
      invalidConfig: false
    }
    writeJsonResult(result)
    writeEvidenceMarkdown(result, config.evidenceFile)
    console.error(`${result.status}: ${error.message}`)
    process.exitCode = 1
  }
}

main()
