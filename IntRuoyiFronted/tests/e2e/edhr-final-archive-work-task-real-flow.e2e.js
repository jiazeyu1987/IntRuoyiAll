const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260612-edhr-final-archive-todo-assessment'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-final-archive-work-task')
const EVIDENCE_FILE = path.resolve(process.cwd(), '..', 'doc', 'tasks', TASK_ID, 'real-e2e-evidence.md')
const REQUIRED_BASE_URL = 'http://localhost:8081'
const DEFAULT_TENANT = '测试租户'
const DEFAULT_USERNAME = 'aoteman'
const WORK_TASK_ROUTE = '/mes/pro/feedback/edhr-work-task'
const BATCH_DETAIL_ROUTE = '/mes/pro/feedback/edhr-batch-execution/detail'
const ARCHIVE_GENERATE_ENDPOINT = '/mes/pro/edhr-batch-execution-archive/generate'
const ARCHIVE_DOWNLOAD_ENDPOINT = '/mes/pro/edhr-batch-execution-archive/download'

const BDD_SCENARIOS = [
  'BDD: 归档责任人从待办生成最终归档 -> Given 测试租户存在真实 ARCHIVE/TODO 工作任务 / When 归档责任人登录工作任务看板并点击处理 / Then 前端进入批次详情且 URL 携带 workTaskId。',
  'BDD: 归档生成携带待办上下文 -> Given 批次详情由归档待办打开 / When 用户点击生成最终归档 / Then 归档生成请求 payload 包含同一个 workTaskId，响应为 SEALED。',
  'BDD: 归档成功关闭待办 -> Given 最终归档生成成功 / When 使用已登录上下文做最终只读校验 / Then 批次为 ARCHIVED，归档为 SEALED，工作任务为 DONE，最终归档 PDF 可下载。',
  'BDD: 缺少真实前置即阻塞 -> Given 缺少测试租户密码、真实 ARCHIVE/TODO 待办、批次 ID 或签名前置 / When 执行真实 E2E / Then 写入 BLOCKED 证据并退出非零，不使用 mock、API-only 或测试专用 UI。'
]

