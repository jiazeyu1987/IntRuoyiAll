const fs = require('fs')
const path = require('path')
const { execFileSync } = require('child_process')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(
      "Playwright is required for NAS permission real-data E2E tests. Run in an environment that can resolve the 'playwright' package."
    )
  }
}

function parseArgs() {
  const result = {}
  for (const arg of process.argv.slice(2)) {
    if (!arg.startsWith('--')) continue
    const [rawKey, ...rawValue] = arg.slice(2).split('=')
    result[rawKey] = rawValue.length ? rawValue.join('=') : 'true'
  }
  return result
}

const args = parseArgs()
const mode = args.mode || process.env.NAS_PERMISSION_E2E_MODE
const allowedModes = new Set(['test-write', 'test-mapping', 'test-blocker', 'admin-readonly', 'all'])

function requireMode() {
  if (!mode || !allowedModes.has(mode)) {
    throw new Error(
      'Missing E2E mode. Use --mode=test-write, --mode=test-mapping, --mode=test-blocker, --mode=admin-readonly, or --mode=all.'
    )
  }
}

function env(name, fallback) {
  const value = process.env[name]
  return value === undefined || value === '' ? fallback : value
}

function requireEnv(name, impact) {
  const value = process.env[name]
  if (!value) {
    throw new Error(`Missing required environment variable ${name}. Impact: ${impact}`)
  }
  return value
}

function integerEnv(name, fallback) {
  const rawValue = env(name, fallback)
  const value = Number(rawValue)
  if (!Number.isInteger(value) || value < 0) {
    throw new Error(`${name} must be a non-negative integer, got ${rawValue}`)
  }
  return value
}

function requireDbFixtureAllowed(fixtureName) {
  if (process.env.NAS_PERMISSION_E2E_ALLOW_DB_FIXTURE !== '1') {
    throw new Error(
      `${fixtureName} requires NAS_PERMISSION_E2E_ALLOW_DB_FIXTURE=1 because it mutates only the test tenant ACL snapshot fixture rows.`
    )
  }
}

const activeTransferStatuses = new Set(['WAITING', 'RUNNING'])
const activeSnapshotStatuses = new Set(['WAITING', 'RUNNING'])
const successfulSnapshotStatuses = new Set(['CAPTURED', 'SUCCESS'])
const activeRestoreStatuses = new Set(['READY', 'WAITING', 'EXECUTING'])
const subjectTypeCodeMap = {
  USER: 1,
  DEPT: 2,
  ROLE: 3,
  POSITION: 4
}

const testConfig = {
  label: 'test tenant write path',
  baseUrl: env('NAS_PERMISSION_E2E_TEST_BASE_URL', 'http://172.30.30.58:8081'),
  tenantName: env('NAS_PERMISSION_E2E_TEST_TENANT', '测试租户'),
  username: env('NAS_PERMISSION_E2E_TEST_USERNAME', 'aoteman'),
  password: env('NAS_PERMISSION_E2E_TEST_PASSWORD', 'admin123')
}

const fixtureConfig = {
  sshHost: env('NAS_PERMISSION_E2E_FIXTURE_SSH_HOST', 'root@172.30.30.58'),
  mysqlContainer: env('NAS_PERMISSION_E2E_FIXTURE_MYSQL_CONTAINER', 'intruoyi-mysql'),
  mysqlDatabase: env('NAS_PERMISSION_E2E_FIXTURE_MYSQL_DATABASE', 'ruoyi-vue-pro'),
  mysqlUser: env('NAS_PERMISSION_E2E_FIXTURE_MYSQL_USER', 'root'),
  tenantId: env('NAS_PERMISSION_E2E_FIXTURE_TENANT_ID', '122')
}

const adminConfig = {
  label: 'admin readonly path',
  baseUrl: env('NAS_PERMISSION_E2E_ADMIN_BASE_URL', 'http://172.30.30.57:8081'),
  tenantName: env('NAS_PERMISSION_E2E_ADMIN_TENANT', '芋道源码'),
  username: env('NAS_PERMISSION_E2E_ADMIN_USERNAME', 'admin'),
  password: env('NAS_PERMISSION_E2E_ADMIN_PASSWORD', 'admin123')
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
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
  throw new Error(`No visible input found for ${label}`)
}

async function fillFirstVisibleIfPresent(locator, value) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return true
    }
  }
  return false
}

async function clickButton(root, text, label = text) {
  const button = root.locator(`button:has-text("${text}")`).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  if (await button.isDisabled()) {
    throw new Error(`Button is disabled: ${label}`)
  }
  await button.click()
}

async function isVisibleWithin(locator, timeout = 2000) {
  try {
    await locator.waitFor({ state: 'visible', timeout })
    return true
  } catch (error) {
    return false
  }
}

async function assertNasPageVisible(page, account, context) {
  const notFound = page.locator('text=抱歉，您访问的页面不存在').first()
  if (await isVisibleWithin(notFound)) {
    throw new Error(
      `${account.label} cannot access /system/nas during ${context}: route returned 404. ` +
        'Missing precondition: the tenant user must have the NAS management menu and route permission deployed.'
    )
  }
  try {
    await page.waitForSelector('text=NAS 管理', { timeout: 60000 })
  } catch (error) {
    if (await isVisibleWithin(notFound)) {
      throw new Error(
        `${account.label} cannot access /system/nas during ${context}: route returned 404. ` +
          'Missing precondition: the tenant user must have the NAS management menu and route permission deployed.'
      )
    }
    throw error
  }
}

async function selectLoginTenant(page, tenantName) {
  const tenantSelect = page.locator('.el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) {
    await fillFirstVisibleIfPresent(page.locator('input[placeholder="请输入租户名称"]'), tenantName)
    return
  }
  const currentTenantText = await tenantSelect.textContent().catch(() => '')
  const currentTenantValue = await tenantSelect.locator('input').first().inputValue().catch(() => '')
  if (currentTenantText.includes(tenantName) || currentTenantValue.includes(tenantName)) {
    return
  }
  await tenantSelect.click()
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: tenantName })
    .first()
  try {
    await option.waitFor({ state: 'visible', timeout: 10000 })
    await option.click()
    return
  } catch (error) {
    const input = tenantSelect.locator('input').first()
    await input.fill(tenantName)
    await page.keyboard.press('Enter')
    await page.keyboard.press('Escape').catch(() => undefined)
  }
}

async function login(page, account, redirectPath) {
  await page.goto(`${account.baseUrl}/login?redirect=${redirectPath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)

  if (page.url().includes('/login')) {
    await selectLoginTenant(page, account.tenantName)
    await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), account.username, 'username')
    await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), account.password, 'password')
    const loginError = page.locator('.el-message, .el-notification').filter({ hasText: /错误|失败|不存在|无效|密码/ }).last()
    await clickButton(page, '登录', `${account.label} login`)
    const loggedIn = await Promise.race([
      page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 }).then(() => true),
      loginError.waitFor({ state: 'visible', timeout: 60000 }).then(() => false)
    ])
    if (!loggedIn) {
      const message = await loginError.textContent().catch(() => '')
      throw new Error(`${account.label} login failed: ${message || 'unknown login error'}`)
    }
  }

  if (!page.url().includes(redirectPath)) {
    await page.goto(`${account.baseUrl}${redirectPath}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
  }
  await assertNasPageVisible(page, account, 'login redirect')
}

