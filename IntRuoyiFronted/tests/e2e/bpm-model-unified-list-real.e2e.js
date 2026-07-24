const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.BPM_MODEL_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.BPM_MODEL_E2E_TENANT || '测试租户',
  username: process.env.BPM_MODEL_E2E_USERNAME || 'aoteman',
  password: process.env.BPM_MODEL_E2E_PASSWORD || '111111',
  targetPath: '/bpm/manager/model',
  taskDir:
    process.env.BPM_MODEL_E2E_TASK_DIR ||
    path.resolve(__dirname, '..', '..', '..', 'doc/tasks/20260715-bpm-model-chinese-name-view/e2e-artifacts')
}

const screenshots = {
  page: path.join(config.taskDir, 'bpm-model-unified-list.png'),
  loginFailed: path.join(config.taskDir, 'bpm-model-unified-list-login-failed.png')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(500)
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

  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise.catch(async (error) => {
    await page.screenshot({ path: screenshots.loginFailed, fullPage: true }).catch(() => null)
    throw error
  })
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(
    loginPayload && (loginPayload.code === 0 || loginPayload.code === 200),
    `login failed: ${JSON.stringify(loginPayload)}`
  )
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function collectMetrics(page) {
  return page.evaluate(() => {
    const text = document.body.innerText || ''
    const headers = Array.from(
      document.querySelectorAll('.bpm-model-unified-table .el-table__header th')
    ).map((header) => (header.textContent || '').replace(/\s+/g, ' ').trim())
    return {
      unifiedTemplateCount: document.querySelectorAll('.unified-list-template[data-table-key="bpm.model.main"]').length,
      unifiedTableCount: document.querySelectorAll('.bpm-model-unified-table').length,
      tableShellCount: document.querySelectorAll('.unified-list-template__table-shell').length,
      dataRows: document.querySelectorAll('.bpm-model-unified-table .el-table__body .el-table__row').length,
      headers,
      hasWordPrintTemplate: text.includes('Word 打印模板'),
      hasDisplayFieldControl: text.includes('显示字段'),
      hasColumnResetControl: text.includes('重置列'),
      hasResetAction: text.includes('重置'),
      hasCreateModelAction: text.includes('新建模型'),
      hasCategorySortAction: text.includes('分类排序'),
      hasToolbarSettingsAction: Boolean(document.querySelector('.bpm-model-page__icon-button')),
      hasOldCategoryCards: Boolean(document.querySelector('[class*="category-draggable"]')),
      hasSortAction: text.includes('排序'),
      hasSearchPlaceholder: Boolean(document.querySelector('input[placeholder="搜索流程"]')),
      hasViewAction: text.includes('查看'),
      hasAnyTranslatedTargetName:
        text.includes('DCC 受控文件审批') ||
        text.includes('费用部门负责人审批') ||
        text.includes('eDHR 审批 V1'),
      rawEnglishNames: {
        dcc: text.includes('DCC Controlled File Approval'),
        expense: text.includes('Expense Dept Leader Approval'),
        edhr: text.includes('eDHR Approval V1')
      }
    }
  })
}

async function main() {
  if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
    throw new Error(`bpm_model_e2e_must_use_test_tenant_aoteman:${JSON.stringify(config)}`)
  }

  fs.mkdirSync(config.taskDir, { recursive: true })
  const browser = await chromium.launch({ headless: process.env.BPM_MODEL_E2E_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1680, height: 920 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page)
    await page.goto(`${config.baseUrl}${config.targetPath}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await page.getByRole('heading', { name: '流程模型' }).waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.unified-list-template[data-table-key="bpm.model.main"]').waitFor({
      state: 'visible',
      timeout: 60000
    })
    await page.locator('.bpm-model-unified-table').waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)
    await page.screenshot({ path: screenshots.page, fullPage: true })

    const metrics = await collectMetrics(page)
    assert.equal(metrics.unifiedTemplateCount, 1, `standard list template missing: ${JSON.stringify(metrics)}`)
    assert.equal(metrics.unifiedTableCount, 1, `single unified model table missing: ${JSON.stringify(metrics)}`)
    assert.equal(metrics.tableShellCount, 1, `model page must expose one table shell: ${JSON.stringify(metrics)}`)
    for (const label of ['流程名', '流程分类', '可见范围', '流程类型', '表单信息', '最后发布', '操作']) {
      assert.ok(metrics.headers.includes(label), `missing unified list header ${label}: ${JSON.stringify(metrics)}`)
    }
    assert.equal(metrics.hasWordPrintTemplate, false, 'word print template tag must be hidden')
    assert.equal(metrics.hasDisplayFieldControl, false, 'standard display field control must be hidden')
    assert.equal(metrics.hasColumnResetControl, false, 'column reset control must be hidden')
    assert.equal(metrics.hasResetAction, false, 'toolbar reset action must be hidden')
    assert.equal(metrics.hasCreateModelAction, true, 'create model action must remain visible')
    assert.equal(metrics.hasToolbarSettingsAction, false, 'settings dropdown beside create model must be hidden')
    assert.equal(metrics.hasCategorySortAction, false, 'old category sorting action must not remain on the model list')
    assert.equal(metrics.hasOldCategoryCards, false, 'old draggable category cards must not remain on the model list')
    assert.equal(metrics.hasSortAction, false, 'model sorting action must be hidden')
    assert.equal(metrics.hasSearchPlaceholder, true, 'standard quick filter must expose the process search input')
    assert.equal(metrics.hasViewAction, true, 'model row actions must expose the view action')
    assert.equal(metrics.hasAnyTranslatedTargetName, true, `expected at least one translated model name in list: ${JSON.stringify(metrics)}`)
    assert.deepEqual(
      metrics.rawEnglishNames,
      { dcc: false, expense: false, edhr: false },
      `English model names must not remain visible in the list: ${JSON.stringify(metrics)}`
    )

    await page.getByRole('button', { name: '查看' }).first().click()
    await page.getByText('查看流程模型').waitFor({ state: 'visible', timeout: 30000 })
    for (const label of ['谁发起', '谁审核', '谁审批']) {
      await page
        .locator('.el-dialog:visible .el-descriptions__label')
        .filter({ hasText: label })
        .first()
        .waitFor({ state: 'visible', timeout: 30000 })
    }
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' || ')}`)

    const result = {
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      targetPath: config.targetPath,
      metrics,
      screenshots
    }
    fs.writeFileSync(path.join(config.taskDir, 'bpm-model-unified-list-result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    process.stdout.write(`bpm model unified list real e2e passed\n${JSON.stringify(result, null, 2)}\n`)
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(1)
})
