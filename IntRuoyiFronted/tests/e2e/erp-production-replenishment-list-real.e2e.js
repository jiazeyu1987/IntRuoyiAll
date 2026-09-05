const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '../..')
const config = {
  baseUrl: (process.env.ERP_REPLENISHMENT_LIST_E2E_BASE_URL || 'http://localhost:8160').replace(/\/+$/, ''),
  tenant: process.env.ERP_REPLENISHMENT_LIST_E2E_TENANT || '芋道源码',
  username: process.env.ERP_REPLENISHMENT_LIST_E2E_USERNAME || 'admin',
  password: process.env.ERP_REPLENISHMENT_LIST_E2E_PASSWORD,
  headed: process.env.ERP_REPLENISHMENT_LIST_E2E_HEADED === '1',
  artifactDir: path.resolve(
    process.env.ERP_REPLENISHMENT_LIST_E2E_ARTIFACT_DIR ||
      path.join(frontendRoot, 'output', 'playwright', 'erp-production-replenishment-list-real')
  )
}

assert.ok(config.password, 'ERP_REPLENISHMENT_LIST_E2E_PASSWORD is required')

const listPath = '/erp/production/replenishment-list'
const syncType = 'PRODUCTION_REPLENISHMENT_LIST'
const syncLabel = '生产补料单列表'
const jobLogStatus = {
  RUNNING: 0,
  SUCCESS: 1,
  FAILURE: 2
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(800)
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
  throw new Error(`missing visible ${label}`)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(listPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }

  await fillFirstVisible(form.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(form.locator('input[type="password"]'), config.password, 'password')

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: '登录' }).click()
  ])
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.status(), 200, 'login http status')
  assert.ok([0, 200].includes(loginPayload.code), `login failed: ${loginPayload.msg || loginPayload.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function waitForJson(responsePromise, label) {
  const response = await responsePromise
  let payload
  try {
    payload = await response.json()
  } catch {
    payload = { raw: await response.text().catch(() => '') }
  }
  assert.equal(response.status(), 200, `${label} http status: ${JSON.stringify(payload).slice(0, 500)}`)
  assert.ok([0, 200].includes(payload.code), `${label} business code: ${JSON.stringify(payload).slice(0, 1000)}`)
  return payload
}

function collectEvents(page) {
  const events = {
    listResponses: [],
    syncResponses: [],
    targetWrites: [],
    consoleErrors: [],
    pageErrors: []
  }
  page.on('console', (message) => {
    if (message.type() === 'error') {
      events.consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => events.pageErrors.push(error.message))
  page.on('request', (request) => {
    const url = request.url()
    const method = request.method()
    if (
      ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) &&
      (url.includes('/admin-api/erp/production-replenishment-list') ||
        url.includes('/admin-api/erp/kingdee-sync/incremental-sync'))
    ) {
      events.targetWrites.push({ method, url, postData: request.postData() || '' })
    }
  })
  page.on('response', async (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/erp/production-replenishment-list') && !url.includes('/admin-api/erp/kingdee-sync')) {
      return
    }
    let payload = ''
    try {
      payload = (await response.text()).slice(0, 1200)
    } catch {
      payload = '<unreadable>'
    }
    const event = {
      method: response.request().method(),
      url,
      status: response.status(),
      payload
    }
    if (url.includes('/admin-api/erp/production-replenishment-list')) {
      events.listResponses.push(event)
    } else {
      events.syncResponses.push(event)
    }
  })
  return events
}

async function verifyListPage(page, events) {
  const listResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/erp/production-replenishment-list/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}${listPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const payload = await waitForJson(listResponsePromise, 'production replenishment list page')
  await page.locator('.replenishment-list-table').waitFor({ state: 'visible', timeout: 60000 })

  for (const header of ['生产补料单号', '单据状态', '单据日期', '生产订单编号', '物料', '库存组织', '生产组织', '补料部门']) {
    await page.getByText(header, { exact: true }).first().waitFor({ state: 'visible', timeout: 60000 })
  }
  await page.getByRole('button', { name: /增量同步/ }).first().waitFor({ state: 'visible', timeout: 60000 })

  const data = payload.data || {}
  const rows = Array.isArray(data.list) ? data.list : []
  if (rows.length > 0) {
    const expandButton = page.locator('.replenishment-list-table .el-table__expand-icon').first()
    await expandButton.click()
    for (const detailHeader of ['物料编码', '物料名称', '申请数量', '实发数量', '仓库', '生产订单编号']) {
      await page.getByText(detailHeader, { exact: true }).first().waitFor({ state: 'visible', timeout: 60000 })
    }
  }

  await page.screenshot({
    path: path.join(config.artifactDir, 'replenishment-list-page.png'),
    fullPage: true
  })

  const writeCountAfterRead = events.targetWrites.length
  assert.equal(writeCountAfterRead, 0, `list page should be read-only before sync: ${JSON.stringify(events.targetWrites)}`)

  return {
    total: Number(data.total || 0),
    firstBillNo: rows[0]?.sourceBillNo || null,
    firstItemCount: Array.isArray(rows[0]?.items) ? rows[0].items.length : 0
  }
}

async function verifyManualSyncFromListPage(page) {
  await page.screenshot({
    path: path.join(config.artifactDir, 'replenishment-list-before-submit.png'),
    fullPage: true
  })

  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/erp/kingdee-sync/incremental-sync') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: /增量同步/ }).first().click()
  const submitPayload = await waitForJson(submitResponsePromise, 'kingdee replenishment incremental sync submit')

  assert.equal(submitPayload.data?.syncType, syncType, `unexpected submitted syncType: ${JSON.stringify(submitPayload)}`)
  assert.equal(
    submitPayload.data?.handlerName,
    'kingdeeProductionReplenishmentListSyncJob',
    `unexpected handler: ${JSON.stringify(submitPayload)}`
  )
  await page.getByText('生产补料单列表增量同步任务已提交', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await page.waitForTimeout(1500)
  await page.screenshot({
    path: path.join(config.artifactDir, 'replenishment-list-after-submit.png'),
    fullPage: true
  })

  const jobLog = await waitForJobLogTerminal(page, submitPayload.data?.jobId)

  return {
    submitMessage: submitPayload.data?.message,
    jobId: submitPayload.data?.jobId,
    jobLog
  }
}

async function waitForJobLogTerminal(page, jobId) {
  assert.ok(jobId, 'incremental sync response missing jobId')
  let lastPayload = null
  let lastRows = []

  for (let attempt = 0; attempt < 30; attempt += 1) {
    const logResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/infra/job-log/page') &&
        response.url().includes(`jobId=${jobId}`) &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )

    if (attempt === 0) {
      await page.goto(`${config.baseUrl}/job/job-log?id=${jobId}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    } else {
      await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
    }

    lastPayload = await waitForJson(logResponsePromise, 'infra job log page')
    await page.locator('.el-table').first().waitFor({ state: 'visible', timeout: 60000 })
    lastRows = Array.isArray(lastPayload.data?.list) ? lastPayload.data.list : []
    const targetRows = lastRows.filter((row) => row.jobId === jobId)

    if (targetRows.length > 0) {
      const latest = targetRows[0]
      if (latest.status === jobLogStatus.SUCCESS) {
        await page.screenshot({
          path: path.join(config.artifactDir, 'job-log-terminal-success.png'),
          fullPage: true
        })
        return {
          id: latest.id,
          jobId: latest.jobId,
          handlerName: latest.handlerName,
          status: latest.status,
          duration: latest.duration,
          beginTime: latest.beginTime,
          endTime: latest.endTime
        }
      }
      if (latest.status === jobLogStatus.FAILURE) {
        await page.screenshot({
          path: path.join(config.artifactDir, 'job-log-terminal-failure.png'),
          fullPage: true
        })
        throw new Error(`kingdee replenishment job failed: ${JSON.stringify(latest).slice(0, 1200)}`)
      }
    }

    await page.waitForTimeout(2000)
  }

  throw new Error(
    `kingdee replenishment job did not reach terminal status: ${JSON.stringify({
      jobId,
      payload: lastPayload,
      rows: lastRows
    }).slice(0, 2000)}`
  )
}

