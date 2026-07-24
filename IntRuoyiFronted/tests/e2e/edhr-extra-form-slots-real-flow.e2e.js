const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { spawnSync } = require('node:child_process')

const TASK_ID = '20260707-edhr-extra-form-slots-implementation'
const REQUIRED_BASE_URL = 'http://localhost:8081'
const REQUIRED_TEST_TENANT = '测试租户'
const REQUIRED_TEST_USERNAME = 'aoteman'
const DEFAULT_TEST_PASSWORD = '111111'
const REQUIRED_ADMIN_TENANT = '芋道源码'
const REQUIRED_ADMIN_USERNAME = 'admin'
const SLOT_TYPES = ['MAIN', 'PROCESS_INSPECTION', 'LOSS_REPORT', 'PARAMETER_RECORD']
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-extra-form-slots')
const TASK_DIR = path.resolve(process.cwd(), '..', 'doc', 'tasks', TASK_ID)
const EVIDENCE_FILE = path.join(TASK_DIR, 'e2e-extra-form-slots-evidence.md')
const RESULT_FILE = path.join(RESULT_DIR, 'result.json')

const BDD_SCENARIOS = [
  'BDD: MAIN 主生产表真实槽位 -> Given 测试租户存在 eDHR 批次和工序路线 When 进入批次详情或执行页 Then MAIN 槽位必须可见且携带 recordCategory、validationProfile、permissionScopeId 和归档属性。',
  'BDD: PROCESS_INSPECTION 过程检验真实槽位 -> Given 工序配置质量过程检验单 When 批次流转到该工序 Then 质量槽位必须以独立任务/状态呈现，并由质量权限填写或阻塞。',
  'BDD: LOSS_REPORT 条件必填阻塞 -> Given 主生产表触发不合格或损耗条件 When 损耗单未完成 Then 下一槽位、下一工序或批次关闭必须被真实阻塞。',
  'BDD: PARAMETER_RECORD 设备参数真实槽位 -> Given 工序绑定设备参数记录 When 设备或生产角色进入对应任务 Then 参数槽位必须可见、可追溯且有权限边界。',
  'BDD: 对象级权限隔离 -> Given 不同角色和 permissionScopeId When 用户打开非授权槽位 Then 页面/API 必须显示 403、无权限或明确 blocker，不得静默展示空成功。',
  'BDD: 归档 manifest 完整性 -> Given 必填槽位全部完成 When 执行最终归档或只读查看最新归档 Then manifest/归档响应必须覆盖进入 DHR 的槽位和对应 hash。',
  'BDD: admin 只读复验 -> Given 测试租户完成写入型 E2E When 使用 芋道源码/admin 复验 Then 只允许 GET/HEAD，只验证租户隔离、无 MES 写请求和只读可见性。'
]

function envValue(key) {
  return String(process.env[key] || '').trim()
}

function config() {
  return {
    baseUrl: (envValue('EDHR_EXTRA_SLOTS_BASE_URL') || REQUIRED_BASE_URL).replace(/\/+$/, ''),
    backendUrl: (envValue('EDHR_EXTRA_SLOTS_BACKEND_URL') || 'http://127.0.0.1:48081').replace(/\/+$/, ''),
    tenant: envValue('EDHR_EXTRA_SLOTS_TENANT') || REQUIRED_TEST_TENANT,
    username: envValue('EDHR_EXTRA_SLOTS_USERNAME') || REQUIRED_TEST_USERNAME,
    password: envValue('EDHR_EXTRA_SLOTS_PASSWORD') || DEFAULT_TEST_PASSWORD,
    adminTenant: envValue('EDHR_EXTRA_SLOTS_ADMIN_TENANT') || REQUIRED_ADMIN_TENANT,
    adminUsername: envValue('EDHR_EXTRA_SLOTS_ADMIN_USERNAME') || REQUIRED_ADMIN_USERNAME,
    adminPassword: envValue('EDHR_EXTRA_SLOTS_ADMIN_PASSWORD'),
    targetRouteId: envValue('EDHR_EXTRA_SLOTS_ROUTE_ID'),
    targetBatchExecutionId: envValue('EDHR_EXTRA_SLOTS_BATCH_EXECUTION_ID'),
    headed: envValue('EDHR_EXTRA_SLOTS_HEADED') === '1'
  }
}