function resolveApiBase(url) {
  const marker = '/admin-api/'
  const index = url.indexOf(marker)
  if (index < 0) return undefined
  return url.slice(0, index + '/admin-api'.length)
}

async function readCommonResult(response, label) {
  if (!response.ok()) {
    throw new Error(`${label} HTTP ${response.status()} from ${response.url()}`)
  }
  let payload
  try {
    payload = await response.json()
  } catch (error) {
    throw new Error(`${label} did not return JSON from ${response.url()}`)
  }
  if (payload && Object.prototype.hasOwnProperty.call(payload, 'code')) {
    const code = Number(payload.code)
    if (code !== 0 && code !== 200) {
      throw new Error(`${label} failed with code=${payload.code}, msg=${payload.msg || ''}`)
    }
    return payload.data
  }
  return payload
}

async function waitForCommonResponse(page, state, endpoint, method, label, action, timeout = 60000) {
  let actionFailed = false
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(endpoint) &&
      (!method || response.request().method().toUpperCase() === method.toUpperCase()),
    { timeout }
  ).catch((error) => {
    if (actionFailed) return undefined
    throw error
  })
  try {
    await action()
  } catch (error) {
    actionFailed = true
    throw error
  }
  const response = await responsePromise
  if (!response) {
    throw new Error(`${label} response was not captured because the triggering action failed`)
  }
  state.apiBase = state.apiBase || resolveApiBase(response.url())
  return await readCommonResult(response, label)
}

async function openNasPage(page, account) {
  await page.goto(`${account.baseUrl}/system/nas`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await assertNasPageVisible(page, account, 'direct navigation')
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
}

async function testNasConnection(page, state) {
  const result = await waitForCommonResponse(
    page,
    state,
    '/infra/file/nas-config/test',
    'POST',
    'NAS connection test',
    async () => clickButton(page, '测试连接')
  )
  if (!result || typeof result.rootPath !== 'string') {
    throw new Error('NAS connection test response is missing rootPath')
  }
  await page.waitForSelector('text=NAS 测试连接成功', { timeout: 30000 }).catch(() => undefined)
  return result
}

async function refreshNasRoot(page, state) {
  const result = await waitForCommonResponse(
    page,
    state,
    '/infra/file/nas-files',
    'GET',
    'NAS root directory list',
    async () => clickButton(page, '刷新目录')
  )
  if (!result || !Array.isArray(result.items)) {
    throw new Error('NAS root directory response is missing items')
  }
  await page.waitForSelector('.el-tree-node', { timeout: 60000 })
  return result
}

async function findTreeNode(page, fullPath, partName) {
  const nodeFromContent = (content) =>
    content.locator(
      'xpath=ancestor::div[contains(concat(" ", normalize-space(@class), " "), " el-tree-node ")][1]'
    )
  const byFullPathContent = page.locator('.el-tree-node__content').filter({ hasText: fullPath }).first()
  try {
    await byFullPathContent.waitFor({ state: 'visible', timeout: 3000 })
    return nodeFromContent(byFullPathContent)
  } catch (error) {
    // Continue with the visible label lookup below.
  }
  const byNameContent = page.locator('.el-tree-node__content').filter({ hasText: partName }).first()
  await byNameContent.waitFor({ state: 'visible', timeout: 30000 })
  return nodeFromContent(byNameContent)
}

async function expandTreeNode(page, node, expectedPath) {
  const icon = node.locator(':scope > .el-tree-node__content .el-tree-node__expand-icon').first()
  if ((await icon.count()) === 0) return
  const className = await icon.evaluate((element) => element.className)
  if (String(className).includes('is-leaf') || String(className).includes('expanded')) return
  const responsePromise = page.waitForResponse(
    (response) => {
      if (!response.url().includes('/infra/file/nas-files')) return false
      if (response.request().method() !== 'GET') return false
      return decodeURIComponent(response.url()).includes(`path=${expectedPath}`)
    },
    { timeout: 30000 }
  )
  await icon.click()
  await responsePromise
}

async function selectNasPath(page, nasPath) {
  await clickButton(page, '选择')
  const parts = nasPath.split(/[\\/]/).map((item) => item.trim()).filter(Boolean)
  if (!parts.length) {
    throw new Error('NAS path must not be blank')
  }
  let currentPath = ''
  for (let index = 0; index < parts.length; index += 1) {
    currentPath = currentPath ? `${currentPath}/${parts[index]}` : parts[index]
    const node = await findTreeNode(page, currentPath, parts[index])
    if (index < parts.length - 1) {
      await expandTreeNode(page, node, currentPath)
    } else {
      const checkbox = node.locator(':scope > .el-tree-node__content .el-checkbox__inner').first()
      await checkbox.waitFor({ state: 'visible', timeout: 30000 })
      await checkbox.click()
    }
  }
  await page.waitForSelector(`text=${nasPath}`, { timeout: 30000 })
}

async function selectFirstNasRootReadonly(page) {
  await clickButton(page, '选择')
  const firstNode = page.locator('.el-tree-node').first()
  await firstNode.waitFor({ state: 'visible', timeout: 30000 })
  await firstNode.locator('.el-checkbox__inner').first().click()
}

function parseRequestJson(request, label) {
  const raw = request.postData()
  if (!raw) {
    throw new Error(`${label} request body is empty`)
  }
  try {
    return JSON.parse(raw)
  } catch (error) {
    throw new Error(`${label} request body is not JSON: ${raw.slice(0, 200)}`)
  }
}

async function openTransferDialogAndAssertOther(page) {
  let otherCategory
  const categoryResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/file-categories') &&
      response.request().method() === 'GET',
    { timeout: 30000 }
  ).catch(() => undefined)
  await clickButton(page, '转移')
  const categoryResponse = await categoryResponsePromise
  if (categoryResponse) {
    const categories = await readCommonResult(categoryResponse, 'DCC file categories')
    if (!Array.isArray(categories)) {
      throw new Error('DCC file categories response is not an array')
    }
    otherCategory = categories.find((item) => item && item.name === '其他' && item.active)
    if (!otherCategory?.id) {
      throw new Error('DCC file categories are missing active "其他"; transfer dialog must fail fast')
    }
  }
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '转移到 DCC' }).last()
  const errorMessage = page
    .locator('.el-message, .el-alert')
    .filter({ hasText: 'DCC 模板类别' })
    .last()
  const opened = await Promise.race([
    dialog.waitFor({ state: 'visible', timeout: 30000 }).then(() => true),
    errorMessage.waitFor({ state: 'visible', timeout: 30000 }).then(() => false)
  ])
  if (!opened) {
    const message = await errorMessage.textContent().catch(() => '')
    throw new Error(`Transfer dialog did not open: ${message || 'DCC template category error'}`)
  }
  await dialog.getByText('其他', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  return { dialog, otherCategory }
}

