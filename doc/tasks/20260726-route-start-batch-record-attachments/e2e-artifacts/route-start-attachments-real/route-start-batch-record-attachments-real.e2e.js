const assert = require('node:assert/strict')
const fs = require('node:fs')
const { createRequire } = require('node:module')
const path = require('node:path')

const frontendRequire = createRequire(
  'D:/IntRuoyiWorktree/route-start-batch-record-attachments-e2e/IntRuoyiFronted/package.json'
)
const { chromium } = frontendRequire('playwright')

const config = {
  baseUrl: (process.env.MES_ROUTE_START_ATTACHMENT_E2E_BASE_URL || 'http://127.0.0.1:8087').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_START_ATTACHMENT_E2E_TENANT || '测试租户',
  username: process.env.MES_ROUTE_START_ATTACHMENT_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_START_ATTACHMENT_E2E_PASSWORD || '',
  routeCode: process.env.MES_ROUTE_START_ATTACHMENT_E2E_ROUTE_CODE || 'RT000017',
  headed: process.env.MES_ROUTE_START_ATTACHMENT_E2E_HEADED === '1',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_START_ATTACHMENT_E2E_ARTIFACT_DIR ||
      path.join(__dirname, 'result')
  )
}

const expectedOwners = [
  ['INCOMING_INSPECTION_REPORT', '来料检报告', '来料检报告上传1'],
  ['STERILIZATION_REPORT', '灭菌报告', '灭菌报告上传1'],
  ['FINISHED_PRODUCT_INSPECTION_REPORT', '成品检报告', '成品检报告上传1'],
  ['FINISHED_PRODUCT_INSPECTION_RECORD', '成品检记录', '成品检记录上传1']
]

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertPreconditions() {
  const parsed = new URL(config.baseUrl)
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(parsed.hostname),
    `E2E must stay local, got ${config.baseUrl}`
  )
  assert.equal(config.tenant, '测试租户', `write E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `write E2E must use aoteman, got ${config.username}`)
  assert.ok(config.password, 'Missing MES_ROUTE_START_ATTACHMENT_E2E_PASSWORD')
  assert.ok(fs.existsSync(executablePath), `Chrome not found: ${executablePath}`)
}

function writeJson(name, payload) {
  fs.mkdirSync(config.artifactDir, { recursive: true })
  const filePath = path.join(config.artifactDir, name)
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return filePath
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(700)
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await form.locator('input[placeholder="请输入租户名称"]').first().fill(config.tenant)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/route')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  await selectTenant(page, form)
  const accountInput = form.locator('input.el-input__inner:not([role="combobox"]):visible').first()
  await accountInput.fill('')
  await accountInput.fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(
    loginResponse.ok() && [0, 200].includes(loginPayload.code),
    `login failed: HTTP ${loginResponse.status()} ${loginPayload.msg || loginPayload.code}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function openRouteGraph(page) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)

  const codeInput = page
    .locator('input[placeholder="请输入路线编码"], input[placeholder="请输入工艺路线编码"]')
    .first()
  await codeInput.fill(config.routeCode)
  await page.getByRole('button', { name: /查询|搜索/ }).first().click()
  await settle(page)

  const row = page.locator('.el-table__body-wrapper .el-table__row').filter({ hasText: config.routeCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '编辑' }).first().click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit'), { timeout: 60000 })

  const editor = page.locator('.route-edit-page').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  await editor.getByRole('tab', { name: '流转关系图' }).click()
  await editor.locator('.route-flow-graph-designer').waitFor({ state: 'visible', timeout: 60000 })
  await editor.locator('[data-flow-node="route-boundary"][data-flow-boundary="START"]').waitFor({
    state: 'visible',
    timeout: 60000
  })
  await settle(page)
  return editor
}

function assertOwnerPayload(data, source) {
  assert.equal(data.length, 4, `${source} must return 4 batch record attachment owners`)
  for (const [code, name, roleName] of expectedOwners) {
    const owner = data.find((item) => item.attachmentCode === code)
    assert.ok(owner, `${source} missing ${code}`)
    assert.equal(owner.attachmentName, name, `${source} ${code} attachment name mismatch`)
    assert.equal(owner.defaultRoleName, roleName, `${source} ${code} role name mismatch`)
    assert.equal(owner.candidateSourceType, 'ROLE', `${source} ${code} must be ROLE-based`)
    assert.ok(Array.isArray(owner.candidateSourceIds), `${source} ${code} candidateSourceIds missing`)
    if (source === 'init') {
      assert.ok(owner.candidateSourceIds.length >= 1, `${source} ${code} must include role candidate id`)
      assert.ok(
        owner.assignedUserIds.length >= 2 && owner.assignedUserIds.length <= 4,
        `${source} ${code} must assign 2-4 enabled tenant users`
      )
      assert.ok(
        owner.assignedUserNames.length >= 2 && owner.assignedUserNames.length <= 4,
        `${source} ${code} must display 2-4 assigned user names`
      )
    }
  }
}

