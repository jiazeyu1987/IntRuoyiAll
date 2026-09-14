const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const BASE_URL = (process.env.EDHR_FRONTLINE_E2E_BASE_URL || 'http://127.0.0.1:8083').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_FRONTLINE_E2E_BACKEND_URL || 'http://127.0.0.1:48083').replace(/\/+$/, '')
const TENANT = process.env.EDHR_FRONTLINE_E2E_TENANT || '芋道源码'
const USERNAME = process.env.EDHR_FRONTLINE_E2E_USERNAME || 'admin'
const PASSWORD = process.env.EDHR_FRONTLINE_E2E_PASSWORD || ''
const MARKER = 'CODX-EDHR-FRONTLINE-20260730'
const OUTPUT_DIR = path.resolve(process.cwd(), 'output', 'playwright', '20260730-edhr-frontline-fill-tabs-yudao-real')
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.PLAYWRIGHT_CHROME_EXECUTABLE ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function mysql(sql) {
  return execFileSync(
    'docker',
    [
      'exec',
      'int-ruoyi-mysql',
      'sh',
      '-lc',
      `mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" -D ruoyi-vue-pro -N -B <<'SQL'\n${sql}\nSQL`
    ],
    { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }
  )
}

function cleanupFixture() {
  const sql = `
SET @marker = _utf8mb4'${MARKER}' COLLATE utf8mb4_unicode_ci;
DELETE wsm FROM mes_md_workstation_machine wsm
JOIN mes_md_workstation ws ON ws.id = wsm.workstation_id
WHERE ws.remark = @marker;
DELETE wsw FROM mes_md_workstation_worker wsw
JOIN mes_md_workstation ws ON ws.id = wsw.workstation_id
WHERE ws.remark = @marker;
DELETE rp FROM mes_pro_route_process rp
JOIN mes_pro_route r ON r.id = rp.route_id
WHERE r.remark = @marker;
DELETE FROM mes_pro_route WHERE remark = @marker;
DELETE FROM mes_pro_process WHERE remark = @marker;
DELETE FROM mes_dv_machinery WHERE remark = @marker;
DELETE FROM mes_md_workstation WHERE remark = @marker;
SELECT 'remaining', (
  SELECT COUNT(*) FROM mes_pro_route WHERE remark = @marker
) + (
  SELECT COUNT(*) FROM mes_pro_process WHERE remark = @marker
) + (
  SELECT COUNT(*) FROM mes_md_workstation WHERE remark = @marker
) + (
  SELECT COUNT(*) FROM mes_dv_machinery WHERE remark = @marker
);
`
  const output = mysql(sql)
  const remainingLine = output
    .trim()
    .split(/\r?\n/)
    .find((line) => line.startsWith('remaining\t'))
  const remaining = Number(remainingLine?.split('\t')[1] || 0)
  assert.equal(remaining, 0, `fixture cleanup left task-owned rows: ${output}`)
  return output
}

