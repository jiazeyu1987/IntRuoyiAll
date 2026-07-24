const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')
const runId =
  process.env.MES_CLICKABLE_RUN_ID ||
  `SMART-SCHED-CLICKABLE-${new Date()
    .toISOString()
    .replace(/[-:.TZ]/g, '')
    .slice(0, 14)}`

const DANGEROUS_WRITE_TEXT =
  /新增|创建|保存|提交|删除|审批|通过|驳回|导入|导出|同步|入池|排产|重排|应用|发布|确认|确定|锁定|解锁|模拟|推进|清空|重置场景|保存到本地|插入并重排|报结束/
const SAFE_TEXT =
  /搜索|查询|重置$|刷新|返回|本月|今日|关闭|取消|详情|查看|展开|收起|更多|上一页|下一页|上月|下月|读取/
const DANGEROUS_OPENERS =
  /新增|创建|删除|审批|导入|导出|自动排产|手动重排|甘特图编辑|待同步工单入池|选择归属|编辑|提交|详情|报工对比/
const WRITE_METHODS = new Set(['POST', 'PUT', 'DELETE', 'PATCH'])
const NON_MUTATING_POST_PATHS = [/\/admin-api\/mes\/pro\/auto-schedule\/dependencies(?:\?|$)/]

const PAGE_SPECS = [
  {
    id: 'PAGE-SWB',
    title: '排产员工作台',
    path: '/mes/pro/scheduler-workbench',
    role: 'planner',
    expectedText: '排产员工作台',
    contentSelector: '.scheduler-workbench'
  },
  {
    id: 'PAGE-SO',
    title: '排产工单',
    path: '/mes/pro/schedule-order',
    role: 'planner',
    expectedText: '排产工单',
    contentSelector: '.schedule-order-pool'
  },
  {
    id: 'PAGE-TASK',
    title: '生产排产',
    path: '/mes/pro/task',
    role: 'planner',
    expectedText: '生产排产',
    contentSelector: '.app-main'
  },
  {
    id: 'PAGE-CAL',
    title: '排程日历',
    path: '/mes/pro/schedule-calendar',
    role: 'planner',
    expectedText: '返回排产',
    contentSelector: '.schedule-calendar-page'
  },
  {
    id: 'PAGE-ROUTE',
    title: '工艺流程排产配置',
    path: '/mes/pro/route?tab=schedule-config',
    role: 'planner',
    expectedText: '工艺流程排产配置',
    contentSelector: '.route-flow-config-panel-page'
  },
  {
    id: 'PAGE-FDB',
    title: '报工',
    path: '/mes/pro/feedback',
    role: 'supervisor',
    expectedText: '正式报工',
    contentSelector: '.app-main'
  },
  {
    id: 'PAGE-PHS',
    title: '璞慧排产',
    path: '/mes/pro/puhui-schedule',
    role: 'supervisor',
    expectedText: '璞慧排产',
    contentSelector: '.app-main'
  },
  {
    id: 'PAGE-HOME',
    title: '排产看板',
    path: '/mes/home/index',
    role: 'planner',
    expectedText: '近 7 天',
    contentSelector: '.app-main'
  }
]

function optionalEnv(name, defaultValue) {
  const value = process.env[name]
  return value && value.trim() ? value.trim() : defaultValue
}

function assertLocalOnly(baseUrl) {
  const parsed = new URL(baseUrl)
  const allowedHosts = new Set(['localhost', '127.0.0.1', '::1', '[::1]'])
  assert.ok(
    allowedHosts.has(parsed.hostname),
    `MES_CLICKABLE_BASE_URL must be local, got ${baseUrl}`
  )
}

