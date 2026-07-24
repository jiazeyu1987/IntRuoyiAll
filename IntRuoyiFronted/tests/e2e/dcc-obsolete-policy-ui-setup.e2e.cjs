const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_OBSOLETE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_OBSOLETE_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_OBSOLETE_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_OBSOLETE_E2E_PASSWORD || '111111'
const TARGET_APPROVAL_MODE = (process.env.DCC_OBSOLETE_E2E_TARGET_APPROVAL_MODE || '').trim()
const TASK_DIR = process.env.DCC_OBSOLETE_E2E_TASK_DIR
  ? path.resolve(process.env.DCC_OBSOLETE_E2E_TASK_DIR)
  : path.resolve(__dirname, '../../../doc/tasks/20260720-form-center-controlled-state-machine-implementation/e2e-artifacts')
const RESULT_PATH = path.join(TASK_DIR, 'dcc-obsolete-policy-ui-setup.json')

function writeResult(result) {
  fs.mkdirSync(TASK_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, JSON.stringify(result, null, 2) + '\n', 'utf8')
}

async function login(page) {
  await page.goto(BASE_URL + '/login?redirect=/index', { waitUntil: 'commit', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  await tenantInput.fill(TENANT)
  const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
  await tenantOption.waitFor({ state: 'visible', timeout: 60000 })
  await tenantOption.click()
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, 'login HTTP status ' + loginResponse.status())
  assert.ok([0, 200].includes(loginPayload.code), 'login business code ' + loginPayload.code)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
}

async function responseData(response, label) {
  const payload = await response.json()
  assert.ok([0, 200].includes(payload.code), label + ' business code ' + payload.code + ': ' + (payload.msg || ''))
  return payload.data
}

async function fillByLabel(dialog, label, value) {
  const item = dialog.locator('.el-form-item').filter({ hasText: label }).first()
  await item.locator('input').first().fill(value)
}

async function main() {
  assert.equal(TENANT, process.env.DCC_OBSOLETE_E2E_EXPECT_TENANT || '测试租户')
  assert.equal(USERNAME, process.env.DCC_OBSOLETE_E2E_EXPECT_USERNAME || 'aoteman')
  assert.ok(
    TARGET_APPROVAL_MODE === '' || ['BPM_REQUIRED', 'DIRECT'].includes(TARGET_APPROVAL_MODE),
    'DCC_OBSOLETE_E2E_TARGET_APPROVAL_MODE must be BPM_REQUIRED, DIRECT or empty'
  )
  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
    args: ['--disable-dev-shm-usage']
  })
  const pageErrors = []
  const writeRequests = []
  const observedResponses = []
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('pageerror', (error) => pageErrors.push(error.message))
    page.on('request', (request) => {
      if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method()) && request.url().includes('/admin-api/')) {
        writeRequests.push({ method: request.method(), url: request.url() })
      }
    })
    page.on('response', (response) => {
      const url = response.url()
      if (url.includes('/form-center/')) {
        observedResponses.push({ method: response.request().method(), status: response.status(), url })
      }
    })
    await login(page)

    const initialPageResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/form-center/policies') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(BASE_URL + '/mdm/form-center/policy', { waitUntil: 'commit' })
    await responseData(await initialPageResponsePromise, 'policy initial page')

    const queryInput = page.locator('.form-center-policy__toolbar input[placeholder="动作编码"]').first()
    await queryInput.fill('OBSOLETE')
    const queryResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/form-center/policies') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.getByRole('button', { name: /查询/ }).click()
    const existingPage = await responseData(await queryResponsePromise, 'policy query')
    const existingList = existingPage.list || []
    const existingPublished = existingList.find((item) =>
      item.dataDomain === 'DCC' &&
      item.systemCode === 'DCC' &&
      item.objectType === 'CONTROLLED_FILE' &&
      item.actionCode === 'OBSOLETE' &&
      item.objectState === 'ACTIVE' &&
      item.status === 'PUBLISHED'
    )
    let policyId = existingPublished && existingPublished.id
    let setupMode = existingPublished ? 'EXISTING_PUBLISHED' : 'CREATED_AND_PUBLISHED'

    if (!policyId) {
      const templateResponsePromise = page.waitForResponse(
        (response) => response.url().includes('/form-center/template-pool') && response.request().method() === 'GET',
        { timeout: 60000 }
      )
      await page.getByRole('button', { name: /新增/ }).click()
      const templatesPage = await responseData(await templateResponsePromise, 'published templates')
      const publishedTemplates = templatesPage.list || []
      assert.ok(publishedTemplates.length > 0, 'BLOCKER: no PUBLISHED form-center template available for DCC obsolete policy')

      const dialog = page.locator('.el-dialog:visible').filter({ hasText: '表单策略' }).first()
      await dialog.waitFor({ state: 'visible', timeout: 60000 })
      await fillByLabel(dialog, '动作编码', 'OBSOLETE')
      await fillByLabel(dialog, '对象状态', 'ACTIVE')
      await fillByLabel(dialog, '执行器编码', 'DCC_OBSOLETE')
      await fillByLabel(dialog, '槽位编码', 'OBSOLETE_FORM')
      await fillByLabel(dialog, '备注', 'E2E DCC obsolete form-center policy')

      const saveResponsePromise = page.waitForResponse(
        (response) => response.url().includes('/form-center/policies') && response.request().method() === 'POST',
        { timeout: 60000 }
      )
      await dialog.getByRole('button', { name: '保存' }).click()
      const saved = await responseData(await saveResponsePromise, 'save policy')
      policyId = saved.id
      assert.ok(policyId, 'saved policy id is missing')

      const publishResponsePromise = page.waitForResponse(
        (response) => response.url().includes('/form-center/policies/' + policyId + '/publish') && response.request().method() === 'POST',
        { timeout: 60000 }
      )
      await page.locator('tr', { hasText: 'OBSOLETE' }).filter({ hasText: 'ACTIVE' }).first().getByRole('button', { name: '发布' }).click()
      const published = await responseData(await publishResponsePromise, 'publish policy')
      assert.equal(published, true, 'publish policy should return true')
    }

    const verifyQueryResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/form-center/policies') && response.url().includes('actionCode=OBSOLETE') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await queryInput.fill('OBSOLETE')
    await page.getByRole('button', { name: /查询/ }).click()
    const verifyPage = await responseData(await verifyQueryResponsePromise, 'verify obsolete policy query')
    const verifyList = verifyPage.list || []
    const verifyPublished = verifyList.find((item) =>
      item.dataDomain === 'DCC' &&
      item.systemCode === 'DCC' &&
      item.objectType === 'CONTROLLED_FILE' &&
      item.actionCode === 'OBSOLETE' &&
      item.objectState === 'ACTIVE' &&
      item.status === 'PUBLISHED'
    )
    assert.ok(verifyPublished, 'DCC obsolete ACTIVE policy must be published after setup')

    let finalPolicy = verifyPublished
    if (TARGET_APPROVAL_MODE && verifyPublished.approvalMode !== TARGET_APPROVAL_MODE) {
      if (TARGET_APPROVAL_MODE === 'BPM_REQUIRED') {
        assert.ok(
          verifyPublished.bpmProcessKey && String(verifyPublished.bpmProcessKey).trim(),
          'DCC obsolete policy must keep bpmProcessKey before switching back to BPM_REQUIRED'
        )
      }
      const row = page.locator('tr', { hasText: 'OBSOLETE' }).filter({ hasText: 'ACTIVE' }).first()
      await row.waitFor({ state: 'visible', timeout: 60000 })
      const switchResponsePromise = page.waitForResponse(
        (response) =>
          response.url().includes('/form-center/policies/' + verifyPublished.id + '/switch-approval-mode') &&
          response.request().method() === 'POST',
        { timeout: 60000 }
      )
      await row.locator('.el-switch').first().click()
      const confirmBox = page.locator('.el-message-box:visible').first()
      await confirmBox.waitFor({ state: 'visible', timeout: 60000 })
      await confirmBox.getByRole('button', { name: /确\s*定/ }).click()
      finalPolicy = await responseData(await switchResponsePromise, 'switch approval mode')
      assert.equal(
        finalPolicy.approvalMode,
        TARGET_APPROVAL_MODE,
        'DCC obsolete policy switch must return target approval mode'
      )
      await page
        .getByText(TARGET_APPROVAL_MODE === 'BPM_REQUIRED' ? '审批已开启' : '审批已关闭')
        .first()
        .waitFor({ state: 'visible', timeout: 30000 })
        .catch(() => null)
    }

    assert.deepEqual(pageErrors, [], 'policy setup page errors must be empty')

    writeResult({
      status: 'PASS',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      setupMode,
      targetApprovalMode: TARGET_APPROVAL_MODE || null,
      policyId: finalPolicy.id || verifyPublished.id || policyId,
      publishedPolicy: finalPolicy,
      writeRequests,
      observedResponses,
      pageErrors
    })
    console.log(
      'GREEN: dcc-obsolete-policy-ui-setup -> PASS, policyId=' +
        policyId +
        ', mode=' +
        setupMode +
        ', approvalMode=' +
        finalPolicy.approvalMode +
        ', artifact=' +
        RESULT_PATH
    )
  } catch (error) {
    writeResult({
      status: 'FAIL',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      error: error.stack || error.message,
      writeRequests,
      observedResponses,
      pageErrors
    })
    console.error(error.stack || error.message)
    process.exit(1)
  } finally {
    await browser.close()
  }
}

main()