function setupFixture() {
  cleanupFixture()
  const sql = `
SET @marker = _utf8mb4'${MARKER}' COLLATE utf8mb4_unicode_ci;
START TRANSACTION;
INSERT INTO mes_pro_process (product_name, code, name, attention, status, remark, creator, tenant_id)
VALUES
  ('CODX', '${MARKER}-PROC-THREE', 'CODX Three Device', NULL, 0, @marker, 'codex', 1);
SET @proc_three = LAST_INSERT_ID();
INSERT INTO mes_pro_process (product_name, code, name, attention, status, remark, creator, tenant_id)
VALUES
  ('CODX', '${MARKER}-PROC-NODEV', 'CODX No Device', NULL, 0, @marker, 'codex', 1);
SET @proc_nodev = LAST_INSERT_ID();
INSERT INTO mes_pro_process (product_name, code, name, attention, status, remark, creator, tenant_id)
VALUES
  ('CODX', '${MARKER}-PROC-PQC', 'CODX PQC Check', NULL, 0, @marker, 'codex', 1);
SET @proc_pqc = LAST_INSERT_ID();

INSERT INTO mes_md_workstation (code, name, process_id, status, remark, creator, tenant_id)
VALUES
  ('${MARKER}-WS-THREE', 'CODX Three Device WS', @proc_three, 0, @marker, 'codex', 1);
SET @ws_three = LAST_INSERT_ID();
INSERT INTO mes_md_workstation (code, name, process_id, status, remark, creator, tenant_id)
VALUES
  ('${MARKER}-WS-NODEV', 'CODX No Device WS', @proc_nodev, 0, @marker, 'codex', 1);
SET @ws_nodev = LAST_INSERT_ID();
INSERT INTO mes_md_workstation (code, name, process_id, status, remark, creator, tenant_id)
VALUES
  ('${MARKER}-WS-PQC', 'CODX PQC WS', @proc_pqc, 0, @marker, 'codex', 1);
SET @ws_pqc = LAST_INSERT_ID();

INSERT INTO mes_dv_machinery (code, name, status, remark, creator, tenant_id)
VALUES
  ('${MARKER}-DEV-1', 'CODX Pump 1', 0, @marker, 'codex', 1);
SET @dev1 = LAST_INSERT_ID();
INSERT INTO mes_dv_machinery (code, name, status, remark, creator, tenant_id)
VALUES
  ('${MARKER}-DEV-2', 'CODX Pump 2', 0, @marker, 'codex', 1);
SET @dev2 = LAST_INSERT_ID();
INSERT INTO mes_dv_machinery (code, name, status, remark, creator, tenant_id)
VALUES
  ('${MARKER}-DEV-3', 'CODX Pump 3', 0, @marker, 'codex', 1);
SET @dev3 = LAST_INSERT_ID();

INSERT INTO mes_md_workstation_machine (workstation_id, machinery_id, quantity, remark, creator, tenant_id)
VALUES
  (@ws_three, @dev1, 1, @marker, 'codex', 1),
  (@ws_three, @dev2, 1, @marker, 'codex', 1),
  (@ws_three, @dev3, 1, @marker, 'codex', 1);

INSERT INTO mes_md_workstation_worker (workstation_id, post_id, quantity, remark, creator, tenant_id)
VALUES
  (@ws_three, 14, 1, @marker, 'codex', 1),
  (@ws_nodev, 14, 1, @marker, 'codex', 1),
  (@ws_pqc, 14, 1, @marker, 'codex', 1);

INSERT INTO mes_pro_route (code, name, description, status, remark, creator, tenant_id)
VALUES ('${MARKER}-ROUTE', 'CODX Frontline Route', @marker, 0, @marker, 'codex', 1);
SET @route = LAST_INSERT_ID();

INSERT INTO mes_pro_route_process (
  route_id, process_id, workstation_id, sort, next_process_id, link_type,
  prepare_time, wait_time, key_flag, check_flag, remark, creator, tenant_id
) VALUES
  (@route, @proc_three, @ws_three, 10, NULL, 1, 0, 0, b'1', b'0', @marker, 'codex', 1);
SET @rp_three = LAST_INSERT_ID();
INSERT INTO mes_pro_route_process (
  route_id, process_id, workstation_id, sort, next_process_id, link_type,
  prepare_time, wait_time, key_flag, check_flag, remark, creator, tenant_id
) VALUES
  (@route, @proc_nodev, @ws_nodev, 20, NULL, 1, 0, 0, b'0', b'0', @marker, 'codex', 1);
SET @rp_nodev = LAST_INSERT_ID();
INSERT INTO mes_pro_route_process (
  route_id, process_id, workstation_id, sort, next_process_id, link_type,
  prepare_time, wait_time, key_flag, check_flag, remark, creator, tenant_id
) VALUES
  (@route, @proc_pqc, @ws_pqc, 30, NULL, 1, 0, 0, b'0', b'1', @marker, 'codex', 1);
SET @rp_pqc = LAST_INSERT_ID();
COMMIT;

SELECT JSON_OBJECT(
  'routeId', @route,
  'threeDeviceRouteProcessId', @rp_three,
  'noDeviceRouteProcessId', @rp_nodev,
  'pqcRouteProcessId', @rp_pqc,
  'threeDeviceProcessId', @proc_three,
  'noDeviceProcessId', @proc_nodev,
  'pqcProcessId', @proc_pqc,
  'threeDeviceWorkstationId', @ws_three,
  'noDeviceWorkstationId', @ws_nodev,
  'pqcWorkstationId', @ws_pqc,
  'device1Id', @dev1,
  'device2Id', @dev2,
  'device3Id', @dev3
);
`
  const output = mysql(sql).trim()
  const jsonLine = output
    .split(/\r?\n/)
    .reverse()
    .find((line) => line.trim().startsWith('{'))
  assert.ok(jsonLine, `fixture setup did not return ids: ${output}`)
  return JSON.parse(jsonLine)
}

