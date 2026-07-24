const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '../..')
const artifactDir = path.join(
  repoRoot,
  'doc',
  'tasks',
  '20260710-route-process-schedule-config-unification',
  'artifacts',
  'real-e2e'
)
const baseUrl = process.env.SCHEDULER_WORKBENCH_E2E_BASE_URL || 'http://127.0.0.1:8091'
const tenant = '\u6d4b\u8bd5\u79df\u6237'
const username = 'aoteman'
const password = '111111'
const targetPath = '/mes/pro/scheduler-workbench'

function writeJson(name, value) {
  fs.mkdirSync(artifactDir, { recursive: true })
  fs.writeFileSync(path.join(artifactDir, name), JSON.stringify(value, null, 2), 'utf8')
}

async function login(page) {
  await page.goto(`${baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'commit',
    timeout: 120000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.fill(tenant)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: tenant }).first()
    if ((await option.count()) > 0) {
      await option.click()
    } else {
      await tenantInput.press('Enter')
    }
  }
  await form.locator('input.el-input__inner:not([role="combobox"])').first().fill(username)
  await form.locator('input[type="password"]').first().fill(password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '\u767b\u5f55' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login HTTP ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login failed: ${loginPayload.code} ${loginPayload.msg || ''}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

function routeProcessKey(row) {
  return `${row.routeVersionId}:${row.routeProcessId}`
}

async function loadRows(page) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/schedule-order/process-wip-statistics') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${baseUrl}${targetPath}`, { waitUntil: 'commit', timeout: 120000 })
  const response = await responsePromise
  const payload = await response.json()
  writeJson('process-wip-response.json', {
    status: response.status(),
    payload
  })
  assert.ok(response.ok(), `process WIP HTTP ${response.status()}`)
  assert.ok([0, 200].includes(payload.code), `process WIP failed: ${payload.code} ${payload.msg || ''}`)
  assert.ok(Array.isArray(payload.data), 'process WIP data must be an array')
  return payload.data
}

async function locateRow(page, row) {
  const table = page.locator('.scheduler-workbench__process-wip-table').first()
  await table.waitFor({ state: 'visible', timeout: 60000 })
  const tokens = [row.routeCode, row.processCode || row.processName].filter(Boolean)
  for (let pageIndex = 0; pageIndex < 20; pageIndex += 1) {
    let candidates = table.locator('.el-table__body-wrapper tbody tr')
    for (const token of tokens) {
      candidates = candidates.filter({ hasText: token })
    }
    if ((await candidates.count()) > 0) {
      return candidates.first()
    }
    const nextButton = page.locator('.scheduler-workbench__wip-tabs-panel .el-pagination button.btn-next').first()
    if ((await nextButton.count()) === 0 || (await nextButton.isDisabled())) break
    await nextButton.click()
    await page.waitForTimeout(300)
  }
  throw new Error(`route-process row not found: ${routeProcessKey(row)}`)
}

async function toggleAndRestoreNightShift(page, row) {
  const getReadyNightSwitch = async () => {
    for (let attempt = 0; attempt < 120; attempt += 1) {
      const targetRow = await locateRow(page, row)
      const nightSwitch = targetRow.locator('.el-switch').first()
      if ((await nightSwitch.isVisible()) && !(await nightSwitch.isDisabled())) {
        return nightSwitch
      }
      await page.waitForTimeout(250)
    }
    throw new Error(`night shift switch did not become ready: ${routeProcessKey(row)}`)
  }

  const saveOnce = async (expectedValue, label) => {
    const nightSwitch = await getReadyNightSwitch()
    const responsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/schedule-order/process-wip-settings') &&
        response.request().method() === 'PUT',
      { timeout: 60000 }
    )
    const refreshPromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/schedule-order/process-wip-statistics') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await nightSwitch.click()
    const response = await responsePromise
    const requestPayload = JSON.parse(response.request().postData() || '{}')
    const responsePayload = await response.json()
    writeJson(`${label}.json`, {
      status: response.status(),
      requestPayload,
      responsePayload
    })
    assert.ok(response.ok(), `${label} HTTP ${response.status()}`)
    assert.ok([0, 200].includes(responsePayload.code), `${label} failed: ${responsePayload.code} ${responsePayload.msg || ''}`)
    assert.equal(Number(requestPayload.routeVersionId), Number(row.routeVersionId))
    assert.equal(Number(requestPayload.routeProcessId), Number(row.routeProcessId))
    assert.equal(Object.hasOwn(requestPayload, 'processId'), false, 'settings request must not use processId')
    assert.equal(Boolean(requestPayload.nightShiftEnabled), expectedValue)
    const refreshResponse = await refreshPromise
    assert.ok(refreshResponse.ok(), `${label} refresh HTTP ${refreshResponse.status()}`)
  }

  await saveOnce(!Boolean(row.nightShiftEnabled), 'night-shift-toggle')
  await saveOnce(Boolean(row.nightShiftEnabled), 'night-shift-restore')
}