function loadConfig() {
  const baseUrl = optionalEnv('MES_CLICKABLE_BASE_URL', 'http://localhost:8081').replace(/\/$/, '')
  assertLocalOnly(baseUrl)
  const tenant = optionalEnv('MES_CLICKABLE_TENANT', '测试租户')
  assert.equal(tenant, '测试租户', 'clickable coverage must use 测试租户')
  const defaultPassword = optionalEnv('MES_CLICKABLE_DEFAULT_PASSWORD', '111111')
  return {
    baseUrl,
    tenant,
    headless: optionalEnv('MES_CLICKABLE_HEADLESS', '1') !== '0',
    artifactDir: path.resolve(
      optionalEnv(
        'MES_CLICKABLE_ARTIFACT_DIR',
        path.join(workspaceRoot, 'output', 'smart-scheduling-clickable', runId)
      )
    ),
    accounts: {
      planner: {
        tenant,
        username: optionalEnv('MES_CLICKABLE_PLANNER_USERNAME', 'showroomsupervisor'),
        password: optionalEnv('MES_CLICKABLE_PLANNER_PASSWORD', defaultPassword)
      },
      supervisor: {
        tenant,
        username: optionalEnv('MES_CLICKABLE_SUPERVISOR_USERNAME', 'edhrmatrixapprover'),
        password: optionalEnv('MES_CLICKABLE_SUPERVISOR_PASSWORD', defaultPassword)
      }
    }
  }
}

const config = loadConfig()

function ensureArtifactDir() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
}

function writeJsonArtifact(name, payload) {
  const filePath = path.join(config.artifactDir, name)
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return filePath
}

function normalizeText(value) {
  return String(value || '')
    .replace(/\s+/g, ' ')
    .trim()
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function isNonMutatingPost(response) {
  const request = response.request()
  if (request.method() !== 'POST') {
    return false
  }
  return NON_MUTATING_POST_PATHS.some((pattern) => pattern.test(response.url()))
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function selectTenant(page, loginForm, tenantName) {
  const tenantSelect = loginForm
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  await tenantSelect.waitFor({ state: 'visible', timeout: 30000 })
  const tenantResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/system/tenant/get-id-by-name') &&
        response.url().includes(encodeURIComponent(tenantName)) &&
        response.ok(),
      { timeout: 30000 }
    )
    .catch(() => null)
  await tenantSelect.click()
  await tenantSelect.fill('')
  await tenantSelect.fill(tenantName)
  await tenantSelect.press('Enter')
  await tenantResponsePromise
  const matchedOption = page
    .locator('.el-select-dropdown__item')
    .filter({ hasText: tenantName })
    .first()
  if ((await matchedOption.count()) > 0) {
    await matchedOption.click({ timeout: 3000 }).catch(() => null)
  }
}

async function login(page, account, targetPath) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (page.url().includes('/login')) {
    const loginForm = page.locator('.login-form:visible').first()
    await loginForm.waitFor({ state: 'visible', timeout: 60000 })
    if ((await loginForm.locator('.verify-img-panel, .verify-bar-area').count()) > 0) {
      throw new Error('Captcha is enabled; clickable coverage cannot continue unattended.')
    }
    await selectTenant(page, loginForm, account.tenant)
    await fillFirstVisible(
      loginForm.locator('input[placeholder="请输入用户名"]'),
      account.username,
      'username'
    )
    await fillFirstVisible(
      loginForm.locator('input[placeholder="请输入密码"]'),
      account.password,
      'password'
    )
    await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
        { timeout: 60000 }
      ),
      loginForm.locator('.el-button--primary').first().click()
    ])
  }

  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await openAppPath(page, targetPath)
}

