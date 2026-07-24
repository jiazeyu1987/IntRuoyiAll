const assert = require('node:assert/strict')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_T11_E2E_BASE_URL || 'http://localhost:8081'
const BACKEND_URL = process.env.EDHR_T11_E2E_BACKEND_URL || 'http://127.0.0.1:48081'
const TEST_TENANT = '测试租户'
const TEST_USERNAME = 'aoteman'
const TEST_PASSWORD = process.env.EDHR_T11_E2E_LOGIN_PASSWORD || '111111'
const ADMIN_TENANT = '芋道源码'
const ADMIN_USERNAME = 'admin'
const ADMIN_PASSWORD = process.env.EDHR_T11_E2E_ADMIN_PASSWORD || 'admin123'
const DETAIL_ROUTE = '/mes/pro/feedback/edhr-batch-execution/detail'
const LIST_ROUTE = '/mes/pro/feedback/edhr-batch-execution'
const TASK_STATE_PATH = path.resolve(__dirname, '../../../doc/tasks/20260613-batch-record-gap-implementation/task-state.json')
const RUN_KEY = `T11-FINAL-ARCHIVE-PDF-${Date.now()}`

const SPECIAL_NODE_LABELS = ['来料检报告', '灭菌报告', '成品检报告', '成品检记录']
const REQUIRED_PDF_TERMS = [
  'eDHR',
  '批次',
  '路线',
  '普通表单',
  '来料检报告',
  '灭菌报告',
  '成品检报告',
  '成品检记录',
  '附件',
  '跳过',
  '操作人',
  '操作时间',
  '签名',
  '审核',
  '批准',
  '返工',
  '审计',
  '追踪',
  'manifest'
]

function blocked(message, details = []) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  return error
}

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'T11 E2E must use local frontend http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'T11 E2E must use local backend 48081')
}

function readTaskState() {
  return JSON.parse(fs.readFileSync(TASK_STATE_PATH, 'utf8'))
}

function requireRealGate(taskId) {
  const task = readTaskState().tasks.find((item) => item.task_id === taskId)
  if (!task) {
    throw blocked(`task-state.json 缺少 ${taskId} 状态，不能判断 T11 真实 E2E 依赖门禁。`)
  }
  if (task.status !== 'validated_real_e2e_pass') {
    throw blocked(`T11 真实 E2E 依赖 ${taskId} 真实门禁先通过。`, [
      `当前 ${taskId} 状态：${task.status}`,
      `当前 ${taskId} 结果：${task.last_outcome || '--'}`
    ])
  }
}

function requirePrerequisites() {
  const blockers = []
  for (const taskId of ['T9', 'T10']) {
    try {
      requireRealGate(taskId)
    } catch (error) {
      if (!error.blocked) throw error
      blockers.push(error.message)
      blockers.push(...(error.details || []))
    }
  }
  if (blockers.length > 0) {
    throw blocked('T11 real E2E 前置条件未满足。', [
      ...blockers,
      '不得使用 mock PDF、SQL 造关闭/归档状态、接口造成功、hash-only 文件或外部服务器替代。'
    ])
  }
}

function mysql(sql) {
  return execFileSync('docker', [
    'exec',
    '-i',
    'int-ruoyi-mysql',
    'mysql',
    '-uroot',
    '-p123456',
    '--batch',
    '--raw',
    '--skip-column-names',
    '--default-character-set=utf8mb4',
    'ruoyi-vue-pro'
  ], { input: sql, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] }).trim()
}

function parseJsonRow(output, label) {
  const line = output.split(/\r?\n/).find(Boolean)
  if (!line || line === 'NULL') return null
  try {
    return JSON.parse(line)
  } catch (error) {
    throw new Error(`${label} returned non JSON output: ${line}`)
  }
}

