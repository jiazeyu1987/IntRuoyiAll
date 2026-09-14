const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const repoRoot = 'D:\\IntRuoyiWorktree\\20260728-codex-node-chain-first-node-contract'
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(frontendRoot, 'package.json'))
const { chromium } = frontendRequire('playwright')

const frontendBaseUrl = 'http://127.0.0.1:8083'
const apiBase = 'http://127.0.0.1:48083/admin-api'
const outputDir = path.join(
  'E:\\IntRuoyi',
  'output',
  'playwright',
  '20260728-codex-node-chain-first-node-contract'
)
const summaryPath = path.join(outputDir, 'summary.json')
const screenshotPath = path.join(outputDir, 'final.png')
const startedAt = Date.now()
const chainName = `Codex首节点契约-${startedAt}`
const firstCaseName = `${chainName}-第1节点`
const secondCaseName = `${chainName}-第2节点`

function parseEnvFile(filePath) {
  const result = {}
  for (const rawLine of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue
    const match = line.match(/^([^=]+?)\s*=\s*(.*)$/)
    if (!match) continue
    result[match[1].trim()] = match[2].trim().replace(/^['"]|['"]$/g, '')
  }
  return result
}

function mysql(query) {
  const escapedQuery = query.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$')
  const command = `mysql --default-character-set=utf8mb4 --batch --raw --skip-column-names -uroot -p"$MYSQL_ROOT_PASSWORD" ruoyi-vue-pro -e "${escapedQuery}"`
  return execFileSync('docker', ['exec', 'int-ruoyi-mysql', 'sh', '-lc', command], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe']
  }).trim()
}

async function waitFor(predicate, timeoutMs, description) {
  const started = Date.now()
  let lastValue
  while (Date.now() - started < timeoutMs) {
    lastValue = await predicate()
    if (lastValue) return lastValue
    await new Promise((resolve) => setTimeout(resolve, 3000))
  }
  throw new Error(`Timed out waiting for ${description}; last=${JSON.stringify(lastValue)}`)
}

async function firstVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) return item
  }
  throw new Error(`No visible element found for ${label}`)
}

async function fillFirstVisible(locator, value, label) {
  const item = await firstVisible(locator, label)
  await item.fill(value)
}