async function openAppPath(page, routePath) {
  await page.goto(`${config.baseUrl}${routePath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
}

async function waitForExpectedText(page, text, pageTitle) {
  await page
    .getByText(text, { exact: false })
    .first()
    .waitFor({ state: 'visible', timeout: 60000 })
    .catch((error) => {
      throw new Error(`Expected text "${text}" not visible on ${pageTitle}: ${error.message}`)
    })
}

async function collectClickableControls(page, contentSelector) {
  const raw = await page.evaluate((scopeSelector) => {
    const selector = [
      'button',
      'a[href]',
      '[role="button"]',
      '.el-tabs__item',
      '.el-link',
      '.el-dropdown',
      '.el-switch',
      '.el-checkbox',
      '.el-radio',
      '.el-pagination button',
      'summary',
      '[data-date]',
      '[data-state]',
      '.scheduler-workbench__metric-button',
      '.scheduler-workbench__rehearsal-link',
      '.calendar-day',
      '[class*="cursor-pointer"]'
    ].join(',')

    const normalize = (value) =>
      String(value || '')
        .replace(/\s+/g, ' ')
        .trim()
    const isVisible = (element) => {
      if (typeof element.checkVisibility === 'function' && !element.checkVisibility()) {
        return false
      }
      const style = window.getComputedStyle(element)
      const rect = element.getBoundingClientRect()
      return (
        style &&
        style.visibility !== 'hidden' &&
        style.display !== 'none' &&
        Number(rect.width) > 0 &&
        Number(rect.height) > 0
      )
    }
    const textOf = (element, index) => {
      const explicit =
        element.getAttribute('aria-label') ||
        element.getAttribute('title') ||
        element.getAttribute('data-date') ||
        element.getAttribute('data-state') ||
        element.getAttribute('value') ||
        ''
      const text = normalize(element.innerText || element.textContent || explicit)
      if (text) {
        return text.slice(0, 160)
      }
      const href = element.getAttribute('href')
      if (href) {
        return href
      }
      return `${element.tagName.toLowerCase()}#${index}`
    }
    const disabledOf = (element) =>
      element.disabled === true ||
      element.getAttribute('aria-disabled') === 'true' ||
      element.classList.contains('is-disabled') ||
      element.classList.contains('disabled')

    const seen = new Set()
    const root =
      document.querySelector(scopeSelector) || document.querySelector('.app-main') || document.body
    return Array.from(root.querySelectorAll(selector))
      .filter((element) => {
        if (!isVisible(element)) {
          return false
        }
        if (seen.has(element)) {
          return false
        }
        seen.add(element)
        return true
      })
      .map((element, index) => {
        const rect = element.getBoundingClientRect()
        const tagName = element.tagName.toLowerCase()
        const role = element.getAttribute('role') || ''
        const href = element.getAttribute('href') || ''
        const text = textOf(element, index)
        const classes = Array.from(element.classList || [])
          .slice(0, 8)
          .join('.')
        const matchKey = [tagName, role, href, text, classes].join('|')
        return {
          index,
          tagName,
          role,
          href,
          text,
          classes,
          matchKey,
          disabled: disabledOf(element),
          rect: {
            x: Math.round(rect.x),
            y: Math.round(rect.y),
            width: Math.round(rect.width),
            height: Math.round(rect.height)
          }
        }
      })
  }, contentSelector || '.app-main')

  const occurrences = new Map()
  return raw.map((control) => {
    const occurrence = occurrences.get(control.matchKey) || 0
    occurrences.set(control.matchKey, occurrence + 1)
    return {
      ...control,
      occurrence,
      category: classifyControl(control)
    }
  })
}

function classifyControl(control) {
  const text = normalizeText(control.text)
  if (control.disabled) {
    return 'disabled'
  }
  if (control.role === 'tab' || control.classes.includes('el-tabs__item')) {
    return 'safe'
  }
  if (control.tagName === 'a' && control.href) {
    return 'safe'
  }
  if (SAFE_TEXT.test(text)) {
    return 'safe'
  }
  if (DANGEROUS_WRITE_TEXT.test(text)) {
    return 'dangerous'
  }
  return 'exploratory'
}