async function main() {
  fs.mkdirSync(artifactDir, { recursive: true })
  const browser = await chromium.launch({
    headless: true,
    executablePath:
      process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
      'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    args: ['--disable-dev-shm-usage']
  })
  try {
    const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } })
    page.setDefaultTimeout(60000)
    await login(page)
    const rows = await loadRows(page)
    assert.ok(rows.length > 1, `all active route processes must be listed, got ${rows.length}`)

    const keys = rows.map(routeProcessKey)
    assert.equal(new Set(keys).size, keys.length, 'each route process must appear once')
    for (const row of rows) {
      assert.ok(row.routeId, `routeId missing for ${routeProcessKey(row)}`)
      assert.ok(row.routeCode, `routeCode missing for ${routeProcessKey(row)}`)
      assert.ok(row.routeName, `routeName missing for ${routeProcessKey(row)}`)
      assert.ok(row.routeVersionId, `routeVersionId missing for ${routeProcessKey(row)}`)
      assert.ok(row.routeProcessId, `routeProcessId missing for ${routeProcessKey(row)}`)
      assert.ok(row.processId, `processId missing for ${routeProcessKey(row)}`)
      assert.ok(row.processCode, `processCode missing for ${routeProcessKey(row)}`)
      assert.ok(row.processName, `processName missing for ${routeProcessKey(row)}`)
    }

    const balloonCatheterRows = rows.filter((row) => row.routeName === '\u7403\u56ca\u6269\u5f20\u5bfc\u7ba1')
    assert.ok(balloonCatheterRows.length > 0, '\u7403\u56ca\u6269\u5f20\u5bfc\u7ba1 route must have active workbench processes')
    assert.equal(
      balloonCatheterRows.some((row) => row.processName === '\u5168\u68c0\u5bfc\u4e1d'),
      false,
      '\u7403\u56ca\u6269\u5f20\u5bfc\u7ba1 route must not contain the deleted \u5168\u68c0\u5bfc\u4e1d process'
    )

    const mergedRow = rows.find((row) => Number(row.wipOrderCount) > 1)
    assert.ok(mergedRow, 'must contain a route process aggregating multiple product orders')
    assert.equal(
      new Set(mergedRow.scheduleOrderIds || []).size,
      Number(mergedRow.wipOrderCount),
      'aggregated row order count must match distinct schedule orders'
    )

    const processRoutes = new Map()
    for (const row of rows) {
      const routeKeys = processRoutes.get(row.processId) || new Set()
      routeKeys.add(routeProcessKey(row))
      processRoutes.set(row.processId, routeKeys)
    }
    const hasSameBaseProcessAcrossRoutes = [...processRoutes.values()].some(
      (routeKeys) => routeKeys.size > 1
    )

    await page.getByText('\u5de5\u827a\u8def\u7ebf\u7f16\u7801', { exact: true }).first().waitFor({ state: 'visible' })
    await page.getByText('\u5de5\u827a\u8def\u7ebf\u540d\u79f0', { exact: true }).first().waitFor({ state: 'visible' })
    await toggleAndRestoreNightShift(page, rows[0])
    await page.screenshot({ path: path.join(artifactDir, 'workbench.png'), fullPage: true })
    writeJson('result.json', {
      result: hasSameBaseProcessAcrossRoutes ? 'PASS' : 'BLOCKED',
      rowCount: rows.length,
      mergedRouteProcess: routeProcessKey(mergedRow),
      sameBaseProcessAcrossRoutes: hasSameBaseProcessAcrossRoutes
    })
    assert.ok(
      hasSameBaseProcessAcrossRoutes,
      '\u6d4b\u8bd5\u79df\u6237\u5f53\u524d\u5728\u6392\u6570\u636e\u4e2d\u6ca1\u6709\u540c\u4e00\u57fa\u7840\u5de5\u5e8f\u540c\u65f6\u5c5e\u4e8e\u4e24\u6761\u5de5\u827a\u8def\u7ebf\uff0c\u65e0\u6cd5\u5b8c\u6210\u8de8\u8def\u7ebf\u5206\u884c\u7684\u771f\u5b9e E2E \u9a8c\u8bc1'
    )
    console.log(`GREEN: route-process-workbench-real-e2e -> PASS, rows=${rows.length}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  fs.mkdirSync(artifactDir, { recursive: true })
  fs.writeFileSync(path.join(artifactDir, 'error.txt'), `${error.stack || error.message}\n`, 'utf8')
  console.error(`BLOCKER: route-process-workbench-real-e2e -> ${error.stack || error.message}`)
  process.exit(1)
})
