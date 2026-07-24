const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')
const artifactDir = path.join(
  workspaceRoot,
  'doc',
  'tasks',
  '20260710-schedule-date-row-isolation',
  'artifacts',
  'real-e2e'
)
const baseUrl = process.env.SCHEDULER_WORKBENCH_E2E_BASE_URL || 'http://localhost:8081'
const tenant = process.env.SCHEDULER_WORKBENCH_E2E_TENANT || '\u6d4b\u8bd5\u79df\u6237'
const username = process.env.SCHEDULER_WORKBENCH_E2E_USERNAME || 'aoteman'
const password = process.env.SCHEDULER_WORKBENCH_E2E_PASSWORD
const targetPath = '/mes/pro/scheduler-workbench'

assert.ok(password, 'SCHEDULER_WORKBENCH_E2E_PASSWORD is required')

function writeJson(name, value) {
  fs.mkdirSync(artifactDir, { recursive: true })
  fs.writeFileSync(path.join(artifactDir, name), JSON.stringify(value, null, 2), 'utf8')
}

function routeProcessKey(row) {
  return `${row.routeVersionId}:${row.routeProcessId}`
}

function normalizeDateValue(value) {
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  return typeof value === 'string' && value ? value.slice(0, 10) : undefined
}

function chooseAdjacentDate(value) {
  const [year, month, day] = value.split('-').map(Number)
  const targetDay = day < 28 ? day + 1 : day - 1
  return `${year}-${String(month).padStart(2, '0')}-${String(targetDay).padStart(2, '0')}`
}

function rowsByKey(rows) {
  return new Map(rows.map((row) => [routeProcessKey(row), row]))
}

async function login(page) {
  await page.goto(`${baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'commit',
    timeout: 120000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.fill(tenant)
    const option = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: tenant })
      .first()
    if ((await option.count()) > 0) {
      await option.click()
    } else {
      await tenantInput.press('Enter')
    }
  }
  await form.locator('input.el-input__inner:not([role="combobox"])').first().fill(username)
  await form.locator('input[type="password"]').first().fill(password)
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '\u767b\u5f55' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login HTTP ${loginResponse.status()}`)
  assert.ok(
    [0, 200].includes(loginPayload.code),
    `login failed: ${loginPayload.code} ${loginPayload.msg || ''}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function parseRowsResponse(response, label) {
  const payload = await response.json()
  writeJson(`${label}.json`, {
    status: response.status(),
    payload
  })
  assert.ok(response.ok(), `${label} HTTP ${response.status()}`)
  assert.ok(
    [0, 200].includes(payload.code),
    `${label} failed: ${payload.code} ${payload.msg || ''}`
  )
  assert.ok(Array.isArray(payload.data), `${label} data must be an array`)
  return payload.data
}

async function loadRows(page) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/schedule-order/process-wip-statistics') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${baseUrl}${targetPath}`, { waitUntil: 'commit', timeout: 120000 })
  const rows = await parseRowsResponse(await responsePromise, 'initial-process-wip')
  for (const row of rows) {
    assert.ok(
      row.routeVersionId && row.routeProcessId,
      `runtime process WIP contract is stale for processId=${row.processId}`
    )
  }
  await page.getByText('\u5f00\u6392\u65e5\u671f', { exact: true }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  return rows
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
    const nextButton = page
      .locator('.scheduler-workbench__wip-tabs-panel .el-pagination button.btn-next')
      .first()
    if ((await nextButton.count()) === 0 || (await nextButton.isDisabled())) break
    await nextButton.click()
    await page.waitForTimeout(300)
  }
  throw new Error(`route-process row not found: ${routeProcessKey(row)}`)
}

async function getReadyDateInput(page, row) {
  for (let attempt = 0; attempt < 120; attempt += 1) {
    const targetRow = await locateRow(page, row)
    const input = targetRow.locator('.scheduler-workbench__process-wip-date input').first()
    if ((await input.isVisible()) && !(await input.isDisabled())) {
      return input
    }
    await page.waitForTimeout(250)
  }
  throw new Error(`planned start date input did not become ready: ${routeProcessKey(row)}`)
}

async function chooseVisibleDate(page, targetDate) {
  const targetDay = String(Number(targetDate.slice(8, 10)))
  const popper = page.locator('.el-picker__popper:visible').last()
  await popper.waitFor({ state: 'visible', timeout: 60000 })
  const dayCell = popper
    .locator(
      'td.available:not(.prev-month):not(.next-month) .el-date-table-cell__text'
    )
    .filter({ hasText: new RegExp(`^${targetDay}$`) })
    .first()
  await dayCell.click()
}

