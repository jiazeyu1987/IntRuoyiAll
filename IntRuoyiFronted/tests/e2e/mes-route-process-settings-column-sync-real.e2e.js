const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const targetColumn = {
  key: 'formSlots',
  label: '表单槽位'
}
const config = {
  baseUrl: (process.env.MES_ROUTE_PROCESS_COLUMN_SYNC_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_PROCESS_COLUMN_SYNC_TENANT || '测试租户',
  username: process.env.MES_ROUTE_PROCESS_COLUMN_SYNC_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_PROCESS_COLUMN_SYNC_PASSWORD || '111111',
  routeCode: process.env.MES_ROUTE_PROCESS_COLUMN_SYNC_ROUTE_CODE || 'RT000017',
  headed: process.env.MES_ROUTE_PROCESS_COLUMN_SYNC_HEADED === '1',
  artifactDir: path.resolve(
    process.env.MES_ROUTE_PROCESS_COLUMN_SYNC_ARTIFACT_DIR ||
      path.join(__dirname, '..', 'output', 'route-process-settings-column-sync-real')
  )
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertLocalOnly(baseUrl) {
  const parsed = new URL(baseUrl)
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(parsed.hostname),
    `MES_ROUTE_PROCESS_COLUMN_SYNC_BASE_URL must be local, got ${baseUrl}`
  )
}

function ensureArtifactDir() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
}

function writeArtifact(name, payload) {
  fs.writeFileSync(path.join(config.artifactDir, name), `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(800)
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
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 60000 })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.ok(
    loginResponse.ok() && [0, 200].includes(payload.code),
    `login failed: HTTP ${loginResponse.status()} ${JSON.stringify(payload)}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function openRouteEditPage(page) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)
  await page
    .locator('input[placeholder="请输入路线编码"], input[placeholder="请输入工艺路线编码"]')
    .first()
    .fill(config.routeCode)
  await page.getByRole('button', { name: /查询|搜索/ }).first().click()
  await settle(page)
  const row = page.locator('tr.el-table__row').filter({ hasText: config.routeCode }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.getByRole('button', { name: '编辑' }).click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit'), { timeout: 60000 })
  const editor = page.locator('.route-edit-page').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  return editor
}

async function openTab(editor, name) {
  const tab = editor.getByRole('tab', { name }).first()
  if (await tab.count()) {
    await tab.click()
  }
  await settle(editor.page())
}

function detailPanel(editor) {
  return editor.locator('[data-flow-panel="selected-process-detail"]').first()
}

async function selectFirstRouteNode(editor) {
  const node = editor.locator('[data-flow-node="route-process"]').first()
  await node.waitFor({ state: 'visible', timeout: 60000 })
  await node.click()
  await detailPanel(editor).waitFor({ state: 'visible', timeout: 10000 })
}

async function assertDetailFieldVisible(editor, visible) {
  const field = detailPanel(editor).locator(`[data-flow-detail-field="${targetColumn.key}"]`)
  if (visible) {
    await field.waitFor({ state: 'visible', timeout: 10000 })
    await assertFormSlotAggregateCard(field)
    return
  }
  await field.waitFor({ state: 'detached', timeout: 10000 })
}

async function assertFormSlotAggregateCard(field) {
  await field.locator('[data-form-slot-aggregate="true"]').waitFor({ state: 'visible', timeout: 10000 })
  const slotLabels = await field
    .locator('.route-flow-graph-designer__record-binding-label')
    .evaluateAll((items) => items.map((item) => item.textContent?.trim()).filter(Boolean))
  for (const expected of ['批记录表单', '损耗单', '过程检验单', '参数记录表']) {
    assert.ok(
      slotLabels.includes(expected),
      `表单槽位聚合卡片缺少 ${expected}: ${JSON.stringify(slotLabels)}`
    )
  }
}

async function waitForColumnConfigSave(page, action) {
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/user-table-column-config/save') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await action()
  const saveResponse = await saveResponsePromise
  const payload = await saveResponse.json()
  assert.equal(payload.code, 0, `save user interest config failed: ${JSON.stringify(payload)}`)
}

async function verifyFlowDetailPanel(editor) {
  await openTab(editor, '流转关系图')
  await selectFirstRouteNode(editor)
  await editor
    .locator('.route-flow-graph-designer__process-detail-field-picker')
    .first()
    .waitFor({ state: 'visible', timeout: 10000 })
  assert.ok(
    await detailPanel(editor).locator('[data-flow-action="add-process-config-item"]').count(),
    'red box must expose user interest column add action'
  )
  assert.ok(
    await detailPanel(editor).locator('[data-flow-action="remove-process-detail-field"]').count(),
    'red box must expose user interest column remove action'
  )
  assert.equal(
    await detailPanel(editor).locator('.route-schedule-strategy-editor').count(),
    0,
    'red box must not render schedule strategy internal controls'
  )
}

async function isInterestFieldVisible(editor) {
  return (await detailPanel(editor).locator(`[data-flow-detail-field="${targetColumn.key}"]`).count()) > 0
}

async function removeInterestField(editor) {
  const page = editor.page()
  const field = detailPanel(editor).locator(`[data-flow-detail-field="${targetColumn.key}"]`).first()
  await field.waitFor({ state: 'visible', timeout: 10000 })
  await waitForColumnConfigSave(page, async () => {
    await field.locator('[data-flow-action="remove-process-detail-field"]').click()
  })
  await assertDetailFieldVisible(editor, false)
}

async function addInterestField(editor) {
  const page = editor.page()
  const picker = detailPanel(editor).locator('.route-flow-graph-designer__process-detail-field-picker').first()
  await picker.locator('.el-select').click()
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: targetColumn.label }).first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()
  await waitForColumnConfigSave(page, async () => {
    await picker.locator('[data-flow-action="add-process-config-item"]').click()
  })
  await assertDetailFieldVisible(editor, true)
}