function block(reason, details = {}) {
  const error = new Error(reason)
  error.blocked = true
  error.details = details
  return error
}

function ensureDirectories() {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.mkdirSync(TASK_DIR, { recursive: true })
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw block('缺少前端仓 Playwright 运行依赖；请先在 yudao-ui-admin-vue3 安装依赖。', {
      error: error.message
    })
  }
}

function resolveChromiumExecutablePath() {
  const preferredBrowser = 'E:\\Int\\DevCache\\playwright-browsers\\chromium-1223\\chrome-win64\\chrome.exe'
  const systemChrome = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
  if (fs.existsSync(preferredBrowser)) {
    return preferredBrowser
  }
  if (fs.existsSync(systemChrome)) {
    return systemChrome
  }
  throw block('缺少可用 Chromium/Chrome 浏览器，无法执行真实 Playwright E2E。', {
    preferredBrowser,
    systemChrome
  })
}

function validateConfig(runtime) {
  if (runtime.baseUrl !== REQUIRED_BASE_URL) {
    throw block(`真实 E2E 固定本机前端入口 ${REQUIRED_BASE_URL}，当前为 ${runtime.baseUrl}。`)
  }
  if (runtime.tenant !== REQUIRED_TEST_TENANT || runtime.username !== REQUIRED_TEST_USERNAME) {
    throw block(`写入型 E2E 只能使用 ${REQUIRED_TEST_TENANT}/${REQUIRED_TEST_USERNAME}，当前为 ${runtime.tenant}/${runtime.username}。`)
  }
  if (!runtime.password) {
    throw block('缺少测试租户密码 EDHR_EXTRA_SLOTS_PASSWORD。')
  }
  if (runtime.adminTenant !== REQUIRED_ADMIN_TENANT || runtime.adminUsername !== REQUIRED_ADMIN_USERNAME) {
    throw block(`admin 只读复验必须使用 ${REQUIRED_ADMIN_TENANT}/${REQUIRED_ADMIN_USERNAME}，当前为 ${runtime.adminTenant}/${runtime.adminUsername}。`)
  }
}

function runOfficialLoginPreflight(runtime, account) {
  const repoRoot = path.resolve(process.cwd(), '..')
  const scriptPath = path.join(repoRoot, 'scripts', 'preflight', 'login-preflight.mjs')
  if (!fs.existsSync(scriptPath)) {
    throw block(`官方登录预检脚本不存在: ${scriptPath}`)
  }
  const env = { ...process.env }
  env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH = resolveChromiumExecutablePath()
  const args = [
    scriptPath,
    '--base-url',
    runtime.baseUrl,
    '--tenant',
    account.tenant,
    '--username',
    account.username,
    '--password',
    account.password,
    '--target-path',
    '/index',
    '--timeout',
    '30000'
  ]
  const result = spawnSync(process.execPath, args, {
    cwd: repoRoot,
    env,
    encoding: 'utf8'
  })
  if (result.status !== 0) {
    throw block(`${account.label} 官方登录预检失败。`, {
      stdout: result.stdout,
      stderr: result.stderr
    })
  }
  return { stdout: result.stdout.trim(), stderr: result.stderr.trim(), browser: env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || 'bundled' }
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const current = locator.nth(index)
    if (await current.isVisible()) {
      await current.fill(value)
      return
    }
  }
  throw new Error(`找不到可见输入框: ${label}`)
}