async function submitTransfer(page, state, dialog, expectedCategory, expectedNasPath) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/controlled-files/nas-transfer') &&
      !response.url().includes('/tasks/') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickButton(dialog, '确认转移')
  const confirmDialog = page.locator('.el-message-box:visible').filter({ hasText: '开始转移确认' }).last()
  await confirmDialog.waitFor({ state: 'visible', timeout: 30000 })
  await clickButton(confirmDialog, '确认开始', 'confirm NAS transfer')
  const response = await responsePromise
  state.apiBase = state.apiBase || resolveApiBase(response.url())
  const body = parseRequestJson(response.request(), 'NAS transfer create')
  if (!expectedCategory?.id) {
    throw new Error('Cannot assert NAS transfer templateCategoryId because active "其他" category id was not captured')
  }
  if (String(body.templateCategoryId) !== String(expectedCategory.id)) {
    throw new Error(
      `NAS transfer submitted templateCategoryId=${body.templateCategoryId}, expected active "其他" id=${expectedCategory.id}`
    )
  }
  if (!Array.isArray(body.selectedNasPaths) || !body.selectedNasPaths.includes(expectedNasPath)) {
    throw new Error(
      `NAS transfer selectedNasPaths did not include ${expectedNasPath}: ${JSON.stringify(body.selectedNasPaths)}`
    )
  }
  const result = await readCommonResult(response, 'NAS transfer create')
  if (!result?.taskId) {
    throw new Error('NAS transfer response is missing taskId')
  }
  return result
}

async function pageApiFetch(page, state, endpoint, label) {
  const apiBase = state.apiBase || process.env.NAS_PERMISSION_E2E_API_BASE_URL
  if (!apiBase) {
    throw new Error(`Missing API base for ${label}; no previous /admin-api response was captured`)
  }
  const result = await page.evaluate(
    async ({ apiBaseUrl, apiEndpoint }) => {
      const parseStorageValue = (raw) => {
        if (!raw) return undefined
        const unwrap = (value) => {
          if (value && typeof value === 'object') {
            if (Object.prototype.hasOwnProperty.call(value, 'v')) return unwrap(value.v)
            if (Object.prototype.hasOwnProperty.call(value, 'value')) return unwrap(value.value)
            if (Object.prototype.hasOwnProperty.call(value, 'data')) return unwrap(value.data)
            return value
          }
          if (typeof value === 'string') {
            try {
              const parsedString = JSON.parse(value)
              return parsedString === value ? value : unwrap(parsedString)
            } catch (error) {
              return value
            }
          }
          return value
        }
        try {
          const parsed = JSON.parse(raw)
          return unwrap(parsed)
        } catch (error) {
          return unwrap(raw)
        }
      }
      const findStoredValue = (exactKey, containsKey) => {
        const exactValue = parseStorageValue(localStorage.getItem(exactKey))
        if (exactValue !== undefined) return exactValue
        for (let index = 0; index < localStorage.length; index += 1) {
          const key = localStorage.key(index)
          if (key && key.includes(containsKey)) {
            const value = parseStorageValue(localStorage.getItem(key))
            if (value !== undefined) return value
          }
        }
        return undefined
      }
      const accessToken = findStoredValue('ACCESS_TOKEN', 'ACCESS_TOKEN')
      const tenantId = findStoredValue('tenantId', 'tenantId')
      const visitTenantId = findStoredValue('visitTenantId', 'visitTenantId')
      if (!accessToken) {
        throw new Error('ACCESS_TOKEN is missing from localStorage')
      }
      if (!tenantId) {
        throw new Error('tenantId is missing from localStorage')
      }
      const headers = {
        Authorization: String(accessToken).startsWith('Bearer ')
          ? String(accessToken)
          : `Bearer ${accessToken}`,
        'tenant-id': String(tenantId),
        'Content-Type': 'application/json'
      }
      if (visitTenantId) {
        headers['visit-tenant-id'] = String(visitTenantId)
      }
      const response = await fetch(`${apiBaseUrl}${apiEndpoint}`, { headers })
      const text = await response.text()
      let payload
      try {
        payload = JSON.parse(text)
      } catch (error) {
        throw new Error(`API response is not JSON: ${text.slice(0, 200)}`)
      }
      return {
        ok: response.ok,
        status: response.status,
        payload
      }
    },
    { apiBaseUrl: apiBase, apiEndpoint: endpoint }
  )
  if (!result.ok) {
    throw new Error(`${label} HTTP ${result.status}`)
  }
  const payload = result.payload
  if (payload && Object.prototype.hasOwnProperty.call(payload, 'code')) {
    const code = Number(payload.code)
    if (code !== 0 && code !== 200) {
      throw new Error(`${label} failed with code=${payload.code}, msg=${payload.msg || ''}`)
    }
    return payload.data
  }
  return payload
}

function posixShellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`
}

function mysqlFixtureCommand() {
  const password = requireEnv(
    'NAS_PERMISSION_E2E_FIXTURE_MYSQL_PASSWORD',
    'DB fixture modes must authenticate to the test MySQL container; no default password is embedded in the E2E script'
  )
  const parts = [
    'docker',
    'exec',
    '-i',
    fixtureConfig.mysqlContainer,
    'mysql',
    '--default-character-set=utf8mb4',
    `-u${fixtureConfig.mysqlUser}`,
    `-p${password}`,
    '--batch',
    '--raw',
    '--skip-column-names',
    '-D',
    fixtureConfig.mysqlDatabase
  ]
  return parts.map(posixShellQuote).join(' ')
}

function runMysqlFixtureSql(sql, label) {
  const output = execFileSync(
    'ssh',
    [fixtureConfig.sshHost, mysqlFixtureCommand()],
    {
      input: sql,
      encoding: 'utf8',
      maxBuffer: 1024 * 1024
    }
  )
  if (!output.includes('fixture_candidate_count')) {
    throw new Error(`${label} did not report fixture_candidate_count. Output: ${output}`)
  }
  if (!output.includes('fixture_affected_count')) {
    throw new Error(`${label} did not report fixture_affected_count. Output: ${output}`)
  }
  return output
}

function metricFromFixtureOutput(output, metricName) {
  const line = output
    .split(/\r?\n/)
    .map((item) => item.trim())
    .find((item) => item.startsWith(`${metricName}\t`))
  if (!line) {
    throw new Error(`Fixture output is missing ${metricName}: ${output}`)
  }
  const value = Number(line.split('\t')[1])
  if (!Number.isInteger(value)) {
    throw new Error(`Fixture metric ${metricName} is not an integer: ${line}`)
  }
  return value
}

function requirePositiveFixtureMetric(output, label) {
  const candidateCount = metricFromFixtureOutput(output, 'fixture_candidate_count')
  const affectedCount = metricFromFixtureOutput(output, 'fixture_affected_count')
  if (candidateCount <= 0 || affectedCount <= 0) {
    throw new Error(
      `${label} did not mutate a test tenant fixture row. candidate=${candidateCount}, affected=${affectedCount}`
    )
  }
}

function fixturePreamble(taskId) {
  const numericTaskId = Number(taskId)
  const numericTenantId = Number(fixtureConfig.tenantId)
  if (!Number.isInteger(numericTaskId) || numericTaskId <= 0) {
    throw new Error(`Fixture taskId must be a positive integer, got ${taskId}`)
  }
  if (!Number.isInteger(numericTenantId) || numericTenantId <= 0) {
    throw new Error(`Fixture tenantId must be a positive integer, got ${fixtureConfig.tenantId}`)
  }
  return `SET @e2e_task_id := ${numericTaskId};\nSET @e2e_tenant_id := ${numericTenantId};\n`
}

function applyMappingFixture(taskId) {
  requireDbFixtureAllowed('test-mapping')
  const sql = `${fixturePreamble(taskId)}