async function waitCaseTableIdle(page) {
  await page.locator('#pane-cases .el-loading-mask:visible').waitFor({ state: 'hidden', timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(300)
}

async function selectTenant(page, tenant) {
  const form = page.locator('form.login-form:visible').first()
  const tenantSelect = form.locator('.el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) return
  await tenantSelect.click()
  const input = form.locator('.el-select__input').first()
  if ((await input.count()) > 0 && (await input.isVisible())) {
    await input.fill(tenant)
  }
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function login(page, config) {
  await page.goto(`${frontendBaseUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  if (!page.url().includes('/login')) return
  await selectTenant(page, config.tenant)
  await fillFirstVisible(
    page.locator('.login-form input[placeholder="请输入用户名"], .login-form input.el-input__inner:not([type="password"]):not([role="combobox"])'),
    config.username,
    'username input'
  )
  await fillFirstVisible(
    page.locator('.login-form input[type="password"], .login-form input[placeholder="请输入密码"]'),
    config.password,
    'password input'
  )
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await page.locator('button:has-text("登录")').first().click()
  const loginPayload = await (await loginResponsePromise).json()
  assert.equal(loginPayload.code, 0, `login should succeed for ${config.tenant}/${config.username}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
}

async function openTestManagement(page) {
  await page.getByText('系统管理', { exact: true }).first().click()
  const menu = page.getByText('测试管理', { exact: true }).first()
  await menu.waitFor({ state: 'visible', timeout: 30000 })
  const pageResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/codex-test-case/page') && response.status() === 200,
    { timeout: 30000 }
  )
  await menu.click()
  const payload = await (await pageResponsePromise).json()
  assert.equal(payload.code, 0, 'test case page API should return business code 0')
  await page.locator('#pane-cases .codex-runner-status__label').waitFor({ state: 'visible', timeout: 30000 })
}

async function chooseVisibleSelectOption(page, formItem, optionText) {
  await formItem.locator('.el-select').first().click()
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function fillNodeChain(dialog, page, sort) {
  const chainItem = dialog.locator('.el-form-item').filter({ hasText: /^节点串/ }).first()
  const chainInput = chainItem.locator('input').first()
  await chainInput.click()
  await chainInput.fill(chainName)
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: chainName }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  const sortItem = dialog.locator('.el-form-item').filter({ hasText: '串内序号' }).first()
  const sortInput = sortItem.locator('input').first()
  await sortInput.waitFor({ state: 'visible', timeout: 30000 })
  if ((await sortInput.inputValue()) !== String(sort)) {
    await waitFor(async () => (await sortInput.isEnabled()) ? true : null, 10000, 'node chain sort input enabled')
    await sortInput.fill(String(sort))
  }
}

async function createNodeChainCase(page, caseName, sort) {
  await page.locator('#pane-cases button:has-text("新增测试项")').first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '测试项名称' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[placeholder="例如：排产手动重排工单校验"]').fill(caseName)
  await chooseVisibleSelectOption(page, dialog.locator('.el-form-item').filter({ hasText: '项目' }).first(), '批记录')
  await fillNodeChain(dialog, page, sort)
  await dialog.locator('input[placeholder="测试方法，例如：打开排产工单页"]').fill(
    `只读检查：使用 Playwright 登录 ${frontendBaseUrl}，进入系统管理 > 测试管理，确认页面显示“测试管理”“测试项”“Runner 状态”。不要新增、修改或删除任何业务数据。`
  )
  await dialog.locator('textarea[placeholder^="用户手写数据"]').fill(
    `任务自有节点串=${chainName}；当前节点序号=${sort}；只读检查测试管理页面。`
  )
  await dialog.locator('input[placeholder="目标项名称"]').fill('测试管理只读区域可见')
  await dialog.locator('textarea[placeholder^="按行录入测试目标"]').fill(
    '页面显示“测试管理”“测试项”“Runner 状态”。'
  )
  const createResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/codex-test-case/create') && response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await dialog.locator('button:has-text("保存")').click()
  const payload = await (await createResponsePromise).json()
  assert.equal(payload.code, 0, `create case should succeed for ${caseName}`)
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  await waitCaseTableIdle(page)
  await findNodeChainCaseRow(page, chainName, sort)
  return payload.data
}

async function selectTargetTenant(page, tenant) {
  const tenantFilter = page.locator('.codex-test-tenant-filter .el-select').first()
  await tenantFilter.click()
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function findCaseRow(page, caseName) {
  await waitCaseTableIdle(page)
  await page.getByText(caseName, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  const row = page.locator('.el-table__body-wrapper tbody tr:visible').filter({ hasText: caseName }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  return row
}

async function findNodeChainCaseRow(page, nodeChainName, sort) {
  await waitCaseTableIdle(page)
  const nodeTagText = `第 ${sort} 节点`
  const row = page
    .locator('.el-table__body-wrapper tbody tr:visible')
    .filter({ hasText: nodeChainName })
    .filter({ hasText: nodeTagText })
    .first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await assertVisibleText(row, nodeTagText)
  return row
}

async function executeFirstNode(page) {
  const row = await findNodeChainCaseRow(page, chainName, 1)
  await assertVisibleText(row, firstCaseName)
  await assertVisibleText(row, '第 1 节点')
  const startResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/codex-test-execution/start') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const executeButton = row.locator('button:has-text("执行")').first()
  await executeButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await executeButton.isEnabled(), true, 'first node row execute button should be enabled')
  const [response] = await Promise.all([
    startResponsePromise,
    executeButton.click()
  ])
  const payload = await response.json()
  assert.equal(payload.code, 0, `start execution should succeed: ${payload.msg || payload.code}`)
  await page.getByText('节点串必须从第 1 节点开始连续选择', { exact: false })
    .waitFor({ state: 'hidden', timeout: 3000 })
    .catch(() => {
      throw new Error('page still shows node-chain continuity contract error')
    })
  return payload.data
}

async function assertVisibleText(scope, text) {
  await scope.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
}

function readExecutionState(executionId) {
  const executionRaw = mysql(`SELECT status,COALESCE(summary,''),COALESCE(runner_session_id,0) FROM system_codex_test_execution WHERE id=${Number(executionId)};`)
  const caseRaw = mysql(`SELECT status,COALESCE(failure_reason,'') FROM system_codex_test_execution_case WHERE execution_id=${Number(executionId)} AND deleted=0 ORDER BY id LIMIT 1;`)
  const checkpointRaw = mysql(`SELECT r.status,COALESCE(r.actual_text,''),COALESCE(r.mismatch_description,'') FROM system_codex_test_checkpoint_result r JOIN system_codex_test_execution_case ec ON ec.id=r.execution_case_id WHERE ec.execution_id=${Number(executionId)} AND r.deleted=0 ORDER BY r.checkpoint_sort LIMIT 1;`)
  const [status, summary, runnerSessionId] = executionRaw.split('\t')
  const [caseStatus, failureReason] = caseRaw.split('\t')
  const [checkpointStatus, actualText, mismatchDescription] = checkpointRaw.split('\t')
  return {
    status,
    summary,
    runnerSessionId: Number(runnerSessionId),
    caseStatus,
    failureReason,
    checkpointStatus,
    actualText,
    mismatchDescription
  }
}

async function waitExecutionPass(executionId) {
  return await waitFor(() => {
    const state = readExecutionState(executionId)
    if (['PASS', 'FAIL', 'CANCELLED'].includes(state.status)) {
      return state
    }
    return null
  }, 600000, `execution ${executionId} terminal state`)
}

async function deleteCaseByName(page, caseName) {
  await page.getByRole('tab', { name: '测试项' }).click()
  const row = await findCaseRow(page, caseName)
  await row.locator('button:has-text("删除")').first().click()
  const confirmButton = page.locator('.el-message-box:visible button:has-text("确定")').last()
  await confirmButton.waitFor({ state: 'visible', timeout: 30000 })
  const [response] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/codex-test-case/delete') && response.request().method() === 'DELETE',
      { timeout: 30000 }
    ),
    confirmButton.click()
  ])
  const payload = await response.json()
  assert.equal(payload.code, 0, `delete case should succeed for ${caseName}`)
  await waitCaseTableIdle(page)
  await page.getByText(caseName, { exact: false }).first().waitFor({ state: 'hidden', timeout: 30000 })
}

async function main() {
  fs.mkdirSync(outputDir, { recursive: true })
  const env = parseEnvFile(path.join(frontendRoot, '.env'))
  const loginConfig = {
    tenant: env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: env.VITE_APP_DEFAULT_LOGIN_PASSWORD
  }
  assert.ok(loginConfig.tenant && loginConfig.username && loginConfig.password, 'default login config is required')

  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const consoleErrors = []
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })

  const summary = {
    frontendBaseUrl,
    apiBase,
    tenant: loginConfig.tenant,
    username: loginConfig.username,
    chainName,
    createdCaseIds: [],
    executionId: null,
    executionState: null,
    consoleErrorCount: 0
  }

  try {
    await login(page, loginConfig)
    await openTestManagement(page)
    await selectTargetTenant(page, loginConfig.tenant)
    summary.createdCaseIds.push(await createNodeChainCase(page, firstCaseName, 1))
    summary.createdCaseIds.push(await createNodeChainCase(page, secondCaseName, 2))
    summary.executionId = await executeFirstNode(page)
    summary.executionState = await waitExecutionPass(summary.executionId)
    assert.equal(summary.executionState.status, 'PASS', `execution should pass: ${JSON.stringify(summary.executionState)}`)
    assert.equal(summary.executionState.caseStatus, 'PASS', `execution case should pass: ${JSON.stringify(summary.executionState)}`)
    assert.equal(summary.executionState.checkpointStatus, 'PASS', `checkpoint should pass: ${JSON.stringify(summary.executionState)}`)
    await deleteCaseByName(page, secondCaseName)
    await deleteCaseByName(page, firstCaseName)
    summary.consoleErrorCount = consoleErrors.length
    assert.equal(consoleErrors.length, 0, `console errors: ${consoleErrors.join('\n')}`)
    await page.screenshot({ path: screenshotPath, fullPage: true })
    fs.writeFileSync(summaryPath, JSON.stringify(summary, null, 2), 'utf8')
    console.log(`PASS: node-chain first-node execution id=${summary.executionId}`)
  } catch (error) {
    summary.error = error.stack || String(error)
    summary.consoleErrors = consoleErrors
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => undefined)
    fs.writeFileSync(summaryPath, JSON.stringify(summary, null, 2), 'utf8')
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