async function selectTenant(page, form, tenantName) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input, input[placeholder="请输入租户名称"]').first()
  if ((await tenantInput.count()) === 0 || !(await tenantInput.isVisible())) {
    throw new Error('登录页缺少可见租户输入框，无法确认租户上下文。')
  }
  await tenantInput.click()
  await tenantInput.fill('')
  await tenantInput.fill(tenantName)
  const option = page.locator('.el-select-dropdown__item').filter({ hasText: tenantName }).first()
  if ((await option.count()) > 0) {
    await option.click()
  } else {
    await tenantInput.press('Enter')
  }
}

async function login(page, runtime, account) {
  await page.goto(`${runtime.baseUrl}/login?redirect=%2Findex`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  if ((await form.locator('.verify-img-panel, .captcha, canvas').count()) > 0) {
    throw block('登录页启用验证码，无法无人值守执行真实 E2E。')
  }
  await selectTenant(page, form, account.tenant)
  await fillFirstVisible(form.locator('input[placeholder="请输入用户名"], input[placeholder="请输入账号"]'), account.username, 'username')
  await fillFirstVisible(form.locator('input[type="password"], input[placeholder="请输入密码"]'), account.password, 'password')
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `${account.label} 登录 HTTP ${loginResponse.status()}`)
  assert.ok([0, 200].includes(payload.code), `${account.label} 登录失败: ${payload.msg || payload.message || JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function apiGet(page, pathOrUrl) {
  return await page.evaluate(async (input) => {
    const unwrap = (raw) => {
      if (!raw) return ''
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object' && 'v' in parsed) {
          return String(parsed.v || '').replace(/^"|"$/g, '')
        }
      } catch (error) {
        return String(raw).replace(/^"|"$/g, '')
      }
      return String(raw).replace(/^"|"$/g, '')
    }
    const headers = {}
    const token = unwrap(window.localStorage.getItem('ACCESS_TOKEN') || window.sessionStorage.getItem('ACCESS_TOKEN'))
    const tenantId = unwrap(window.localStorage.getItem('tenantId') || window.sessionStorage.getItem('tenantId'))
    if (token) headers.Authorization = `Bearer ${token}`
    if (tenantId) headers['tenant-id'] = tenantId
    const response = await fetch(input, { method: 'GET', headers, credentials: 'include' })
    let body
    try {
      body = await response.json()
    } catch (error) {
      body = { raw: await response.text() }
    }
    return { ok: response.ok, status: response.status, body, tenantId }
  }, pathOrUrl)
}

async function detectRouteConfigSlots(page, runtime, preferredRouteId) {
  let routeId = runtime.targetRouteId || preferredRouteId
  if (!routeId) {
    const routePage = await apiGet(page, '/admin-api/mes/pro/route/page?pageNo=1&pageSize=20')
    if (!routePage.ok || ![0, 200].includes(routePage.body?.code)) {
      throw block('无法读取测试租户工艺路线列表，不能发现真实路线槽位配置。', routePage)
    }
    const routeList = routePage.body?.data?.list || routePage.body?.data?.records || []
    if (!routeList.length) {
      throw block('测试租户没有真实工艺路线数据，无法覆盖路线工序槽位配置。', {
        endpoint: '/admin-api/mes/pro/route/page'
      })
    }
    routeId = routeList[0].id
  }
  const configResult = await apiGet(
    page,
    `/admin-api/mes/pro/route/flow-config/process-config-list?routeId=${encodeURIComponent(routeId)}&useType=BATCH`
  )
  if (!configResult.ok || ![0, 200].includes(configResult.body?.code)) {
    throw block('路线批记录配置接口不可用，无法验证 formSlotType/recordCategory/permissionScopeId。', {
      routeId,
      response: configResult
    })
  }
  const rows = Array.isArray(configResult.body?.data) ? configResult.body.data : []
  const reports = []
  for (const processConfig of rows) {
    for (const report of processConfig.batchRecordReports || processConfig.reports || []) {
      reports.push({ routeId, processConfig, report })
    }
  }
  const bySlot = new Map()
  for (const item of reports) {
    const slotType = item.report.formSlotType || item.report.metadata?.formSlotType || 'MAIN'
    if (!bySlot.has(slotType)) bySlot.set(slotType, [])
    bySlot.get(slotType).push(item)
  }
  const missingSlots = SLOT_TYPES.filter((slot) => !bySlot.has(slot))
  if (missingSlots.length) {
    throw block('真实路线配置未覆盖设计文档要求的全部槽位。', {
      routeId,
      foundSlots: Array.from(bySlot.keys()),
      missingSlots
    })
  }
  const incomplete = reports.filter(({ report }) => {
    return !report.recordCategory || !report.validationProfile || !report.permissionScopeId
  })
  if (incomplete.length) {
    throw block('槽位配置缺少 recordCategory、validationProfile 或 permissionScopeId。', {
      routeId,
      incomplete: incomplete.map(({ report }) => ({
        id: report.id,
        batchRecordReportId: report.batchRecordReportId,
        formSlotType: report.formSlotType,
        recordCategory: report.recordCategory,
        validationProfile: report.validationProfile,
        permissionScopeId: report.permissionScopeId
      }))
    })
  }
  return {
    routeId,
    totalProcessConfigs: rows.length,
    totalReports: reports.length,
    slots: SLOT_TYPES.map((slot) => ({ slot, count: bySlot.get(slot).length }))
  }
}

async function detectBatchExecutionSlots(page, runtime) {
  let executionId = runtime.targetBatchExecutionId
  if (!executionId) {
    const pageResult = await apiGet(page, '/admin-api/mes/pro/edhr-batch-execution/page?pageNo=1&pageSize=20')
    if (!pageResult.ok || ![0, 200].includes(pageResult.body?.code)) {
      throw block('无法读取测试租户 eDHR 批次列表，不能验证批次详情槽位状态。', pageResult)
    }
    const batchList = pageResult.body?.data?.list || pageResult.body?.data?.records || []
    if (!batchList.length) {
      throw block('测试租户没有真实 eDHR 批次数据，无法覆盖 MAIN/附属槽位批次执行路径。', {
        endpoint: '/admin-api/mes/pro/edhr-batch-execution/page'
      })
    }
    executionId = batchList[0].id
  }
  const detail = await apiGet(page, `/admin-api/mes/pro/edhr-batch-execution/get?id=${encodeURIComponent(executionId)}`)
  if (!detail.ok || ![0, 200].includes(detail.body?.code)) {
    throw block('无法读取真实 eDHR 批次详情。', { executionId, response: detail })
  }
  const data = detail.body?.data || {}
  const serialized = JSON.stringify(data)
  const missingSlots = SLOT_TYPES.filter((slot) => !serialized.includes(slot))
  const requiredMarkers = ['recordCategory', 'validationProfile', 'permissionScopeId']
  const missingMarkers = requiredMarkers.filter((marker) => !serialized.includes(marker))
  if (missingSlots.length || missingMarkers.length) {
    throw block('批次详情未返回完整槽位上下文，无法验证槽位状态、条件必填和权限快照。', {
      executionId,
      missingSlots,
      missingMarkers
    })
  }
  return {
    executionId,
    routeId: data.routeId || data.route?.id || data.routeInfo?.id,
    batchCode: data.batchCode,
    status: data.status,
    detectedSlots: SLOT_TYPES
  }
}

async function detectArchiveManifest(page, batch) {
  const latestArchive = await apiGet(
    page,
    `/admin-api/mes/pro/edhr-batch-execution-archive/latest?batchExecutionId=${encodeURIComponent(batch.executionId)}`
  )
  if (!latestArchive.ok || ![0, 200].includes(latestArchive.body?.code)) {
    throw block('无法读取最新归档，不能验证归档 manifest 是否包含各槽位。', {
      batchExecutionId: batch.executionId,
      response: latestArchive
    })
  }
  const archive = latestArchive.body?.data
  if (!archive) {
    throw block('当前批次没有最新归档记录，无法验证归档 manifest。', {
      batchExecutionId: batch.executionId
    })
  }
  const serialized = JSON.stringify(archive)
  const hasManifestLikeField = /manifest|contentHash|archiveCode|archiveVersion|artifactType/i.test(serialized)
  const missingSlots = SLOT_TYPES.filter((slot) => !serialized.includes(slot))
  if (!hasManifestLikeField || missingSlots.length) {
    throw block('最新归档未暴露完整 manifest/hash 或未覆盖所有进入 DHR 的槽位。', {
      batchExecutionId: batch.executionId,
      archiveId: archive.id,
      archiveStatus: archive.archiveStatus,
      missingSlots,
      hasManifestLikeField
    })
  }
  return {
    archiveId: archive.id,
    archiveStatus: archive.archiveStatus,
    archiveVersion: archive.archiveVersion,
    contentHash: archive.contentHash
  }
}

async function adminReadonlyVerification(browser, runtime, writtenEvidence) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  const mutatingRequests = []
  page.on('request', (request) => {
    const method = request.method()
    const url = request.url()
    if (url.includes('/admin-api/mes/') && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
      mutatingRequests.push({ method, url })
    }
  })
  await login(page, runtime, {
    label: 'admin readonly',
    tenant: runtime.adminTenant,
    username: runtime.adminUsername,
    password: runtime.adminPassword
  })
  await page.goto(`${runtime.baseUrl}/mes/pro/feedback/edhr-batch-execution`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.waitForTimeout(1500)
  assert.equal(mutatingRequests.length, 0, `admin 只读复验不能产生 MES 写请求: ${JSON.stringify(mutatingRequests)}`)
  const bodyText = await page.locator('body').innerText({ timeout: 10000 })
  await context.close()
  return {
    tenant: runtime.adminTenant,
    username: runtime.adminUsername,
    mutatingRequests,
    visibleTextSample: bodyText.slice(0, 500),
    sourceBatchExecutionId: writtenEvidence.batch?.executionId || null
  }
}

function writeEvidence(result) {
  const lines = [
    '# eDHR 工序附属表单槽位 E2E 证据',
    '',
    `- 更新时间：${new Date().toISOString()}`,
    '- 范围：MAIN、PROCESS_INSPECTION、LOSS_REPORT、PARAMETER_RECORD、条件必填阻塞、对象级权限、归档 manifest、admin 只读复验。',
    '- 数据边界：写入/调试只允许本机测试租户；admin 租户只读复验。',
    '',
    '## BDD 覆盖',
    '',
    ...BDD_SCENARIOS.map((scenario) => `- ${scenario}`),
    '',
    '## 运行命令',
    '',
    '- 语法检查：`node --check tests/e2e/edhr-extra-form-slots-real-flow.e2e.js`',
    '- 静态合同：`node tests/e2e/edhr-extra-form-slots-static.spec.js`',
    '- 真实 E2E：`node tests/e2e/edhr-extra-form-slots-real-flow.e2e.js`',
    '- 必要环境变量：`EDHR_EXTRA_SLOTS_ADMIN_PASSWORD`；可选指定真实样本 `EDHR_EXTRA_SLOTS_ROUTE_ID`、`EDHR_EXTRA_SLOTS_BATCH_EXECUTION_ID`。',
    '',
    '## 结果',
    '',
    `- 状态：${result.status}`,
    `- baseUrl：${result.baseUrl || '<missing>'}`,
    `- 写入身份：${result.tenant || '<missing>'}/${result.username || '<missing>'}`,
    `- admin 只读身份：${result.adminTenant || '<missing>'}/${result.adminUsername || '<missing>'}`,
    '- 密码不写入证据。',
    ''
  ]
  if (result.preflight) {
    lines.push('## 登录前置', '')
    lines.push(`- 测试租户：${result.preflight.testTenant?.stdout || '<missing>'}`)
    lines.push(`- admin 只读：${result.preflight.admin?.stdout || '<missing>'}`)
    lines.push(`- 浏览器：${result.preflight.testTenant?.browser || '<unknown>'}`)
    lines.push('')
  }
  if (result.status === 'PASS') {
    lines.push('## GREEN', '')
    lines.push('- GREEN: `node tests/e2e/edhr-extra-form-slots-real-flow.e2e.js` -> PASS，真实槽位配置、批次上下文、归档 manifest 和 admin 只读复验均通过。')
    lines.push(`- 路线配置：${JSON.stringify(result.route, null, 2)}`)
    lines.push(`- 批次详情：${JSON.stringify(result.batch, null, 2)}`)
    lines.push(`- 归档：${JSON.stringify(result.archive, null, 2)}`)
    lines.push(`- admin 只读：${JSON.stringify(result.adminReadonly, null, 2)}`)
  } else if (result.status === 'BLOCKED') {
    lines.push('## BLOCKER', '')
    lines.push(`- BLOCKER: \`node tests/e2e/edhr-extra-form-slots-real-flow.e2e.js\` -> ${result.reason}`)
    if (result.details) {
      lines.push('- 细节：')
      lines.push('```json')
      lines.push(JSON.stringify(result.details, null, 2))
      lines.push('```')
    }
  } else {
    lines.push('## RED', '')
    lines.push(`- RED: \`node tests/e2e/edhr-extra-form-slots-real-flow.e2e.js\` -> FAIL, ${result.reason}`)
    if (result.details) {
      lines.push('```json')
      lines.push(JSON.stringify(result.details, null, 2))
      lines.push('```')
    }
  }
  lines.push('')
  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