SET @e2e_mapping_sid := CONVERT(CONCAT('S-1-5-21-1174157445-4080048154-3916697584-', 800000 + @e2e_task_id) USING utf8mb4) COLLATE utf8mb4_unicode_ci;
SET @e2e_mapping_sid_hash := UPPER(CONVERT(SHA2(@e2e_mapping_sid, 256) USING utf8mb4)) COLLATE utf8mb4_unicode_ci;
CREATE TEMPORARY TABLE tmp_e2e_mapping_sid AS
SELECT ds.id AS directory_snapshot_id,
       ds.descriptor_id,
       a.id AS ace_id
FROM dcc_nas_acl_snapshot s
JOIN dcc_nas_acl_directory_snapshot ds
  ON ds.snapshot_id = s.id AND ds.tenant_id = s.tenant_id AND ds.deleted = b'0'
JOIN dcc_nas_acl_ace a
  ON a.descriptor_id = ds.descriptor_id AND a.tenant_id = s.tenant_id AND a.deleted = b'0'
JOIN dcc_nas_acl_identity_mapping m
  ON m.sid_hash = a.trustee_sid_hash AND m.tenant_id = s.tenant_id
  AND m.mapping_status = 'MAPPED' AND m.deleted = b'0'
WHERE s.tenant_id = @e2e_tenant_id
  AND s.transfer_task_id = @e2e_task_id
  AND s.status = 'CAPTURED'
  AND ds.collect_status = 'SUCCESS'
  AND a.ace_type = 'ALLOW'
  AND NOT EXISTS (
    SELECT 1
    FROM dcc_nas_acl_identity_mapping existing
    WHERE existing.tenant_id = s.tenant_id
      AND existing.sid_hash = @e2e_mapping_sid_hash
      AND existing.deleted = b'0'
  )
ORDER BY a.id
LIMIT 1;
SELECT 'fixture_candidate_count', COUNT(*) FROM tmp_e2e_mapping_sid;
INSERT INTO dcc_nas_acl_descriptor (
  descriptor_hash, owner_sid, group_sid, control_flags, dacl_present, dacl_protected,
  sacl_present, raw_descriptor_sha256, raw_descriptor_blob, normalized_descriptor_json,
  capture_capability, tenant_id, creator, create_time, updater, update_time, deleted
)
SELECT
  SHA2(CONCAT(d.descriptor_hash, ':nas-e2e-mapping:', @e2e_task_id, ':', picked.directory_snapshot_id, ':', UTC_TIMESTAMP(6)), 256),
  d.owner_sid, d.group_sid, d.control_flags, d.dacl_present, d.dacl_protected,
  d.sacl_present, d.raw_descriptor_sha256, d.raw_descriptor_blob, d.normalized_descriptor_json,
  d.capture_capability, d.tenant_id, 'nas-e2e-mapping-fixture-clone', NOW(), 'nas-e2e-mapping-fixture-clone', NOW(), b'0'
FROM dcc_nas_acl_descriptor d
JOIN tmp_e2e_mapping_sid picked ON picked.descriptor_id = d.id
WHERE d.tenant_id = @e2e_tenant_id
  AND d.deleted = b'0';
SET @e2e_mapping_descriptor_id := LAST_INSERT_ID();
SELECT 'fixture_cloned_descriptor_id', @e2e_mapping_descriptor_id;
INSERT INTO dcc_nas_acl_ace (
  descriptor_id, ace_index, ace_hash, ace_type, ace_flags, access_mask, trustee_sid,
  trustee_sid_hash, inherited, inheritance_flags, propagation_flags, object_type_guid,
  inherited_object_type_guid, raw_ace_json, tenant_id, creator, create_time, updater,
  update_time, deleted
)
SELECT
  @e2e_mapping_descriptor_id,
  a.ace_index,
  SHA2(CONCAT(a.ace_hash, ':nas-e2e-mapping:', @e2e_task_id, ':', a.id, ':', @e2e_mapping_descriptor_id), 256),
  a.ace_type,
  a.ace_flags, a.access_mask,
  CASE WHEN a.id = picked.ace_id THEN @e2e_mapping_sid ELSE a.trustee_sid END,
  CASE WHEN a.id = picked.ace_id THEN @e2e_mapping_sid_hash ELSE a.trustee_sid_hash END,
  a.inherited, a.inheritance_flags, a.propagation_flags, a.object_type_guid,
  a.inherited_object_type_guid, a.raw_ace_json, a.tenant_id,
  'nas-e2e-mapping-fixture-clone', NOW(), 'nas-e2e-mapping-fixture-clone', NOW(), b'0'
FROM dcc_nas_acl_ace a
JOIN tmp_e2e_mapping_sid picked ON picked.descriptor_id = a.descriptor_id
WHERE a.tenant_id = @e2e_tenant_id
  AND a.deleted = b'0'
ORDER BY a.ace_index;
UPDATE dcc_nas_acl_directory_snapshot ds
JOIN tmp_e2e_mapping_sid picked ON picked.directory_snapshot_id = ds.id
SET ds.descriptor_id = @e2e_mapping_descriptor_id,
    ds.updater = 'nas-e2e-mapping-fixture-clone',
    ds.update_time = NOW()
WHERE ds.tenant_id = @e2e_tenant_id
  AND ds.transfer_task_id = @e2e_task_id
  AND ds.deleted = b'0';
SELECT 'fixture_affected_count', ROW_COUNT();
DROP TEMPORARY TABLE tmp_e2e_mapping_sid;
`
  const output = runMysqlFixtureSql(sql, 'test-mapping fixture')
  requirePositiveFixtureMetric(output, 'test-mapping fixture')
}

function applyBlockerFixture(taskId) {
  requireDbFixtureAllowed('test-blocker')
  const sql = `${fixturePreamble(taskId)}
CREATE TEMPORARY TABLE tmp_e2e_blocker_ace AS
SELECT ds.id AS directory_snapshot_id,
       ds.descriptor_id,
       a.id AS ace_id
FROM dcc_nas_acl_snapshot s
JOIN dcc_nas_acl_directory_snapshot ds
  ON ds.snapshot_id = s.id AND ds.tenant_id = s.tenant_id AND ds.deleted = b'0'
JOIN dcc_nas_acl_ace a
  ON a.descriptor_id = ds.descriptor_id AND a.tenant_id = s.tenant_id AND a.deleted = b'0'
