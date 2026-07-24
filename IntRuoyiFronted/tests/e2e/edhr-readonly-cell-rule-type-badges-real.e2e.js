const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.EDHR_READONLY_BADGE_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.EDHR_READONLY_BADGE_TENANT || '测试租户',
  username: process.env.EDHR_READONLY_BADGE_USERNAME || 'aoteman',
  password: process.env.EDHR_READONLY_BADGE_PASSWORD || '111111',
  targetPath: '/mes/pro/batch-record-form-list',
  taskDir:
    process.env.EDHR_READONLY_BADGE_TASK_DIR ||
    path.resolve(__dirname, '..', '..', '..', 'doc/tasks/20260718-readonly-cell-rule-type-badges-fix/e2e-artifacts')
}

const screenshots = {
  page: path.join(config.taskDir, 'readonly-cell-rule-type-badges-real.png'),
  failure: path.join(config.taskDir, 'readonly-cell-rule-type-badges-real-failure.png'),
  loginFailed: path.join(config.taskDir, 'readonly-cell-rule-type-badges-login-failed.png')
}

function assertLocalOnly() {
  assert.equal(config.baseUrl, 'http://localhost:8081', '只读预览规则徽标 E2E 必须固定使用本机前端')
  assert.equal(config.tenant, '测试租户', '真实 E2E 必须使用测试租户')
  assert.equal(config.username, 'aoteman', '真实 E2E 必须使用测试租户 aoteman')
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
  if ((await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await form.locator('input[placeholder="请输入租户名称"]').first().fill(config.tenant)
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

async function collectReadonlyPreviewMetrics(page) {
  return page.evaluate(() => {
    const preview = document.querySelector('.batch-record-form-preview')
    const visibleText = (element) => (element.textContent || '').replace(/\s+/g, ' ').trim()
    const placeholders = Array.from(
      preview?.querySelectorAll('.edhr-template-sheet__fillable-placeholder') || []
    ).map((item) => {
      const cell = item.closest('td')
      return {
        text: visibleText(item),
        title: item.getAttribute('title') || item.getAttribute('aria-label') || '',
        className: item.className,
        cellClass: cell?.className || ''
      }
    })
    const badges = Array.from(preview?.querySelectorAll('.edhr-template-sheet__rule-type-badge') || []).map((item) => {
      const cell = item.closest('td')
      return {
        text: visibleText(item),
        cellText: visibleText(cell),
        title: item.getAttribute('title') || item.getAttribute('aria-label') || '',
        className: item.className,
        cellClass: cell?.className || ''
      }
    })
    return {
      reportName: visibleText(preview?.querySelector('.batch-record-form-preview__title') || document.body),
      placeholders,
      badges,
      bodyText: visibleText(preview || document.body).slice(0, 1600)
    }
  })
}

async function openPreviewWithDateAndNumberBadges(page) {
  await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('.batch-record-form-layout').waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)

  const quickFilter = page.locator('.table-quick-filter').first()
  if ((await quickFilter.count()) && (await quickFilter.isVisible())) {
    await quickFilter.locator('.table-quick-filter__field').click()
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '表单名称' }).first().click()
    await quickFilter.locator('.table-quick-filter__operator').click()
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '包含' }).first().click()
    await quickFilter.locator('.table-quick-filter__value input:visible').fill('过程检验单')
    const responsePromise = page
      .waitForResponse(
        (response) =>
          response.url().includes('/admin-api/mes/pro/batch-record-report/page') &&
          response.status() === 200,
        { timeout: 60000 }
      )
      .catch(() => null)
    await quickFilter.getByRole('button', { name: /查询/ }).click()
    await responsePromise
    await settle(page)
  }

  const rows = page.locator('.batch-record-form-layout__list .el-table__body-wrapper tbody tr')
  const rowCount = await rows.count()
  assert.ok(rowCount > 0, '批记录表单列表必须至少有一条真实数据')

  for (let index = 0; index < Math.min(rowCount, 12); index += 1) {
    await rows.nth(index).click()
    await page.locator('.batch-record-form-preview .edhr-template-sheet').waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)
    const metrics = await collectReadonlyPreviewMetrics(page)
    const texts = [...metrics.placeholders, ...metrics.badges].map((item) => item.text)
    if (texts.includes('日') && texts.includes('#')) {
      return metrics
    }
  }

  const finalMetrics = await collectReadonlyPreviewMetrics(page)
  throw new Error(`前 12 条表单预览中未找到日期和数字类型符号：${JSON.stringify(finalMetrics, null, 2)}`)
}

function summarizeMetrics(metrics) {
  return {
    reportName: metrics.reportName,
    placeholderCounts: metrics.placeholders.reduce((result, item) => {
      result[item.text] = (result[item.text] || 0) + 1
      return result
    }, {}),
    samplePlaceholders: metrics.placeholders.slice(0, 24),
    sampleBadges: metrics.badges.slice(0, 12),
    bodyText: metrics.bodyText
  }
}

async function main() {
  assertLocalOnly()
  fs.mkdirSync(config.taskDir, { recursive: true })
  const launchOptions = { headless: process.env.EDHR_READONLY_BADGE_HEADED !== '1' }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({ viewport: { width: 1680, height: 940 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page)
    const metrics = await openPreviewWithDateAndNumberBadges(page)
    const typeMarks = [...metrics.placeholders, ...metrics.badges]
    assert.ok(typeMarks.some((item) => item.text === '日' && item.title.includes('日期')), '只读预览日期格必须显示日期符号和 tooltip')
    assert.ok(typeMarks.some((item) => item.text === '#' && item.title.includes('数字')), '只读预览数字格必须显示数字符号和 tooltip')
    assert.equal(
      metrics.badges.filter((item) => item.cellText.includes('?')).length,
      0,
      '带规则徽标的只读填写格不能再残留通用问号占位'
    )
    assert.ok(
      typeMarks.some((item) => /is-rule-auto|is-rule-reviewed|is-rule-manual/.test(item.cellClass)),
      '只读预览规则格必须具备状态 class'
    )
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' || ')}`)
    await page.screenshot({ path: screenshots.page, fullPage: true })

    const result = {
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      metrics: summarizeMetrics(metrics),
      screenshots
    }
    fs.writeFileSync(
      path.join(config.taskDir, 'readonly-cell-rule-type-badges-real-result.json'),
      `${JSON.stringify(result, null, 2)}\n`,
      'utf8'
    )
    process.stdout.write(`edhr readonly cell rule type badges real e2e passed\n${JSON.stringify(result, null, 2)}\n`)
  } catch (error) {
    await page.screenshot({ path: screenshots.failure, fullPage: true }).catch(() => null)
    throw error
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(1)
})
