const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { spawnSync } = require('node:child_process')

const TASK_ID = '20260612-edhr-attachment-prepare-upload-api'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-attachment-upload')
const EVIDENCE_FILE = path.resolve(process.cwd(), 'doc', 'tasks', TASK_ID, 'real-upload-e2e-evidence.md')
const REQUIRED_BASE_URL = 'http://localhost:8081'
const EXECUTION_DETAIL_ROUTE = '/mes/pro/feedback/edhr-execution/detail'
const FORBIDDEN_TENANTS = new Set(['芋道源码', 'yudao', 'prod', 'production'])

const REQUIRED_ENV = [
  ['EDHR_ATTACHMENT_E2E_PASSWORD', '测试租户登录密码'],
  ['EDHR_ATTACHMENT_E2E_SIGNATURE_PASSWORD', '字段审计电子签名密码']
]

const BDD_SCENARIOS = [
  'BDD: 真实页面附件上传预登记 -> Given 测试租户存在真实 DRAFT 执行记录且模板包含附件字段 / When 操作员登录执行页并选择文件 / Then 前端必须调用真实 `/mes/pro/batch-record-execution/attachment/prepare-upload` 并获得 fileId、storagePath、sha256 与 storageRetentionHash。',
  'BDD: 附件签名保存进入审计链 -> Given prepareUpload 返回完整结构化元数据 / When 操作员填写原因并输入电子签名密码保存 / Then 前端必须调用真实 `/field-audit/save-changes`，请求体包含 attachmentChanges，响应 hashVerification.status 必须为 VALID。',
  'BDD: 保存后附件当前态可见 -> Given 附件保存成功 / When 执行页刷新详情 / Then 页面“当前附件证据”展示同一文件名、sha256 和附件 Hash。'
]