JOIN dcc_nas_acl_identity_mapping m
  ON m.sid_hash = a.trustee_sid_hash AND m.tenant_id = s.tenant_id
  AND m.mapping_status = 'MAPPED' AND m.deleted = b'0'
WHERE s.tenant_id = @e2e_tenant_id
  AND s.transfer_task_id = @e2e_task_id
  AND s.status = 'CAPTURED'
  AND ds.collect_status = 'SUCCESS'
  AND a.ace_type = 'ALLOW'
ORDER BY a.id
LIMIT 1;
SELECT 'fixture_candidate_count', COUNT(*) FROM tmp_e2e_blocker_ace;
INSERT INTO dcc_nas_acl_descriptor (
  descriptor_hash, owner_sid, group_sid, control_flags, dacl_present, dacl_protected,
  sacl_present, raw_descriptor_sha256, raw_descriptor_blob, normalized_descriptor_json,
  capture_capability, tenant_id, creator, create_time, updater, update_time, deleted
)
SELECT
  SHA2(CONCAT(d.descriptor_hash, ':nas-e2e-blocker:', @e2e_task_id, ':', picked.directory_snapshot_id, ':', UTC_TIMESTAMP(6)), 256),
  d.owner_sid, d.group_sid, d.control_flags, d.dacl_present, d.dacl_protected,
  d.sacl_present, d.raw_descriptor_sha256, d.raw_descriptor_blob, d.normalized_descriptor_json,
  d.capture_capability, d.tenant_id, 'nas-e2e-blocker-fixture-clone', NOW(), 'nas-e2e-blocker-fixture-clone', NOW(), b'0'
FROM dcc_nas_acl_descriptor d
JOIN tmp_e2e_blocker_ace picked ON picked.descriptor_id = d.id
WHERE d.tenant_id = @e2e_tenant_id
  AND d.deleted = b'0';
SET @e2e_descriptor_id := LAST_INSERT_ID();
SELECT 'fixture_cloned_descriptor_id', @e2e_descriptor_id;
INSERT INTO dcc_nas_acl_ace (
  descriptor_id, ace_index, ace_hash, ace_type, ace_flags, access_mask, trustee_sid,
  trustee_sid_hash, inherited, inheritance_flags, propagation_flags, object_type_guid,
  inherited_object_type_guid, raw_ace_json, tenant_id, creator, create_time, updater,
  update_time, deleted
)
SELECT
  @e2e_descriptor_id,
  a.ace_index,
  SHA2(CONCAT(a.ace_hash, ':nas-e2e-blocker:', @e2e_task_id, ':', a.id, ':', @e2e_descriptor_id), 256),
  CASE WHEN a.id = picked.ace_id THEN 'DENY' ELSE a.ace_type END,
  a.ace_flags, a.access_mask, a.trustee_sid, a.trustee_sid_hash, a.inherited,
  a.inheritance_flags, a.propagation_flags, a.object_type_guid,
  a.inherited_object_type_guid, a.raw_ace_json, a.tenant_id,
  'nas-e2e-blocker-fixture-clone', NOW(), 'nas-e2e-blocker-fixture-clone', NOW(), b'0'
FROM dcc_nas_acl_ace a
JOIN tmp_e2e_blocker_ace picked ON picked.descriptor_id = a.descriptor_id
WHERE a.tenant_id = @e2e_tenant_id
  AND a.deleted = b'0'
ORDER BY a.ace_index;
UPDATE dcc_nas_acl_directory_snapshot ds
JOIN tmp_e2e_blocker_ace picked ON picked.directory_snapshot_id = ds.id
SET ds.descriptor_id = @e2e_descriptor_id,
    ds.updater = 'nas-e2e-blocker-fixture-clone',
    ds.update_time = NOW()
WHERE ds.tenant_id = @e2e_tenant_id
  AND ds.transfer_task_id = @e2e_task_id
  AND ds.deleted = b'0';