function envValue(key) {
  return (process.env[key] || '').trim()
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function collectConfig() {
  const config = {
    baseUrl: envValue('EDHR_ARCHIVE_TASK_E2E_BASE_URL') || REQUIRED_BASE_URL,
    tenant: envValue('EDHR_ARCHIVE_TASK_E2E_TENANT') || DEFAULT_TENANT,
    username: envValue('EDHR_ARCHIVE_TASK_E2E_ARCHIVER_USERNAME') || DEFAULT_USERNAME,
    password: envValue('EDHR_ARCHIVE_TASK_E2E_ARCHIVER_PASSWORD'),
    workTaskId: envValue('EDHR_ARCHIVE_TASK_E2E_WORK_TASK_ID'),
    batchExecutionId: envValue('EDHR_ARCHIVE_TASK_E2E_BATCH_EXECUTION_ID'),
    batchCode: envValue('EDHR_ARCHIVE_TASK_E2E_BATCH_CODE'),
    headed: envValue('EDHR_ARCHIVE_TASK_E2E_HEADED') === '1'
  }
  return {
    ...config,
    missing: collectInvalidConfig(config)
  }
}

function collectInvalidConfig(config) {
  const invalid = []
  const explicitTarget = hasExplicitArchiveTarget(config)
  if (config.baseUrl !== REQUIRED_BASE_URL) {
    invalid.push({
      key: 'EDHR_ARCHIVE_TASK_E2E_BASE_URL',
      description: `真实前端入口必须固定为 ${REQUIRED_BASE_URL}。`
    })
  }
  if (config.tenant !== DEFAULT_TENANT) {
    invalid.push({
      key: 'EDHR_ARCHIVE_TASK_E2E_TENANT',
      description: '最终归档待办真实 E2E 只能使用测试租户，禁止使用芋道源码或正式租户写入。'
    })
  }
  if (!config.password) {
    invalid.push({
      key: 'EDHR_ARCHIVE_TASK_E2E_ARCHIVER_PASSWORD',
      description: '归档责任人登录密码必须由当前进程环境注入，不得写入脚本默认值。'
    })
  }
  if (explicitTarget) {
    if (!/^\d+$/.test(config.workTaskId)) {
      invalid.push({
        key: 'EDHR_ARCHIVE_TASK_E2E_WORK_TASK_ID',
        description: '显式指定归档目标时必须提供真实 ARCHIVE/TODO 工作任务 ID；也可以不传目标，让脚本从工作任务看板发现。'
      })
    }
    if (!/^\d+$/.test(config.batchExecutionId)) {
      invalid.push({
        key: 'EDHR_ARCHIVE_TASK_E2E_BATCH_EXECUTION_ID',
        description: '显式指定归档目标时必须提供该待办对应的真实批次执行 ID；也可以不传目标，让脚本从工作任务看板发现。'
      })
    }
    if (!config.batchCode) {
      invalid.push({
        key: 'EDHR_ARCHIVE_TASK_E2E_BATCH_CODE',
        description: '显式指定归档目标时必须提供批次号；也可以不传目标，让脚本从工作任务看板发现。'
      })
    }
  }
  return invalid
}

function hasExplicitArchiveTarget(config) {
  return Boolean(config.workTaskId || config.batchExecutionId || config.batchCode)
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    const blocked = new Error('Missing Playwright runtime. Run `pnpm install`, then re-run `pnpm e2e:edhr:final-archive-task`.')
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
  fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function writeEvidence(result) {
  ensureDir(path.dirname(EVIDENCE_FILE))
  const lines = [
    '# eDHR 最终归档待办真实路径 E2E Evidence',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- 生成时间：${new Date().toISOString()}`,
    `- 前端 worktree：${process.cwd()}`,
    `- 固定前端入口：\`${REQUIRED_BASE_URL}\``,
    '- 真实 E2E 复跑命令：`pnpm e2e:edhr:final-archive-task`',
    '- 静态语法检查命令：`pnpm e2e:edhr:final-archive-task:check`',
    '- 静态合同命令：`node tests/e2e/edhr-final-archive-work-task-static.spec.js`',
    '- 临时产物目录：`test-results/edhr-final-archive-work-task/`（截图、trace 与 result.json 不提交）',
    `- 当前状态：${result.status}`,
    `- 租户：\`${result.tenant || '<missing>'}\``,
    `- 账号：\`${result.username || '<missing>'}\`；密码由环境变量注入，不写入仓库证据。`,
    `- 归档目标来源：\`${result.targetSource || '<pending>'}\``,
    `- batchExecutionId：\`${result.batchExecutionId || '<missing>'}\``,
    `- workTaskId：\`${result.workTaskId || '<missing>'}\``,
    '',
    '## BDD',
    '',
    ...BDD_SCENARIOS.map((scenario) => `- ${scenario}`),
    ''
  ]

  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED', '')
    lines.push(`- BLOCKED: \`pnpm e2e:edhr:final-archive-task\` -> FAIL, ${result.reason}`)
    if (result.missing?.length) {
      lines.push('- 不满足的真实 E2E 前置条件：')
      for (const item of result.missing) lines.push(`  - \`${item.key}\`：${item.description}`)
    }
    lines.push('- 影响：无法通过真实页面证明归档责任人从 `ARCHIVE/TODO` 待办生成最终归档；未使用 mock、API-only 或测试专用 UI。', '')
  }

  if (result.status === 'PASS') {
    lines.push('## GREEN', '')
    lines.push('- GREEN: `pnpm e2e:edhr:final-archive-task` -> PASS，归档责任人从真实待办生成最终归档，payload 携带 workTaskId，最终任务为 DONE。')
    for (const step of result.steps || []) {
      lines.push(`- ${step.name} -> PASS${step.screenshot ? `, screenshot: \`${step.screenshot}\`` : ''}`)
    }
    lines.push(`- 归档响应状态：\`${result.archiveStatus}\``)
    lines.push(`- 最终批次状态：\`${result.finalBatchStatus}\``)
    lines.push(`- 最终工作任务状态：\`${result.finalWorkTaskStatus}\``)
    lines.push(`- 最终归档下载字节数：\`${result.downloadedBytes}\``)
    lines.push(`- Trace: \`${result.trace}\``, '')
  }

  if (result.status === 'FAIL') {
    lines.push('## RED', '')
    lines.push(`- RED: \`pnpm e2e:edhr:final-archive-task\` -> FAIL, ${result.error?.message || '未知错误'}`)
    lines.push('- 影响：真实 UI E2E 未放行；不得提交为通过。', '')
  }

  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
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
  throw new Error(`Cannot find visible ${label} input`)
}

async function clickFirstEnabledButton(root, name, label) {
  const buttons = root.getByRole('button', { name })
  const count = await buttons.count()
  for (let index = 0; index < count; index += 1) {
    const button = buttons.nth(index)
    if ((await button.isVisible()) && !(await button.isDisabled())) {
      await button.click()
      return
    }
  }
  throw new Error(`Cannot find enabled button: ${label}`)
}

function createBlockedError(message) {
  const error = new Error(message)
  error.blocked = true
  return error
}

async function selectArchiveTaskTypeFilter(page) {
  const toolbar = page.locator('.edhr-work-task-page__toolbar').first()
  await toolbar.waitFor({ state: 'visible', timeout: 60000 })
  await toolbar.locator('.el-select').first().click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: '最终归档' }).first()
  await option.waitFor({ state: 'visible', timeout: 60000 })
  await option.click()
  await clickFirstEnabledButton(toolbar, /^查询$/, '查询最终归档待办')
  await page.waitForLoadState('networkidle', { timeout: 60000 }).catch(() => undefined)
}