async function markControl(page, control, contentSelector) {
  return page.evaluate(
    ({ target, scopeSelector }) => {
      const selector = [
        'button',
        'a[href]',
        '[role="button"]',
        '.el-tabs__item',
        '.el-link',
        '.el-dropdown',
        '.el-switch',
        '.el-checkbox',
        '.el-radio',
        '.el-pagination button',
        'summary',
        '[data-date]',
        '[data-state]',
        '.scheduler-workbench__metric-button',
        '.scheduler-workbench__rehearsal-link',
        '.calendar-day',
        '[class*="cursor-pointer"]'
      ].join(',')
      const normalize = (value) =>
        String(value || '')
          .replace(/\s+/g, ' ')
          .trim()
      const isVisible = (element) => {
        if (typeof element.checkVisibility === 'function' && !element.checkVisibility()) {
          return false
        }
        const style = window.getComputedStyle(element)
        const rect = element.getBoundingClientRect()
        return (
          style &&
          style.visibility !== 'hidden' &&
          style.display !== 'none' &&
          Number(rect.width) > 0 &&
          Number(rect.height) > 0
        )
      }
      const textOf = (element, index) => {
        const explicit =
          element.getAttribute('aria-label') ||
          element.getAttribute('title') ||
          element.getAttribute('data-date') ||
          element.getAttribute('data-state') ||
          element.getAttribute('value') ||
          ''
        const text = normalize(element.innerText || element.textContent || explicit)
        if (text) {
          return text.slice(0, 160)
        }
        const href = element.getAttribute('href')
        if (href) {
          return href
        }
        return `${element.tagName.toLowerCase()}#${index}`
      }
      const root =
        document.querySelector(scopeSelector) ||
        document.querySelector('.app-main') ||
        document.body
      const candidates = Array.from(root.querySelectorAll(selector)).filter(isVisible)
      let occurrence = 0
      for (let index = 0; index < candidates.length; index += 1) {
        const element = candidates[index]
        const tagName = element.tagName.toLowerCase()
        const role = element.getAttribute('role') || ''
        const href = element.getAttribute('href') || ''
        const text = textOf(element, index)
        const classes = Array.from(element.classList || [])
          .slice(0, 8)
          .join('.')
        const matchKey = [tagName, role, href, text, classes].join('|')
        if (matchKey === target.matchKey) {
          if (occurrence === target.occurrence) {
            document
              .querySelectorAll('[data-smart-scheduling-clickable-target]')
              .forEach((item) => {
                item.removeAttribute('data-smart-scheduling-clickable-target')
              })
            element.setAttribute('data-smart-scheduling-clickable-target', '1')
            return {
              text,
              tagName,
              role,
              href,
              classes
            }
          }
          occurrence += 1
        }
      }
      return null
    },
    { target: control, scopeSelector: contentSelector || '.app-main' }
  )
}

async function clickLastVisible(locator) {
  const count = await locator.count()
  for (let index = count - 1; index >= 0; index -= 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.click({ timeout: 3000 }).catch(() => null)
      return true
    }
  }
  return false
}

async function cancelOpenOverlays(page) {
  let actionCount = 0
  for (let pass = 0; pass < 3; pass += 1) {
    const clickedCancel = await clickLastVisible(page.getByRole('button', { name: /取消|关闭/ }))
    if (clickedCancel) {
      actionCount += 1
      await settle(page)
      continue
    }
    const clickedClose = await clickLastVisible(
      page.locator('.el-dialog__headerbtn, .el-message-box__headerbtn')
    )
    if (clickedClose) {
      actionCount += 1
      await settle(page)
      continue
    }
    await page.keyboard.press('Escape').catch(() => null)
    await page.waitForTimeout(200)
  }
  return actionCount
}

async function clickControl(page, pageSpec, control) {
  await openAppPath(page, pageSpec.path)
  await waitForExpectedText(page, pageSpec.expectedText, pageSpec.title)
  const marked = await markControl(page, control, pageSpec.contentSelector)
  if (!marked) {
    return {
      status: 'MISSING_AFTER_RELOAD',
      action: 'not-clicked',
      reason: 'control signature was not found after page reload'
    }
  }

  const text = normalizeText(control.text)
  if (control.category === 'disabled') {
    return {
      status: 'SKIPPED_DISABLED',
      action: 'not-clicked'
    }
  }
  const shouldClickDanger = control.category === 'dangerous' && DANGEROUS_OPENERS.test(text)
  if (control.category === 'dangerous' && !shouldClickDanger) {
    return {
      status: 'COVERED_BY_SMOKE_OR_CONFIRMATION_GATE',
      action: 'classified-only',
      reason:
        'write confirmation action must be covered by the dedicated full smoke or a seeded scenario'
    }
  }

  const writeResponses = []
  const onResponse = (response) => {
    const request = response.request()
    if (
      WRITE_METHODS.has(request.method()) &&
      response.url().includes('/admin-api/') &&
      !isNonMutatingPost(response)
    ) {
      writeResponses.push({
        method: request.method(),
        url: response.url(),
        status: response.status()
      })
    }
  }
  page.on('response', onResponse)
  try {
    await page.locator('[data-smart-scheduling-clickable-target="1"]').click({ timeout: 6000 })
    await settle(page)
    const overlayCancelCount = await cancelOpenOverlays(page)
    return {
      status:
        writeResponses.length > 0 && control.category !== 'dangerous'
          ? 'WRITE_RESPONSE_FROM_SAFE_CLICK'
          : 'PASS',
      action: control.category === 'dangerous' ? 'clicked-and-cancelled' : 'clicked',
      currentUrl: page.url(),
      overlayCancelCount,
      writeResponses
    }
  } catch (error) {
    return {
      status: 'FAIL',
      action: 'click-failed',
      error: error.message
    }
  } finally {
    page.off('response', onResponse)
  }
}