async function savePlannedStartDate(page, row, targetDate, label) {
  const input = await getReadyDateInput(page, row)
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/schedule-order/process-wip-settings') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  ).catch(() => null)
  const refreshResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/schedule-order/process-wip-statistics') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  ).catch(() => null)
  await input.click()
  await chooseVisibleDate(page, targetDate)

  const saveResponse = await saveResponsePromise
  assert.ok(saveResponse, `${label} save response was not observed`)
  const requestPayload = JSON.parse(saveResponse.request().postData() || '{}')
  const savePayload = await saveResponse.json()
  writeJson(`${label}-save.json`, {
    status: saveResponse.status(),
    requestPayload,
    savePayload
  })
  assert.ok(saveResponse.ok(), `${label} save HTTP ${saveResponse.status()}`)
  assert.ok(
    [0, 200].includes(savePayload.code),
    `${label} save failed: ${savePayload.code} ${savePayload.msg || ''}`
  )
  assert.equal(Number(requestPayload.routeVersionId), Number(row.routeVersionId))
  assert.equal(Number(requestPayload.routeProcessId), Number(row.routeProcessId))
  assert.equal(Object.hasOwn(requestPayload, 'processId'), false)
  assert.equal(requestPayload.plannedStartDate, targetDate)

  const refreshResponse = await refreshResponsePromise
  assert.ok(refreshResponse, `${label} refresh response was not observed`)
  return parseRowsResponse(refreshResponse, `${label}-process-wip`)
}

function assertOnlyTargetChanged(beforeRows, afterRows, targetRow, targetDate) {
  const before = rowsByKey(beforeRows)
  const after = rowsByKey(afterRows)
  assert.equal(after.size, before.size, 'route-process row count must remain unchanged')
  for (const [key, beforeRow] of before.entries()) {
    const afterRow = after.get(key)
    assert.ok(afterRow, `route-process row disappeared: ${key}`)
    const expectedDate =
      key === routeProcessKey(targetRow)
        ? targetDate
        : normalizeDateValue(beforeRow.plannedStartDate)
    assert.equal(
      normalizeDateValue(afterRow.plannedStartDate),
      expectedDate,
      `unexpected planned start date change for route-process ${key}`
    )
  }
}

async function assertVisibleRowsMatchApi(page, rows, label) {
  const tableRows = page
    .locator('.scheduler-workbench__process-wip-table')
    .first()
    .locator('.el-table__body-wrapper tbody tr')
  const count = await tableRows.count()
  assert.ok(count > 0, `${label} must have visible rows`)
  for (let index = 0; index < count; index += 1) {
    const tableRow = tableRows.nth(index)
    const text = await tableRow.innerText()
    const matches = rows.filter(
      (row) =>
        row.routeCode &&
        text.includes(row.routeCode) &&
        text.includes(row.processCode || row.processName || '')
    )
    assert.equal(matches.length, 1, `${label} visible row must resolve to one route-process`)
    const inputValue = await tableRow
      .locator('.scheduler-workbench__process-wip-date input')
      .first()
      .inputValue()
    assert.equal(
      inputValue || undefined,
      normalizeDateValue(matches[0].plannedStartDate),
      `${label} visible input must match its own route-process date`
    )
  }
}

async function main() {
  fs.mkdirSync(artifactDir, { recursive: true })
  const launchOptions = {
    headless: true,
    args: ['--disable-dev-shm-usage']
  }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  try {
    const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } })
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(120000)
    await login(page)

    const initialRows = await loadRows(page)
    assert.ok(initialRows.length > 1, 'at least two real route-process rows are required')
    const targetRow = initialRows.find((row) => normalizeDateValue(row.plannedStartDate))
    assert.ok(targetRow, 'a route-process row with an existing planned start date is required')
    const originalDate = normalizeDateValue(targetRow.plannedStartDate)
    const targetDate = chooseAdjacentDate(originalDate)
    writeJson('selected-target.json', {
      routeProcessKey: routeProcessKey(targetRow),
      processId: targetRow.processId,
      routeCode: targetRow.routeCode,
      processCode: targetRow.processCode,
      originalDate,
      targetDate
    })

    const changedRows = await savePlannedStartDate(
      page,
      targetRow,
      targetDate,
      'planned-date-change'
    )
    assertOnlyTargetChanged(initialRows, changedRows, targetRow, targetDate)
    await assertVisibleRowsMatchApi(page, changedRows, 'after target change')
    await page.screenshot({
      path: path.join(artifactDir, 'after-target-change.png'),
      fullPage: true
    })

    const restoredRows = await savePlannedStartDate(
      page,
      targetRow,
      originalDate,
      'planned-date-restore'
    )
    assertOnlyTargetChanged(changedRows, restoredRows, targetRow, originalDate)
    const restored = rowsByKey(restoredRows)
    for (const row of initialRows) {
      assert.equal(
        normalizeDateValue(restored.get(routeProcessKey(row)).plannedStartDate),
        normalizeDateValue(row.plannedStartDate),
        `restored date mismatch for route-process ${routeProcessKey(row)}`
      )
    }
    await assertVisibleRowsMatchApi(page, restoredRows, 'after restore')
    writeJson('result.json', {
      result: 'PASS',
      tenant,
      username,
      targetPath,
      targetRouteProcessKey: routeProcessKey(targetRow),
      rowCount: initialRows.length,
      originalDate,
      temporaryDate: targetDate,
      restored: true
    })
    console.log(
      `GREEN: planned-date-row-isolation-real-e2e -> PASS, target=${routeProcessKey(targetRow)}, rows=${initialRows.length}, restored=${originalDate}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  fs.mkdirSync(artifactDir, { recursive: true })
  fs.writeFileSync(path.join(artifactDir, 'error.txt'), `${error.stack || error.message}\n`, 'utf8')
  console.error(`BLOCKER: planned-date-row-isolation-real-e2e -> ${error.stack || error.message}`)
  process.exit(1)
})