function envValue(name) {
  return (process.env[name] || '').trim()
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function collectConfig() {
  const tenant = envValue('EDHR_ATTACHMENT_E2E_TENANT') || '测试租户'
  const config = {
    baseUrl: envValue('EDHR_ATTACHMENT_E2E_BASE_URL') || REQUIRED_BASE_URL,
    tenant,
    username: envValue('EDHR_ATTACHMENT_E2E_USERNAME') || 'aoteman',
    password: envValue('EDHR_ATTACHMENT_E2E_PASSWORD'),
    signaturePassword: envValue('EDHR_ATTACHMENT_E2E_SIGNATURE_PASSWORD'),
    executionId: envValue('EDHR_ATTACHMENT_E2E_EXECUTION_ID'),
    workTaskId: envValue('EDHR_ATTACHMENT_E2E_WORK_TASK_ID'),
    headed: envValue('EDHR_ATTACHMENT_E2E_HEADED') === '1'
  }
  const missing = REQUIRED_ENV.filter(([name]) => !envValue(name)).map(([name, description]) => ({
    name,
    description
  }))
  const discovered = discoverAttachmentCandidate(config)
  if (!config.executionId && discovered.executionId) {
    config.executionId = String(discovered.executionId)
  }
  if (!config.workTaskId && discovered.workTaskId) {
    config.workTaskId = String(discovered.workTaskId)
  }
  config.prerequisiteDiscovery = discovered
  if (config.baseUrl !== REQUIRED_BASE_URL) {
    missing.push({ name: 'EDHR_ATTACHMENT_E2E_BASE_URL', description: `必须固定为 ${REQUIRED_BASE_URL}` })
  }
  if (FORBIDDEN_TENANTS.has(config.tenant.toLowerCase()) || config.tenant.includes('芋道源码')) {
    missing.push({ name: 'EDHR_ATTACHMENT_E2E_TENANT', description: '真实上传 E2E 禁止使用正式或芋道源码租户' })
  }
  if (!config.executionId) {
    missing.push({
      name: 'EDHR_ATTACHMENT_E2E_EXECUTION_ID',
      description: discovered.reason || '未发现真实 DRAFT 执行记录，且模板包含 upload-file/upload-image/upload-images 字段'
    })
  }
  if (!config.workTaskId) {
    missing.push({
      name: 'EDHR_ATTACHMENT_E2E_WORK_TASK_ID',
      description: discovered.reason || '未发现真实 TODO/DOING 工作任务，处理人必须为当前测试账号'
    })
  }
  for (const [name, value] of [
    ['EDHR_ATTACHMENT_E2E_EXECUTION_ID', config.executionId],
    ['EDHR_ATTACHMENT_E2E_WORK_TASK_ID', config.workTaskId]
  ]) {
    if (value && !/^\d+$/.test(value)) {
      missing.push({ name, description: '必须为真实数字 ID' })
    }
  }
  return { ...config, missing }
}

function discoverAttachmentCandidate(config) {
  const mysqlContainer = envValue('EDHR_ATTACHMENT_E2E_MYSQL_CONTAINER') || 'int-ruoyi-mysql'
  const mysqlUser = envValue('EDHR_ATTACHMENT_E2E_MYSQL_USER') || 'root'
  const mysqlPassword = envValue('EDHR_ATTACHMENT_E2E_MYSQL_PASSWORD') || '123456'
  const mysqlDatabase = envValue('EDHR_ATTACHMENT_E2E_MYSQL_DATABASE') || 'ruoyi-vue-pro'
  const tenantId = envValue('EDHR_ATTACHMENT_E2E_TENANT_ID') || '122'
  const assigneeUserId = envValue('EDHR_ATTACHMENT_E2E_ASSIGNEE_USER_ID') || '113'
  const sql = `
SELECT JSON_OBJECT(
  'executionId', candidate.execution_id,
  'workTaskId', candidate.work_task_id,
  'executionCode', candidate.execution_code,
  'taskCode', candidate.task_code,
  'draftUploadExecutionCount', (
    SELECT COUNT(*)
    FROM mes_pro_batch_record_execution e
    WHERE e.tenant_id=${tenantId}
      AND e.deleted=0
      AND e.status=0
      AND (
        e.execution_snapshot_json LIKE '%upload-file%'
        OR e.execution_snapshot_json LIKE '%upload-image%'
        OR e.execution_snapshot_json LIKE '%upload-images%'
      )
  ),
  'allUploadExecutionCount', (
    SELECT COUNT(*)
    FROM mes_pro_batch_record_execution e
    WHERE e.tenant_id=${tenantId}
      AND e.deleted=0
      AND (
        e.execution_snapshot_json LIKE '%upload-file%'
        OR e.execution_snapshot_json LIKE '%upload-image%'
        OR e.execution_snapshot_json LIKE '%upload-images%'
      )
  ),
  'activeAssigneeWorkTaskCount', (
    SELECT COUNT(*)
    FROM mes_pro_edhr_work_task w
    WHERE w.tenant_id=${tenantId}
      AND w.deleted=0
      AND w.assignee_user_id=${assigneeUserId}
      AND w.status IN ('TODO','DOING')
  )
) AS discovery
FROM (
  SELECT e.id AS execution_id, w.id AS work_task_id, e.execution_code, w.task_code
  FROM mes_pro_batch_record_execution e
  JOIN mes_pro_edhr_work_task w
    ON w.execution_id=e.id
   AND w.tenant_id=e.tenant_id
   AND w.deleted=0
  WHERE e.tenant_id=${tenantId}
    AND e.deleted=0
    AND e.status=0
    AND w.assignee_user_id=${assigneeUserId}
    AND w.status IN ('TODO','DOING')
    AND (
      e.execution_snapshot_json LIKE '%upload-file%'
      OR e.execution_snapshot_json LIKE '%upload-image%'
      OR e.execution_snapshot_json LIKE '%upload-images%'
    )
  ORDER BY e.update_time DESC
  LIMIT 1
) candidate;`
  const result = spawnSync(
    'docker',
    [
      'exec',
      mysqlContainer,
      'mysql',
      `-u${mysqlUser}`,
      `-p${mysqlPassword}`,
      '-N',
      '-B',
      mysqlDatabase,
      '-e',
      sql
    ],
    { encoding: 'utf8' }
  )
  if (result.status !== 0) {
    return {
      status: 'UNKNOWN',
      reason: `只读候选发现失败：${(result.stderr || result.stdout || '').trim() || `exit ${result.status}`}`
    }
  }
  const output = (result.stdout || '').trim()
  if (!output) {
    const counts = discoverAttachmentCounts({
      mysqlContainer,
      mysqlUser,
      mysqlPassword,
      mysqlDatabase,
      tenantId,
      assigneeUserId
    })
    return {
      status: 'NOT_FOUND',
      ...counts,
      reason: `未发现测试租户 ${tenantId} 下处理人 ${assigneeUserId} 可用的 DRAFT 附件执行任务。DRAFT 上传快照数=${counts.draftUploadExecutionCount ?? '未知'}，全部上传快照数=${counts.allUploadExecutionCount ?? '未知'}，处理人活动任务数=${counts.activeAssigneeWorkTaskCount ?? '未知'}。`
    }
  }
  try {
    return {
      status: 'FOUND',
      ...JSON.parse(output)
    }
  } catch (error) {
    return {
      status: 'UNKNOWN',
      reason: `只读候选发现返回不可解析 JSON：${output}`
    }
  }
}

function discoverAttachmentCounts({ mysqlContainer, mysqlUser, mysqlPassword, mysqlDatabase, tenantId, assigneeUserId }) {
  const sql = `
SELECT
  (SELECT COUNT(*) FROM mes_pro_batch_record_execution e WHERE e.tenant_id=${tenantId} AND e.deleted=0 AND e.status=0 AND (e.execution_snapshot_json LIKE '%upload-file%' OR e.execution_snapshot_json LIKE '%upload-image%' OR e.execution_snapshot_json LIKE '%upload-images%')) AS draft_upload_execution_count,
  (SELECT COUNT(*) FROM mes_pro_batch_record_execution e WHERE e.tenant_id=${tenantId} AND e.deleted=0 AND (e.execution_snapshot_json LIKE '%upload-file%' OR e.execution_snapshot_json LIKE '%upload-image%' OR e.execution_snapshot_json LIKE '%upload-images%')) AS all_upload_execution_count,
  (SELECT COUNT(*) FROM mes_pro_edhr_work_task w WHERE w.tenant_id=${tenantId} AND w.deleted=0 AND w.assignee_user_id=${assigneeUserId} AND w.status IN ('TODO','DOING')) AS active_assignee_work_task_count;`
  const result = spawnSync(
    'docker',
    [
      'exec',
      mysqlContainer,
      'mysql',
      `-u${mysqlUser}`,
      `-p${mysqlPassword}`,
      '-N',
      '-B',
      mysqlDatabase,
      '-e',
      sql
    ],
    { encoding: 'utf8' }
  )
  if (result.status !== 0) return {}
  const [draftUploadExecutionCount, allUploadExecutionCount, activeAssigneeWorkTaskCount] = (result.stdout || '')
    .trim()
    .split(/\s+/)
  return {
    draftUploadExecutionCount,
    allUploadExecutionCount,
    activeAssigneeWorkTaskCount
  }
}

function sqlString(value) {
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`
}

function runMysqlScalar(sql, config) {
  const mysqlContainer = envValue('EDHR_ATTACHMENT_E2E_MYSQL_CONTAINER') || 'int-ruoyi-mysql'
  const mysqlUser = envValue('EDHR_ATTACHMENT_E2E_MYSQL_USER') || 'root'
  const mysqlPassword = envValue('EDHR_ATTACHMENT_E2E_MYSQL_PASSWORD') || '123456'
  const mysqlDatabase = envValue('EDHR_ATTACHMENT_E2E_MYSQL_DATABASE') || 'ruoyi-vue-pro'
  const result = spawnSync(
    'docker',
    [
      'exec',
      mysqlContainer,
      'mysql',
      `-u${mysqlUser}`,
      `-p${mysqlPassword}`,
      '-N',
      '-B',
      mysqlDatabase,
      '-e',
      sql
    ],
    { encoding: 'utf8' }
  )
  if (result.status !== 0) {
    throw new Error(`只读附件账本核验失败：${(result.stderr || result.stdout || '').trim() || `exit ${result.status}`}`)
  }
  return (result.stdout || '').trim()
}

function verifyAttachmentLedger(config, metadata) {
  assert.match(String(config.executionId), /^\d+$/, 'executionId 必须为数字 ID')
  assert.match(String(config.workTaskId), /^\d+$/, 'workTaskId 必须为数字 ID')
  assert.match(String(metadata.fileId), /^\d+$/, 'fileId 必须为数字 ID')
  const sql = `
SELECT JSON_OBJECT(
  'count', COUNT(*),
  'attachmentHash', MAX(attachment_hash),
  'auditBatchId', MAX(audit_batch_id),
  'signatureId', MAX(signature_id),
  'attachmentAction', MAX(attachment_action),
  'fileName', MAX(file_name),
  'storagePath', MAX(storage_path)
) AS ledger_check
FROM mes_pro_batch_record_execution_attachment
WHERE deleted=0
  AND execution_id=${Number(config.executionId)}
  AND work_task_id=${Number(config.workTaskId)}
  AND file_id=${Number(metadata.fileId)}
  AND sha256=${sqlString(metadata.sha256)}
  AND storage_retention_hash=${sqlString(metadata.storageRetentionHash)}
  AND attachment_action IN ('ADD', 'REPLACE')
  AND audit_batch_id IS NOT NULL
  AND signature_id IS NOT NULL
  AND attachment_hash REGEXP '^[0-9a-f]{64}$';`
  const output = runMysqlScalar(sql, config)
  if (!output) {
    throw new Error('附件账本核验无返回结果')
  }
  let parsed
  try {
    parsed = JSON.parse(output)
  } catch (error) {
    throw new Error(`附件账本核验返回不可解析 JSON：${output}`)
  }
  assert.equal(parsed.count, 1, `附件账本必须存在且仅存在一条匹配记录，实际 ${parsed.count}`)
  assert.ok(parsed.attachmentHash, '附件账本缺少 attachmentHash')
  assert.ok(parsed.auditBatchId, '附件账本缺少 auditBatchId')
  assert.ok(parsed.signatureId, '附件账本缺少 signatureId')
  return parsed
}

function writeEvidence(result) {
  ensureDir(path.dirname(EVIDENCE_FILE))
  const lines = [
    '# eDHR 附件上传真实路径 E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- 状态：${result.status}`,
    `- 前端入口：\`${REQUIRED_BASE_URL}\``,
    '- 测试租户：`测试租户`；账号名默认 `aoteman`，密码和签名密码由环境变量注入，不写入仓库。',
    '- 真实 E2E 命令：`pnpm e2e:edhr:attachment-upload`',
    '- 静态语法检查命令：`pnpm e2e:edhr:attachment-upload:check`',
    '',
    '## BDD',
    '',
    ...BDD_SCENARIOS.map((item) => `- ${item}`),
    '',
    '## Result',
    ''
  ]
  if (result.status === 'BLOCKED') {
    lines.push(`- BLOCKED: \`pnpm e2e:edhr:attachment-upload\` -> FAIL, ${result.reason}`)
    for (const item of result.missing || []) {
      lines.push(`- 缺失前置：\`${item.name}\`，${item.description}`)
    }
    if (result.prerequisiteDiscovery) {
      lines.push('- 只读候选发现：')
      lines.push(`  - 状态：\`${result.prerequisiteDiscovery.status || 'UNKNOWN'}\``)
      if (result.prerequisiteDiscovery.reason) {
        lines.push(`  - 原因：${result.prerequisiteDiscovery.reason}`)
      }
      if (result.prerequisiteDiscovery.draftUploadExecutionCount != null) {
        lines.push(`  - DRAFT 上传快照数：\`${result.prerequisiteDiscovery.draftUploadExecutionCount}\``)
      }
      if (result.prerequisiteDiscovery.allUploadExecutionCount != null) {
        lines.push(`  - 全部上传快照数：\`${result.prerequisiteDiscovery.allUploadExecutionCount}\``)
      }
      if (result.prerequisiteDiscovery.activeAssigneeWorkTaskCount != null) {
        lines.push(`  - 处理人活动任务数：\`${result.prerequisiteDiscovery.activeAssigneeWorkTaskCount}\``)
      }
    }
    lines.push('- 影响：无法通过真实页面验证附件上传、签名保存和当前附件证据展示；未使用 mock、API-only 或测试专用控件。')
  } else if (result.status === 'PASS') {
    lines.push('- GREEN: 真实附件上传、签名保存和当前附件证据展示已完成。')
    lines.push(`- executionId：\`${result.executionId}\``)
    lines.push(`- workTaskId：\`${result.workTaskId}\``)
    lines.push(`- fileName：\`${result.fileName}\``)
    lines.push(`- sha256：\`${result.sha256}\``)
    lines.push(`- attachmentHash：\`${result.attachmentHash || '--'}\``)
    if (result.ledgerVerification) {
      lines.push(`- DB 只读附件账本核验：PASS，记录数 \`${result.ledgerVerification.count}\`，auditBatchId \`${result.ledgerVerification.auditBatchId}\`，signatureId \`${result.ledgerVerification.signatureId}\`。`)
    }
    lines.push(`- Trace：\`${result.trace || '--'}\``)
  } else {
    lines.push(`- RED: 真实上传 E2E 失败，${result.error || '未知错误'}`)
  }
  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('缺少 Playwright runtime，请先在此前端 worktree 执行 pnpm install。')
  }
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`页面缺少可见输入框：${label}`)
}

