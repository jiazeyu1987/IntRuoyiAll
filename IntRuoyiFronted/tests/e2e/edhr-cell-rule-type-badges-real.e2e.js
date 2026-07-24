const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.EDHR_CELL_BADGE_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.EDHR_CELL_BADGE_TENANT || '测试租户',
  username: process.env.EDHR_CELL_BADGE_USERNAME || 'aoteman',
  password: process.env.EDHR_CELL_BADGE_PASSWORD || '111111',
  targetPath: '/mes/pro/batch-record-form-list',
  taskDir:
    process.env.EDHR_CELL_BADGE_TASK_DIR ||
    path.resolve(__dirname, '..', '..', '..', 'doc/tasks/20260718-cell-rule-type-badges/e2e-artifacts')
}

const screenshots = {
  page: path.join(config.taskDir, 'cell-rule-type-badges-real.png'),
  failure: path.join(config.taskDir, 'cell-rule-type-badges-real-failure.png'),
  loginFailed: path.join(config.taskDir, 'cell-rule-type-badges-login-failed.png')
}

function assertLocalOnly() {
  assert.equal(config.baseUrl, 'http://localhost:8081', '单元格规则类型徽标 E2E 必须固定使用本机前端')
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

async function openFirstReportSimulatePage(page) {
  await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('.batch-record-form-layout').waitFor({ state: 'visible', timeout: 60000 })
  await settle(page)

  let previewActions = page.locator('.batch-record-form-preview__actions').first()
  if (!(await previewActions.count())) {
    const firstRow = page.locator('.batch-record-form-layout__list .el-table__body-wrapper tbody tr').first()
    await firstRow.waitFor({ state: 'visible', timeout: 60000 })
    await firstRow.click()
    previewActions = page.locator('.batch-record-form-preview__actions').first()
  }

  await previewActions.waitFor({ state: 'visible', timeout: 60000 })
  const simulateButton = previewActions.getByRole('button', { name: '填写' }).first()
  await simulateButton.waitFor({ state: 'visible', timeout: 60000 })
  await Promise.all([
    page.waitForURL((url) => url.pathname.includes('/template-simulate'), { timeout: 60000 }),
    simulateButton.click()
  ])
}

async function collectBadgeMetrics(page) {
  return page.evaluate(() => {
    const visibleText = (element) => (element.textContent || '').replace(/\s+/g, ' ').trim()
    const badges = Array.from(
      document.querySelectorAll('.edhr-template-editable-form__sheet .edhr-template-editable-form__rule-type-badge')
    ).map((badge) => {
      const cell = badge.closest('td')
      const cellStyle = cell ? window.getComputedStyle(cell) : null
      return {
        symbol: visibleText(badge),
        title: badge.getAttribute('title') || badge.getAttribute('aria-label') || '',
        badgeClass: badge.className,
        cellClass: cell?.className || '',
        background: cellStyle?.backgroundColor || '',
        borderColor: cellStyle?.borderColor || ''
      }
    })
    const legend = Array.from(
      document.querySelectorAll('.edhr-template-editable-form__rule-legend-item')
    ).map((item) => visibleText(item))
    const states = Array.from(document.querySelectorAll('.edhr-template-editable-form__rule-state')).map((item) =>
      visibleText(item)
    )
    return {
      url: location.href,
      legend,
      states,
      badges,
      bodyText: (document.body.innerText || '').replace(/\s+/g, ' ').slice(0, 1200)
    }
  })
}

function summarizeMetrics(metrics) {
  const symbolCounts = metrics.badges.reduce((result, badge) => {
    result[badge.symbol] = (result[badge.symbol] || 0) + 1
    return result
  }, {})
  return {
    url: metrics.url,
    legend: metrics.legend,
    states: metrics.states,
    badgeCount: metrics.badges.length,
    symbolCounts,
    sampleBadges: metrics.badges.slice(0, 16),
    bodyText: metrics.bodyText
  }
}

async function main() {
  assertLocalOnly()
  fs.mkdirSync(config.taskDir, { recursive: true })
  const launchOptions = {
    headless: process.env.EDHR_CELL_BADGE_HEADED !== '1'
  }
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
    await openFirstReportSimulatePage(page)
    await page.locator('.edhr-batch-template-simulate__section-title', { hasText: '模板内填写' }).waitFor({
      state: 'visible',
      timeout: 120000
    })
    const visibleErrors = await page.locator('.edhr-batch-template-simulate .el-alert--error:visible').allTextContents()
    assert.deepEqual(visibleErrors, [], `模拟填写页不得出现加载错误：${visibleErrors.join(' | ')}`)

    await page.locator('.edhr-template-editable-form__rule-legend').waitFor({ state: 'visible', timeout: 120000 })
    await page.locator('.edhr-template-editable-form__sheet .edhr-template-editable-form__rule-type-badge').first().waitFor({
      state: 'visible',
      timeout: 120000
    })
    await page.screenshot({ path: screenshots.page, fullPage: true })

    const metrics = await collectBadgeMetrics(page)
    for (const label of ['文本', '数字', '日期', '日期时间', '勾选', '签名', '附件']) {
      assert.ok(metrics.legend.some((item) => item.includes(label)), `图例必须包含 ${label}：${JSON.stringify(metrics.legend)}`)
    }
    assert.ok(metrics.states.includes('自动待确认'), `图例必须说明自动待确认状态：${JSON.stringify(metrics.states)}`)
    assert.ok(metrics.states.includes('已确认'), `图例必须说明已确认状态：${JSON.stringify(metrics.states)}`)
    assert.ok(metrics.badges.length > 0, '真实模板内必须渲染至少一个单元格类型徽标')
    assert.ok(
      metrics.badges.every((badge) => badge.title.includes(' / ')),
      `每个徽标必须有可理解 tooltip：${JSON.stringify(metrics.badges.slice(0, 5))}`
    )
    assert.ok(
      metrics.badges.some((badge) => /is-rule-auto|is-rule-reviewed|is-rule-manual/.test(badge.cellClass)),
      `规则单元格必须具备状态 class：${JSON.stringify(metrics.badges.slice(0, 5))}`
    )
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' || ')}`)

    const result = {
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      metrics: summarizeMetrics(metrics),
      screenshots
    }
    fs.writeFileSync(path.join(config.taskDir, 'cell-rule-type-badges-real-result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    process.stdout.write(`edhr cell rule type badges real e2e passed\n${JSON.stringify(result, null, 2)}\n`)
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