function selectRepresentativeControls(controls, maxPerPage) {
  const seen = new Set()
  const unique = []
  for (const control of controls) {
    const key = `${control.category}::${control.matchKey}`
    if (seen.has(key)) {
      continue
    }
    seen.add(key)
    unique.push(control)
  }
  const priority = { safe: 0, exploratory: 1, dangerous: 2, disabled: 3 }
  unique.sort((left, right) => {
    const categoryDelta = (priority[left.category] ?? 9) - (priority[right.category] ?? 9)
    if (categoryDelta !== 0) {
      return categoryDelta
    }
    return left.index - right.index
  })
  return unique.slice(0, maxPerPage)
}

async function coverPage(page, pageSpec) {
  await openAppPath(page, pageSpec.path)
  await waitForExpectedText(page, pageSpec.expectedText, pageSpec.title)
  const screenshotPath = path.join(config.artifactDir, `${pageSpec.id}.png`)
  await page.screenshot({ path: screenshotPath, fullPage: true })
  const controls = await collectClickableControls(page, pageSpec.contentSelector)
  assert.ok(controls.length > 0, `${pageSpec.id} ${pageSpec.title} must expose clickable controls`)

  const maxPerPage = Number(optionalEnv('MES_CLICKABLE_MAX_PER_PAGE', '25'))
  const representativeControls = selectRepresentativeControls(controls, maxPerPage)
  const results = []
  for (const control of representativeControls) {
    const result = await clickControl(page, pageSpec, control)
    results.push({
      ...control,
      result
    })
  }

  const categoryCounts = results.reduce((acc, item) => {
    acc[item.category] = (acc[item.category] || 0) + 1
    return acc
  }, {})
  const failed = results.filter((item) =>
    ['FAIL', 'MISSING_AFTER_RELOAD', 'WRITE_RESPONSE_FROM_SAFE_CLICK'].includes(item.result.status)
  )
  return {
    pageId: pageSpec.id,
    title: pageSpec.title,
    path: pageSpec.path,
    role: pageSpec.role,
    screenshotPath,
    controlCount: controls.length,
    representativeControlCount: representativeControls.length,
    coveredCount: results.length,
    skippedControlCount: Math.max(0, controls.length - representativeControls.length),
    categoryCounts,
    failedCount: failed.length,
    failed,
    results
  }
}

async function main() {
  ensureArtifactDir()
  const browser = await chromium.launch({ headless: config.headless })
  const contexts = []
  try {
    const pagesByRole = {}
    for (const roleName of ['planner', 'supervisor']) {
      const context = await browser.newContext({
        locale: 'zh-CN',
        viewport: { width: 1600, height: 1100 },
        acceptDownloads: true
      })
      contexts.push(context)
      const page = await context.newPage()
      await login(page, config.accounts[roleName], '/index')
      pagesByRole[roleName] = page
    }

    const pageReports = []
    for (const pageSpec of PAGE_SPECS) {
      const report = await coverPage(pagesByRole[pageSpec.role], pageSpec)
      pageReports.push(report)
    }

    const failures = pageReports.flatMap((pageReport) =>
      pageReport.failed.map((failure) => ({
        pageId: pageReport.pageId,
        pageTitle: pageReport.title,
        text: failure.text,
        category: failure.category,
        status: failure.result.status,
        error: failure.result.error || failure.result.reason || ''
      }))
    )
    const report = {
      status: failures.length > 0 ? 'FAIL' : 'PASS',
      runId,
      generatedAt: new Date().toISOString(),
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      pages: pageReports,
      failures,
      note: 'Dangerous write entries are either opened and cancelled or classified for coverage by the full smart scheduling smoke.'
    }
    const reportPath = writeJsonArtifact('smart-scheduling-clickable-coverage-report.json', report)
    if (failures.length > 0) {
      throw new Error(`smart scheduling clickable coverage failed; report=${reportPath}`)
    }
    console.log(`PASS: smart scheduling clickable coverage ${runId}; report=${reportPath}`)
  } finally {
    for (const context of contexts.reverse()) {
      await context.close().catch(() => null)
    }
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exit(1)
})