async function main() {
  fs.mkdirSync(config.artifactDir, { recursive: true })

  const browser = await chromium.launch({
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  })

  try {
    const context = await browser.newContext({
      viewport: { width: 1440, height: 960 },
      locale: 'zh-CN'
    })
    const page = await context.newPage()
    const events = collectEvents(page)

    await login(page)
    const list = await verifyListPage(page, events)
    const sync = await verifyManualSyncFromListPage(page)

    assert.deepEqual(events.consoleErrors, [], `console errors: ${JSON.stringify(events.consoleErrors)}`)
    assert.deepEqual(events.pageErrors, [], `page errors: ${JSON.stringify(events.pageErrors)}`)

    const incrementalWrites = events.targetWrites.filter((event) =>
      event.url.includes('/admin-api/erp/kingdee-sync/incremental-sync')
    )
    assert.equal(incrementalWrites.length, 1, `expected one incremental sync write: ${JSON.stringify(events.targetWrites)}`)
    assert.ok(
      incrementalWrites[0].postData.includes(syncType),
      `incremental sync payload missing syncType: ${incrementalWrites[0].postData}`
    )

    const report = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      listPath,
      syncTriggerPath: listPath,
      syncType,
      list,
      sync,
      observed: {
        listResponses: events.listResponses,
        syncResponses: events.syncResponses,
        targetWrites: events.targetWrites.map((event) => ({
          method: event.method,
          url: event.url,
          postDataContainsSyncType: event.postData.includes(syncType)
        }))
      },
      screenshots: [
        path.join(config.artifactDir, 'replenishment-list-page.png'),
        path.join(config.artifactDir, 'replenishment-list-before-submit.png'),
        path.join(config.artifactDir, 'replenishment-list-after-submit.png'),
        path.join(config.artifactDir, 'job-log-terminal-success.png')
      ]
    }

    fs.writeFileSync(
      path.join(config.artifactDir, 'erp-production-replenishment-list-real-report.json'),
      `${JSON.stringify(report, null, 2)}\n`,
      'utf8'
    )

    await context.close()
    console.log(JSON.stringify(report, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exit(1)
})
