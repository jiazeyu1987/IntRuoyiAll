const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('Playwright is required for MES scheduler night shift save validation E2E.')
  }
}

function requiredEnv(name) {
  const value = process.env[name]?.trim()
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`)
  }
  return value
}

function requiredPositiveIntegerEnv(name) {
  const value = Number(requiredEnv(name))
  if (!Number.isSafeInteger(value) || value <= 0) {
    throw new Error(`Environment variable must be a positive integer: ${name}`)
  }
  return value
}

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')
const artifactDir = path.join(
  workspaceRoot,
  'doc',
  'tasks',
  '20260813-scheduler-seven-issues-closure',
  'artifacts',
  'night-shift-save-validation'
)

const config = {
  baseUrl: (process.env.MES_SCHEDULER_NIGHT_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: requiredEnv('MES_SCHEDULER_NIGHT_E2E_TENANT'),
  username: requiredEnv('MES_SCHEDULER_NIGHT_E2E_USERNAME'),
  password: requiredEnv('MES_SCHEDULER_NIGHT_E2E_PASSWORD'),
  scheduleOrderId: requiredPositiveIntegerEnv('MES_SCHEDULER_NIGHT_E2E_SCHEDULE_ORDER_ID'),
  processName: process.env.MES_SCHEDULER_NIGHT_E2E_PROCESS_NAME || '吹球囊成型',
  headed: process.env.MES_SCHEDULER_NIGHT_E2E_HEADED === '1'
}

function ensureArtifactDir() {
  fs.mkdirSync(artifactDir, { recursive: true })
  fs.rmSync(path.join(artifactDir, 'e2e-error.txt'), { force: true })
}

function writeJson(name, payload) {
  fs.writeFileSync(path.join(artifactDir, name), JSON.stringify(payload, null, 2), 'utf8')
}

function parseResponseBody(response) {
  return response.json().catch(async () => ({ raw: await response.text().catch(() => '') }))
}

async function settle(page) {
  await page.waitForLoadState('domcontentloaded', { timeout: 60000 }).catch(() => {})
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.click()
      await item.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/scheduler-workbench')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/scheduler-workbench')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }
  if ((await page.locator('.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
    await page.keyboard.type(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    if ((await tenantOption.count()) > 0) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
      await tenantInput.press('Tab')
    }
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"], input.el-input__inner').first(), config.tenant, 'tenant')
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"])'), config.username, 'username')
  await fillFirstVisible(form.locator('input[placeholder="请输入密码"], input[type="password"]'), config.password, 'password')

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: /登录/ }).click()
  ])
  const loginBody = await parseResponseBody(loginResponse)
  assert.ok(loginResponse.ok(), `login HTTP ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginBody.code), `login failed: ${loginBody.msg || loginBody.code}`)
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
  await page.waitForFunction(() => !window.location.href.includes('/login'), null, { timeout: 60000 })
}

async function openWorkbenchAndReadRows(page) {
  const rowsPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/process-wip-statistics') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/scheduler-workbench?nightShiftValidationE2e=${Date.now()}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const rowsResponse = await rowsPromise
  const rowsBody = await parseResponseBody(rowsResponse)
  assert.equal(rowsBody.code, 0, `process WIP failed: ${rowsBody.msg || rowsBody.code}`)
  const rows = Array.isArray(rowsBody.data) ? rowsBody.data : []
  assert.ok(rows.length > 0, '测试租户工序在制列表必须有真实数据')
  await page.locator('.scheduler-workbench__process-wip-table').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByText('夜班', { exact: true }).first().waitFor({ state: 'visible', timeout: 60000 })
  writeJson('process-wip-statistics-before.json', {
    rowCount: rows.length,
    sample: rows.slice(0, 20).map((row) => ({
      processId: row.processId,
      processCode: row.processCode,
      processName: row.processName,
      nightShiftEnabled: row.nightShiftEnabled,
      nightShiftMixed: row.nightShiftMixed,
      shiftCapacityTotal: row.shiftCapacityTotal,
      scheduleOrderIds: row.scheduleOrderIds
    }))
  })
  return rows
}

function selectTargetRow(rows) {
  const normalizedName = config.processName.trim()
  const candidates = rows.filter(
    (row) =>
      row.routeCode &&
      row.processCode &&
      String(row.processName || '').includes(normalizedName) &&
      Array.isArray(row.scheduleOrderIds) &&
      row.scheduleOrderIds.length === 1 &&
      Number(row.scheduleOrderIds[0]) === config.scheduleOrderId &&
      !row.nightShiftMixed &&
      !Boolean(row.nightShiftEnabled)
  )
  assert.equal(
    candidates.length,
    1,
    `排产工单 ${config.scheduleOrderId} 必须且只能对应一条未开启夜班的“${normalizedName}”工作台行`
  )
  const target = candidates[0]
  const visibleIdentityMatches = rows.filter(
    (row) => row.routeCode === target.routeCode && row.processCode === target.processCode
  )
  assert.equal(
    visibleIdentityMatches.length,
    1,
    `目标行的可见身份不唯一：routeCode=${target.routeCode}, processCode=${target.processCode}`
  )
  return target
}

