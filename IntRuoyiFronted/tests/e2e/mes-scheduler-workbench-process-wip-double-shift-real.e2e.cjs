const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '../..')
const artifactDir = path.join(
  repoRoot,
  'doc',
  'tasks',
  '20260710-mes-process-shift-capacity-integer',
  'artifacts',
  'real-e2e'
)
const baseUrl = 'http://localhost:8081'
const targetPath = '/mes/pro/scheduler-workbench'
const tenant = '\u6d4b\u8bd5\u79df\u6237'
const username = 'aoteman'
const password = '111111'

function ensureArtifactDir() {
  fs.mkdirSync(artifactDir, { recursive: true })
  fs.rmSync(path.join(artifactDir, 'e2e-error.txt'), { force: true })
}

function writeJson(name, payload) {
  fs.writeFileSync(path.join(artifactDir, name), JSON.stringify(payload, null, 2), 'utf8')
}

async function login(page) {
  await page.goto(`${baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first()
    if (await tenantOption.count()) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(tenant)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(username)
  await form.locator('input[type="password"]').first().fill(password)
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '\u767b\u5f55' }).click()
  const response = await responsePromise
  const payload = await response.json()
  assert.ok(response.ok(), `login HTTP ${response.status()}`)
  assert.ok([0, 200].includes(payload.code), `login failed: ${payload.msg || payload.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function openWorkbench(page) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/schedule-order/process-wip-statistics') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${baseUrl}${targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const response = await responsePromise
  const payload = await response.json()
  assert.ok(response.ok(), `process WIP HTTP ${response.status()}`)
  assert.ok([0, 200].includes(payload.code), `process WIP failed: ${payload.msg || payload.code}`)
  const rows = Array.isArray(payload.data) ? payload.data : []
  assert.ok(rows.length > 0, 'test tenant process WIP list must contain real rows')
  await page.getByText('\u5de5\u5e8f\u5217\u8868', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  return rows
}

async function locateRowOnCurrentPage(page, row) {
  const identifier = row.processCode || row.processName
  if (!identifier) return null
  const match = page
    .locator('.scheduler-workbench__process-wip-table .el-table__body-wrapper tbody tr')
    .filter({ hasText: identifier })
  return (await match.count()) > 0 ? match.first() : null
}

async function locateRow(page, row) {
  for (let pageIndex = 0; pageIndex < 10; pageIndex += 1) {
    const match = await locateRowOnCurrentPage(page, row)
    if (match) return match
    const nextButton = page.locator('.el-pagination button.btn-next').first()
    if ((await nextButton.count()) === 0 || (await nextButton.isDisabled())) break
    await nextButton.click()
    await page.waitForTimeout(400)
  }
  throw new Error(`cannot locate process row ${row.processCode || row.processName || row.processId}`)
}

async function saveNightShift(page, row, enabled) {
  const targetRow = await locateRow(page, row)
  const switchControl = targetRow.locator('.el-switch').first()
  await switchControl.waitFor({ state: 'visible', timeout: 60000 })
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/schedule-order/process-wip-settings') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  const statisticsResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/schedule-order/process-wip-statistics') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await switchControl.click()
  const saveResponse = await saveResponsePromise
  const requestPayload = JSON.parse(saveResponse.request().postData() || '{}')
  const payload = await saveResponse.json()
  assert.ok(saveResponse.ok(), `save night shift HTTP ${saveResponse.status()}`)
  assert.ok([0, 200].includes(payload.code), `save night shift failed: ${payload.msg || payload.code}`)
  assert.equal(Number(requestPayload.processId), Number(row.processId), 'must update selected process')
  assert.equal(Boolean(requestPayload.nightShiftEnabled), enabled, 'night shift payload must match target state')
  await statisticsResponsePromise
  await page.waitForTimeout(400)
  return await locateRow(page, row)
}

async function assertDoubleShiftDisplay(targetRow) {
  await targetRow.getByText('\u767d\u591c\u73ed', { exact: true }).waitFor({
    state: 'visible',
    timeout: 60000
  })
  const multiplier = targetRow.locator('.scheduler-workbench__shift-capacity-multiplier').first()
  await multiplier.waitFor({ state: 'visible', timeout: 60000 })
  assert.equal((await multiplier.innerText()).trim(), 'X2', 'enabled night shift row must show X2')
  assert.ok(
    /\bel-tag--success\b/.test((await multiplier.getAttribute('class')) || ''),
    'X2 tag must use success green style'
  )
}

async function assertDayShiftDisplay(targetRow) {
  await targetRow.getByText('\u767d\u73ed', { exact: true }).waitFor({
    state: 'visible',
    timeout: 60000
  })
  assert.equal(
    await targetRow.locator('.scheduler-workbench__shift-capacity-multiplier').count(),
    0,
    'disabled night shift row must not show X2'
  )
}

async function main() {
  ensureArtifactDir()
  const launchOptions = { headless: true, args: ['--disable-dev-shm-usage'] }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  try {
    const context = await browser.newContext({ viewport: { width: 1680, height: 920 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    await login(page)
    const rows = await openWorkbench(page)
    const target =
      rows.find(
        (row) =>
          row.processId &&
          (row.processCode || row.processName) &&
          !row.nightShiftMixed &&
          !row.nightShiftEnabled &&
          Array.isArray(row.scheduleOrderIds) &&
          row.scheduleOrderIds.length > 0
      ) ||
      rows.find(
        (row) =>
          row.processId &&
          (row.processCode || row.processName) &&
          !row.nightShiftMixed &&
          Array.isArray(row.scheduleOrderIds) &&
          row.scheduleOrderIds.length > 0
      )
    assert.ok(target, 'must find a non-mixed real process WIP row')
    const originalEnabled = Boolean(target.nightShiftEnabled)
    writeJson('selected-row-before.json', {
      tenantId: 122,
      username,
      processId: target.processId,
      processCode: target.processCode,
      processName: target.processName,
      nightShiftEnabled: originalEnabled,
      nightShiftMixed: target.nightShiftMixed,
      shiftCapacityTotal: target.shiftCapacityTotal,
      scheduleOrderIds: target.scheduleOrderIds
    })

    let currentEnabled = originalEnabled
    try {
      if (originalEnabled) {
        await assertDayShiftDisplay(await saveNightShift(page, target, false))
        currentEnabled = false
      }

      const enabledRow = await saveNightShift(page, target, true)
      currentEnabled = true
      await assertDoubleShiftDisplay(enabledRow)
      await enabledRow.screenshot({ path: path.join(artifactDir, 'double-shift-row.png') })
      await page.screenshot({ path: path.join(artifactDir, 'double-shift-page.png'), fullPage: true })
    } finally {
      if (currentEnabled !== originalEnabled) {
        const restoredRow = await saveNightShift(page, target, originalEnabled)
        currentEnabled = originalEnabled
        if (originalEnabled) {
          await assertDoubleShiftDisplay(restoredRow)
        } else {
          await assertDayShiftDisplay(restoredRow)
        }
      }
    }

    writeJson('e2e-result.json', {
      result: 'PASS',
      baseUrl,
      tenantId: 122,
      tenant,
      username,
      processId: target.processId,
      processCode: target.processCode,
      processName: target.processName,
      originalNightShiftEnabled: originalEnabled,
      restoredNightShiftEnabled: originalEnabled,
      verifiedStatus: '\u767d\u591c\u73ed',
      verifiedMultiplier: 'X2'
    })
    console.log(
      `GREEN: process-wip-double-shift-real-e2e -> PASS, tenantId=122, username=${username}, processId=${target.processId}, processCode=${target.processCode}, restored=${originalEnabled}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  ensureArtifactDir()
  fs.writeFileSync(path.join(artifactDir, 'e2e-error.txt'), `${error.stack || error.message}\n`, 'utf8')
  console.error(`BLOCKER: process-wip-double-shift-real-e2e -> ${error.stack || error.message}`)
  process.exit(1)
})