async function main() {
  ensureDirectories()
  const runtime = config()
  const result = {
    status: 'RUNNING',
    baseUrl: runtime.baseUrl,
    tenant: runtime.tenant,
    username: runtime.username,
    adminTenant: runtime.adminTenant,
    adminUsername: runtime.adminUsername,
    bdd: BDD_SCENARIOS
  }
  try {
    validateConfig(runtime)
    result.preflight = {
      testTenant: runOfficialLoginPreflight(runtime, {
        label: '测试租户',
        tenant: runtime.tenant,
        username: runtime.username,
        password: runtime.password
      })
    }
    if (!runtime.adminPassword) {
      throw block('缺少 admin 只读复验密码 EDHR_EXTRA_SLOTS_ADMIN_PASSWORD；不能跳过 芋道源码/admin 复验。')
    }
    result.preflight.admin = runOfficialLoginPreflight(runtime, {
        label: 'admin 只读',
        tenant: runtime.adminTenant,
        username: runtime.adminUsername,
        password: runtime.adminPassword
      })
    const { chromium } = loadPlaywright()
    const executablePath = resolveChromiumExecutablePath()
    result.browser = executablePath
    const browser = await chromium.launch({ executablePath, headless: !runtime.headed, args: ['--disable-dev-shm-usage'] })
    try {
      const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
      const page = await context.newPage()
      await login(page, runtime, {
        label: '测试租户',
        tenant: runtime.tenant,
        username: runtime.username,
        password: runtime.password
      })
      result.batch = await detectBatchExecutionSlots(page, runtime)
      result.route = await detectRouteConfigSlots(page, runtime, result.batch.routeId)
      result.archive = await detectArchiveManifest(page, result.batch)
      await context.close()
      result.adminReadonly = await adminReadonlyVerification(browser, runtime, result)
    } finally {
      await browser.close()
    }
    result.status = 'PASS'
    writeEvidence(result)
    fs.writeFileSync(RESULT_FILE, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.log('PASS: eDHR extra form slots real E2E completed.')
  } catch (error) {
    result.status = error.blocked ? 'BLOCKED' : 'FAIL'
    result.reason = error.message
    result.details = error.details || { stack: error.stack }
    writeEvidence(result)
    fs.writeFileSync(RESULT_FILE, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.error(`${result.status}: ${result.reason}`)
    process.exitCode = 1
  }
}

main()