async function locateTargetRow(page, target) {
  const table = page.locator('.scheduler-workbench__process-wip-table').first()
  for (let pageIndex = 0; pageIndex < 10; pageIndex += 1) {
    const row = table
      .locator('.el-table__body-wrapper tbody tr')
      .filter({ hasText: target.routeCode })
      .filter({ hasText: target.processCode })
      .first()
    if ((await row.count()) > 0 && (await row.isVisible().catch(() => false))) {
      return row
    }
    const nextButton = page.locator('.scheduler-workbench__wip-tabs-panel .el-pagination button.btn-next').first()
    if ((await nextButton.count()) === 0 || (await nextButton.isDisabled())) {
      break
    }
    await nextButton.click()
    await page.waitForTimeout(500)
  }
  throw new Error(
    `页面中找不到目标工序行：routeCode=${target.routeCode}, processCode=${target.processCode}`
  )
}

async function readRowByProcessKey(page, target) {
  const rows = await page.evaluate(async (targetKey) => {
    const unwrapCacheValue = (value) => {
      if (!value || typeof value !== 'object') {
        return value
      }
      for (const field of ['accessToken', 'v', 'value', 'data']) {
        if (Object.prototype.hasOwnProperty.call(value, field)) {
          return unwrapCacheValue(value[field])
        }
      }
      return value
    }
    const readCache = (key) => {
      const storages = [localStorage, sessionStorage]
      let raw
      for (const storage of storages) {
        raw = storage.getItem(key)
        if (raw) break
        const matchedKey = Object.keys(storage).find((item) => item === key || item.endsWith(key))
        if (matchedKey) {
          raw = storage.getItem(matchedKey)
          break
        }
      }
      if (!raw) return undefined
      try {
        const parsed = JSON.parse(raw)
        const unwrapped = unwrapCacheValue(parsed)
        if (typeof unwrapped === 'string' && unwrapped.startsWith('"') && unwrapped.endsWith('"')) {
          return unwrapped.slice(1, -1)
        }
        return unwrapped
      } catch (error) {
        if (raw.startsWith('"') && raw.endsWith('"')) {
          return raw.slice(1, -1)
        }
        return raw
      }
    }
    const accessToken = readCache('ACCESS_TOKEN')
    const tenantId = readCache('tenantId')
    const visitTenantId = readCache('visitTenantId')
    const headers = { 'Cache-Control': 'no-cache', Pragma: 'no-cache' }
    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`
    }
    if (tenantId) {
      headers['tenant-id'] = String(tenantId)
    }
    if (visitTenantId && accessToken) {
      headers['visit-tenant-id'] = String(visitTenantId)
    }
    const response = await fetch('/admin-api/mes/pro/schedule-order/process-wip-statistics', {
      method: 'GET',
      credentials: 'omit',
      headers
    })
    const body = await response.json()
    if (body.code !== 0) {
      throw new Error(body.msg || `process WIP code ${body.code}`)
    }
    return Array.isArray(body.data) ? body.data : []
  }, { routeVersionId: target.routeVersionId, routeProcessId: target.routeProcessId })
  return rows.find(
    (row) =>
      Number(row.routeVersionId) === Number(target.routeVersionId) &&
      Number(row.routeProcessId) === Number(target.routeProcessId)
  )
}

async function restoreOriginalNightShiftState(page, target, originalEnabled) {
  const current = await readRowByProcessKey(page, target)
  if (!current || Boolean(current.nightShiftEnabled) === originalEnabled) {
    return { restored: false, current }
  }
  const row = await locateTargetRow(page, target)
  const switchControl = row.locator('.el-switch').first()
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/process-wip-settings') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await switchControl.click()
  const response = await responsePromise
  const body = await parseResponseBody(response)
  writeJson('restore-response.json', {
    status: response.status(),
    requestPayload: JSON.parse(response.request().postData() || '{}'),
    body
  })
  if (!response.ok() || ![0, 200].includes(body.code)) {
    throw new Error(`夜班状态异常成功后恢复失败: ${body.msg || body.code || response.status()}`)
  }
  return { restored: true, body }
}

async function verifyNightShiftSaveValidation(page, target) {
  const originalEnabled = Boolean(target.nightShiftEnabled)
  assert.equal(originalEnabled, false, '负向校验目标工序必须原本未开启夜班，避免污染现有夜班配置')
  const row = await locateTargetRow(page, target)
  await row.scrollIntoViewIfNeeded()
  await row.screenshot({ path: path.join(artifactDir, 'target-row-before-click.png') })
  const switchControl = row.locator('.el-switch').first()
  await switchControl.waitFor({ state: 'visible', timeout: 60000 })

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/process-wip-settings') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  await switchControl.click()
  const response = await responsePromise
  const requestPayload = JSON.parse(response.request().postData() || '{}')
  const body = await parseResponseBody(response)
  writeJson('night-shift-save-response.json', {
    status: response.status(),
    requestPayload,
    body
  })

  if (response.ok() && [0, 200].includes(body.code)) {
    const restoreResult = await restoreOriginalNightShiftState(page, target, originalEnabled)
    writeJson('unexpected-success-restore-result.json', restoreResult)
    throw new Error('缺夜班资源工序开启夜班保存异常成功，已尝试恢复原夜班状态')
  }

  const uiMessage = page
    .locator('.el-message:visible, .el-notification:visible')
    .filter({ hasText: /夜班|班次|产能|设备|工作站|产线/ })
    .last()
  await uiMessage.waitFor({ state: 'visible', timeout: 30000 })
  const uiMessageText = (await uiMessage.innerText()).trim()

  writeJson('night-shift-save-response.json', {
    status: response.status(),
    requestPayload,
    body,
    uiMessageText
  })

  assert.equal(
    Number(requestPayload.routeVersionId),
    Number(target.routeVersionId),
    '夜班保存请求必须命中当前路线版本'
  )
  assert.equal(
    Number(requestPayload.routeProcessId),
    Number(target.routeProcessId),
    '夜班保存请求必须命中当前路线工序'
  )
  assert.equal(Boolean(requestPayload.nightShiftEnabled), true, '夜班保存请求必须是开启夜班')
  assert.ok(response.ok(), `夜班保存接口 HTTP 必须返回可解析响应，got ${response.status()}`)
  assert.notEqual(body.code, 0, '缺夜班资源时保存不应成功')
  const messageText = String(body.msg || body.message || uiMessageText)
  assert.match(messageText, /夜班/, '失败信息必须明确指向夜班')
  assert.match(messageText, /(班次|产能|设备|工作站|产线)/, '失败信息必须指出缺少班次、设备、工作站、产线或产能')
  assert.match(uiMessageText, /夜班/, '页面必须即时展示夜班失败提示')
  assert.match(uiMessageText, /(班次|产能|设备|工作站|产线)/, '页面提示必须说明缺少哪类夜班资源')

  const after = await readRowByProcessKey(page, target)
  writeJson('selected-row-after-failed-save.json', after)
  assert.ok(after, `保存失败后仍应能读取工序 ${target.processId}`)
  assert.equal(Boolean(after.nightShiftEnabled), originalEnabled, '保存失败后夜班开关不得被写入为开启')
  await page.screenshot({ path: path.join(artifactDir, 'page-after-failed-save.png'), fullPage: true })
  return { response, body, uiMessageText, after }
}

async function main() {
  ensureArtifactDir()
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  let selectedTarget
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    await login(page)
    const rows = await openWorkbenchAndReadRows(page)
    const target = selectTargetRow(rows)
    selectedTarget = target
    writeJson('selected-row-before.json', target)
    const result = await verifyNightShiftSaveValidation(page, target)
    writeJson('e2e-result.json', {
      result: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      scheduleOrderId: config.scheduleOrderId,
      processId: target.processId,
      processCode: target.processCode,
      processName: target.processName,
      originalNightShiftEnabled: Boolean(target.nightShiftEnabled),
      savedNightShiftEnabled: Boolean(result.after.nightShiftEnabled),
      apiCode: result.body.code,
      apiMessage: result.body.msg || result.body.message,
      uiMessageText: result.uiMessageText,
      artifacts: [
        'process-wip-statistics-before.json',
        'selected-row-before.json',
        'target-row-before-click.png',
        'night-shift-save-response.json',
        'selected-row-after-failed-save.json',
        'page-after-failed-save.png'
      ]
    })
    console.log(
      `GREEN: night-shift-save-validation-real-e2e -> PASS, processId=${target.processId}, processName=${target.processName}, apiCode=${result.body.code}`
    )
  } catch (error) {
    fs.writeFileSync(path.join(artifactDir, 'e2e-error.txt'), `${error.stack || error.message}\n`, 'utf8')
    console.error(`BLOCKER: night-shift-save-validation-real-e2e -> ${error.stack || error.message}`)
    process.exit(1)
  } finally {
    await browser.close()
  }
}

main()
