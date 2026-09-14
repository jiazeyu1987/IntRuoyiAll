import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { chromium } from 'playwright'

const zh = {
  tenant: '测试租户',
  login: '登录',
  tenantPlaceholder: '请输入租户名称',
  usernamePlaceholder: '请输入用户名',
  passwordPlaceholder: '请输入密码',
  pageTitle: '审批中心',
  timeline: '流程',
  open: '查看',
  srmReviewTitle: '供应商门户审核台',
  srmDetailTitle: '门户申请详情'
}

const config = {
  baseUrl: (process.env.APPROVAL_CENTER_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  backendUrl: (process.env.APPROVAL_CENTER_E2E_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, ''),
  tenant: process.env.APPROVAL_CENTER_E2E_TENANT || zh.tenant,
  username: process.env.APPROVAL_CENTER_E2E_USERNAME || 'aoteman',
  password: process.env.APPROVAL_CENTER_E2E_PASSWORD || '111111',
  targetPath: '/approval-center',
  headed: process.env.APPROVAL_CENTER_E2E_HEADED === '1',
  taskDir:
    process.env.APPROVAL_CENTER_TASK_DIR ||
    'D:/ProjectPackage/Int/IntRuoyi/doc/tasks/20260624-unified-approval-platform-phase6/e2e-artifacts'
}

const screenshots = {
  loginFailed: path.join(config.taskDir, 'approval-center-phase6-srm-login-failed.png'),
  center: path.join(config.taskDir, 'approval-center-phase6-srm-list.png'),
  timeline: path.join(config.taskDir, 'approval-center-phase6-srm-timeline.png'),
  detail: path.join(config.taskDir, 'approval-center-phase6-srm-detail-jump.png')
}

const viewSearchOrder = [
  { label: '我发起的', value: 'MY_INITIATED' },
  { label: '已办', value: 'DONE' }
]

const viewRoutePath = {
  TODO: '/approval-center/todo',
  DONE: '/approval-center/done',
  MY_INITIATED: '/approval-center/my-initiated',
  CC: '/approval-center/cc'
}

function isSuccessPayload(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

function extractData(payload) {
  assert.ok(isSuccessPayload(payload), `api payload failed: ${JSON.stringify(payload)}`)
  return payload.data
}

function supportedViewsFor(modules, moduleCode, fallback) {
  const descriptor = modules.find((item) => item.moduleCode === moduleCode)
  const supported = Array.isArray(descriptor?.supportedViewTypes) ? descriptor.supportedViewTypes : []
  return fallback.filter((view) => supported.includes(view.value))
}

function waitForJsonResponse(page, predicate, options = {}) {
  const timeout = options.timeout ?? 60000
  return new Promise((resolve, reject) => {
    let timer = null
    let lastJsonError = null
    const cleanup = () => {
      if (timer) {
        clearTimeout(timer)
      }
      page.off('response', onResponse)
      page.off('close', onClose)
    }
    const onClose = () => {
      cleanup()
      reject(new Error('page_closed_before_response'))
    }
    const onResponse = async (response) => {
      let matched = false
      try {
        matched = predicate(response)
      } catch (error) {
        cleanup()
        reject(error)
        return
      }
      if (!matched) {
        return
      }
      try {
        const payload = await response.json()
        cleanup()
        resolve({ response, payload })
      } catch (error) {
        lastJsonError = error
      }
    }
    timer = setTimeout(() => {
      cleanup()
      reject(new Error(lastJsonError ? `response_json_unavailable:${lastJsonError.message}` : 'response_timeout'))
    }, timeout)
    page.on('response', onResponse)
    page.on('close', onClose)
  })
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill('')
      await item.fill(value)
      return
    }
  }
  throw new Error(`visible_input_missing:${label}`)
}

async function selectTenant(page, loginForm) {
  const tenantInput = loginForm
    .locator(`.el-select input[role="combobox"], input.el-select__input, input[placeholder="${zh.tenantPlaceholder}"]`)
    .first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  const tenantResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/system/tenant/get-id-by-name') &&
        response.url().includes(encodeURIComponent(config.tenant)) &&
        response.ok(),
      { timeout: 30000 }
    )
    .catch(() => null)
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
  await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
  await tenantOption.click()
  await tenantResponsePromise
}