async function readCellText(row, index, label) {
  const value = (await row.locator('td').nth(index).innerText().catch(() => '')).trim()
  if (!value || value === '--') {
    throw createBlockedError(`真实最终归档待办缺少${label}，无法继续 E2E。`)
  }
  return value
}

async function discoverArchiveWorkTaskFromBoard(page, steps) {
  await selectArchiveTaskTypeFilter(page)
  const targetRow = page.locator('.el-table__body-wrapper tr').filter({ hasText: '最终归档' }).first()
  try {
    await targetRow.waitFor({ state: 'visible', timeout: 60000 })
  } catch {
    throw createBlockedError('测试租户工作任务看板没有真实 ARCHIVE/TODO 待办。')
  }
  const batchCode = await readCellText(targetRow, 3, '批次号')
  steps.push({ name: '工作任务看板发现最终归档待办', screenshot: await screenshot(page, 'work-task-board', steps) })
  await clickFirstEnabledButton(targetRow, /处理/, '处理最终归档待办')
  await page.waitForURL(
    (url) => url.pathname === BATCH_DETAIL_ROUTE && Boolean(url.searchParams.get('workTaskId')) && Boolean(url.searchParams.get('id')),
    { timeout: 60000 }
  )
  const url = new URL(page.url())
  return {
    workTaskId: url.searchParams.get('workTaskId'),
    batchExecutionId: url.searchParams.get('id'),
    batchCode,
    targetSource: 'WORK_TASK_BOARD'
  }
}

async function selectTenant(page, tenant) {
  const loginForm = page.locator('.login-form:visible').first()
  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input[placeholder="请输入租户名称"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    await page.keyboard.press('Enter')
  }
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(WORK_TASK_ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('Captcha is enabled; unattended real E2E cannot continue.')
  }

  await selectTenant(page, config.tenant)
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'), config.username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, 'password')
  await clickFirstEnabledButton(loginForm, /^登录$/, '登录')
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
  await page.waitForLoadState('networkidle', { timeout: 60000 }).catch(() => undefined)
}

function unwrapStorageValue(raw) {
  if (!raw) return ''
  try {
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object' && Object.prototype.hasOwnProperty.call(parsed, 'v')) {
      try {
        return JSON.parse(parsed.v)
      } catch {
        return parsed.v
      }
    }
    return parsed
  } catch {
    return raw
  }
}

async function readAuthHeaders(page) {
  return page.evaluate(() => {
    function unwrap(raw) {
      if (!raw) return ''
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object' && Object.prototype.hasOwnProperty.call(parsed, 'v')) {
          try {
            return JSON.parse(parsed.v)
          } catch {
            return parsed.v
          }
        }
        return parsed
      } catch {
        return raw
      }
    }
    const accessToken = unwrap(window.localStorage.getItem('ACCESS_TOKEN') || window.sessionStorage.getItem('ACCESS_TOKEN'))
    const tenantId = unwrap(window.localStorage.getItem('tenantId') || window.sessionStorage.getItem('tenantId'))
    const headers = { Accept: 'application/json' }
    if (accessToken) headers.Authorization = String(accessToken).startsWith('Bearer ') ? String(accessToken) : `Bearer ${accessToken}`
    if (tenantId) headers['tenant-id'] = String(tenantId)
    return headers
  })
}

