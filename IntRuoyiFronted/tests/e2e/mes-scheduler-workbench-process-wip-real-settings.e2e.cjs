const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')
const artifactDir = path.join(workspaceRoot, 'doc', 'tasks', '20260708-scheduler-workbench-process-wip-night-shift-start-date', 'artifacts', 'real-e2e-settings')
const baseUrl = 'http://localhost:8081'
const tenant = '测试租户'
const username = 'aoteman'
const password = '111111'
const targetPath = '/mes/pro/scheduler-workbench'
const plannedStartDate = '2026-07-15'
const alternatePlannedStartDate = '2026-07-16'
const nightShiftCapableProcessCodes = new Set(['B020', 'B040', 'B050', 'B140', 'B150', 'B200'])

function ensureArtifactDir() {
  fs.mkdirSync(artifactDir, { recursive: true })
}

function writeJson(name, payload) {
  fs.writeFileSync(path.join(artifactDir, name), JSON.stringify(payload, null, 2), 'utf8')
}

function normalizeDateValue(value) {
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  return typeof value === 'string' && value ? value : undefined
}

async function settle(page) {
  await page.waitForLoadState('domcontentloaded', { timeout: 60000 }).catch(() => {})
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(700)
}

async function login(page) {
  await page.goto(`${baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.fill(tenant)
    await tenantInput.press('Enter')
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(tenant)
  }
  await form.locator('input.el-input__inner:not([role="combobox"])').first().fill(username)
  await form.locator('input[type="password"]').first().fill(password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login HTTP status ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login code ${loginPayload.code}: ${loginPayload.msg || ''}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function getProcessWipRows(page, label) {
  const response = await page.waitForResponse(
    (resp) => resp.url().includes('/mes/pro/schedule-order/process-wip-statistics') && resp.request().method() === 'GET',
    { timeout: 60000 }
  ).catch(() => null)
  if (!response) {
    return []
  }
  const payload = await response.json()
  const rows = Array.isArray(payload.data) ? payload.data : Array.isArray(payload) ? payload : []
  writeJson(`${label}-process-wip-response.json`, { status: response.status(), rowCount: rows.length, rows })
  return rows
}

async function waitForProcessWipData(page) {
  await page.goto(`${baseUrl}${targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const rowsPromise = getProcessWipRows(page, 'initial')
  await settle(page)
  await page.getByText('工序列表', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('夜班', { exact: true }).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('开排日期', { exact: true }).first().waitFor({ state: 'visible', timeout: 60000 })
  const rows = await rowsPromise
  const fallbackRows = rows.length ? rows : await page.evaluate(async () => {
    const resp = await fetch('/admin-api/mes/pro/schedule-order/process-wip-statistics')
    const payload = await resp.json()
    return Array.isArray(payload.data) ? payload.data : []
  })
  assert.ok(fallbackRows.length > 0, 'process wip statistics must contain real test tenant rows')
  return fallbackRows
}

async function waitForSuccessfulSettingsSave(page, expectedProcessId, label) {
  const response = await page.waitForResponse(
    (resp) =>
      resp.url().includes('/mes/pro/schedule-order/process-wip-settings') &&
      resp.request().method() === 'PUT',
    { timeout: 60000 }
  )
  const requestPayload = JSON.parse(response.request().postData() || '{}')
  const payload = await response.json()
  const result = { label, status: response.status(), requestPayload, payload }
  writeJson(`${label}-save-response.json`, result)
  assert.equal(result.status, 200, `${label} save API HTTP status must be 200`)
  assert.equal(Number(requestPayload.processId), Number(expectedProcessId), `${label} must save selected processId`)
  assert.ok(
    [0, 200].includes(payload.code),
    `${label} save API code must succeed, got ${payload.code}: ${payload.msg || ''}`
  )
  return result
}

async function locateTargetRow(page, row) {
  const table = page.locator('.scheduler-workbench__process-wip-table').first()
  await table.waitFor({ state: 'visible', timeout: 60000 })
  for (let pageIndex = 0; pageIndex < 5; pageIndex += 1) {
    const rowLocator = await locateTargetRowOnCurrentPage(page, row)
    if (rowLocator) {
      return rowLocator
    }
    const nextButton = page.locator('.scheduler-workbench__wip-tabs-panel .el-pagination button.btn-next').first()
    if ((await nextButton.count()) === 0 || (await nextButton.isDisabled())) {
      break
    }
    await nextButton.click()
    await page.waitForTimeout(500)
  }
  throw new Error(`Cannot locate target process row in table: ${row.processCode || row.processName || row.processId}`)
}

async function locateTargetRowOnCurrentPage(page, row) {
  const table = page.locator('.scheduler-workbench__process-wip-table').first()
  const byCode = row.processCode
    ? table.locator('.el-table__body-wrapper tbody tr').filter({ hasText: row.processCode })
    : page.locator([])
  if ((await byCode.count()) > 0) {
    return byCode.first()
  }
  const byName = row.processName
    ? table.locator('.el-table__body-wrapper tbody tr').filter({ hasText: row.processName })
    : page.locator([])
  if ((await byName.count()) > 0) {
    return byName.first()
  }
  return null
}

async function saveSettingThroughPageControls(page, row) {
  const targetRow = await locateTargetRow(page, row)
  await targetRow.scrollIntoViewIfNeeded()
  await targetRow.screenshot({ path: path.join(artifactDir, 'target-row-before-save.png') })

  const switchButton = targetRow.locator('.el-switch').first()
  await switchButton.waitFor({ state: 'visible', timeout: 60000 })
  const switchClass = await switchButton.getAttribute('class')
  if (!/\bis-checked\b/.test(switchClass || '')) {
    const savePromise = waitForSuccessfulSettingsSave(page, row.processId, 'night-shift')
    await switchButton.click()
    const saveResult = await savePromise
    assert.equal(saveResult.requestPayload.nightShiftEnabled, true, 'night shift request must set true')
  }

  const currentPlannedStartDate = normalizeDateValue(row.plannedStartDate)
  if (currentPlannedStartDate === plannedStartDate) {
    const resetResult = await savePlannedStartDateThroughPageControls(
      page,
      row,
      alternatePlannedStartDate,
      'planned-start-date-reset'
    )
    assert.equal(
      resetResult.requestPayload.plannedStartDate,
      alternatePlannedStartDate,
      'planned start date reset request must save alternate date'
    )
  }

  const dateSaveResult = await savePlannedStartDateThroughPageControls(
    page,
    row,
    plannedStartDate,
    'planned-start-date'
  )
  assert.equal(dateSaveResult.requestPayload.plannedStartDate, plannedStartDate, 'planned start date request must save selected date')
}

async function savePlannedStartDateThroughPageControls(page, row, targetDate, label) {
  const refreshedRow = await locateTargetRow(page, row)
  const dateEditor = refreshedRow.locator('.scheduler-workbench__process-wip-date input').first()
  await dateEditor.waitFor({ state: 'visible', timeout: 60000 })
  await dateEditor.click()
  const dateSavePromise = waitForSuccessfulSettingsSave(page, row.processId, label)
  await chooseVisibleDate(page, targetDate)
  return await dateSavePromise
}

async function chooseVisibleDate(page, targetDate) {
  const [, , dayTextRaw] = targetDate.split('-')
  const dayText = String(Number(dayTextRaw))
  const visiblePopperIndex = await page.waitForFunction(() => {
    const poppers = Array.from(document.querySelectorAll('.el-picker__popper'))
    const index = poppers.findIndex((el) => {
      const rect = el.getBoundingClientRect()
      const style = window.getComputedStyle(el)
      return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0
    })
    return index >= 0 ? index : false
  }, null, { timeout: 60000 })
  const popperIndex = await visiblePopperIndex.jsonValue()
  const popper = page.locator('.el-picker__popper').nth(popperIndex)
  const dayCell = popper
    .locator('td.available:not(.prev-month):not(.next-month)')
    .filter({ hasText: new RegExp(`^\\s*${dayText}\\s*$`) })
    .first()
  await dayCell.waitFor({ state: 'visible', timeout: 60000 })
  await dayCell.click({ position: { x: 14, y: 14 } })
}

async function readRowsFromPage(page, label) {
  await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
  const response = await page.waitForResponse(
    (resp) => resp.url().includes('/mes/pro/schedule-order/process-wip-statistics') && resp.request().method() === 'GET',
    { timeout: 60000 }
  )
  const payload = await response.json()
  assert.ok(response.ok(), `statistics HTTP status must be OK, got ${response.status()}`)
  assert.ok([0, 200].includes(payload.code), `statistics code must succeed, got ${payload.code}: ${payload.msg || ''}`)
  const rows = Array.isArray(payload.data) ? payload.data : []
  writeJson(`${label}-process-wip-response.json`, { rowCount: rows.length, rows })
  return rows
}

async function verifyPageShowsSavedValues(page, row) {
  await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  await page.getByText('工序列表', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  const tableText = await page.locator('.scheduler-workbench__process-wip-table').first().innerText({ timeout: 60000 })
  fs.writeFileSync(path.join(artifactDir, 'page-table-after-save.txt'), tableText, 'utf8')
  assert.ok(tableText.includes('夜班'), 'page table still shows night shift column')
  assert.ok(tableText.includes('开排日期'), 'page table still shows planned start date column')
  const targetRow = await locateTargetRow(page, row)
  const targetRowText = await targetRow.innerText({ timeout: 60000 })
  const plannedStartDateInputValue = await targetRow
    .locator('.scheduler-workbench__process-wip-date input')
    .first()
    .inputValue({ timeout: 60000 })
  fs.writeFileSync(path.join(artifactDir, 'page-target-row-after-save.txt'), targetRowText, 'utf8')
  writeJson('page-target-row-after-save.json', {
    processId: row.processId,
    processCode: row.processCode,
    targetRowText,
    plannedStartDateInputValue
  })
  assert.ok(targetRowText.includes('夜班'), 'target row shows night shift state')
  assert.equal(
    plannedStartDateInputValue,
    plannedStartDate,
    `target row planned start date input shows ${plannedStartDate}`
  )
  await page.screenshot({ path: path.join(artifactDir, 'page-after-save.png'), fullPage: true })
}

async function main() {
  ensureArtifactDir()
  const launchOptions = { headless: true, args: ['--disable-dev-shm-usage'] }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    await login(page)
    const rows = await waitForProcessWipData(page)
    const target =
      rows.find(
        (row) =>
          row.processId &&
          nightShiftCapableProcessCodes.has(row.processCode) &&
          Array.isArray(row.scheduleOrderIds) &&
          row.scheduleOrderIds.length > 0
      ) || rows.find((row) => row.processId && nightShiftCapableProcessCodes.has(row.processCode))
    assert.ok(target, 'must find a real process wip row with processId')
    writeJson('selected-row-before.json', target)
    await saveSettingThroughPageControls(page, target)
    const afterRows = await readRowsFromPage(page, 'after-save')
    const after = afterRows.find((row) => Number(row.processId) === Number(target.processId))
    assert.ok(after, `saved processId ${target.processId} must still exist after save`)
    writeJson('selected-row-after.json', after)
    assert.equal(Boolean(after.nightShiftEnabled), true, 'nightShiftEnabled must persist as true')
    assert.equal(normalizeDateValue(after.plannedStartDate), plannedStartDate, 'plannedStartDate must persist after save')
    await verifyPageShowsSavedValues(page, after)
    const evidence = {
      result: 'PASS',
      baseUrl,
      tenant,
      username,
      targetPath,
      processId: target.processId,
      processCode: target.processCode,
      processName: target.processName,
      scheduleOrderIds: target.scheduleOrderIds,
      savedNightShiftEnabled: true,
      savedPlannedStartDate: plannedStartDate,
      artifacts: ['initial-process-wip-response.json', 'night-shift-save-response.json', 'planned-start-date-save-response.json', 'after-save-process-wip-response.json', 'selected-row-before.json', 'selected-row-after.json', 'target-row-before-save.png', 'page-table-after-save.txt', 'page-after-save.png']
    }
    writeJson('e2e-result.json', evidence)
    console.log(`GREEN: real-process-wip-settings-e2e -> PASS, processId=${target.processId}, plannedStartDate=${plannedStartDate}, nightShiftEnabled=true`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  ensureArtifactDir()
  fs.writeFileSync(path.join(artifactDir, 'e2e-error.txt'), `${error.stack || error.message}\n`, 'utf8')
  console.error(`BLOCKER: real-process-wip-settings-e2e -> ${error.stack || error.message}`)
  process.exit(1)
})
