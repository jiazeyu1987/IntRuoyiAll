const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.APPROVAL_TODO_BADGE_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.APPROVAL_TODO_BADGE_TENANT || '测试租户',
  username: process.env.APPROVAL_TODO_BADGE_USERNAME || 'aoteman',
  password: process.env.APPROVAL_TODO_BADGE_PASSWORD || '111111',
  targetPath: '/approval-center/todo',
  managerTabs: [
    { path: '/approval-center/manager/model', title: '流程模型' },
    { path: '/approval-center/manager/form', title: '流程表单' },
    { path: '/approval-center/manager/category', title: '流程分类' },
    { path: '/approval-center/manager/user-group', title: '用户分组' },
    { path: '/approval-center/manager/process-expression', title: '流程表达式' }
  ],
  taskDir:
    process.env.APPROVAL_TODO_BADGE_TASK_DIR ||
    path.resolve(__dirname, '..', '..', '..', 'doc/tasks/20260715-approval-badge-tags-todo-only/e2e-artifacts')
}

const screenshots = {
  page: path.join(config.taskDir, 'approval-todo-badge.png'),
  failure: path.join(config.taskDir, 'approval-todo-badge-failed.png')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(800)
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
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
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
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(
    loginPayload && (loginPayload.code === 0 || loginPayload.code === 200),
    `login failed: ${JSON.stringify(loginPayload)}`
  )
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

function extractPageData(payload) {
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `api payload failed: ${JSON.stringify(payload)}`)
  assert.ok(payload.data && typeof payload.data.total === 'number', `todo page payload missing total: ${JSON.stringify(payload)}`)
  return payload.data
}

function isTodoPageResponse(response) {
  if (!response.url().includes('/admin-api/approval-center/tasks/page') || response.request().method() !== 'GET') {
    return false
  }
  const current = new URL(response.url())
  return current.searchParams.get('viewType') === 'TODO'
}

function isTodoBadgeCountResponse(response) {
  if (!isTodoPageResponse(response)) {
    return false
  }
  const current = new URL(response.url())
  return current.searchParams.get('pageNo') === '1' && current.searchParams.get('pageSize') === '10'
}

async function collectBadgeMetrics(page) {
  return page.evaluate(() => {
    const isVisible = (element) => {
      if (!(element instanceof HTMLElement)) return false
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
    }
    const resolveContext = (element) =>
      element.closest('[class*="tab-menu__item"]') ||
      element.closest('.el-menu-item') ||
      element.closest('.el-sub-menu__title') ||
      element.closest('[class*="tags-view__item"]') ||
      element.parentElement
    const rectMetric = (element) => {
      if (!(element instanceof HTMLElement)) return null
      const rect = element.getBoundingClientRect()
      return {
        left: Number(rect.left.toFixed(1)),
        right: Number(rect.right.toFixed(1)),
        width: Number(rect.width.toFixed(1))
      }
    }
    const contextText = (context) => {
      return (context?.textContent || '').replace(/\s+/g, ' ').trim()
    }

    return Array.from(document.querySelectorAll('.approval-todo-badge'))
      .filter(isVisible)
      .map((badge) => {
        const context = resolveContext(badge)
        const inlineGroup = badge.closest('.approval-menu-title')
        const inlineTitle = inlineGroup?.querySelector('.approval-menu-title__text')
        const badgeRect = rectMetric(badge)
        const titleRect = rectMetric(inlineTitle)
        const contextRect = rectMetric(context)
        return {
          text: (badge.textContent || '').replace(/\s+/g, ' ').trim(),
          ariaLabel: badge.getAttribute('aria-label') || '',
          context: contextText(context),
          badgeWidthPx: badgeRect?.width ?? null,
          inlineGroup: Boolean(inlineGroup),
          inlineGapPx:
            badgeRect && titleRect ? Number((badgeRect.left - titleRect.right).toFixed(1)) : null,
          contextRightGapPx:
            badgeRect && contextRect ? Number((contextRect.right - badgeRect.right).toFixed(1)) : null
        }
      })
  })
}

async function collectTagsViewItems(page) {
  return page.evaluate(() =>
    Array.from(document.querySelectorAll('[class*="tags-view__item"]')).map((item) => {
      const badge = item.querySelector('.approval-todo-badge')
      return {
        text: (item.textContent || '').replace(/\s+/g, ' ').trim(),
        badgeText: (badge?.textContent || '').replace(/\s+/g, ' ').trim()
      }
    })
  )
}