async function getLoginProbe(page) {
  return page.evaluate(() => ({
    url: window.location.href,
    inputs: Array.from(document.querySelectorAll('input')).map((input) => ({
      type: input.getAttribute('type'),
      placeholder: input.getAttribute('placeholder'),
      value: input.value,
      visible: Boolean(input.offsetParent),
      disabled: input.disabled
    })),
    buttons: Array.from(document.querySelectorAll('button')).map((button) => ({
      text: (button.textContent || '').replace(/\s+/g, ' ').trim(),
      disabled: button.disabled,
      visible: Boolean(button.offsetParent)
    })),
    body: (document.body.innerText || '').slice(0, 1000)
  }))
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(config.targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(config.targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page, loginForm)

  const textboxes = loginForm.getByRole('textbox')
  const textboxCount = await textboxes.count()
  if (textboxCount >= 2) {
    const usernameInput = textboxes.nth(textboxCount >= 3 ? 1 : 0)
    await usernameInput.fill('')
    await usernameInput.fill(config.username)
  } else {
    await fillFirstVisible(loginForm.locator(`input[placeholder="${zh.usernamePlaceholder}"]`), config.username, 'username')
  }
  await fillFirstVisible(
    loginForm.locator(`input[type="password"], input[placeholder="${zh.passwordPlaceholder}"]`),
    config.password,
    'password'
  )

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const loginButton = loginForm.getByRole('button', { name: zh.login }).first()
  await loginButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await loginButton.isEnabled(), true, 'login button must be enabled')
  await loginButton.click()

  let loginResponse
  try {
    loginResponse = await loginResponsePromise
  } catch (error) {
    await page.screenshot({ path: screenshots.loginFailed, fullPage: true }).catch(() => null)
    const probe = await getLoginProbe(page).catch((probeError) => ({ probeError: probeError.message }))
    throw new Error(`login_response_timeout:${error.message}; probe=${JSON.stringify(probe)}; screenshot=${screenshots.loginFailed}`)
  }
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(isSuccessPayload(loginPayload), `login failed: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openApprovalCenter(page) {
  const modulesPromise = waitForJsonResponse(
    page,
    (response) => response.url().includes('/admin-api/approval-center/modules') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByRole('heading', { name: zh.pageTitle }).waitFor({ state: 'visible', timeout: 60000 })
  const modulesResponse = await modulesPromise
  assert.ok(
    new URL(modulesResponse.response.url()).origin === new URL(config.baseUrl).origin && new URL(modulesResponse.response.url()).pathname === '/admin-api/approval-center/modules',
    `modules request did not hit target backend: ${modulesResponse.response.url()}`
  )
  const modules = extractData(modulesResponse.payload)
  await settle(page, 30000)
  return modules
}

async function loadSrmTaskView(page, view) {
  const tasksPromise = waitForJsonResponse(
    page,
    (response) => {
      if (!response.url().includes('/admin-api/approval-center/tasks/page') || response.request().method() !== 'GET') {
        return false
      }
      const url = new URL(response.url())
      return url.searchParams.get('moduleCode') === 'SRM' && url.searchParams.get('viewType') === view.value
    },
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}${viewRoutePath[view.value]}?moduleCode=SRM`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByRole('heading', { name: zh.pageTitle }).waitFor({ state: 'visible', timeout: 60000 })
  const tasksResponse = await tasksPromise
  assert.ok(
    new URL(tasksResponse.response.url()).origin === new URL(config.baseUrl).origin && new URL(tasksResponse.response.url()).pathname === '/admin-api/approval-center/tasks/page',
    `SRM tasks request did not hit target backend: ${tasksResponse.response.url()}`
  )
  await settle(page, 30000)
  return extractData(tasksResponse.payload)
}

function samplePage(pageData, viewType) {
  const list = Array.isArray(pageData?.list) ? pageData.list : []
  return {
    viewType,
    total: pageData?.total,
    modules: [...new Set(list.map((item) => item.moduleCode))],
    sample: list.slice(0, 8).map((item) => ({
      moduleCode: item.moduleCode,
      sourceTaskType: item.sourceTaskType,
      businessTitle: item.businessTitle,
      detailRoute: item.detailRoute,
      detailQuery: item.detailQuery,
      capabilities: item.capabilities
    }))
  }
}

function selectSrmTask(pageData, viewType) {
  const list = Array.isArray(pageData?.list) ? pageData.list : []
  const rowIndex = list.findIndex((item) => item.moduleCode === 'SRM')
  if (rowIndex < 0) {
    return null
  }
  const task = list[rowIndex]
  const errors = []
  if (task.detailRoute !== '/srm/supplier-portal-review') {
    errors.push(`detailRoute=${task.detailRoute}`)
  }
  if (!task.detailQuery?.applicationId) {
    errors.push('detailQuery.applicationId missing')
  }
  if (!Array.isArray(task.capabilities) || !task.capabilities.includes('TIMELINE') || !task.capabilities.includes('AUDIT')) {
    errors.push(`capabilities=${JSON.stringify(task.capabilities)}`)
  }
  if (errors.length > 0) {
    throw new Error(
      `srm_unified_task_contract_incomplete:${JSON.stringify({
        viewType,
        errors,
        task: {
          moduleCode: task.moduleCode,
          sourceTaskType: task.sourceTaskType,
          businessTitle: task.businessTitle,
          detailRoute: task.detailRoute,
          detailQuery: task.detailQuery,
          capabilities: task.capabilities
        }
      })}`
    )
  }
  return { rowIndex, task, viewType }
}

async function findSrmTask(page, supportedSearchViews) {
  const samples = []
  for (const view of supportedSearchViews) {
    const pageData = await loadSrmTaskView(page, view)
    const result = selectSrmTask(pageData, view.value)
    if (result) {
      return result
    }
    samples.push(samplePage(pageData, view.value))
  }
  throw new Error(`srm_real_approved_task_missing_in_unified_center:${JSON.stringify(samples)}`)
}

async function openTimeline(page, rowIndex) {
  const rows = page.locator('.el-table__body-wrapper tbody tr')
  await rows.nth(rowIndex).waitFor({ state: 'visible', timeout: 30000 })
  const timelineResponsePromise = waitForJsonResponse(
    page,
    (response) => response.url().includes('/admin-api/approval-center/tasks/timeline') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await rows.nth(rowIndex).getByRole('button', { name: zh.timeline }).click()
  const timelineResponse = await timelineResponsePromise
  assert.ok(
    new URL(timelineResponse.response.url()).origin === new URL(config.baseUrl).origin && new URL(timelineResponse.response.url()).pathname === '/admin-api/approval-center/tasks/timeline',
    `timeline request did not hit target backend: ${timelineResponse.response.url()}`
  )
  const timelineRows = extractData(timelineResponse.payload)
  assert.ok(Array.isArray(timelineRows) && timelineRows.length > 0, 'SRM timeline rows must not be empty')
  assert.equal(timelineRows.every((entry) => entry.moduleCode === 'SRM'), true, 'timeline must belong to SRM')
  assert.equal(timelineRows.every((entry) => entry.evidenceType), true, 'timeline must expose evidence types')
  await page.locator('.approval-center__timeline-drawer').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('.approval-center__timeline-node').first().waitFor({ state: 'visible', timeout: 30000 })
  await page.screenshot({ path: screenshots.timeline, fullPage: true })
  return timelineRows
}

async function jumpToSrmOfficialPage(page, rowIndex, task) {
  await page.keyboard.press('Escape').catch(() => null)
  await settle(page, 5000)
  const rows = page.locator('.el-table__body-wrapper tbody tr')
  const applicationId = String(task.detailQuery.applicationId)
  const pageResponsePromise = waitForJsonResponse(
    page,
    (response) => {
      if (!response.url().includes('/admin-api/srm/supplier-portal/page') || response.request().method() !== 'GET') {
        return false
      }
      const url = new URL(response.url())
      return url.searchParams.get('id') === applicationId
    },
    { timeout: 60000 }
  )
  await rows.nth(rowIndex).getByRole('button', { name: zh.open }).click()
  await page.waitForURL((url) => url.pathname === '/srm/supplier-portal-review', { timeout: 60000 })
  const pageResponse = await pageResponsePromise
  assert.ok(
    new URL(pageResponse.response.url()).origin === new URL(config.baseUrl).origin && new URL(pageResponse.response.url()).pathname === '/admin-api/srm/supplier-portal/page',
    `SRM page request did not hit target backend: ${pageResponse.response.url()}`
  )
  const pageData = extractData(pageResponse.payload)
  const applications = Array.isArray(pageData?.list) ? pageData.list : []
  const routedApplication = applications.find((item) => String(item.id) === applicationId)
  assert.ok(routedApplication, `SRM formal page response missing routed application ${applicationId}`)
  await page.getByRole('heading', { name: zh.srmReviewTitle }).waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText(zh.srmDetailTitle).waitFor({ state: 'visible', timeout: 60000 })
  await settle(page, 30000)
  await page.screenshot({ path: screenshots.detail, fullPage: true })
  const url = new URL(page.url())
  assert.equal(url.pathname, '/srm/supplier-portal-review', `SRM formal route mismatch: ${page.url()}`)
  assert.equal(url.searchParams.get('applicationId'), applicationId, `SRM route must preserve applicationId: ${page.url()}`)
  const bodyText = await page.locator('body').innerText({ timeout: 10000 }).catch(() => '')
  assert.match(bodyText, new RegExp(routedApplication.companyName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), 'SRM page must show routed application')
  assert.doesNotMatch(bodyText, /BPM 通用审批|bpmApprove/i, 'SRM official page must not expose BPM generic approval action')
  return routedApplication
}

async function main() {
  if (config.tenant !== zh.tenant || config.username !== 'aoteman') {
    throw new Error(`approval_center_e2e_must_use_test_tenant_aoteman:${JSON.stringify(config)}`)
  }
  fs.mkdirSync(config.taskDir, { recursive: true })
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page)
    const modules = await openApprovalCenter(page)
    assert.ok(modules.some((item) => item.moduleCode === 'SRM'), 'modules descriptor must include SRM')
    const supportedSearchViews = supportedViewsFor(modules, 'SRM', viewSearchOrder)
    assert.ok(supportedSearchViews.length > 0, `SRM supported views missing from modules: ${JSON.stringify(modules)}`)
    const { rowIndex, task, viewType } = await findSrmTask(page, supportedSearchViews)
    await page.screenshot({ path: screenshots.center, fullPage: true })
    const timelineRows = await openTimeline(page, rowIndex)
    const routedApplication = await jumpToSrmOfficialPage(page, rowIndex, task)
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' || ')}`)
    const result = {
      baseUrl: config.baseUrl,
      backendUrl: config.backendUrl,
      tenant: config.tenant,
      username: config.username,
      modules: modules.map((item) => item.moduleCode),
      viewType,
      task: {
        moduleCode: task.moduleCode,
        sourceTaskType: task.sourceTaskType,
        sourceTaskId: task.sourceTaskId,
        businessTitle: task.businessTitle,
        detailRoute: task.detailRoute,
        detailQuery: task.detailQuery
      },
      routedApplication: {
        id: routedApplication.id,
        companyName: routedApplication.companyName,
        applicationStatus: routedApplication.applicationStatus
      },
      timelineCount: timelineRows.length,
      timelineEvidenceTypes: [...new Set(timelineRows.map((entry) => entry.evidenceType))],
      screenshots
    }
    fs.writeFileSync(path.join(config.taskDir, 'approval-center-phase6-srm-real-e2e.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    process.stdout.write(`approval-center phase6 SRM real e2e passed\n${JSON.stringify(result, null, 2)}\n`)
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(1)
})