async function main() {
  assertPreconditions()
  fs.mkdirSync(config.artifactDir, { recursive: true })
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const pageErrors = []
  const consoleErrors = []
  const targetResponses = []
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('pageerror', (error) => pageErrors.push(error.message))
    page.on('console', (message) => {
      if (message.type() === 'error') consoleErrors.push(message.text())
    })
    page.on('response', (response) => {
      if (response.url().includes('/mes/pro/route/flow-config/batch-record-attachment-owners')) {
        targetResponses.push({
          method: response.request().method(),
          url: response.url().replace(config.baseUrl, ''),
          status: response.status()
        })
      }
    })

    await login(page)
    const editor = await openRouteGraph(page)

    const getResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/route/flow-config/batch-record-attachment-owners') &&
        !response.url().includes('/init-defaults') &&
        !response.url().includes('/save') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await editor.locator('[data-flow-node="route-boundary"][data-flow-boundary="START"]').click()
    const getResponse = await getResponsePromise
    const getPayload = await getResponse.json()
    assert.equal(getPayload.code, 0, `GET owners failed: ${getPayload.msg || getPayload.code}`)
    assertOwnerPayload(getPayload.data, 'get')

    const panel = editor.locator('[data-flow-panel="batch-record-attachment-owner-detail"]').first()
    await panel.waitFor({ state: 'visible', timeout: 60000 })
    for (const [, name, roleName] of expectedOwners) {
      await panel.getByText(name, { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
      await panel.getByText(roleName, { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
    }
    assert.equal(await page.getByText('请求地址不存在', { exact: false }).count(), 0, 'page must not show route missing error')

    const initButton = panel.locator('[data-flow-action="init-batch-record-attachment-owners"]').first()
    await initButton.waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(await initButton.isEnabled(), true, 'init default role button must be enabled')
    const initResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/route/flow-config/batch-record-attachment-owners/init-defaults') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await initButton.click()
    const initResponse = await initResponsePromise
    const initPayload = await initResponse.json()
    assert.equal(initPayload.code, 0, `init owners failed: ${initPayload.msg || initPayload.code}`)
    assertOwnerPayload(initPayload.data, 'init')
    await page.getByText('批记录附件默认角色已初始化', { exact: false }).first().waitFor({
      state: 'visible',
      timeout: 30000
    })

    const saveButton = panel.locator('[data-flow-action="save-batch-record-attachment-owners"]').first()
    await saveButton.waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(await saveButton.isEnabled(), true, 'save button must be enabled after init')
    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/route/flow-config/batch-record-attachment-owners/save') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await saveButton.click()
    const saveResponse = await saveResponsePromise
    const savePayload = await saveResponse.json()
    assert.equal(savePayload.code, 0, `save owners failed: ${savePayload.msg || savePayload.code}`)
    await page.getByText('批记录附件负责人已保存', { exact: false }).first().waitFor({
      state: 'visible',
      timeout: 30000
    })

    const rowTexts = await panel
      .locator('[data-batch-record-attachment-owner]')
      .evaluateAll((rows) => rows.map((row) => row.textContent.replace(/\s+/g, ' ').trim()))
    const screenshotPath = path.join(config.artifactDir, 'route-start-batch-record-attachments-real.png')
    await page.screenshot({ path: screenshotPath, fullPage: true })
    const result = {
      ok: true,
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      routeCode: config.routeCode,
      targetResponses,
      initializedOwners: initPayload.data.map((owner) => ({
        attachmentCode: owner.attachmentCode,
        attachmentName: owner.attachmentName,
        defaultRoleName: owner.defaultRoleName,
        candidateSourceType: owner.candidateSourceType,
        assignedUserCount: owner.assignedUserIds.length,
        assignedUserNames: owner.assignedUserNames
      })),
      rowTexts,
      pageErrors,
      consoleErrors,
      screenshotPath
    }
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join('\n')}`)
    assert.deepEqual(consoleErrors, [], `console errors: ${consoleErrors.join('\n')}`)
    writeJson('route-start-batch-record-attachments-real-result.json', result)
    await context.close()
    console.log(`PASS: route start batch record attachments real E2E route=${config.routeCode}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