SELECT 'fixture_affected_count', ROW_COUNT();
DROP TEMPORARY TABLE tmp_e2e_blocker_ace;
`
  const output = runMysqlFixtureSql(sql, 'test-blocker fixture')
  requirePositiveFixtureMetric(output, 'test-blocker fixture')
}

async function waitForTransferCompletion(page, state, taskId, initialResult) {
  const timeoutMs = Number(env('NAS_PERMISSION_E2E_TRANSFER_TIMEOUT_MS', '600000'))
  const deadline = Date.now() + timeoutMs
  let latest = initialResult
  while (activeTransferStatuses.has(latest.status)) {
    if (Date.now() > deadline) {
      throw new Error(`NAS transfer task ${taskId} did not finish within ${timeoutMs}ms`)
    }
    await page.waitForTimeout(3000)
    latest = await pageApiFetch(
      page,
      state,
      `/dcc/controlled-files/nas-transfer/tasks/${taskId}`,
      'NAS transfer task status'
    )
  }
  if (latest.status !== 'COMPLETED') {
    throw new Error(
      `NAS transfer task ${taskId} ended as ${latest.status}: ${latest.lastFailureMessage || JSON.stringify(latest.failures || [])}`
    )
  }
  await page.locator('.el-dialog:visible').getByText('已完成').waitFor({
    state: 'visible',
    timeout: 30000
  }).catch(() => undefined)
  return latest
}

function unwrapListPayload(payload, label) {
  if (!payload || !Array.isArray(payload.list)) {
    throw new Error(`${label} response is missing list`)
  }
  return payload.list
}

async function selectVisibleDropdownOption(page, selectLocator, optionText) {
  const currentText = (await selectLocator.innerText().catch(() => '')).replace(/\s+/g, '')
  if (currentText.includes(optionText.replace(/\s+/g, ''))) {
    return
  }
  await selectLocator.click()
  const option = page
    .locator('.el-select-dropdown:visible')
    .last()
    .locator('.el-select-dropdown__item')
    .filter({ hasText: new RegExp(`^\\s*${escapeRegExp(optionText)}\\s*$`) })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click({ force: true })
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

async function saveUnmappedPrincipals(page, state, unmappedList) {
  if (!unmappedList.length) return 0
  const targetLabel = requireEnv(
    'NAS_PERMISSION_E2E_TARGET_SUBJECT_LABEL',
    'unmapped NAS principals cannot be mapped through the UI without an explicit DCC subject label'
  )
  const accountTypeLabel = env('NAS_PERMISSION_E2E_NAS_ACCOUNT_TYPE_LABEL', 'NAS 用户')
  const subjectTypeLabel = env('NAS_PERMISSION_E2E_TARGET_SUBJECT_TYPE_LABEL', '用户')
  const maxRows = Number(env('NAS_PERMISSION_E2E_MAX_MAPPING_ROWS', '20'))
  if (unmappedList.length > maxRows) {
    throw new Error(
      `Unmapped principal count ${unmappedList.length} exceeds NAS_PERMISSION_E2E_MAX_MAPPING_ROWS=${maxRows}`
    )
  }

  await page.getByRole('tab', { name: '身份映射' }).click()
  let savedCount = 0
  for (let index = 0; index < unmappedList.length; index += 1) {
    const rows = page.locator('.el-drawer:visible .el-table__body-wrapper tbody tr')
      .filter({ has: page.locator('button:has-text("保存")') })
    const row = rows.first()
    await row.waitFor({ state: 'visible', timeout: 30000 })
    await selectVisibleDropdownOption(page, row.locator('.el-select').nth(0), accountTypeLabel)
    await selectVisibleDropdownOption(page, row.locator('.el-select').nth(1), subjectTypeLabel)
    await selectVisibleDropdownOption(page, row.locator('.el-select').nth(2), targetLabel)
    await row.locator('input[placeholder="填写映射依据"]').fill(
      `真实 E2E 身份映射 ${new Date().toISOString()}`
    )
    const responsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/dcc/nas-permission/principal-mappings') &&
        response.request().method() === 'PUT',
      { timeout: 60000 }
    )
    await clickButton(row, '保存', 'save principal mapping')
    const response = await responsePromise
    state.apiBase = state.apiBase || resolveApiBase(response.url())
    await readCommonResult(response, 'save NAS principal mapping')
    savedCount += 1
    await page.waitForTimeout(800)
  }
  return savedCount
}

async function loadPermissionSnapshotSummary(page, state, taskId) {
  const summary = await waitForCommonResponse(
    page,
    state,
    `/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-snapshot`,
    'GET',
    'NAS permission snapshot summary',
    async () => clickButton(page.locator('.el-dialog:visible'), '刷新权限状态')
  )
  if (!summary || summary.taskId !== taskId) {
    throw new Error('Permission snapshot summary is missing the expected taskId')
  }
  return summary
}

async function waitForPermissionSnapshotCaptured(page, state, taskId, initialSummary) {
  const timeoutMs = Number(env('NAS_PERMISSION_E2E_SNAPSHOT_TIMEOUT_MS', '600000'))
  const deadline = Date.now() + timeoutMs
  let latest = initialSummary
  while (activeSnapshotStatuses.has(latest.snapshotStatus)) {
    if (Date.now() > deadline) {
      throw new Error(`NAS permission snapshot for task ${taskId} did not finish within ${timeoutMs}ms`)
    }
    await page.waitForTimeout(3000)
    latest = await pageApiFetch(
      page,
      state,
      `/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-snapshot`,
      'NAS permission snapshot summary'
    )
    if (!latest || latest.taskId !== taskId) {
      throw new Error('Permission snapshot summary polling returned an unexpected taskId')
    }
  }
  if (!successfulSnapshotStatuses.has(latest.snapshotStatus)) {
    throw new Error(
      `Permission snapshot is not captured: ${latest.snapshotStatus}, failure=${latest.failureMessage || ''}`
    )
  }
  return latest
}

async function openRestoreDrawer(page, state, taskId, summary) {
  const snapshotItemsPromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-snapshot/items`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const unmappedPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/nas-permission/principals/unmapped') &&
      response.url().includes(`taskId=${taskId}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickButton(page.locator('.el-dialog:visible'), '查看恢复')
  await page.waitForSelector('text=NAS 权限恢复', { timeout: 60000 })
  const snapshotItemsResponse = await snapshotItemsPromise
  state.apiBase = state.apiBase || resolveApiBase(snapshotItemsResponse.url())
  const snapshotItemsPayload = await readCommonResult(snapshotItemsResponse, 'NAS permission snapshot items')
  const snapshotItems = unwrapListPayload(snapshotItemsPayload, 'NAS permission snapshot items')
  if (Number(snapshotItemsPayload.total) <= 0 || snapshotItems.length <= 0) {
    throw new Error('NAS permission snapshot items list is empty')
  }
  if (summary && Number(summary.directorySnapshotCount) > 0 && Number(snapshotItemsPayload.total) <= 0) {
    throw new Error('Permission snapshot summary has directory snapshots but items list total is zero')
  }
  const firstItem = snapshotItems[0]
  if (!firstItem?.taskItemId || !firstItem.nasPath || !firstItem.snapshotStatus) {
    throw new Error(`NAS permission snapshot item is incomplete: ${JSON.stringify(firstItem)}`)
  }
  await page
    .locator('.el-drawer:visible .el-table__body-wrapper')
    .getByText(firstItem.nasPath, { exact: false })
    .first()
    .waitFor({ state: 'visible', timeout: 30000 })
  const unmappedResponse = await unmappedPromise
  state.apiBase = state.apiBase || resolveApiBase(unmappedResponse.url())
  const payload = await readCommonResult(unmappedResponse, 'NAS unmapped principals')
  return unwrapListPayload(payload, 'NAS unmapped principals')
}

async function previewRestore(page, state, taskId) {
  const preview = await waitForCommonResponse(
    page,
    state,
    `/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-restore/preview`,
    'GET',
    'NAS permission restore preview',
    async () => clickButton(page.locator('.el-drawer:visible'), '恢复预览')
  )
  if (!preview?.planHash || !preview.restoreMode) {
    throw new Error('Restore preview response is missing planHash or restoreMode')
  }
  return preview
}

async function applyRestore(page, state, taskId, preview) {
  await page.locator('.el-drawer:visible input[placeholder="填写应用恢复原因"]').fill(
    `真实 E2E 应用恢复 ${new Date().toISOString()}`
  )
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-restore`) &&
      !response.url().includes('/preview') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickButton(page.locator('.el-drawer:visible'), '应用恢复')
  const confirmDialog = page.locator('.el-message-box:visible').filter({ hasText: '应用 NAS 权限恢复' }).last()
  await confirmDialog.waitFor({ state: 'visible', timeout: 30000 })
  await clickButton(confirmDialog, '确认应用', 'confirm NAS permission restore')
  const response = await responsePromise
  state.apiBase = state.apiBase || resolveApiBase(response.url())
  const result = await readCommonResult(response, 'apply NAS permission restore')
  if (!result?.restoreId || result.taskId !== taskId) {
    throw new Error('Restore apply response is missing restoreId or expected taskId')
  }
  if (result.directoryCount !== preview.directoryCount || result.ruleCount !== preview.ruleCount) {
    throw new Error('Restore apply counts do not match restore preview counts')
  }
  return result
}

async function waitForRestoreCompletion(page, state, taskId, restoreId, initialResult) {
  const timeoutMs = Number(env('NAS_PERMISSION_E2E_RESTORE_TIMEOUT_MS', '600000'))
  const deadline = Date.now() + timeoutMs
  let latest = initialResult
  while (activeRestoreStatuses.has(latest.status)) {
    if (Date.now() > deadline) {
      throw new Error(`NAS permission restore ${restoreId} did not finish within ${timeoutMs}ms`)
    }
    await page.waitForTimeout(3000)
    latest = await pageApiFetch(
      page,
      state,
      `/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-restore/${restoreId}`,
      'NAS permission restore status'
    )
  }
  if (latest.status !== 'COMPLETED') {
    throw new Error(
      `NAS permission restore ${restoreId} ended as ${latest.status}: ${latest.lastFailureMessage || ''}`
    )
  }
  return latest
}