async function main() {
  if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
    throw new Error(`approval_todo_badge_e2e_must_use_test_tenant_aoteman:${JSON.stringify(config)}`)
  }

  fs.mkdirSync(config.taskDir, { recursive: true })
  const launchOptions = { headless: process.env.APPROVAL_TODO_BADGE_HEADED !== '1' }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({ viewport: { width: 1680, height: 920 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  const todoBadgeCountPayloads = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('response', async (response) => {
    if (!isTodoBadgeCountResponse(response)) {
      return
    }
    const payload = await response.json().catch(() => null)
    if (payload) {
      todoBadgeCountPayloads.push(payload)
    }
  })

  try {
    await login(page)
    await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByRole('heading', { name: '审批中心' }).waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.approval-center__table').waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.approval-todo-badge').first().waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)

    const todoPageData = extractPageData(todoBadgeCountPayloads.at(-1))
    const expectedText = String(todoPageData.total)
    const badges = await collectBadgeMetrics(page)
    await page.screenshot({ path: screenshots.page, fullPage: true })

    assert.ok(badges.length >= 2, `expected approval center and todo badges: ${JSON.stringify(badges)}`)
    assert.ok(
      badges.some((badge) => badge.context.includes('审批中心') && badge.text === expectedText),
      `approval center tab must show todo total ${expectedText}: ${JSON.stringify(badges)}`
    )
    assert.ok(
      badges.some((badge) => badge.context.includes('待办') && badge.text === expectedText),
      `todo tab must show todo total ${expectedText}: ${JSON.stringify(badges)}`
    )
    const inlineMenuBadge = badges.find(
      (badge) => badge.inlineGroup && badge.context.includes('审批中心') && badge.text === expectedText
    )
    assert.ok(
      inlineMenuBadge,
      `approval center left menu badge must be rendered in the title inline group: ${JSON.stringify(badges)}`
    )
    assert.ok(
      inlineMenuBadge.inlineGapPx >= 0 && inlineMenuBadge.inlineGapPx <= 8,
      `approval center left menu badge must sit next to the title text: ${JSON.stringify(inlineMenuBadge)}`
    )
    assert.ok(
      inlineMenuBadge.contextRightGapPx >= 24,
      `approval center left menu badge must not stay pinned to the far right edge: ${JSON.stringify(inlineMenuBadge)}`
    )
    if (expectedText.length >= 2) {
      for (const badge of badges.filter((item) => item.text === expectedText)) {
        assert.ok(
          badge.badgeWidthPx >= 24,
          `two-digit approval todo badge must have enough visible width: ${JSON.stringify(badge)}`
        )
      }
    }
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' || ')}`)

    const managerTagChecks = []
    for (const managerTab of config.managerTabs) {
      await page.goto(`${config.baseUrl}${managerTab.path}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
      await page.locator('#v-tags-view').getByText(managerTab.title, { exact: false }).waitFor({
        state: 'visible',
        timeout: 60000
      })
      await settle(page)
      const managerTagsViewItems = await collectTagsViewItems(page)
      const tag = managerTagsViewItems.find((item) => item.text.includes(managerTab.title))
      assert.ok(
        tag,
        `manager tab must exist in top tags view: ${managerTab.title}, ${JSON.stringify(managerTagsViewItems)}`
      )
      assert.equal(
        tag.badgeText,
        '',
        `manager tab must not show approval todo badge: ${managerTab.title}, ${JSON.stringify(managerTagsViewItems)}`
      )
      managerTagChecks.push({
        title: managerTab.title,
        path: managerTab.path,
        badgeText: tag.badgeText,
        tagsViewItems: managerTagsViewItems
      })
    }

    await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByRole('heading', { name: '审批中心' }).waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)
    const tagsViewItems = await collectTagsViewItems(page)
    assert.ok(
      tagsViewItems.some((item) => item.text.includes('审批中心') && item.badgeText === expectedText),
      `todo tags view item must keep the approval todo badge: ${JSON.stringify(tagsViewItems)}`
    )

    const result = {
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      targetPath: config.targetPath,
      todoTotal: todoPageData.total,
      badges,
      managerTagChecks,
      tagsViewItems,
      screenshots
    }
    fs.writeFileSync(path.join(config.taskDir, 'approval-todo-badge-result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    process.stdout.write(`PASS: approval todo badge real e2e\n${JSON.stringify(result, null, 2)}\n`)
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
