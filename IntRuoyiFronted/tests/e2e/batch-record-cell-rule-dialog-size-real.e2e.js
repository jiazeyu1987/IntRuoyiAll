const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: process.env.BATCH_RECORD_CELL_RULE_SIZE_E2E_BASE_URL || 'http://localhost:8081',
  tenant: process.env.BATCH_RECORD_CELL_RULE_SIZE_E2E_TENANT || '测试租户',
  username: process.env.BATCH_RECORD_CELL_RULE_SIZE_E2E_USERNAME || 'aoteman',
  password: process.env.BATCH_RECORD_CELL_RULE_SIZE_E2E_PASSWORD || '111111',
  headed: process.env.BATCH_RECORD_CELL_RULE_SIZE_E2E_HEADED === '1'
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(
    `batch_record_cell_rule_size_e2e_requires_test_tenant_aoteman:${JSON.stringify({
      tenant: config.tenant,
      username: config.username
    })}`
  )
}

const outputDir = path.resolve(__dirname, '..', 'output', 'batch-record-cell-rule-dialog-size')
fs.mkdirSync(outputDir, { recursive: true })

const isWriteMethod = (method) => !['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase())
const isMesRequest = (url) => url.includes('/admin-api/mes/') || url.includes('/mes/')

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
    return
  }
  await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit' })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  await selectTenant(page, form)
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login_payload_failed:${JSON.stringify(loginPayload)}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
}

async function main() {
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const writeRequests = []

  try {
    const viewport = { width: 1440, height: 960 }
    const context = await browser.newContext({ viewport })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    page.on('request', (request) => {
      if (isWriteMethod(request.method()) && isMesRequest(request.url())) {
        writeRequests.push({ method: request.method(), url: request.url() })
      }
    })

    await login(page)
    await page.goto(new URL('/mes/pro/batch-record-form-list', config.baseUrl).toString(), { waitUntil: 'commit' })
    await page.getByText('批记录表单', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    assert.equal(
      await page.getByRole('button', { name: /单元格规则/ }).count(),
      0,
      'left_table_cell_rule_button_should_be_hidden'
    )

    const cellRulesResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/batch-record-report/cell-rules') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    const previewActions = page.locator('.batch-record-form-preview__actions').first()
    await previewActions.waitFor({ state: 'visible', timeout: 60000 })
    await previewActions.getByRole('button', { name: '规则' }).click()
    await cellRulesResponsePromise

    const editor = page.locator('.batch-record-cell-rules-editor').first()
    await editor.waitFor({ state: 'visible', timeout: 60000 })

    const dialog = page.locator('.el-dialog').filter({ has: editor }).first()
    const workspace = editor.locator('.batch-record-cell-rules-editor__workspace').first()
    const sidePanel = editor.locator('.batch-record-cell-rules-editor__side-panel').first()
    const fillableToggle = editor.locator('.batch-record-cell-rules-editor__fillable-toggle').first()
    const promptField = sidePanel.locator('.el-form-item').filter({ hasText: '单元格提示词' }).first()

    await fillableToggle.waitFor({ state: 'visible', timeout: 30000 })
    await promptField.waitFor({ state: 'visible', timeout: 30000 })

    const dialogBox = await dialog.boundingBox()
    const workspaceBox = await workspace.boundingBox()
    const sidePanelBox = await sidePanel.boundingBox()
    const fillableToggleBox = await fillableToggle.boundingBox()
    const promptFieldBox = await promptField.boundingBox()
    assert.ok(dialogBox, 'dialog_box_missing')
    assert.ok(workspaceBox, 'workspace_box_missing')
    assert.ok(sidePanelBox, 'side_panel_box_missing')
    assert.ok(fillableToggleBox, 'fillable_toggle_box_missing')
    assert.ok(promptFieldBox, 'prompt_field_box_missing')

    assert.ok(dialogBox.width >= viewport.width - 48, `dialog_width_not_red_frame:${dialogBox.width}`)
    assert.ok(dialogBox.x <= 20, `dialog_left_margin_too_large:${dialogBox.x}`)
    assert.ok(viewport.width - dialogBox.x - dialogBox.width <= 20, `dialog_right_margin_too_large:${dialogBox.width}`)
    assert.ok(workspaceBox.height >= viewport.height - 260, `workspace_height_too_short:${workspaceBox.height}`)
    assert.ok(fillableToggleBox.y >= sidePanelBox.y, 'fillable_toggle_top_outside_side_panel')
    assert.ok(
      fillableToggleBox.y + fillableToggleBox.height <= sidePanelBox.y + sidePanelBox.height,
      'fillable_toggle_bottom_outside_side_panel'
    )
    assert.ok(promptFieldBox.y >= fillableToggleBox.y + fillableToggleBox.height, 'prompt_field_overlaps_fillable_toggle')
    assert.equal(
      await editor.locator('.batch-record-cell-rules-editor__rule-list').count(),
      0,
      'rule_list_should_be_hidden'
    )
    assert.equal(
      await editor.locator('.batch-record-cell-rules-editor__selected-card').count(),
      0,
      'selected_card_should_be_hidden'
    )
    assert.equal(await editor.locator('.batch-record-cell-rules-editor__save-tip').count(), 0, 'save_tip_should_be_hidden')
    assert.equal(
      await editor.locator('.batch-record-cell-rules-editor__static-tip').count(),
      0,
      'static_tip_should_be_hidden'
    )
    const sidePanelText = await sidePanel.innerText()
    assert.doesNotMatch(sidePanelText, /规则设置|当前单元格|白色为不可填写|保存后该单元格/)
    assert.deepEqual(writeRequests, [], 'dialog size readonly e2e must not send MES write requests')

    const screenshot = path.join(outputDir, 'batch-record-cell-rule-dialog-size-pass.png')
    await page.screenshot({ path: screenshot, fullPage: true })
    const result = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      viewport,
      dialogBox,
      workspaceBox,
      sidePanelBox,
      fillableToggleBox,
      promptFieldBox,
      writeRequests,
      screenshot
    }
    fs.writeFileSync(
      path.join(outputDir, 'batch-record-cell-rule-dialog-size-result.json'),
      JSON.stringify(result, null, 2),
      'utf8'
    )
    console.log(JSON.stringify(result, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