async function getJson(page, url) {
  const headers = await readAuthHeaders(page)
  assert.ok(headers.Authorization, '最终 API 交叉确认缺少已登录 Authorization。')
  assert.ok(headers['tenant-id'], '最终 API 交叉确认缺少 tenant-id。')
  return page.evaluate(
    async ({ targetUrl, headers }) => {
      const response = await fetch(targetUrl, { credentials: 'include', headers })
      if (!response.ok) throw new Error(`${targetUrl} HTTP ${response.status}`)
      const json = await response.json()
      if (json && Object.prototype.hasOwnProperty.call(json, 'code') && json.code !== 0) {
        throw new Error(`${targetUrl} business ${json.code}: ${json.msg || json.message}`)
      }
      return json.data ?? json
    },
    { targetUrl: url, headers }
  )
}

async function getBinaryLength(page, url) {
  const headers = await readAuthHeaders(page)
  assert.ok(headers.Authorization, '最终下载校验缺少已登录 Authorization。')
  assert.ok(headers['tenant-id'], '最终下载校验缺少 tenant-id。')
  return page.evaluate(
    async ({ targetUrl, headers }) => {
      const response = await fetch(targetUrl, { credentials: 'include', headers })
      if (!response.ok) throw new Error(`${targetUrl} HTTP ${response.status}`)
      const buffer = await response.arrayBuffer()
      return buffer.byteLength
    },
    { targetUrl: url, headers }
  )
}

async function screenshot(page, name, steps) {
  const filePath = path.join(RESULT_DIR, `${String(steps.length + 1).padStart(2, '0')}-${name}.png`)
  await page.screenshot({ path: filePath, fullPage: true })
  return filePath
}