async function clickButton(root, name) {
  const button = root.getByRole('button', { name }).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  if (await button.isDisabled()) throw new Error(`按钮不可用：${name}`)
  await button.click()
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(EXECUTION_DETAIL_ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，真实 E2E 无法无人值守执行。')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    await page.keyboard.press('Enter')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, '用户名')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, '密码')
  await clickButton(loginForm, /^登录$/)
  await page.waitForURL((url) => !url.href.includes('/login'), { waitUntil: 'domcontentloaded', timeout: 60000 })
}

function createUploadFixture() {
  ensureDir(RESULT_DIR)
  const filePath = path.join(RESULT_DIR, `edhr-attachment-${Date.now()}.txt`)
  fs.writeFileSync(filePath, `eDHR attachment real E2E ${new Date().toISOString()}\n`, 'utf8')
  return filePath
}

async function runRealFlow(config) {
  const { chromium } = loadPlaywright()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
  const page = await context.newPage()
  await context.tracing.start({ screenshots: true, snapshots: true })
  const trace = path.join(RESULT_DIR, 'trace.zip')
  try {
    await login(page, config)
    const detailUrl = `${config.baseUrl}${EXECUTION_DETAIL_ROUTE}?id=${config.executionId}&workTaskId=${config.workTaskId}`
    await page.goto(detailUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByText('eDHR 执行详情').first().waitFor({ state: 'visible', timeout: 60000 })
    await page.getByText('eDHR 受控附件').first().waitFor({ state: 'visible', timeout: 60000 })

    const fixture = createUploadFixture()
    const prepareResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/mes/pro/batch-record-execution/attachment/prepare-upload'),
      { timeout: 60000 }
    )
    await page.locator('.edhr-page-shell__attachment-field input[type="file"]').first().setInputFiles(fixture)
    const prepareResponse = await prepareResponsePromise
    assert.equal(prepareResponse.status(), 200, 'prepareUpload HTTP 状态必须为 200')
    const prepareBody = await prepareResponse.json()
    assert.equal(
      prepareBody.code,
      0,
      `prepareUpload 业务响应必须成功：${prepareBody.msg || prepareBody.code}，响应：${JSON.stringify(prepareBody)}`
    )
    const metadata = prepareBody.data || {}
    for (const key of ['uploadToken', 'fileId', 'storageConfigId', 'storagePath', 'fileName', 'contentType', 'fileSize', 'sha256', 'storageRetentionJson', 'storageRetentionHash']) {
      assert.ok(metadata[key], `prepareUpload 响应缺少 ${key}`)
    }

    await page.locator(`a[href="${metadata.fileUrl}"]`).first().waitFor({ state: 'visible', timeout: 30000 })
    const reasonForm = page.locator('.edhr-page-shell__field-audit-reason').first()
    await reasonForm.locator('.el-select').first().click()
    await page.getByRole('option', { name: '操作录入' }).click()
    await fillFirstVisible(reasonForm.locator('input[placeholder="请输入字段变更原因"]'), '真实附件上传进入 eDHR 审计链', '字段变更原因')

    await clickButton(page, /^保存变更$/)
    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '字段变更电子签名' }).last()
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    await dialog.getByText('待保存附件').first().waitFor({ state: 'visible', timeout: 30000 })
    await dialog.getByText(metadata.fileUrl).first().waitFor({ state: 'visible', timeout: 30000 })
    await fillFirstVisible(dialog.locator('input[type="password"], input[placeholder*="签名"]'), config.signaturePassword, '签名密码')
    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        response.url().includes('/mes/pro/batch-record-execution/field-audit/save-changes'),
      { timeout: 60000 }
    ).catch((error) => ({ error }))
    await clickButton(dialog, /^确认签名并保存$|^确认保存$|^确\s*认\s*保\s*存$|^确\s*定$|^确认$/)
    const saveResponseResult = await saveResponsePromise
    if (saveResponseResult.error) {
      throw saveResponseResult.error
    }
    const saveResponse = saveResponseResult
    assert.equal(saveResponse.status(), 200, 'save-changes HTTP 状态必须为 200')
    const saveBody = await saveResponse.json()
    assert.equal(
      saveBody.code,
      0,
      `save-changes 业务响应必须成功：${saveBody.msg || saveBody.code}，响应：${JSON.stringify(saveBody)}`
    )
    assert.equal(saveBody.data?.hashVerification?.status, 'VALID', '附件保存后 hashVerification.status 必须为 VALID')

    await page.getByText('当前附件证据').first().waitFor({ state: 'visible', timeout: 60000 })
    await page.getByText(metadata.fileName).first().waitFor({ state: 'visible', timeout: 60000 })
    await page.getByText(metadata.sha256).first().waitFor({ state: 'visible', timeout: 60000 })
    const attachmentHashText = await page.locator('.edhr-page-shell__attachments').locator('text=/[0-9a-f]{64}/i').last().textContent()
    const ledgerVerification = verifyAttachmentLedger(config, metadata)
    await context.tracing.stop({ path: trace })
    return {
      executionId: config.executionId,
      workTaskId: config.workTaskId,
      fileName: metadata.fileName,
      sha256: metadata.sha256,
      attachmentHash: ledgerVerification.attachmentHash || attachmentHashText?.trim(),
      ledgerVerification,
      trace
    }
  } catch (error) {
    await context.tracing.stop({ path: trace }).catch(() => null)
    throw error
  } finally {
    await browser.close()
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    writeEvidence({
      status: 'BLOCKED',
      reason: '真实附件上传 E2E 前置条件缺失或命中受保护租户。',
      missing: config.missing,
      prerequisiteDiscovery: config.prerequisiteDiscovery
    })
    console.error('真实附件上传 E2E 前置条件缺失或命中受保护租户。')
    process.exitCode = 1
    return
  }
  try {
    const result = await runRealFlow(config)
    writeEvidence({ status: 'PASS', ...result })
    console.log('PASS: eDHR attachment upload real flow')
  } catch (error) {
    writeEvidence({ status: 'FAIL', error: error instanceof Error ? error.message : String(error) })
    throw error
  }
}

main()
