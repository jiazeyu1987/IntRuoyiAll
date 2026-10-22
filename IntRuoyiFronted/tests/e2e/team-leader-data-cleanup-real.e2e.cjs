const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const FRONTEND_URL = process.env.TEAM_LEADER_CLEANUP_E2E_FRONTEND_URL || 'http://127.0.0.1:8081'
const TENANT_NAME = process.env.TEAM_LEADER_CLEANUP_E2E_TENANT || '芋道源码'
const TENANT_ID = Number(process.env.TEAM_LEADER_CLEANUP_E2E_TENANT_ID || '1')
const USERNAME = process.env.TEAM_LEADER_CLEANUP_E2E_USERNAME || 'admin'
const PASSWORD = process.env.TEAM_LEADER_CLEANUP_E2E_PASSWORD || 'admin123'

function runDbQuery(sql) {
  const script = [
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 -N -B ruoyi-vue-pro <<\'SQL\'',
    sql.trim(),
    'SQL'
  ].join('\n')
  return execFileSync('docker', ['exec', 'int-ruoyi-mysql', 'sh', '-lc', script], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe']
  }).trim()
}

function queryCounts() {
  const sql = `
SELECT 'tenant', COUNT(*) FROM system_tenant WHERE id = ${TENANT_ID} AND name = '${TENANT_NAME.replace(/'/g, "''")}';
SELECT 'active_orders', COUNT(*) FROM mes_pro_process_pool_active_order WHERE tenant_id = ${TENANT_ID} AND deleted = 0 AND active_status = 'ACTIVE';
SELECT 'report_events', COUNT(*) FROM mes_pro_process_pool_event WHERE tenant_id = ${TENANT_ID} AND deleted = 0;
SELECT 'report_allocations', COUNT(*) FROM mes_pro_process_pool_report_allocation WHERE tenant_id = ${TENANT_ID} AND deleted = 0;
SELECT 'order_process_completions', COUNT(*) FROM mes_pro_process_pool_order_process_completion WHERE tenant_id = ${TENANT_ID} AND deleted = 0;
SELECT 'release_applications', COUNT(*) FROM mes_pro_process_pool_active_order_release_application WHERE tenant_id = ${TENANT_ID} AND deleted = 0;
SELECT 'batch_executions', COUNT(*) FROM mes_pro_edhr_batch_execution WHERE tenant_id = ${TENANT_ID} AND deleted = 0;
`
  const output = runDbQuery(sql)
  const counts = new Map()
  for (const line of output.split(/\r?\n/).filter(Boolean)) {
    const [key, value] = line.split('\t')
    counts.set(key, Number(value))
  }
  assert.equal(counts.get('tenant'), 1, `测试租户不存在或名称不匹配：${TENANT_ID}/${TENANT_NAME}`)
  return Object.fromEntries(counts.entries())
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let i = 0; i < count; i += 1) {
    const item = locator.nth(i)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`未找到可见输入框：${label}`)
}