function normalizeSubjectType(value) {
  if (typeof value === 'string') {
    if (subjectTypeCodeMap[value]) return value
    const numeric = Number(value)
    return Object.entries(subjectTypeCodeMap).find(([, code]) => code === numeric)?.[0]
  }
  return Object.entries(subjectTypeCodeMap).find(([, code]) => code === Number(value))?.[0]
}

async function verifyDirectoryRulesFromPreview(page, state, preview) {
  if (preview.ruleCount > 0 && !preview.sampleRules.length) {
    throw new Error('Restore preview has ruleCount > 0 but no sampleRules for final verification')
  }
  const sampleLimit = Number(env('NAS_PERMISSION_E2E_VERIFY_SAMPLE_RULE_LIMIT', '10'))
  const sampleRules = preview.sampleRules.slice(0, sampleLimit)
  for (const sampleRule of sampleRules) {
    const rules = await pageApiFetch(
      page,
      state,
      `/dcc/directories/${sampleRule.directoryId}/access-rules`,
      `DCC directory ${sampleRule.directoryId} access rules`
    )
    if (!Array.isArray(rules)) {
      throw new Error(`DCC directory ${sampleRule.directoryId} access rules response is not an array`)
    }
    const matched = rules.some((rule) => {
      return (
        normalizeSubjectType(rule.subjectType) === sampleRule.subjectType &&
        Number(rule.subjectId) === Number(sampleRule.subjectId) &&
        Boolean(rule.canQuery) === Boolean(sampleRule.canQuery) &&
        Boolean(rule.canPreview) === Boolean(sampleRule.canPreview) &&
        Boolean(rule.canDownload) === Boolean(sampleRule.canDownload) &&
        Boolean(rule.active)
      )
    })
    if (!matched) {
      throw new Error(
        `No restored directory access rule matched preview sample: ${JSON.stringify(sampleRule)}`
      )
    }
  }
}

function installNoWriteGuard(page, label) {
  const forbidden = []
  page.on('request', (request) => {
    const url = request.url()
    const method = request.method().toUpperCase()
    if (
      method !== 'GET' &&
      (
        url.includes('/dcc/controlled-files/nas-transfer') ||
        url.includes('/dcc/nas-permission/principal-mappings') ||
        url.includes('/dcc/directories/')
      )
    ) {
      forbidden.push(`${method} ${url}`)
    }
  })
  return () => {
    if (forbidden.length) {
      throw new Error(`${label} must be readonly, but write requests were sent: ${forbidden.join(', ')}`)
    }
  }
}

function expectedApiOrigin(account, envName) {
  const configured = process.env[envName]
  if (configured) return configured
  const url = new URL(account.baseUrl)
  return `${url.protocol}//${url.hostname}:48081`
}

async function installApiOriginGuard(page, label, expectedOrigin) {
  const wrongOrigins = []
  await page.route('**/admin-api/**', async (route) => {
    const url = route.request().url()
    if (!url.includes('/admin-api')) {
      await route.continue()
      return
    }
    if (!url.startsWith(`${expectedOrigin}/admin-api`)) {
      wrongOrigins.push(url)
      await route.abort('blockedbyclient')
      return
    }
    await route.continue()
  })
  return () => {
    if (wrongOrigins.length) {
      throw new Error(
        `${label} API origin mismatch. Expected ${expectedOrigin}, got ${wrongOrigins.join(', ')}`
      )
    }
  }
}

function assertCountAtLeast(actual, minimum, label) {
  if (Number(actual) < Number(minimum)) {
    throw new Error(`${label} expected at least ${minimum}, got ${actual}`)
  }
}

function installRestoreApplyNoWriteGuard(page, taskId) {
  const forbidden = []
  page.on('request', (request) => {
    const url = request.url()
    if (
      request.method().toUpperCase() === 'POST' &&
      url.includes(`/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-restore`)
    ) {
      forbidden.push(`${request.method()} ${url}`)
    }
  })
  return () => {
    if (forbidden.length) {
      throw new Error(`Blocked restore branch must not submit apply requests: ${forbidden.join(', ')}`)
    }
  }
}

async function assertApplyRestoreDisabled(page, preview) {
  if (preview.canRestore) {
    throw new Error('Blocked restore preview unexpectedly returned canRestore=true')
  }
  const blocker = preview.blockers?.[0]
  if (!blocker?.code) {
    throw new Error(`Blocked restore preview is missing blocker rows: ${JSON.stringify(preview)}`)
  }
  await page.waitForFunction(
    (code) =>
      Array.from(document.querySelectorAll('.el-drawer:not([aria-hidden="true"]) *')).some(
        (element) => element.textContent?.includes(code) && element.checkVisibility()
      ),
    blocker.code,
    { timeout: 30000 }
  )
  const applyButton = page.getByRole('button', { name: '应用恢复' }).last()
  await applyButton.waitFor({ state: 'visible', timeout: 30000 })
  if (!(await applyButton.isDisabled())) {
    throw new Error('应用恢复 button must be disabled when restore preview has blockers')
  }
}

async function createTransferAndCaptureSnapshot(page, state, nasPath, label) {
  if (process.env.NAS_PERMISSION_E2E_ALLOW_TEST_WRITE !== '1') {
    throw new Error(
      'Set NAS_PERMISSION_E2E_ALLOW_TEST_WRITE=1 to allow mutating the test tenant. This guard prevents accidental writes.'
    )
  }
  const assertApiOrigin = await installApiOriginGuard(
    page,
    label,
    expectedApiOrigin(testConfig, 'NAS_PERMISSION_E2E_TEST_API_ORIGIN')
  )
  await login(page, testConfig, '/system/nas')
  await openNasPage(page, testConfig)
  await testNasConnection(page, state)
  await refreshNasRoot(page, state)
  await selectNasPath(page, nasPath)
  const { dialog, otherCategory } = await openTransferDialogAndAssertOther(page)
  const transfer = await submitTransfer(page, state, dialog, otherCategory, nasPath)
  const completedTransfer = await waitForTransferCompletion(page, state, transfer.taskId, transfer)
  const initialSummary = await loadPermissionSnapshotSummary(page, state, completedTransfer.taskId)
  const summary = await waitForPermissionSnapshotCaptured(page, state, completedTransfer.taskId, initialSummary)
  return { completedTransfer, summary, assertApiOrigin }
}