async function runRealFlow(config) {
  const { chromium } = loadPlaywright()
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, acceptDownloads: true })
  await context.tracing.start({ screenshots: true, snapshots: true })
  const page = await context.newPage()
  const steps = []
  let archivePayload
  let archiveResponseData
  let target = {
    workTaskId: config.workTaskId,
    batchExecutionId: config.batchExecutionId,
    batchCode: config.batchCode,
    targetSource: hasExplicitArchiveTarget(config) ? 'ENV' : 'WORK_TASK_BOARD'
  }

  try {
    await login(page, config)
    await page.goto(`${config.baseUrl}${WORK_TASK_ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForLoadState('networkidle', { timeout: 60000 }).catch(() => undefined)

    if (hasExplicitArchiveTarget(config)) {
      await page.getByText('最终归档').first().waitFor({ state: 'visible', timeout: 60000 })
      await page.getByText(config.batchCode).first().waitFor({ state: 'visible', timeout: 60000 })
      steps.push({ name: '工作任务看板展示最终归档待办', screenshot: await screenshot(page, 'work-task-board', steps) })

      const targetRow = page.locator('.el-table__body-wrapper tr').filter({ hasText: config.batchCode }).filter({ hasText: '最终归档' }).first()
      await targetRow.waitFor({ state: 'visible', timeout: 60000 })
      await clickFirstEnabledButton(targetRow, /处理/, '处理最终归档待办')
      await page.waitForURL(
        (url) =>
          url.pathname === BATCH_DETAIL_ROUTE &&
          url.searchParams.get('workTaskId') === String(target.workTaskId) &&
          url.searchParams.get('id') === String(target.batchExecutionId),
        { timeout: 60000 }
      )
    } else {
      target = await discoverArchiveWorkTaskFromBoard(page, steps)
    }

    await page.getByText(target.batchCode).first().waitFor({ state: 'visible', timeout: 60000 })
    steps.push({ name: '待办入口进入批次详情并携带 workTaskId', screenshot: await screenshot(page, 'batch-detail-from-task', steps) })

    const archiveResponsePromise = page.waitForResponse(
      (response) => response.url().includes(ARCHIVE_GENERATE_ENDPOINT) && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    page.on('request', (request) => {
      if (request.url().includes(ARCHIVE_GENERATE_ENDPOINT) && request.method() === 'POST') {
        archivePayload = request.postDataJSON()
      }
    })
    await clickFirstEnabledButton(page, /^生成最终归档$/, '生成最终归档')
    const archiveResponse = await archiveResponsePromise
    assert.equal(archiveResponse.status(), 200, `归档生成 HTTP 状态应为 200，实际 ${archiveResponse.status()}`)
    const archiveBody = await archiveResponse.json()
    assert.equal(archiveBody.code, 0, `归档生成业务状态应为 0，实际 ${archiveBody.code}: ${archiveBody.msg || ''}`)
    archiveResponseData = archiveBody.data
    assert.equal(String(archivePayload?.workTaskId), String(target.workTaskId), '归档生成 payload 必须携带同一个 workTaskId。')
    assert.equal(String(archivePayload?.batchExecutionId), String(target.batchExecutionId), '归档生成 payload 必须携带目标 batchExecutionId。')
    assert.equal(archiveResponseData?.archiveStatus, 'SEALED', '归档生成响应必须为 SEALED。')
    steps.push({ name: '生成最终归档并校验 payload', screenshot: await screenshot(page, 'archive-generated', steps) })

    const batch = await getJson(page, `/admin-api/mes/pro/edhr-batch-execution/get?id=${target.batchExecutionId}`)
    const latestArchive = await getJson(page, `/admin-api/mes/pro/edhr-batch-execution-archive/latest?batchExecutionId=${target.batchExecutionId}`)
    const donePage = await getJson(
      page,
      `/admin-api/mes/pro/edhr-work-task/done-page?pageNo=1&pageSize=20&taskType=ARCHIVE&batchCode=${encodeURIComponent(target.batchCode)}`
    )
    const completedTask = (donePage.list || []).find((item) => String(item.id) === String(target.workTaskId))
    assert.equal(batch.status, 40, `批次最终状态必须为 ARCHIVED(40)，实际 ${batch.status}。`)
    assert.equal(latestArchive.archiveStatus, 'SEALED', `最新归档状态必须为 SEALED，实际 ${latestArchive.archiveStatus}。`)
    assert.ok(completedTask, '已处理工作任务分页必须能查到该 ARCHIVE 工作任务。')
    assert.equal(completedTask.status, 'DONE', `归档工作任务最终必须为 DONE，实际 ${completedTask.status}。`)
    const downloadedBytes = await getBinaryLength(page, `/admin-api${ARCHIVE_DOWNLOAD_ENDPOINT}?id=${latestArchive.id}`)
    assert.ok(downloadedBytes > 0, `最终归档下载内容不能为空，实际字节数 ${downloadedBytes}。`)
    steps.push({ name: '最终归档 PDF 可下载', screenshot: await screenshot(page, 'archive-downloadable', steps) })

    const tracePath = path.join(RESULT_DIR, 'trace.zip')
    await context.tracing.stop({ path: tracePath })
    await browser.close()
    return {
      status: 'PASS',
      tenant: config.tenant,
      username: config.username,
      batchExecutionId: target.batchExecutionId,
      workTaskId: target.workTaskId,
      targetSource: target.targetSource,
      archiveStatus: archiveResponseData.archiveStatus,
      finalBatchStatus: batch.status,
      finalWorkTaskStatus: completedTask.status,
      downloadedBytes,
      steps,
      trace: tracePath
    }
  } catch (error) {
    const tracePath = path.join(RESULT_DIR, 'trace-failure.zip')
    await context.tracing.stop({ path: tracePath }).catch(() => undefined)
    await browser.close().catch(() => undefined)
    error.trace = tracePath
    throw error
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length > 0) {
    const blocked = {
      status: 'BLOCKED',
      reason: '缺少真实最终归档待办 E2E 前置条件。',
      missing: config.missing,
      tenant: config.tenant,
      username: config.username,
      batchExecutionId: config.batchExecutionId,
      workTaskId: config.workTaskId
    }
    writeJsonResult(blocked)
    writeEvidence(blocked)
    console.error(blocked.reason)
    process.exitCode = 1
    return
  }

  try {
    const result = await runRealFlow(config)
    writeJsonResult(result)
    writeEvidence(result)
    console.log('PASS: eDHR final archive work task real flow')
  } catch (error) {
    const status = error.blocked ? 'BLOCKED' : 'FAIL'
    const result = {
      status,
      reason: error.blocked ? error.message : '真实最终归档待办 E2E 执行失败。',
      tenant: config.tenant,
      username: config.username,
      batchExecutionId: config.batchExecutionId,
      workTaskId: config.workTaskId,
      targetSource: hasExplicitArchiveTarget(config) ? 'ENV' : 'WORK_TASK_BOARD',
      error: serializeError(error),
      trace: error.trace
    }
    writeJsonResult(result)
    writeEvidence(result)
    console.error(error)
    process.exitCode = 1
  }
}

main()