async function setInterestFieldVisible(editor, visible) {
  await verifyFlowDetailPanel(editor)
  const currentVisible = await isInterestFieldVisible(editor)
  if (currentVisible === visible) return
  if (visible) {
    await addInterestField(editor)
  } else {
    await removeInterestField(editor)
  }
}

async function reloadEditorAndVerify(page, visible) {
  await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
  const editor = page.locator('.route-edit-page').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  await verifyFlowDetailPanel(editor)
  await assertDetailFieldVisible(editor, visible)
  return editor
}

async function main() {
  assertLocalOnly(config.baseUrl)
  ensureArtifactDir()
  const browser = await chromium.launch({ headless: !config.headed, executablePath })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  let initiallyVisible = false
  let editor
  let primaryError
  try {
    await login(page)
    editor = await openRouteEditPage(page)
    await verifyFlowDetailPanel(editor)
    initiallyVisible = await isInterestFieldVisible(editor)
    await setInterestFieldVisible(editor, !initiallyVisible)
    editor = await reloadEditorAndVerify(page, !initiallyVisible)
    await setInterestFieldVisible(editor, initiallyVisible)
    await assertDetailFieldVisible(editor, initiallyVisible)
    writeArtifact('result.json', {
      routeCode: config.routeCode,
      tableKey: 'mes.pro.route.flow.detailFields',
      targetColumn,
      restoredVisible: initiallyVisible,
      verifiedSlotLabels: ['批记录表单', '损耗单', '过程检验单', '参数记录表']
    })
    await page.screenshot({
      path: path.join(config.artifactDir, 'route-process-settings-column-sync-real.png'),
      fullPage: true
    })
  } catch (error) {
    primaryError = error
  } finally {
    if (editor) {
      try {
        await setInterestFieldVisible(editor, initiallyVisible)
      } catch (cleanupError) {
        const cleanupMessage = cleanupError && cleanupError.stack ? cleanupError.stack : String(cleanupError)
        if (primaryError) {
          primaryError = new Error(`${primaryError.stack || primaryError}\nCleanup failed:\n${cleanupMessage}`)
        } else {
          primaryError = cleanupError
        }
      }
    }
    await browser.close()
  }
  if (primaryError) throw primaryError
  console.log(
    `PASS: route process settings column sync real E2E route=${config.routeCode} field=${targetColumn.key}`
  )
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exit(1)
})