function findArchiveCandidate() {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'batchExecutionId', b.id,
  'batchExecutionCode', b.batch_execution_code,
  'batchCode', b.batch_code,
  'status', b.status,
  'closedAt', b.closed_at,
  'archiveId', (
    SELECT a.id
    FROM mes_pro_edhr_batch_execution_archive a
    WHERE a.tenant_id=b.tenant_id AND a.deleted=0 AND a.batch_execution_id=b.id AND a.archive_status='SEALED'
    ORDER BY a.archive_version DESC, a.id DESC
    LIMIT 1
  ),
  'archiveTaskId', (
    SELECT wt.id
    FROM mes_pro_edhr_work_task wt
    WHERE wt.tenant_id=b.tenant_id AND wt.deleted=0 AND wt.batch_execution_id=b.id AND wt.task_type='ARCHIVE'
    ORDER BY wt.id DESC
    LIMIT 1
  ),
  'routeTaskCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_batch_execution_task bt
    WHERE bt.tenant_id=b.tenant_id AND bt.deleted=0 AND bt.batch_execution_id=b.id AND bt.node_type='ROUTE_FORM'
  ),
  'specialNodeCount', (
    SELECT COUNT(DISTINCT bt.node_type)
    FROM mes_pro_edhr_batch_execution_task bt
    WHERE bt.tenant_id=b.tenant_id AND bt.deleted=0 AND bt.batch_execution_id=b.id AND bt.node_type<>'ROUTE_FORM'
  ),
  'attachmentCount', (
    SELECT COUNT(*)
    FROM mes_pro_batch_record_execution_attachment a
    JOIN mes_pro_edhr_batch_execution_task bt ON bt.execution_id=a.execution_id AND bt.tenant_id=a.tenant_id AND bt.deleted=0
    WHERE a.tenant_id=b.tenant_id AND a.deleted=0 AND bt.batch_execution_id=b.id
  ),
  'signatureCount', (
    SELECT COUNT(*)
    FROM mes_pro_batch_record_execution_signature s
    JOIN mes_pro_edhr_batch_execution_task bt ON bt.execution_id=s.execution_id AND bt.tenant_id=s.tenant_id AND bt.deleted=0
    WHERE s.tenant_id=b.tenant_id AND s.deleted=0 AND bt.batch_execution_id=b.id
  ),
  'approvalCount', (
    SELECT COUNT(*)
    FROM mes_pro_batch_record_approval_snapshot ap
    JOIN mes_pro_edhr_batch_execution_task bt ON bt.execution_id=ap.execution_id AND bt.tenant_id=ap.tenant_id AND bt.deleted=0
    WHERE ap.tenant_id=b.tenant_id AND ap.deleted=0 AND bt.batch_execution_id=b.id
  ),
  'fieldAuditBatchCount', (
    SELECT COUNT(*)
    FROM mes_pro_batch_record_execution_field_audit_batch ab
    JOIN mes_pro_edhr_batch_execution_task bt ON bt.execution_id=ab.execution_id AND bt.tenant_id=ab.tenant_id AND bt.deleted=0
    WHERE ab.tenant_id=b.tenant_id AND ab.deleted=0 AND bt.batch_execution_id=b.id
  )
)
FROM mes_pro_edhr_batch_execution b
WHERE b.tenant_id=122
  AND b.deleted=0
  AND b.status IN (30,40)
  AND b.closed_at IS NOT NULL
  AND EXISTS (
    SELECT 1
    FROM mes_pro_edhr_batch_execution_task bt
    WHERE bt.tenant_id=b.tenant_id
      AND bt.deleted=0
      AND bt.batch_execution_id=b.id
      AND bt.node_type='ROUTE_FORM'
  )
  AND (
    SELECT COUNT(DISTINCT bt.node_type)
    FROM mes_pro_edhr_batch_execution_task bt
    WHERE bt.tenant_id=b.tenant_id
      AND bt.deleted=0
      AND bt.batch_execution_id=b.id
      AND bt.node_type IN ('INCOMING_INSPECTION_REPORT','STERILIZATION_REPORT','FINISHED_PRODUCT_INSPECTION_REPORT','FINISHED_PRODUCT_INSPECTION_RECORD')
  ) = 4
ORDER BY b.closed_at DESC, b.id DESC
LIMIT 1;
`)
  return parseJsonRow(output, 'T11 archive candidate')
}

function loadArchiveMetadata(batchExecutionId) {
  const output = mysql(`
SET NAMES utf8mb4;
SELECT JSON_OBJECT(
  'archiveId', a.id,
  'archiveVersion', a.archive_version,
  'archiveStatus', a.archive_status,
  'fileName', a.file_name,
  'contentType', a.content_type,
  'fileSize', a.file_size,
  'contentHash', a.content_hash,
  'generatedBy', a.generated_by,
  'generatedAt', a.generated_at,
  'manifestLength', CHAR_LENGTH(a.source_manifest_json)
)
FROM mes_pro_edhr_batch_execution_archive a
WHERE a.tenant_id=122
  AND a.deleted=0
  AND a.batch_execution_id=${Number(batchExecutionId)}
  AND a.archive_status='SEALED'
