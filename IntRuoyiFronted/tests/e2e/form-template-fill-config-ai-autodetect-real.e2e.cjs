const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: process.env.FORM_TEMPLATE_AI_E2E_BASE_URL || 'http://127.0.0.1:8081',
  tenant: process.env.FORM_TEMPLATE_AI_E2E_TENANT || '芋道源码',
  username: process.env.FORM_TEMPLATE_AI_E2E_USERNAME || 'admin',
  password: process.env.FORM_TEMPLATE_AI_E2E_PASSWORD || 'admin123',
  aiResponseTimeoutMs: Number(process.env.FORM_TEMPLATE_AI_E2E_AI_TIMEOUT_MS || 180000),
  headed: process.env.FORM_TEMPLATE_AI_E2E_HEADED === '1'
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
}

async function login(page) {
  const url = new URL('/login', config.baseUrl)
  url.searchParams.set('redirect', '/index')
  await page.goto(url.toString(), { waitUntil: 'commit' })
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
  const payload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(payload.code), `login_payload_failed:${JSON.stringify(payload)}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
}

async function main() {
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  const aiResponses = []
  const templateListResponses = []
  try {
    page.on('response', async (response) => {
      if (response.url().includes('/fill-rule-auto-detect')) {
        aiResponses.push({ status: response.status(), url: response.url() })
      }
      if (response.url().includes('/form-center/template-pool')) {
        templateListResponses.push({
          status: response.status(),
          url: response.url(),
          body: await response.text().catch(() => '')
        })
      }
    })

    await login(page)
    await page.goto(new URL('/mdm/form-center/template', config.baseUrl).toString(), { waitUntil: 'commit' })
    await page.getByText('表单模板', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
    for (let i = 0; i < 60 && templateListResponses.length === 0; i += 1) {
      await page.waitForTimeout(1000)
    }
    const latestTemplateListResponse = templateListResponses.at(-1)
    if (!latestTemplateListResponse?.body) {
      throw new Error(`template_list_response_missing:url=${page.url()};responses=${JSON.stringify(templateListResponses)}`)
    }
    const templateListPayload = JSON.parse(latestTemplateListResponse.body)
    const templateRows = templateListPayload?.data?.list || []
    const targetTemplateRow = templateRows.find(
      (row) =>
        row.templateName?.includes('按压式压力泵过程检验记录') &&
        row.status &&
        row.status !== 'DRAFT'
    )
    if (!targetTemplateRow) {
      throw new Error(
        `published_template_row_missing:url=${page.url()};payload=${JSON.stringify(templateListPayload).slice(0, 4000)}`
      )
    }
    const templateRow = page
      .getByRole('row')
      .filter({ hasText: targetTemplateRow.templateName })
      .filter({ hasText: targetTemplateRow.versionNo })
      .first()
    await templateRow.waitFor({ state: 'visible', timeout: 60000 })
    await templateRow.click()
    const fillButtons = page.getByRole('button', { name: '填写配置' })
    if ((await fillButtons.count()) === 0) {
      throw new Error(
        `fill_config_button_missing_from_form_list:url=${page.url()};title=${await page.title()};templateResponses=${JSON.stringify(
          templateListResponses
        )};text=${(
          await page.locator('body').innerText()
        ).slice(0, 2000)}`
      )
    }
    await fillButtons.first().click()

    const editor = page.locator('.batch-record-cell-rules-editor').first()
    await editor.waitFor({ state: 'visible', timeout: 60000 })
    const aiButton = editor.getByRole('button', { name: 'AI 自动识别' })
    assert.equal(await aiButton.count(), 1, 'ai_auto_detect_button_missing')
    const text = await editor.innerText()
    assert.match(text, /AI 自动识别可在任意版本执行/, 'ai_draft_generation_boundary_missing')
    const aiResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/fill-rule-auto-detect') && response.request().method() === 'POST',
      { timeout: config.aiResponseTimeoutMs }
    )
    await aiButton.click()
    const aiResponse = await aiResponsePromise
    const aiPayload = await aiResponse.json().catch(() => null)
    assert.ok(aiResponse.ok(), `ai_detect_http_failed:${aiResponse.status()}:${JSON.stringify(aiPayload)}`)
    assert.equal(aiPayload?.code, 0, `ai_detect_business_failed:${JSON.stringify(aiPayload)}`)
    assert.ok(aiPayload?.data?.sourceVersionNo, 'ai_detect_source_version_missing')
    assert.ok(aiPayload?.data?.versionNo, 'ai_detect_target_version_missing')
    assert.equal(aiPayload?.data?.targetStatus, 'DRAFT', 'ai_detect_target_status_not_draft')
    assert.equal(typeof aiPayload?.data?.draftCreated, 'boolean', 'ai_detect_draft_created_flag_missing')

    const result = {
      status: 'PASS',
      url: page.url(),
      title: await page.title(),
      aiButtonVisible: await aiButton.isVisible(),
      aiButtonEnabled: await aiButton.isEnabled(),
      aiButtonDisabled: await aiButton.getAttribute('disabled'),
      editorTextPreview: (await editor.innerText()).slice(0, 1800),
      aiResponseCount: aiResponses.length,
      aiResponses
    }
    console.log(JSON.stringify(result, null, 2))
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