async function runTestWrite(page) {
  const nasPath = requireEnv(
    'NAS_PERMISSION_E2E_TEST_NAS_PATH',
    'test-write mode refuses to pick a default NAS folder because large folders may contain many files'
  )
  const state = {}
  const { completedTransfer, summary, assertApiOrigin } = await createTransferAndCaptureSnapshot(
    page,
    state,
    nasPath,
    'test-write'
  )
  const unmappedList = await openRestoreDrawer(page, state, completedTransfer.taskId, summary)
  const savedMappingCount = await saveUnmappedPrincipals(page, state, unmappedList)
  const preview = await previewRestore(page, state, completedTransfer.taskId)
  if (!preview.runtimeEnforcementReady || !preview.canRestore) {
    throw new Error(
      `Restore preview is blocked: ${JSON.stringify(preview.blockers || [])}, runtime=${preview.runtimeEnforcementBlocker || ''}`
    )
  }
  const applyResult = await applyRestore(page, state, completedTransfer.taskId, preview)
  const restoreStatus = await waitForRestoreCompletion(
    page,
    state,
    completedTransfer.taskId,
    applyResult.restoreId,
    applyResult
  )
  await verifyDirectoryRulesFromPreview(page, state, preview)
  assertApiOrigin()
  console.log(
    `PASS: test-write taskId=${completedTransfer.taskId}, restoreId=${applyResult.restoreId}, directories=${restoreStatus.directoryCount}, rules=${restoreStatus.ruleCount}, unmapped=${unmappedList.length}, savedMappings=${savedMappingCount}, blockers=${(preview.blockers || []).length}`
  )
}

async function runTestMapping(page) {
  const nasPath = requireEnv(
    'NAS_PERMISSION_E2E_TEST_NAS_PATH',
    'test-mapping mode refuses to pick a default NAS folder because large folders may contain many files'
  )
  const expectedUnmapped = integerEnv('NAS_PERMISSION_E2E_EXPECT_UNMAPPED_MIN', '1')
  const state = {}
  const { completedTransfer, assertApiOrigin } = await createTransferAndCaptureSnapshot(
    page,
    state,
    nasPath,
    'test-mapping'
  )
  applyMappingFixture(completedTransfer.taskId)
  const fixtureSummary = await pageApiFetch(
    page,
    state,
    `/dcc/controlled-files/nas-transfer/tasks/${completedTransfer.taskId}/permission-snapshot`,
    'NAS permission snapshot summary after mapping fixture'
  )
  assertCountAtLeast(fixtureSummary.unmappedPrincipalCount, expectedUnmapped, 'unmapped principals after fixture')
  const unmappedList = await openRestoreDrawer(page, state, completedTransfer.taskId, fixtureSummary)
  assertCountAtLeast(unmappedList.length, expectedUnmapped, 'unmapped principal rows in UI')
  const savedMappingCount = await saveUnmappedPrincipals(page, state, unmappedList)
  assertCountAtLeast(savedMappingCount, expectedUnmapped, 'saved principal mappings')
  const preview = await previewRestore(page, state, completedTransfer.taskId)
  if (!preview.runtimeEnforcementReady || !preview.canRestore) {
    throw new Error(
      `Restore preview remains blocked after mapping fixture save: ${JSON.stringify(preview.blockers || [])}`
    )
  }
  const applyResult = await applyRestore(page, state, completedTransfer.taskId, preview)
  const restoreStatus = await waitForRestoreCompletion(
    page,
    state,
    completedTransfer.taskId,
    applyResult.restoreId,
    applyResult
  )
  await verifyDirectoryRulesFromPreview(page, state, preview)
  assertApiOrigin()
  console.log(
    `PASS: test-mapping taskId=${completedTransfer.taskId}, restoreId=${applyResult.restoreId}, directories=${restoreStatus.directoryCount}, rules=${restoreStatus.ruleCount}, unmapped=${unmappedList.length}, savedMappings=${savedMappingCount}, blockers=${(preview.blockers || []).length}`
  )
}

async function runTestBlocker(page) {
  const nasPath = requireEnv(
    'NAS_PERMISSION_E2E_TEST_NAS_PATH',
    'test-blocker mode refuses to pick a default NAS folder because large folders may contain many files'
  )
  const expectedBlockers = integerEnv('NAS_PERMISSION_E2E_EXPECT_BLOCKER_MIN', '1')
  const state = {}
  const { completedTransfer, assertApiOrigin } = await createTransferAndCaptureSnapshot(
    page,
    state,
    nasPath,
    'test-blocker'
  )
  applyBlockerFixture(completedTransfer.taskId)
  const fixtureSummary = await pageApiFetch(
    page,
    state,
    `/dcc/controlled-files/nas-transfer/tasks/${completedTransfer.taskId}/permission-snapshot`,
    'NAS permission snapshot summary after blocker fixture'
  )
  assertCountAtLeast(fixtureSummary.blockerCount, expectedBlockers, 'snapshot blockers after fixture')
  const unmappedList = await openRestoreDrawer(page, state, completedTransfer.taskId, fixtureSummary)
  const preview = await previewRestore(page, state, completedTransfer.taskId)
  assertCountAtLeast((preview.blockers || []).length, expectedBlockers, 'restore preview blockers')
  const assertNoRestoreApply = installRestoreApplyNoWriteGuard(page, completedTransfer.taskId)
  await assertApplyRestoreDisabled(page, preview)
  await page.waitForTimeout(500)
  assertNoRestoreApply()
  assertApiOrigin()
  console.log(
    `PASS: test-blocker taskId=${completedTransfer.taskId}, unmapped=${unmappedList.length}, blockers=${(preview.blockers || []).length}`
  )
}

async function runAdminReadonly(page) {
  const state = {}
  const assertNoWrites = installNoWriteGuard(page, 'admin-readonly')
  const assertApiOrigin = await installApiOriginGuard(
    page,
    'admin-readonly',
    expectedApiOrigin(adminConfig, 'NAS_PERMISSION_E2E_ADMIN_API_ORIGIN')
  )
  await login(page, adminConfig, '/system/nas')
  await openNasPage(page, adminConfig)
  await testNasConnection(page, state)
  await refreshNasRoot(page, state)
  await selectFirstNasRootReadonly(page)
  const { dialog } = await openTransferDialogAndAssertOther(page)
  await dialog.locator('button:has-text("关闭")').first().click()
  assertNoWrites()
  assertApiOrigin()
  console.log(`PASS: admin-readonly baseUrl=${adminConfig.baseUrl}`)
}

async function runMode(selectedMode) {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: env('NAS_PERMISSION_E2E_HEADED', '0') !== '1' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  const outputDir = path.resolve(__dirname, '../../output/playwright')
  ensureDir(outputDir)
  try {
    if (selectedMode === 'test-write') {
      await runTestWrite(page)
    } else if (selectedMode === 'test-mapping') {
      await runTestMapping(page)
    } else if (selectedMode === 'test-blocker') {
      await runTestBlocker(page)
    } else if (selectedMode === 'admin-readonly') {
      await runAdminReadonly(page)
    } else {
      await runTestWrite(page)
      await runTestMapping(page)
      await runTestBlocker(page)
      await runAdminReadonly(page)
    }
  } catch (error) {
    const screenshotPath = path.join(
      outputDir,
      `dcc-nas-permission-real-data-${selectedMode}-${Date.now()}.png`
    )
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => undefined)
    error.message = `${error.message}\nScreenshot: ${screenshotPath}`
    throw error
  } finally {
    await browser.close()
  }
}

async function main() {
  requireMode()
  await runMode(mode)
}

main().catch((error) => {
  console.error(`FAIL: ${error.stack || error.message}`)
  process.exitCode = 1
})