ORDER BY a.archive_version DESC, a.id DESC
LIMIT 1;
`)
  return parseJsonRow(output, 'T11 archive metadata')
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.fill(value)
      return
    }
  }
  throw blocked(`缺少可填写控件：${label}`)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.click()
      return
    }
  }
  throw blocked(`缺少可点击控件：${label}`)
}

async function login(page, tenant, username, password, redirect = LIST_ROUTE) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(redirect)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw blocked('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    await page.keyboard.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), tenant, 'tenant')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), password, 'password')
  await clickFirstEnabled(loginForm.locator('button.el-button--primary'), 'login button')
  await page.waitForFunction(
    () => !window.location.href.includes('/login') || Boolean(window.localStorage.getItem('ACCESS_TOKEN')),
    { timeout: 60000 }
  )
  if (page.url().includes('/login')) {
    await page.goto(`${BASE_URL}${redirect}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  }
}

function extractPdfText(buffer) {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'edhr-pdf-text-'))
  const pdfPath = path.join(tempDir, 'archive.pdf')
  const python = process.env.EDHR_PDF_TEXT_PYTHON || 'python'
  const script = [
    'import sys',
    'from pypdf import PdfReader',
    'reader = PdfReader(sys.argv[1])',
    'texts = []',
    'for page in reader.pages:',
    '    texts.append(page.extract_text() or "")',
    'print("\\n".join(texts))'
  ].join('\n')
  try {
    fs.writeFileSync(pdfPath, buffer)
    return execFileSync(python, ['-X', 'utf8', '-c', script, pdfPath], {
      encoding: 'utf8',
      maxBuffer: 10 * 1024 * 1024,
      windowsHide: true
    })
  } catch (error) {
    const details = [error.message, error.stdout, error.stderr].filter(Boolean).join('\n')
    throw new Error(`最终 PDF 文本解析失败；请确认 Python 可用且已安装 pypdf。${details ? `\n${details}` : ''}`)
  } finally {
    fs.rmSync(tempDir, { recursive: true, force: true })
  }
}

function assertPdfCompleteness(pdfBuffer, candidate, metadata) {
  assert.ok(pdfBuffer.length > 1000, `final PDF must not be blank or hash-only; size=${pdfBuffer.length}`)
  const text = extractPdfText(pdfBuffer)
  assert.ok(text.trim().length > 0, 'final PDF text extraction must expose readable content')
  assert.ok(!/^eDHR Batch Final Archive\s+manifest hash:/i.test(text.trim()), `hash-only PDF is not acceptable: ${text}`)
  assert.ok(text.includes(String(candidate.batchCode || candidate.batchExecutionCode)), 'final PDF must include batch code')
  for (const label of SPECIAL_NODE_LABELS) {
    assert.ok(text.includes(label), `final PDF must include special node label: ${label}`)
  }
  const missingTerms = REQUIRED_PDF_TERMS.filter((term) => !text.includes(term))
  assert.deepEqual(missingTerms, [], `final PDF missing required archive terms: ${JSON.stringify(missingTerms)} text=${text}`)
  assert.ok(metadata.contentHash && String(metadata.contentHash).length >= 32, 'archive metadata must include controlled content hash')
  assert.ok(Number(metadata.archiveVersion || 0) > 0, 'archive metadata must include archive version')
  assert.ok(metadata.generatedBy, 'archive metadata must include generator')
  assert.ok(metadata.generatedAt, 'archive metadata must include generated time')
}