async function selectTenant(page) {
  const form = page.locator('form.login-form:visible, .login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  const tenantInput = form.locator('.el-select input:visible, input[placeholder="请输入租户名称"]:visible').first()
  await tenantInput.waitFor({ state: 'visible', timeout: 15000 })
  await tenantInput.click()
  await tenantInput.fill(TENANT_NAME)
  const option = page.locator('.el-select-dropdown__item:visible', { hasText: TENANT_NAME }).first()
  if ((await option.count()) > 0) {
    await option.click()
  } else {
    await tenantInput.press('Enter')
  }
}

async function login(page) {
  await page.goto(`${FRONTEND_URL}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await selectTenant(page)
  await fillFirstVisible(
    page.locator('.login-form input[placeholder="请输入用户名"]:visible, .login-form input[name="username"]:visible'),
    USERNAME,
    'username'
  )
  await fillFirstVisible(
    page.locator('.login-form input[type="password"]:visible, .login-form input[placeholder="请输入密码"]:visible'),
    PASSWORD,
    'password'
  )
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await page.locator('.login-form button[type="submit"]:visible, .login-form button:has-text("登录"):visible').first().click()
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.ok(), true, `登录接口 HTTP 失败：${loginResponse.status()}`)
  const body = await loginResponse.json()
  assert.ok([0, 200].includes(body.code), `登录接口业务失败：${body.code} ${body.msg || body.message || ''}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

function unwrapCommonResult(body, label) {
  assert.ok(body && typeof body === 'object', `${label} 响应不是 JSON 对象`)
  assert.ok([0, 200].includes(body.code), `${label} 业务失败：${body.code} ${body.msg || body.message || ''}`)
  return body.data
}

async function run() {
  const beforeCounts = queryCounts()
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  const page = await context.newPage()
  const browserErrors = []
  const targetResponses = []
  page.on('console', (message) => {
    if (message.type() === 'error') {
      browserErrors.push(`console:${message.text()}`)
    }
  })
  page.on('pageerror', (error) => browserErrors.push(`pageerror:${error.message}`))
  page.on('response', async (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/mes/pro/process-pool/team-leader/')) return
    const item = { url, method: response.request().method(), status: response.status(), body: null }
    try {
      item.body = await response.json()
    } catch (error) {
      item.body = { parseError: error.message }
    }
    targetResponses.push(item)
  })

  try {
    await login(page)
    await page.goto(`${FRONTEND_URL}/mes/pro/process-pool/production-leader`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await page.locator('[data-production-leader-workbench-page]').waitFor({ state: 'visible', timeout: 60000 })
    const activeOrderTab = page.locator('[data-production-leader-module-tab-active-order], .el-tabs__item:has-text("活跃订单")').first()
    if ((await activeOrderTab.count()) > 0 && (await activeOrderTab.isVisible())) {
      await activeOrderTab.click()
    }
    const cleanupButton = page.locator('[data-team-leader-data-cleanup]:visible').first()
    await cleanupButton.waitFor({ state: 'visible', timeout: 60000 })

    const previewResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/data-cleanup/preview') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await cleanupButton.click()
    const previewResponse = await previewResponsePromise
    assert.equal(previewResponse.ok(), true, `清理预检 HTTP 失败：${previewResponse.status()}`)
    const preview = unwrapCommonResult(await previewResponse.json(), '清理预检')
    const cleanupTotal =
      Number(preview.activeOrderCount || 0) +
      Number(preview.reportEventCount || 0) +
      Number(preview.batchExecutionCount || 0) +
      Number(preview.releaseApplicationCount || 0) +
      Number(preview.releaseTransactionCount || 0)

    let execute = null
    if (cleanupTotal > 0) {
      await page.locator('.el-message-box:visible').waitFor({ state: 'visible', timeout: 30000 })
      const executeResponsePromise = page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/data-cleanup/execute') &&
          response.request().method() === 'POST',
        { timeout: 120000 }
      )
      await page.locator('.el-message-box:visible button:has-text("确认清理")').first().click()
      const executeResponse = await executeResponsePromise
      assert.equal(executeResponse.ok(), true, `清理执行 HTTP 失败：${executeResponse.status()}`)
      execute = unwrapCommonResult(await executeResponse.json(), '清理执行')
      await page.locator('.el-message:has-text("清理完成")').first().waitFor({ state: 'visible', timeout: 30000 })
    } else {
      await page.locator('.el-message:has-text("当前生产组长范围内没有可清理的数据")').first()
        .waitFor({ state: 'visible', timeout: 30000 })
    }

    const afterCounts = queryCounts()
    assert.equal(afterCounts.active_orders, 0, `清理后仍有 ACTIVE 活跃订单：${afterCounts.active_orders}`)
    assert.equal(afterCounts.report_events, 0, `清理后仍有报工事件：${afterCounts.report_events}`)
    assert.equal(afterCounts.report_allocations, 0, `清理后仍有报工分配：${afterCounts.report_allocations}`)
    assert.equal(afterCounts.order_process_completions, 0, `清理后仍有工序完成：${afterCounts.order_process_completions}`)
    assert.equal(afterCounts.release_applications, 0, `清理后仍有生产放行申请：${afterCounts.release_applications}`)
    assert.equal(afterCounts.batch_executions, 0, `清理后仍有批次执行：${afterCounts.batch_executions}`)

    const badTargetResponses = targetResponses.filter((response) => {
      if (response.status >= 400) return true
      if (response.body && typeof response.body === 'object' && response.body.code != null) {
        return ![0, 200].includes(response.body.code)
      }
      return false
    })
    assert.deepEqual(badTargetResponses, [], `生产组长接口存在失败响应：${JSON.stringify(badTargetResponses, null, 2)}`)
    assert.deepEqual(browserErrors, [], `浏览器错误：${browserErrors.join('\n')}`)

    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          beforeCounts,
          preview,
          execute,
          afterCounts,
          targetResponseCount: targetResponses.length
        },
        null,
        2
      )
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error.stack || error.message || String(error))
  process.exit(1)
})
