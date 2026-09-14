const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: process.env.FORM_TEMPLATE_RULE_E2E_BASE_URL || 'http://127.0.0.1:8081',
  tenant: process.env.FORM_TEMPLATE_RULE_E2E_TENANT || '芋道源码',
  username: process.env.FORM_TEMPLATE_RULE_E2E_USERNAME || 'admin',
  password: process.env.FORM_TEMPLATE_RULE_E2E_PASSWORD || '',
  targetVersionNo: process.env.FORM_TEMPLATE_RULE_E2E_VERSION_NO || 'V21.0',
  pageTimeoutMs: Number(process.env.FORM_TEMPLATE_RULE_E2E_PAGE_TIMEOUT_MS || 180000),
  ruleResponseTimeoutMs: Number(process.env.FORM_TEMPLATE_RULE_E2E_TIMEOUT_MS || 180000),
  headed: process.env.FORM_TEMPLATE_RULE_E2E_HEADED === '1'
}

if (!config.password) {
  throw new Error('FORM_TEMPLATE_RULE_E2E_PASSWORD is required for the real login path')
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
  await form.waitFor({ state: 'visible', timeout: config.pageTimeoutMs })
  await selectTenant(page, form)
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.pageTimeoutMs }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(payload.code), `login_payload_failed:${JSON.stringify(payload)}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: config.pageTimeoutMs, waitUntil: 'commit' })
}

async function waitForTemplateRow(page, targetTemplateRow) {
  const rowLocators = [
    page
      .getByRole('row')
      .filter({ hasText: targetTemplateRow.templateName })
      .filter({ hasText: targetTemplateRow.versionNo })
      .first(),
    page
      .locator('.el-table__body-wrapper tbody tr')
      .filter({ hasText: targetTemplateRow.templateName })
      .filter({ hasText: targetTemplateRow.versionNo })
      .first(),
    page
      .locator('tr.el-table__row')
      .filter({ hasText: targetTemplateRow.templateName })
      .filter({ hasText: targetTemplateRow.versionNo })
      .first()
  ]
  for (const locator of rowLocators) {
    try {
      await locator.waitFor({ state: 'visible', timeout: 15000 })
      return locator
    } catch (error) {
      // Try the next selector family; the final error below includes page diagnostics.
    }
  }
  const visibleRows = await page
    .locator('.el-table__body-wrapper tbody tr, tr.el-table__row, [role="row"]')
    .evaluateAll((rows) => rows.map((row) => row.textContent?.replace(/\s+/g, ' ').trim()).filter(Boolean).slice(0, 20))
    .catch(() => [])
  throw new Error(
    `template_row_visible_failed:target=${JSON.stringify({
      templateName: targetTemplateRow.templateName,
      versionNo: targetTemplateRow.versionNo,
      status: targetTemplateRow.status
    })};rows=${JSON.stringify(visibleRows)};body=${(await page.locator('body').innerText()).slice(0, 3000)}`
  )
}

async function main() {
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(config.pageTimeoutMs)
  page.setDefaultNavigationTimeout(config.pageTimeoutMs)
  const ruleResponses = []
  const templateListResponses = []
  try {
    page.on('response', async (response) => {
      if (response.url().includes('/fill-rule-auto-detect')) {
        ruleResponses.push({ status: response.status(), url: response.url() })
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
    await page.getByText('表单模板', { exact: false }).first().waitFor({ state: 'visible', timeout: config.pageTimeoutMs })
    const templateListWaitCount = Math.ceil(config.pageTimeoutMs / 1000)
    for (let i = 0; i < templateListWaitCount && templateListResponses.length === 0; i += 1) {
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
        row.versionNo === config.targetVersionNo &&
        row.status === 'PUBLISHED'
    )
    if (!targetTemplateRow) {
      throw new Error(
        `published_template_row_missing:url=${page.url()};payload=${JSON.stringify(templateListPayload).slice(0, 4000)}`
      )
    }
    const templateRow = await waitForTemplateRow(page, targetTemplateRow)
    await templateRow.click()
    const fillButton = page.getByRole('button', { name: '填写配置' }).first()
    try {
      await fillButton.waitFor({ state: 'visible', timeout: config.pageTimeoutMs })
    } catch (error) {
      throw new Error(
        `fill_config_button_missing_from_form_list:url=${page.url()};title=${await page.title()};templateResponses=${JSON.stringify(
          templateListResponses
        )};text=${(
          await page.locator('body').innerText()
        ).slice(0, 2000)}`
      )
    }
    const autoDraftResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes(`/versions/${encodeURIComponent(targetTemplateRow.versionNo)}/fill-rule-auto-detect`) &&
        response.request().method() === 'POST',
      { timeout: config.ruleResponseTimeoutMs }
    )
    await fillButton.click()

    const editor = page.locator('.batch-record-cell-rules-editor').first()
    await editor.waitFor({ state: 'visible', timeout: config.pageTimeoutMs })
    const ruleButton = editor.getByRole('button', { name: '规则识别' })
    assert.equal(await ruleButton.count(), 1, 'rule_detect_button_missing')
    const text = await editor.innerText()
    assert.match(text, /代码规则识别可在任意版本执行/, 'rule_draft_generation_boundary_missing')
    const ruleResponse = await autoDraftResponsePromise
    const rulePayload = await ruleResponse.json().catch(() => null)
    assert.ok(ruleResponse.ok(), `rule_detect_http_failed:${ruleResponse.status()}:${JSON.stringify(rulePayload)}`)
    assert.equal(rulePayload?.code, 0, `rule_detect_business_failed:${JSON.stringify(rulePayload)}`)
    assert.ok(rulePayload?.data?.sourceVersionNo, 'rule_detect_source_version_missing')
    assert.ok(rulePayload?.data?.versionNo, 'rule_detect_target_version_missing')
    assert.equal(rulePayload?.data?.targetStatus, 'DRAFT', 'rule_detect_target_status_not_draft')
    assert.equal(typeof rulePayload?.data?.draftCreated, 'boolean', 'rule_detect_draft_created_flag_missing')
    try {
      await page.waitForFunction(
        () => {
          const editorElement = document.querySelector('.batch-record-cell-rules-editor')
          const saveButton = Array.from(document.querySelectorAll('button')).find((button) =>
            button.textContent?.includes('保存填写配置')
          )
          const sidebar = document.querySelector('[data-fill-config-panel="template-config-sidebar"]')
          if (!editorElement || !saveButton || !sidebar) return false
          return !saveButton.disabled && !sidebar.textContent?.includes('只有草稿版本可以保存填写配置。')
        },
        { timeout: config.pageTimeoutMs }
      )
    } catch (error) {
      const draftSwitchDiagnostic = await page.locator('body').evaluate(() => {
        const sidebar = document.querySelector('[data-fill-config-panel="template-config-sidebar"]')
        const saveButton = Array.from(document.querySelectorAll('button')).find((button) =>
          button.textContent?.includes('保存填写配置')
        )
        const rows = Array.from(document.querySelectorAll('.el-table__body-wrapper tbody tr, tr.el-table__row'))
          .map((row) => row.textContent?.replace(/\s+/g, ' ').trim())
          .filter(Boolean)
          .slice(0, 12)
        return {
          url: location.href,
          sidebarText: sidebar?.textContent?.replace(/\s+/g, ' ').trim().slice(0, 1200),
          saveButtonDisabled: saveButton?.disabled,
          rows
        }
      })
      throw new Error(
        `draft_switch_not_editable:payload=${JSON.stringify(rulePayload)};diagnostic=${JSON.stringify(
          draftSwitchDiagnostic
        )}`
      )
    }
    const editableRuleCell = editor
      .locator('.batch-record-cell-rules-editor__cell-button')
      .filter({ hasText: '第 4 行第 1 列' })
      .first()
    await editableRuleCell.waitFor({ state: 'visible', timeout: config.pageTimeoutMs })
    await editableRuleCell.click()
    try {
      await page.waitForFunction(
        () => {
          const sidebar = document.querySelector('[data-fill-config-panel="template-config-sidebar"]')
          if (!sidebar) return false
          const labels = Array.from(sidebar.querySelectorAll('.el-form-item__label')).map((label) =>
            label.textContent?.replace(/\s+/g, '').trim()
          )
          if (!labels.includes('字段名称') || !labels.includes('字段类型') || !labels.includes('控件类型')) {
            return false
          }
          const disabledInputs = Array.from(sidebar.querySelectorAll('.el-form-item input, .el-form-item textarea'))
            .filter((node) => node instanceof HTMLInputElement || node instanceof HTMLTextAreaElement)
            .filter((node) => node.disabled)
          const disabledSelects = Array.from(sidebar.querySelectorAll('.el-form-item .el-select')).filter((node) =>
            node.classList.contains('is-disabled')
          )
          return disabledInputs.length === 0 && disabledSelects.length === 0
        },
        { timeout: config.pageTimeoutMs }
      )
    } catch (error) {
      const diagnostic = await page.locator('[data-fill-config-panel="template-config-sidebar"]').evaluate((sidebar) => ({
        text: sidebar.textContent?.replace(/\s+/g, ' ').trim().slice(0, 1200),
        labels: Array.from(sidebar.querySelectorAll('.el-form-item__label')).map((label) =>
          label.textContent?.replace(/\s+/g, '').trim()
        ),
        disabledInputs: Array.from(sidebar.querySelectorAll('.el-form-item input, .el-form-item textarea')).map((node) => ({
          tagName: node.tagName,
          value: node.value,
          placeholder: node.getAttribute('placeholder'),
          disabled: node.disabled
        })),
        disabledSelects: Array.from(sidebar.querySelectorAll('.el-form-item .el-select')).map((node) => ({
          text: node.textContent?.replace(/\s+/g, ' ').trim(),
          disabled: node.classList.contains('is-disabled')
        })),
        readonlyWarning: sidebar.textContent?.includes('代码规则识别可在任意版本执行') || false
      }))
      throw new Error(`selected_rule_controls_not_editable:${JSON.stringify(diagnostic)}`)
    }
    const sidebarState = await page.locator('[data-fill-config-panel="template-config-sidebar"]').evaluate((sidebar) => {
      const valueTypeLabel = Array.from(sidebar.querySelectorAll('label')).find((node) =>
        node.textContent?.includes('字段类型')
      )
      const valueTypeControl = valueTypeLabel?.nextElementSibling
      return {
        text: sidebar.textContent?.replace(/\s+/g, ' ').trim().slice(0, 800),
        valueTypeControlDisabled: valueTypeControl?.classList.contains('is-disabled') || false,
        fieldNameDisabled:
          Array.from(sidebar.querySelectorAll('.el-form-item')).find((node) =>
            node.textContent?.includes('字段名称')
          )?.querySelector('input')?.disabled || false,
        componentFlagControlDisabled:
          Array.from(sidebar.querySelectorAll('.el-form-item')).find((node) =>
            node.textContent?.includes('控件类型')
          )?.querySelector('.el-select')?.classList.contains('is-disabled') || false,
        disabledControlCount: Array.from(sidebar.querySelectorAll('input, textarea, button')).filter(
          (node) => node instanceof HTMLInputElement || node instanceof HTMLTextAreaElement || node instanceof HTMLButtonElement
            ? node.disabled
            : false
        ).length
      }
    })
    assert.equal(sidebarState.valueTypeControlDisabled, false, `value_type_control_still_disabled:${JSON.stringify(sidebarState)}`)

    const result = {
      status: 'PASS',
      url: page.url(),
      title: await page.title(),
      ruleButtonVisible: await ruleButton.isVisible(),
      ruleButtonEnabled: await ruleButton.isEnabled(),
      ruleButtonDisabled: await ruleButton.getAttribute('disabled'),
      targetVersionNo: targetTemplateRow.versionNo,
      draftVersionNo: rulePayload?.data?.versionNo,
      sidebarState,
      editorTextPreview: (await editor.innerText()).slice(0, 1800),
      ruleResponseCount: ruleResponses.length,
      ruleResponses
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