async function openDetail(page, candidate) {
  await page.goto(`${BASE_URL}${DETAIL_ROUTE}?id=${candidate.batchExecutionId}&workTaskId=${candidate.archiveTaskId || ''}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByText('eDHR批次详情').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText(String(candidate.batchCode || candidate.batchExecutionCode)).first().waitFor({ state: 'visible', timeout: 60000 })
}

async function generateArchiveIfNeeded(page, candidate) {
  if (candidate.archiveId) return
  if (!candidate.archiveTaskId) {
    throw blocked('候选已关闭批次没有归档工作任务，无法从真实页面入口生成最终 PDF。', [
      `candidate=${JSON.stringify(candidate)}`,
      '不得直接调用接口造归档成功或 SQL 插入归档记录。'
    ])
  }
  const generateResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution-archive/generate') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(page.getByRole('button', { name: '生成最终归档' }), 'generate final archive button')
  const response = await generateResponsePromise
  const body = await response.json()
  assert.equal(response.status(), 200, 'archive generate API must return business envelope')
  assert.equal(body.code, 0, `archive generation must succeed only for closed controlled batch: ${JSON.stringify(body)}`)
}

async function downloadArchiveFromPage(page) {
  const downloadResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution-archive/download') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickFirstEnabled(page.getByRole('button', { name: '下载打印版 PDF' }), 'download printable PDF button')
  const response = await downloadResponsePromise
  assert.equal(response.status(), 200, 'archive download must return HTTP 200')
  const headers = response.headers()
  assert.match(headers['content-type'] || '', /application\/pdf|application\/octet-stream/i, 'download must return PDF-compatible content type')
  return Buffer.from(await response.body())
}

async function verifyAdminReadonlyIsolation(browser, batchExecutionId, expectedArchiveHash) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  const writeRequests = []
  const downloadRequests = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      writeRequests.push(`${request.method()} ${request.url()}`)
    }
    if (request.url().includes('/admin-api/mes/pro/edhr-batch-execution-archive/download')) {
      downloadRequests.push(`${request.method()} ${request.url()}`)
    }
  })
  try {
    await login(page, ADMIN_TENANT, ADMIN_USERNAME, ADMIN_PASSWORD, `${DETAIL_ROUTE}?id=${batchExecutionId}`)
    await page.goto(`${BASE_URL}${DETAIL_ROUTE}?id=${batchExecutionId}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await page.getByText('eDHR批次详情').first().waitFor({ state: 'visible', timeout: 60000 })
    await page.getByText('eDHR 批次执行不存在').first().waitFor({ state: 'visible', timeout: 60000 })
    const metadata = loadArchiveMetadata(batchExecutionId)
    assert.equal(metadata.contentHash, expectedArchiveHash, 'admin readonly isolation check must not mutate archive metadata')
    assert.deepEqual(downloadRequests, [], `admin readonly isolation must not download cross-tenant archive: ${JSON.stringify(downloadRequests)}`)
    assert.deepEqual(writeRequests, [], `admin readonly archive verification must not issue MES writes: ${JSON.stringify(writeRequests)}`)
  } finally {
    await context.close()
  }
}

async function run() {
  assertLocalOnly()
  requirePrerequisites()

  const candidate = findArchiveCandidate()
  if (!candidate) {
    throw blocked('测试租户/aoteman 当前不存在可用于 T11 最终 PDF 验证的真实已关闭批次。', [
      '需要 T9 真实关闭校验和 T10 只读复盘通过后产生 closedAt 非空的批次。',
      '不得用 SQL 修改状态、接口造成功、mock PDF 或外部服务器替代。'
    ])
  }
  if (Number(candidate.routeTaskCount || 0) === 0 || Number(candidate.specialNodeCount || 0) < 4) {
    throw blocked('候选已关闭批次不具备 T11 完整 PDF 所需路线/四类特殊节点证据。', [
      `candidate=${JSON.stringify(candidate)}`
    ])
  }

  const browser = await chromium.launch({ headless: process.env.EDHR_T11_E2E_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
  const page = await context.newPage()
  const writeRequests = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
      writeRequests.push(`${request.method()} ${request.url()}`)
    }
  })
  try {
    await login(page, TEST_TENANT, TEST_USERNAME, TEST_PASSWORD, `${DETAIL_ROUTE}?id=${candidate.batchExecutionId}`)
    await openDetail(page, candidate)
    await generateArchiveIfNeeded(page, candidate)
    const metadata = loadArchiveMetadata(candidate.batchExecutionId)
    if (!metadata) {
      throw blocked('页面归档后未找到受控归档元数据。', [`candidate=${JSON.stringify(candidate)}`])
    }
    const pdfBuffer = await downloadArchiveFromPage(page)
    assertPdfCompleteness(pdfBuffer, candidate, metadata)
    await verifyAdminReadonlyIsolation(browser, candidate.batchExecutionId, metadata.contentHash)
    console.log(`PASS: T11 final archive PDF real E2E runKey=${RUN_KEY} batch=${candidate.batchExecutionId}`)
    console.log(`PASS: archiveMetadata=${JSON.stringify(metadata)}`)
    console.log(`PASS: writeRequestsDuringTestTenantFlow=${JSON.stringify(writeRequests)}`)
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  if (error.blocked) {
    console.error(`BLOCKED: ${error.message}`)
    for (const detail of error.details || []) {
      console.error(`- ${detail}`)
    }
  } else {
    console.error(error)
  }
  process.exitCode = 1
})