async function assertServiceReady() {
  const frontend = await fetch(BASE_URL)
  assert.equal(frontend.status, 200, `frontend must return HTTP 200 at ${BASE_URL}`)
  const backend = await fetch(`${BACKEND_URL}/actuator/health`)
  assert.equal(backend.status, 200, `backend health must return HTTP 200 at ${BACKEND_URL}`)
  const body = await backend.json()
  assert.equal(body.status, 'UP', `backend health must be UP: ${JSON.stringify(body)}`)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible().catch(() => false)) && (await item.isEnabled().catch(() => false))) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`missing visible input: ${label}`)
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible().catch(() => false))) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TENANT }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), TENANT, 'tenant')
}

async function login(page) {
  const loginUrl = `${BASE_URL}/login?redirect=${encodeURIComponent('/index')}`
  await page.context().clearCookies()
  await page.goto(loginUrl, { waitUntil: 'domcontentloaded', timeout: 90000 })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 90000 })
  if ((await form.locator('.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder*="验证码"]:visible').count()) > 0) {
    throw new Error('login captcha is enabled; real unattended E2E cannot continue')
  }

  await selectTenant(page, form)
  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    USERNAME,
    'username'
  )
  const passwordInput = form.locator('input[type="password"]').first()
  await passwordInput.waitFor({ state: 'visible', timeout: 30000 })
  if (PASSWORD) {
    await passwordInput.fill(PASSWORD)
  } else {
    const existingPassword = await passwordInput.inputValue()
    assert.ok(existingPassword, 'EDHR_FRONTLINE_E2E_PASSWORD is required when the login form is not prefilled')
  }

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  assert.ok(loginResponse.ok(), `login HTTP failed: ${loginResponse.status()}`)
  const loginBody = await loginResponse.json()
  assert.ok([0, 200].includes(loginBody.code), `login business failed: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function parseBusinessResponse(response, label) {
  assert.ok(response.ok(), `${label} HTTP failed: ${response.status()}`)
  const body = await response.json()
  assert.ok([0, 200].includes(body.code), `${label} business failed: ${body.msg || body.code}`)
  return body.data
}

function pageUrl(targetPath, query = {}) {
  const url = new URL(targetPath, BASE_URL)
  for (const [key, value] of Object.entries(query)) {
    url.searchParams.set(key, String(value))
  }
  return url.toString()
}

async function openFrontlinePage(page, targetPath, query, expectedSelector) {
  const processesResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/feedback/frontline/device-account/processes') &&
      response.request().method() === 'GET',
    { timeout: 90000 }
  )
  await page.goto(pageUrl(targetPath, query), { waitUntil: 'domcontentloaded', timeout: 90000 })
  const processData = await parseBusinessResponse(await processesResponsePromise, 'frontline processes')
  assert.ok(Array.isArray(processData), 'frontline processes response data must be an array')
  await page.locator(expectedSelector).waitFor({ state: 'visible', timeout: 90000 })
  return processData
}

async function clickTopCard(screen, label) {
  const card = screen.locator('.frontline-top-card').filter({ hasText: label }).first()
  await card.waitFor({ state: 'visible', timeout: 30000 })
  await card.click()
}

async function selectProcess(page, label) {
  const option = page.locator('.frontline-picker__options button').filter({ hasText: label }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function fillProductionNumber(screen, label, value) {
  const row = screen.locator('.frontline-production-field').filter({ hasText: label }).first()
  await fillFirstVisible(row.locator('input'), value, label)
}

async function fillInspectionNumber(screen, label, value) {
  const row = screen.locator('.frontline-inspection-row').filter({ hasText: label }).first()
  await fillFirstVisible(row.locator('input'), value, label)
}

async function assertNotVisibleText(page, forbiddenText) {
  assert.equal(await page.getByText(forbiddenText, { exact: false }).count(), 0, `${forbiddenText} must not be visible`)
}

async function verifyProductionPage(page, fixture) {
  const processData = await openFrontlinePage(
    page,
    '/mes/pro/feedback/edhr-batch-production-fill',
    {
      workOrderId: 20260730,
      routeId: fixture.routeId,
      productionOrderCode: `${MARKER}-WO`
    },
    '[data-frontline-production-operator]'
  )
  assert.equal(
    processData.filter((item) => item.routeProcessId === fixture.threeDeviceRouteProcessId).length,
    3,
    'three-device process must return three device rows before UI dedupe'
  )

  const screen = page.locator('[data-frontline-production-operator]')
  await screen.getByText('工序', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  await screen.getByText('员工', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  await screen.getByText('主页', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  await assertNotVisibleText(screen, '生产订单')
  await assertNotVisibleText(screen, '生产工单')
  await assertNotVisibleText(screen, '工单')

  await clickTopCard(screen, '工序')
  await selectProcess(page, 'CODX Three Device')
  await screen.getByText('CODX Three Device').waitFor({ state: 'visible', timeout: 30000 })
  await screen.getByText('准备提交', { exact: false }).waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await screen.locator('.frontline-device-card').count(), 3, 'production page must cap device cards at 3')
  for (const [index, value] of ['120C', '0.35MPa', '45Hz'].entries()) {
    await fillFirstVisible(screen.locator('.frontline-device-card').nth(index).locator('input'), value, `device ${index + 1}`)
  }
  await fillProductionNumber(screen, '上工序输入数量', '100')
  await fillProductionNumber(screen, '输出数量', '96')
  await fillProductionNumber(screen, '损耗数量', '4')

  const validateResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/feedback/frontline-template/payload/validate') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await screen.getByRole('button', { name: '提交' }).click()
  const payload = await parseBusinessResponse(await validateResponsePromise, 'production payload validate')
  assert.equal(payload.templateCode, 'PRODUCTION_SIMPLIFIED', 'production submit must validate production template')
  assert.equal(payload.fieldValues.OUTPUT_QUANTITY, 96, 'production output quantity must be sent as number')

  await page.screenshot({ path: path.join(OUTPUT_DIR, 'production-three-device-1920.png'), fullPage: true })

  await clickTopCard(screen, '工序')
  await selectProcess(page, 'CODX No Device')
  await screen.getByText('CODX No Device').waitFor({ state: 'visible', timeout: 30000 })
  await screen.locator('.frontline-no-device').getByText('本工序无设备，直接填数量').waitFor({
    state: 'visible',
    timeout: 30000
  })
  assert.equal(await screen.locator('.frontline-device-card').count(), 0, 'no-device process must show no device cards')
  await page.screenshot({ path: path.join(OUTPUT_DIR, 'production-no-device-1920.png'), fullPage: true })
}

async function verifyPqcPage(page, fixture) {
  await openFrontlinePage(
    page,
    '/mes/pro/feedback/edhr-batch-pqc-fill',
    {
      workOrderId: 20260730,
      routeId: fixture.routeId,
      productionOrderCode: `${MARKER}-WO`
    },
    '[data-frontline-pqc-operator]'
  )

  const screen = page.locator('[data-frontline-pqc-operator]')
  await screen.getByText('生产订单', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  await screen.getByText(`${MARKER}-WO`, { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  await screen.getByText('工序', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  await screen.getByText('员工', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  await screen.getByText('主页', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })

  await clickTopCard(screen, '工序')
  await selectProcess(page, 'CODX PQC Check')
  await screen.getByText('CODX PQC Check').waitFor({ state: 'visible', timeout: 30000 })
  await screen.getByText('准备提交', { exact: false }).waitFor({ state: 'visible', timeout: 30000 })

  for (const text of ['检验内容', '长度', '外观', '密封', '压力', '首检', '巡检', '末检', '检验数量', '损耗数量']) {
    await screen.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  }
  for (const text of ['检验方法', '成功', '失败', '巡检摘要']) {
    await assertNotVisibleText(screen, text)
  }

  await fillInspectionNumber(screen, '长度', '12')
  await fillInspectionNumber(screen, '压力', '0.8')
  await screen.getByRole('button', { name: '巡检' }).click()
  await screen.getByRole('button', { name: '第 2 次' }).click()
  await fillFirstVisible(screen.locator('.frontline-number-grid label').filter({ hasText: '检验数量' }).locator('input'), '25', 'inspection quantity')
  await fillFirstVisible(screen.locator('.frontline-number-grid label').filter({ hasText: '损耗数量' }).locator('input'), '1', 'pqc scrap quantity')
  await page.screenshot({ path: path.join(OUTPUT_DIR, 'pqc-fill-1920.png'), fullPage: true })
}

async function run() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  assert.match(BASE_URL, /^http:\/\/127\.0\.0\.1:8083$/, 'real E2E must use the registered int_main slot frontend 8083')
  assert.match(BACKEND_URL, /^http:\/\/127\.0\.0\.1:48083$/, 'real E2E must use the registered int_main slot backend 48083')
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `Chrome executable not found: ${BROWSER_EXECUTABLE}`)
  await assertServiceReady()

  const fixture = setupFixture()
  const browser = await chromium.launch({
    headless: process.env.EDHR_FRONTLINE_E2E_HEADED !== '1',
    executablePath: BROWSER_EXECUTABLE
  })
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const consoleErrors = []
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => {
    consoleErrors.push(error.message)
  })

  try {
    await login(page)
    await verifyProductionPage(page, fixture)
    await verifyPqcPage(page, fixture)
    assert.deepEqual(consoleErrors, [], `browser console/page errors: ${consoleErrors.join('\n')}`)
    const evidence = {
      marker: MARKER,
      tenant: TENANT,
      username: USERNAME,
      baseUrl: BASE_URL,
      backendUrl: BACKEND_URL,
      fixture,
      screenshots: [
        path.join(OUTPUT_DIR, 'production-three-device-1920.png'),
        path.join(OUTPUT_DIR, 'production-no-device-1920.png'),
        path.join(OUTPUT_DIR, 'pqc-fill-1920.png')
      ]
    }
    fs.writeFileSync(path.join(OUTPUT_DIR, 'result.json'), `${JSON.stringify(evidence, null, 2)}\n`, 'utf8')
    console.log(`PASS: eDHR frontline fill tabs real E2E marker=${MARKER} tenant=${TENANT} username=${USERNAME}`)
  } finally {
    await context.close().catch(() => undefined)
    await browser.close().catch(() => undefined)
    cleanupFixture()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
