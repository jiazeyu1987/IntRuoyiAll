const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: process.env.BATCH_RECORD_PLACEHOLDER_E2E_BASE_URL || 'http://localhost:8081',
  tenant: process.env.BATCH_RECORD_PLACEHOLDER_E2E_TENANT || '测试租户',
  username: process.env.BATCH_RECORD_PLACEHOLDER_E2E_USERNAME || 'aoteman',
  password: process.env.BATCH_RECORD_PLACEHOLDER_E2E_PASSWORD || '111111',
  headed: process.env.BATCH_RECORD_PLACEHOLDER_E2E_HEADED === '1'
}

if (config.baseUrl !== 'http://localhost:8081') {
  throw new Error(`batch_record_placeholder_e2e_requires_localhost_8081:${config.baseUrl}`)
}
if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(
    `batch_record_placeholder_e2e_requires_test_tenant_aoteman:${JSON.stringify({
      tenant: config.tenant,
      username: config.username
    })}`
  )
}

const outputDir = path.resolve(__dirname, '..', 'output', 'batch-record-placeholder-visible')
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
  loginUrl.searchParams.set('redirect', '/mes/pro/batch-record-form-list')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })
  if (!page.url().includes('/login')) return

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
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function openRuleDialog(page) {
  const previewActions = page.locator('.batch-record-form-preview__actions').first()
  await previewActions.waitFor({ state: 'visible', timeout: 60000 })
  const cellRulesResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/batch-record-report/cell-rules') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await previewActions.getByRole('button', { name: '规则' }).click()
  await cellRulesResponsePromise
  const editor = page.locator('.batch-record-cell-rules-editor').first()
  await editor.waitFor({ state: 'visible', timeout: 60000 })
  return editor
}

async function getPromptInput(editor) {
  const sidePanel = editor.locator('.batch-record-cell-rules-editor__side-panel').first()
  const formItem = sidePanel.locator('.el-form-item').filter({ hasText: '单元格提示词' }).first()
  await formItem.waitFor({ state: 'visible', timeout: 30000 })
  return formItem.locator('input').first()
}

async function selectTargetRuleCell(editor, targetIndex) {
  const preview = editor.locator('.batch-record-cell-rules-editor__preview').first()
  const emptyRuleButtons = preview.locator(
    '.batch-record-cell-rules-editor__cell.is-rule.is-empty .batch-record-cell-rules-editor__cell-button'
  )
  const emptyRuleCount = await emptyRuleButtons.count()
  assert.ok(emptyRuleCount > 0, 'placeholder_e2e_requires_at_least_one_empty_rule_cell')
  const index = Math.min(targetIndex ?? 0, emptyRuleCount - 1)
  const target = emptyRuleButtons.nth(index)
  await target.scrollIntoViewIfNeeded()
  const targetText = (await target.innerText()).replace(/\s+/g, ' ').trim()
  await target.click()
  await getPromptInput(editor)
  return { index, targetText }
}

async function saveRules(page) {
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/batch-record-report/cell-rules') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '保存规则' }).click()
  const saveResponse = await saveResponsePromise
  assert.ok(saveResponse.ok(), `save_cell_rules_http_failed:${saveResponse.status()}`)
  const savePayload = await saveResponse.json().catch(() => ({}))
  assert.ok([undefined, 0, 200].includes(savePayload.code), `save_cell_rules_payload_failed:${JSON.stringify(savePayload)}`)
  await page.getByText('单元格规则已保存', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
}

async function setTargetPlaceholder(page, targetIndex, placeholder) {
  const editor = await openRuleDialog(page)
  const target = await selectTargetRuleCell(editor, targetIndex)
  const input = await getPromptInput(editor)
  const originalPlaceholder = await input.inputValue()
  await input.fill(placeholder)
  assert.equal(await input.inputValue(), placeholder, 'placeholder_input_should_accept_marker')
  await saveRules(page)
  await editor.waitFor({ state: 'hidden', timeout: 60000 })
  return { ...target, originalPlaceholder }
}

async function restoreTargetPlaceholder(page, targetIndex, originalPlaceholder) {
  const editor = await openRuleDialog(page)
  await selectTargetRuleCell(editor, targetIndex)
  const input = await getPromptInput(editor)
  await input.fill(originalPlaceholder || '')
  await saveRules(page)
  await editor.waitFor({ state: 'hidden', timeout: 60000 })
}

async function waitForPreviewPlaceholder(page, marker) {
  const preview = page.locator('.batch-record-form-preview').first()
  await preview.waitFor({ state: 'visible', timeout: 60000 })
  await preview.locator('.edhr-template-sheet').first().waitFor({ state: 'visible', timeout: 60000 })
  const markerLocator = preview.locator('.edhr-template-sheet__fillable-placeholder', { hasText: marker }).first()
  await markerLocator.waitFor({ state: 'visible', timeout: 60000 })
  return page.evaluate((text) => {
    const previewElement = document.querySelector('.batch-record-form-preview')
    const normalize = (value) => (value || '').replace(/\s+/g, ' ').trim()
    const placeholders = Array.from(
      previewElement?.querySelectorAll('.edhr-template-sheet__fillable-placeholder') || []
    ).map((item) => ({
      text: normalize(item.textContent),
      cellText: normalize(item.closest('td')?.textContent),
      cellClass: item.closest('td')?.className || ''
    }))
    return {
      markerVisible: placeholders.some((item) => item.text === text),
      placeholders
    }
  }, marker)
}

async function main() {
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const writeRequests = []
  const marker = `E2E提示词-${Date.now()}`
  let targetIndex = 0
  let originalPlaceholder = ''
  let selectedCellText = ''
  let markerSaved = false
  let restored = false

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    page.on('request', (request) => {
      if (isWriteMethod(request.method()) && isMesRequest(request.url())) {
        writeRequests.push({ method: request.method(), url: request.url() })
      }
    })

    await login(page)
    await page.goto(new URL('/mes/pro/batch-record-form-list', config.baseUrl).toString(), {
      waitUntil: 'domcontentloaded'
    })
    await page.locator('.batch-record-form-layout').waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.batch-record-form-preview .edhr-template-sheet').first().waitFor({ state: 'visible', timeout: 60000 })

    const setResult = await setTargetPlaceholder(page, targetIndex, marker)
    targetIndex = setResult.index
    originalPlaceholder = setResult.originalPlaceholder
    selectedCellText = setResult.targetText
    markerSaved = true

    const previewResult = await waitForPreviewPlaceholder(page, marker)
    assert.equal(previewResult.markerVisible, true, 'placeholder_marker_should_be_visible_in_readonly_preview')
    await page.screenshot({ path: path.join(outputDir, 'placeholder-visible-after-save.png'), fullPage: true })

    await restoreTargetPlaceholder(page, targetIndex, originalPlaceholder)
    restored = true

    assert.ok(
      writeRequests.length >= 2 &&
        writeRequests.every((request) => request.method === 'PUT' && request.url.includes('/cell-rules')),
      `placeholder_e2e_should_only_write_cell_rules:${JSON.stringify(writeRequests)}`
    )

    const result = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      selectedCellText,
      marker,
      originalPlaceholder,
      restored,
      writeRequests,
      previewResult,
      screenshot: path.join(outputDir, 'placeholder-visible-after-save.png')
    }
    fs.writeFileSync(path.join(outputDir, 'placeholder-visible-result.json'), JSON.stringify(result, null, 2), 'utf8')
    console.log(JSON.stringify(result, null, 2))
  } catch (error) {
    if (markerSaved && !restored) {
      console.error(`restore_required_after_failure:${error.message}`)
    }
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
