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
  timeline: '轨迹',
  open: '打开',
  errorTexts: [
    'Set of process instance ids is empty',
    'APPROVAL_BUSINESS_OBJECT_REQUIRED: DCC controlled file summary snapshot not found'
  ]
}

const config = {
  baseUrl: (process.env.APPROVAL_CENTER_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  backendUrl: (process.env.APPROVAL_CENTER_E2E_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, ''),
  tenant: process.env.APPROVAL_CENTER_E2E_TENANT || zh.tenant,
  username: process.env.APPROVAL_CENTER_E2E_USERNAME || 'aoteman',
  password: process.env.APPROVAL_CENTER_E2E_PASSWORD || '111111',
  targetPath: '/approval-center/todo',
  headed: process.env.APPROVAL_CENTER_E2E_HEADED === '1',
  taskDir:
    process.env.APPROVAL_CENTER_TASK_DIR ||
    'D:/ProjectPackage/Int/IntRuoyi/doc/tasks/20260623-unified-approval-platform-phase2/e2e-artifacts'
}

const screenshots = {
  loginFailed: path.join(config.taskDir, 'approval-center-login-failed.png'),
  center: path.join(config.taskDir, 'approval-center-list.png'),
  timeline: path.join(config.taskDir, 'approval-center-timeline.png'),
  detail: path.join(config.taskDir, 'approval-center-detail-jump.png')
}

function isSuccessPayload(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

function extractData(payload) {
  assert.ok(isSuccessPayload(payload), `api payload failed: ${JSON.stringify(payload)}`)
  return payload.data
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

async function waitForApprovalPage(page) {
  const modulesPromise = waitForJsonResponse(
    page,
    (response) => response.url().includes('/admin-api/approval-center/modules') && response.request().method() === 'GET',
    { timeout: 60000 }
  ).catch((error) => ({ error }))
  const tasksPromise = waitForJsonResponse(
    page,
    (response) => response.url().includes('/admin-api/approval-center/tasks/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  ).catch((error) => ({ error }))
  try {
    await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByRole('heading', { name: zh.pageTitle }).waitFor({ state: 'visible', timeout: 60000 })
  } catch (error) {
    await page.screenshot({ path: screenshots.center, fullPage: true }).catch(() => null)
    const bodyText = await page.locator('body').innerText({ timeout: 3000 }).catch(() => '')
    throw new Error(
      `approval_center_page_open_failed:${error.message}; url=${page.url()}; body=${bodyText.slice(0, 1000)}; screenshot=${screenshots.center}`
    )
  }
  const [modulesResponse, tasksResponse] = await Promise.all([modulesPromise, tasksPromise])
  if (modulesResponse?.error) {
    throw new Error(`approval_center_modules_response_missing:${modulesResponse.error.message}`)
  }
  if (tasksResponse?.error) {
    throw new Error(`approval_center_tasks_response_missing:${tasksResponse.error.message}`)
  }
  assert.ok(
    new URL(modulesResponse.response.url()).origin === new URL(config.baseUrl).origin && new URL(modulesResponse.response.url()).pathname === '/admin-api/approval-center/modules',
    `modules request did not hit target backend: ${modulesResponse.response.url()}`
  )
  assert.ok(
    new URL(tasksResponse.response.url()).origin === new URL(config.baseUrl).origin && new URL(tasksResponse.response.url()).pathname === '/admin-api/approval-center/tasks/page',
    `tasks request did not hit target backend: ${tasksResponse.response.url()}`
  )
  const modules = extractData(modulesResponse.payload)
  const pageData = extractData(tasksResponse.payload)
  await settle(page, 30000)
  const bodyText = await page.locator('body').innerText({ timeout: 10000 }).catch(() => '')
  const matchedErrors = zh.errorTexts.filter((text) => bodyText.includes(text))
  assert.deepEqual(matchedErrors, [], `approval center still shows backend error text: ${matchedErrors.join(' || ')}`)
  return { modules, pageData }
}

function selectTaskForRealPath(pageData) {
  const list = Array.isArray(pageData?.list) ? pageData.list : []
  assert.ok(list.length > 0, `approval_center_task_list_empty:${JSON.stringify({ total: pageData?.total })}`)
  const rowIndex = list.findIndex(
    (item) => !item.businessDeleted && item.detailRoute && Array.isArray(item.capabilities) && item.capabilities.includes('TIMELINE')
  )
  if (rowIndex < 0) {
    throw new Error(
      `approval_center_real_timeline_task_missing:${JSON.stringify({
        total: pageData?.total,
        sample: list.slice(0, 8).map((item) => ({
          moduleCode: item.moduleCode,
          businessTitle: item.businessTitle,
          detailRoute: item.detailRoute,
          businessDeleted: item.businessDeleted,
          capabilities: item.capabilities
        }))
      })}`
    )
  }
  return { rowIndex, task: list[rowIndex] }
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
  assert.ok(Array.isArray(timelineRows) && timelineRows.length > 0, 'timeline rows must not be empty')
  await page.locator('.approval-center__timeline-drawer').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('.approval-center__timeline-node').first().waitFor({ state: 'visible', timeout: 30000 })
  await page.screenshot({ path: screenshots.timeline, fullPage: true })
  return timelineRows
}

async function jumpToModuleDetail(page, rowIndex, task) {
  await page.keyboard.press('Escape').catch(() => null)
  await settle(page, 5000)
  const rows = page.locator('.el-table__body-wrapper tbody tr')
  await rows.nth(rowIndex).getByRole('button', { name: zh.open }).click()
  await page.waitForURL((url) => url.pathname !== config.targetPath, { timeout: 60000 })
  await settle(page, 30000)
  await page.screenshot({ path: screenshots.detail, fullPage: true })
  const bodyText = await page.locator('body').innerText({ timeout: 10000 }).catch(() => '')
  if (/404|Not Found/i.test(bodyText)) {
    throw new Error(`module_detail_jump_not_found:${JSON.stringify({ url: page.url(), task, body: bodyText.slice(0, 1000) })}`)
  }
  assert.ok(page.url().includes(task.detailRoute), `module detail route mismatch: expected ${task.detailRoute}, actual ${page.url()}`)
}

async function assertCurrentViewNavigationVisible(page) {
  const body = page.locator('body')
  for (const viewName of ['待办', '已办', '我发起的', '抄送我的']) {
    await body.getByText(viewName, { exact: true }).first().waitFor({ state: 'visible', timeout: 30000 })
  }
  await assert.rejects(
    () => body.getByText('签名待处理', { exact: true }).first().waitFor({ state: 'visible', timeout: 1000 }),
    /Timeout|waiting for/i
  )
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
    const { modules, pageData } = await waitForApprovalPage(page)
    await assertCurrentViewNavigationVisible(page)
    await page.screenshot({ path: screenshots.center, fullPage: true })
    const { rowIndex, task } = selectTaskForRealPath(pageData)
    const timelineRows = await openTimeline(page, rowIndex)
    await jumpToModuleDetail(page, rowIndex, task)
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' || ')}`)
    const result = {
      baseUrl: config.baseUrl,
      backendUrl: config.backendUrl,
      tenant: config.tenant,
      username: config.username,
      modules: modules.map((item) => item.moduleCode),
      task: {
        moduleCode: task.moduleCode,
        sourceTaskType: task.sourceTaskType,
        businessTitle: task.businessTitle,
        detailRoute: task.detailRoute
      },
      timelineCount: timelineRows.length,
      screenshots
    }
    process.stdout.write(`approval-center phase2 real e2e passed\n${JSON.stringify(result, null, 2)}\n`)
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(1)
})
