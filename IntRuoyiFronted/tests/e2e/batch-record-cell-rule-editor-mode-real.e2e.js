const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: process.env.BATCH_RECORD_CELL_RULE_E2E_BASE_URL || 'http://localhost:8081',
  tenant: process.env.BATCH_RECORD_CELL_RULE_E2E_TENANT || '测试租户',
  username: process.env.BATCH_RECORD_CELL_RULE_E2E_USERNAME || 'aoteman',
  password: process.env.BATCH_RECORD_CELL_RULE_E2E_PASSWORD || '111111',
  allowSave: process.env.BATCH_RECORD_CELL_RULE_E2E_ALLOW_SAVE === '1',
  headed: process.env.BATCH_RECORD_CELL_RULE_E2E_HEADED === '1'
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(`batch_record_cell_rule_e2e_requires_test_tenant_aoteman:${JSON.stringify({ tenant: config.tenant, username: config.username })}`)
}
if (!config.allowSave) {
  throw new Error('batch_record_cell_rule_e2e_requires_BATCH_RECORD_CELL_RULE_E2E_ALLOW_SAVE=1')
}

const outputDir = path.resolve(__dirname, '..', 'output', 'batch-record-cell-rule-editor-mode')
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

async function getFieldInput(sidePanel, label) {
  const formItem = sidePanel.locator('.el-form-item').filter({ hasText: label }).first()
  await formItem.waitFor({ state: 'visible', timeout: 30000 })
  return formItem.locator('input').first()
}

async function main() {
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const writeRequests = []
  let selectedCellText = ''
  let cellRulesResponseJson = null

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
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

    const previewActions = page.locator('.batch-record-form-preview__actions').first()
    await previewActions.waitFor({ state: 'visible', timeout: 60000 })
    const ruleEntry = previewActions.getByRole('button', { name: '规则' })
    await ruleEntry.waitFor({ state: 'visible', timeout: 60000 })

    const cellRulesResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/mes/pro/batch-record-report/cell-rules') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await ruleEntry.click()
    const cellRulesResponse = await cellRulesResponsePromise
    cellRulesResponseJson = await cellRulesResponse.json().catch(() => null)

    const dialog = page.locator('.batch-record-cell-rules-editor').first()
    await dialog.waitFor({ state: 'visible', timeout: 60000 })
    const preview = dialog.locator('.batch-record-cell-rules-editor__preview').first()
    const sidePanel = dialog.locator('.batch-record-cell-rules-editor__side-panel').first()
    await preview.waitFor({ state: 'visible', timeout: 30000 })
    await sidePanel.waitFor({ state: 'visible', timeout: 30000 })

    const realControlCount = await preview.locator('input, textarea, .el-date-editor, .el-checkbox, canvas').count()
    assert.equal(realControlCount, 0, `preview_should_not_render_real_fill_controls:${realControlCount}`)

    const writeCountBeforeCellClick = writeRequests.length
    const ruleCellButton = preview
      .locator('.batch-record-cell-rules-editor__cell.is-rule .batch-record-cell-rules-editor__cell-button')
      .first()
    await ruleCellButton.waitFor({ state: 'visible', timeout: 60000 })
    selectedCellText = (await ruleCellButton.innerText()).replace(/\s+/g, ' ').trim()
    await ruleCellButton.click()
    await page.waitForTimeout(500)
    assert.equal(writeRequests.length, writeCountBeforeCellClick, 'clicking_preview_cell_must_not_send_write_request')

    await sidePanel.locator('.batch-record-cell-rules-editor__fillable-toggle').waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(
      await sidePanel.locator('.batch-record-cell-rules-editor__selected-card').count(),
      0,
      'selected_card_should_be_hidden'
    )
    assert.equal(await sidePanel.locator('.batch-record-cell-rules-editor__save-tip').count(), 0, 'save_tip_should_be_hidden')
    assert.equal(await sidePanel.locator('.batch-record-cell-rules-editor__static-tip').count(), 0, 'static_tip_should_be_hidden')
    const labelInput = await getFieldInput(sidePanel, '字段名称')
    const originalLabel = await labelInput.inputValue()
    const marker = `E2E规则验证-${Date.now()}`
    await labelInput.fill(marker)
    assert.equal(await labelInput.inputValue(), marker, 'label_input_should_be_editable')
    await labelInput.fill(originalLabel)

    const requiredSwitch = sidePanel.locator('.el-form-item').filter({ hasText: '是否必填' }).locator('.el-switch').first()
    await requiredSwitch.waitFor({ state: 'visible', timeout: 30000 })
    await requiredSwitch.click()
    await requiredSwitch.click()

    const typeSelect = sidePanel.locator('.el-form-item').filter({ hasText: '字段类型' }).locator('.el-select').first()
    await typeSelect.click()
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: /文本|数字|日期|勾选|签名/ }).first().waitFor({ state: 'visible', timeout: 30000 })
    await page.keyboard.press('Escape')

    const componentSelect = sidePanel.locator('.el-form-item').filter({ hasText: '控件类型' }).locator('.el-select').first()
    await componentSelect.click()
    await page.locator('.el-select-dropdown__item:visible').first().waitFor({ state: 'visible', timeout: 30000 })
    await page.keyboard.press('Escape')

    await page.waitForTimeout(500)
    assert.equal(writeRequests.length, writeCountBeforeCellClick, 'editing_side_panel_before_save_must_not_send_write_request')

    const saveResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/mes/pro/batch-record-report/cell-rules') && response.request().method() === 'PUT',
      { timeout: 60000 }
    )
    await page.getByRole('button', { name: '保存规则' }).click()
    const saveResponse = await saveResponsePromise
    assert.ok(saveResponse.ok(), `save_cell_rules_http_failed:${saveResponse.status()}`)
    const savePayload = await saveResponse.json().catch(() => ({}))
    assert.ok([undefined, 0, 200].includes(savePayload.code), `save_cell_rules_payload_failed:${JSON.stringify(savePayload)}`)
    await page.getByText('单元格规则已保存', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })

    await page.screenshot({ path: path.join(outputDir, 'batch-record-cell-rule-editor-mode-pass.png'), fullPage: true })
    const cellRulesData = cellRulesResponseJson?.data || cellRulesResponseJson || {}
    const result = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      reportId: cellRulesData.reportId || null,
      loadedRuleCount: Array.isArray(cellRulesData.rules) ? cellRulesData.rules.length : null,
      loadedSuggestionCount: Array.isArray(cellRulesData.suggestions) ? cellRulesData.suggestions.length : null,
      selectedCellText,
      writeRequests,
      screenshot: path.join(outputDir, 'batch-record-cell-rule-editor-mode-pass.png')
    }
    fs.writeFileSync(path.join(outputDir, 'batch-record-cell-rule-editor-mode-result.json'), JSON.stringify(result, null, 2), 'utf8')
    console.log(JSON.stringify(result, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
