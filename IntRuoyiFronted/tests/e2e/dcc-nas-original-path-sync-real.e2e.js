const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const fs = require('node:fs')
const http = require('node:http')
const https = require('node:https')
const path = require('node:path')
const { spawnSync } = require('node:child_process')
const { chromium } = require('playwright')

const taskRoot = path.resolve(__dirname, '..', '..', '..', 'doc/tasks/20260830-nas-original-path-sync')
const artifactsDir = path.join(taskRoot, 'artifacts')

const config = {
  baseUrl: process.env.DCC_NAS_ORIGINAL_PATH_SYNC_BASE_URL || 'http://127.0.0.1:8310',
  backendHealthUrl:
    process.env.DCC_NAS_ORIGINAL_PATH_SYNC_BACKEND_HEALTH_URL ||
    'http://127.0.0.1:48310/actuator/health',
  tenant: process.env.DCC_NAS_ORIGINAL_PATH_SYNC_TENANT || '测试租户',
  username: process.env.DCC_NAS_ORIGINAL_PATH_SYNC_USERNAME || 'aoteman',
  password: process.env.DCC_NAS_ORIGINAL_PATH_SYNC_PASSWORD || '111111',
  mysqlContainer: process.env.INT_RUOYI_MYSQL_CONTAINER || 'int-ruoyi-mysql',
  mysqlDatabase: process.env.INT_RUOYI_MYSQL_DATABASE || 'ruoyi-vue-pro',
  browserExecutable:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  candidatePaths: (
    process.env.DCC_NAS_ORIGINAL_PATH_SYNC_CANDIDATE_PATHS ||
    [
      '1. QMS documents/0 QM/2025年质量方针与目标.pdf',
      '1. QMS documents/0 QM/2026年质量方针与目标(1).pdf'
    ].join(';')
  )
    .split(';')
    .map((item) => item.trim())
    .filter(Boolean)
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function requestText(url) {
  return new Promise((resolve, reject) => {
    const client = url.startsWith('https:') ? https : http
    const req = client.get(url, { timeout: 15000 }, (res) => {
      let body = ''
      res.setEncoding('utf8')
      res.on('data', (chunk) => {
        body += chunk
      })
      res.on('end', () => resolve({ statusCode: res.statusCode || 0, body }))
    })
    req.on('timeout', () => {
      req.destroy(new Error(`timeout requesting ${url}`))
    })
    req.on('error', reject)
  })
}

async function assertRuntimeReady() {
  if (!fs.existsSync(config.browserExecutable)) {
    throw new Error(`browser executable missing: ${config.browserExecutable}`)
  }
  const frontend = await requestText(config.baseUrl)
  if (frontend.statusCode !== 200) {
    throw new Error(`frontend URL ${config.baseUrl} returned HTTP ${frontend.statusCode}`)
  }
  const health = await requestText(config.backendHealthUrl)
  if (health.statusCode !== 200 || !health.body.includes('"status":"UP"')) {
    throw new Error(`backend health ${config.backendHealthUrl} is not UP: HTTP ${health.statusCode}`)
  }
}

function normalizeRelativePath(value) {
  const parts = String(value || '')
    .replace(/\\/g, '/')
    .split('/')
    .filter((part) => part && part !== '.')
  const normalized = []
  for (const part of parts) {
    if (part === '..') {
      normalized.pop()
    } else {
      normalized.push(part)
    }
  }
  return normalized.join('/')
}

function parentPathOf(relativePath) {
  const normalized = normalizeRelativePath(relativePath)
  const index = normalized.lastIndexOf('/')
  return index >= 0 ? normalized.slice(0, index) : ''
}

function fileNameOf(relativePath) {
  const normalized = normalizeRelativePath(relativePath)
  const index = normalized.lastIndexOf('/')
  return index >= 0 ? normalized.slice(index + 1) : normalized
}

function sha256Hex(value) {
  return crypto.createHash('sha256').update(String(value), 'utf8').digest('hex')
}

function buildPathHash(nasShareName, relativePath) {
  const canonical = `${String(nasShareName || '').trim().toLowerCase()}|${normalizeRelativePath(
    relativePath
  ).toLowerCase()}`
  return sha256Hex(canonical)
}

function buildSourceSignature(pathHash, fileSize, modifiedAt) {
  return sha256Hex(`${pathHash}|${fileSize}|${modifiedAt}`)
}

function sqlString(value) {
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`
}

function runMysql(sql) {
  return spawnSync(
    'docker',
    [
      'exec',
      '-i',
      config.mysqlContainer,
      'sh',
      '-lc',
      `MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 -N -B "${config.mysqlDatabase}"`
    ],
    {
      encoding: 'utf8',
      input: sql,
      maxBuffer: 10 * 1024 * 1024
    }
  )
}

function runMysqlStrict(sql, label) {
  if (!/^[A-Za-z0-9_-]+$/.test(config.mysqlDatabase)) {
    throw new Error(`INT_RUOYI_MYSQL_DATABASE contains unsupported characters: ${config.mysqlDatabase}`)
  }
  const result = runMysql(sql)
  if (result.status !== 0) {
    throw new Error(`${label} failed: ${result.stderr.trim() || result.stdout.trim()}`)
  }
  return result.stdout
}

function parseTabRows(output) {
  return output
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => line.split('\t'))
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

async function clickButton(root, text, label = text) {
  const button = root.locator(`button:has-text("${text}")`).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  if (await button.isDisabled()) {
    throw new Error(`Button is disabled: ${label}`)
  }
  await button.click()
}

async function selectLoginTenant(page, tenantName) {
  const form = page.locator('form.login-form:visible').first()
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) === 0 || !(await tenantInput.isVisible())) {
    await fillFirstVisible(page.locator('input[placeholder="请输入租户名称"]'), tenantName, 'tenant')
    return
  }
  await tenantInput.click()
  await tenantInput.fill(tenantName)
  const tenantOption = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: tenantName })
    .first()
  try {
    await tenantOption.waitFor({ state: 'visible', timeout: 15000 })
    await tenantOption.click()
  } catch (error) {
    await tenantInput.press('Enter')
    await page.keyboard.press('Escape').catch(() => undefined)
  }
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/system/nas')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)

  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(
    await form.locator('.verify-img-panel, .verify-bar-area').count(),
    0,
    'captcha is visible; configure a non-captcha test login path before running this E2E'
  )
  await selectLoginTenant(page, config.tenant)
  await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), config.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickButton(form, '登录', 'login')
  const response = await loginResponsePromise
  const payload = await response.json().catch(() => null)
  const code = payload && Number(payload.code)
  assert.ok(response.ok(), `/system/auth/login returned HTTP ${response.status()}`)
  assert.ok(code === 0 || code === 200, `login failed with code=${payload?.code}, msg=${payload?.msg || ''}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function assertNasPageVisible(page, context) {
  const notFound = page.locator('text=抱歉，您访问的页面不存在').first()
  if (await notFound.isVisible().catch(() => false)) {
    throw new Error(`${context}: /system/nas returned 404 or missing route permission`)
  }
  try {
    await page.waitForSelector('text=NAS 管理', { timeout: 60000 })
  } catch (error) {
    if (await notFound.isVisible().catch(() => false)) {
      throw new Error(`${context}: /system/nas returned 404 or missing route permission`)
    }
    throw error
  }
}

async function openNasPage(page) {
  await page.goto(`${config.baseUrl}/system/nas`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await assertNasPageVisible(page, 'direct navigation')
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
}

async function pageApi(page, endpoint, options = {}) {
  const method = options.method || 'GET'
  const result = await page.evaluate(
    async ({ apiEndpoint, requestMethod, requestBody }) => {
      const unwrap = (value) => {
        if (value && typeof value === 'object') {
          if (Object.prototype.hasOwnProperty.call(value, 'v')) return unwrap(value.v)
          if (Object.prototype.hasOwnProperty.call(value, 'value')) return unwrap(value.value)
          if (Object.prototype.hasOwnProperty.call(value, 'data')) return unwrap(value.data)
        }
        if (typeof value === 'string') {
          try {
            return unwrap(JSON.parse(value))
          } catch (error) {
            return value
          }
        }
        return value
      }
      const storedValue = (exactKey, containsKey) => {
        const exact = unwrap(localStorage.getItem(exactKey))
        if (exact !== null && exact !== undefined) return exact
        for (let index = 0; index < localStorage.length; index += 1) {
          const key = localStorage.key(index)
          if (key && key.includes(containsKey)) {
            const value = unwrap(localStorage.getItem(key))
            if (value !== null && value !== undefined) return value
          }
        }
        return undefined
      }
      const accessToken = storedValue('ACCESS_TOKEN', 'ACCESS_TOKEN')
      const tenantId = storedValue('tenantId', 'tenantId') || storedValue('visitTenantId', 'visitTenantId')
      if (!accessToken) throw new Error('ACCESS_TOKEN is missing from localStorage')
      if (!tenantId) throw new Error('tenantId is missing from localStorage')
      const response = await fetch(apiEndpoint, {
        method: requestMethod,
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'tenant-id': String(tenantId),
          'Content-Type': 'application/json'
        },
        body: requestBody === undefined ? undefined : JSON.stringify(requestBody)
      })
      const text = await response.text()
      let payload
      try {
        payload = JSON.parse(text)
      } catch (error) {
        payload = { raw: text }
      }
      return {
        ok: response.ok,
        status: response.status,
        payload,
        tenantId: String(tenantId)
      }
    },
    {
      apiEndpoint: endpoint,
      requestMethod: method,
      requestBody: options.body
    }
  )
  if (!result.ok) {
    throw new Error(`${method} ${endpoint} returned HTTP ${result.status}`)
  }
  const payload = result.payload
  if (payload && Object.prototype.hasOwnProperty.call(payload, 'code')) {
    const code = Number(payload.code)
    if (code !== 0 && code !== 200) {
      throw new Error(`${method} ${endpoint} failed with code=${payload.code}, msg=${payload.msg || ''}`)
    }
    return { data: payload.data, tenantId: result.tenantId }
  }
  return { data: payload, tenantId: result.tenantId }
}

async function resolveNasCandidate(page) {
  const { data: nasConfig, tenantId } = await pageApi(page, '/admin-api/infra/file/nas-config')
  if (!nasConfig?.share) {
    throw new Error('NAS config is missing share name')
  }
  for (const configuredPath of config.candidatePaths) {
    const normalizedPath = normalizeRelativePath(configuredPath)
    const listingPath = parentPathOf(normalizedPath)
    const search = new URLSearchParams({ path: listingPath })
    const { data: listing } = await pageApi(page, `/admin-api/infra/file/nas-files?${search.toString()}`)
    if (!listing || !Array.isArray(listing.items)) {
      throw new Error(`NAS listing is missing items for ${listingPath}`)
    }
    const item = listing.items.find(
      (candidate) => !candidate.dir && normalizeRelativePath(candidate.path) === normalizedPath
    )
    if (!item) continue
    if (!Number.isFinite(Number(item.size)) || Number(item.size) <= 0) {
      throw new Error(`NAS candidate has invalid file size: ${normalizedPath}`)
    }
    if (!Number.isFinite(Number(item.modifiedAt)) || Number(item.modifiedAt) <= 0) {
      throw new Error(`NAS candidate has invalid modifiedAt: ${normalizedPath}`)
    }
    const pathHash = buildPathHash(nasConfig.share, normalizedPath)
    const activeCount = Number(
      runMysqlStrict(
        `
SELECT COUNT(*)
FROM dcc_nas_original_path_sync_file
WHERE tenant_id = ${Number(tenantId)}
  AND nas_share_name = ${sqlString(nasConfig.share)}
  AND path_hash = ${sqlString(pathHash)}
  AND sync_status = 'ACTIVE'
  AND deleted = b'0';
`,
        'check active original-path sync rows'
      ).trim() || '0'
    )
    if (activeCount > 0) continue
    return {
      tenantId: Number(tenantId),
      nasShareName: nasConfig.share,
      rootPath: listing.rootPath || nasConfig.share,
      normalizedPath,
      fileName: item.name || fileNameOf(normalizedPath),
      fileSize: Number(item.size),
      modifiedAt: Number(item.modifiedAt),
      pathHash,
      sourceSignature: buildSourceSignature(pathHash, Number(item.size), Number(item.modifiedAt))
    }
  }
  throw new Error(
    'No configured NAS candidate file is readable and free of active original-path sync records'
  )
}

function requireLoginUserId(tenantId) {
  const rows = parseTabRows(
    runMysqlStrict(
      `
SELECT id
FROM system_users
WHERE tenant_id = ${Number(tenantId)}
  AND username = ${sqlString(config.username)}
  AND status = 0
  AND deleted = b'0'
ORDER BY id DESC
LIMIT 1;
`,
      'query login user id'
    )
  )
  if (!rows.length || !rows[0][0]) {
    throw new Error(`active user ${config.username} not found in tenant ${tenantId}`)
  }
  return Number(rows[0][0])
}

function createAuditFixture(candidate) {
  const userId = requireLoginUserId(candidate.tenantId)
  const creator = `codex-nas-original-path-sync-e2e-${Date.now()}`
  const output = runMysqlStrict(
    `
INSERT INTO dcc_nas_control_audit_task (
  operator_user_id, nas_share_name, scan_roots_json, status, current_path,
  scanned_file_count, controlled_file_count, not_controlled_file_count,
  ambiguous_file_count, source_missing_count, skipped_directory_count,
  report_file_id, report_file_name, started_at, completed_at, failure_reason,
  creator, updater, deleted, tenant_id
) VALUES (
  ${userId}, ${sqlString(candidate.nasShareName)}, ${sqlString(JSON.stringify([parentPathOf(candidate.normalizedPath)]))},
  'COMPLETED', NULL, 1, 0, 1, 0, 0, 0, NULL, NULL, NOW(), NOW(), NULL,
  ${sqlString(creator)}, ${sqlString(creator)}, b'0', ${candidate.tenantId}
);
SET @codex_audit_task_id = LAST_INSERT_ID();
INSERT INTO dcc_nas_control_audit_file (
  task_id, nas_share_name, root_path, normalized_relative_path, path_hash, file_name,
  file_size, modified_at, source_signature, control_status, classification_status,
  download_status, archive_status, controlled_file_id, original_path_sync_status,
  original_path_sync_file_id, original_path_sync_task_id, original_path_sync_task_item_id,
  original_path_sync_error_code, original_path_sync_error, creator, updater, deleted, tenant_id
) VALUES (
  @codex_audit_task_id, ${sqlString(candidate.nasShareName)}, ${sqlString(candidate.rootPath)},
  ${sqlString(candidate.normalizedPath)}, ${sqlString(candidate.pathHash)}, ${sqlString(candidate.fileName)},
  ${candidate.fileSize}, FROM_UNIXTIME(${Math.trunc(candidate.modifiedAt)} / 1000),
  ${sqlString(candidate.sourceSignature)}, 'NOT_CONTROLLED', 'PENDING_RECOGNITION',
  'NOT_SELECTED', 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
  ${sqlString(creator)}, ${sqlString(creator)}, b'0', ${candidate.tenantId}
);
SET @codex_audit_file_id = LAST_INSERT_ID();
SELECT 'AUDIT_TASK_ID', @codex_audit_task_id;
SELECT 'AUDIT_FILE_ID', @codex_audit_file_id;
`,
    'create task-owned NAS original-path sync audit fixture'
  )
  const values = Object.fromEntries(parseTabRows(output).map(([key, value]) => [key, Number(value)]))
  assert.ok(values.AUDIT_TASK_ID > 0, 'fixture audit task id missing')
  assert.ok(values.AUDIT_FILE_ID > 0, 'fixture audit file id missing')
  return { taskId: values.AUDIT_TASK_ID, auditFileId: values.AUDIT_FILE_ID, userId, creator }
}

function queryFixtureState(fixture) {
  const output = runMysqlStrict(
    `
SELECT 'AUDIT',
       f.id,
       f.original_path_sync_status,
       IFNULL(f.original_path_sync_file_id, ''),
       IFNULL(f.original_path_sync_task_id, ''),
       IFNULL(f.original_path_sync_task_item_id, ''),
       IFNULL(f.original_path_sync_error_code, ''),
       IFNULL(f.original_path_sync_error, '')
FROM dcc_nas_control_audit_file f
WHERE f.id = ${fixture.auditFileId}
  AND f.task_id = ${fixture.taskId}
  AND f.deleted = b'0';

SELECT 'SYNC',
       s.id,
       s.sync_status,
       s.source_file_id,
       s.normalized_relative_path,
       s.file_size,
       s.source_signature
FROM dcc_nas_original_path_sync_file s
WHERE s.audit_task_id = ${fixture.taskId}
  AND s.audit_file_id = ${fixture.auditFileId}
  AND s.deleted = b'0'
ORDER BY s.id DESC
LIMIT 1;

SELECT 'ACTIVE_SYNC_COUNT',
       COUNT(*)
FROM dcc_nas_original_path_sync_file s
WHERE s.audit_task_id = ${fixture.taskId}
  AND s.audit_file_id = ${fixture.auditFileId}
  AND s.sync_status = 'ACTIVE'
  AND s.deleted = b'0';

SELECT 'RESYNC_CANDIDATE_COUNT',
       COUNT(*)
FROM dcc_nas_control_audit_file f
WHERE f.task_id = ${fixture.taskId}
  AND f.deleted = b'0'
  AND f.control_status = 'NOT_CONTROLLED'
  AND f.controlled_file_id IS NULL
  AND (f.original_path_sync_status IS NULL OR f.original_path_sync_status NOT IN (
    'ORIGINAL_PATH_WAITING', 'ORIGINAL_PATH_RUNNING', 'ORIGINAL_PATH_ACTIVE'
  ))
  AND f.original_path_sync_file_id IS NULL;
`,
    'query task-owned NAS original-path sync fixture state'
  )
  const state = {
    audit: null,
    sync: null,
    activeSyncCount: 0,
    resyncCandidateCount: 0
  }
  for (const row of parseTabRows(output)) {
    if (row[0] === 'AUDIT') {
      state.audit = {
        auditFileId: Number(row[1]),
        originalPathSyncStatus: row[2] || null,
        originalPathSyncFileId: row[3] ? Number(row[3]) : null,
        originalPathSyncTaskId: row[4] ? Number(row[4]) : null,
        originalPathSyncTaskItemId: row[5] ? Number(row[5]) : null,
        originalPathSyncErrorCode: row[6] || null,
        originalPathSyncError: row[7] || null
      }
    } else if (row[0] === 'SYNC') {
      state.sync = {
        syncFileId: Number(row[1]),
        syncStatus: row[2],
        sourceFileId: Number(row[3]),
        normalizedRelativePath: row[4],
        fileSize: Number(row[5]),
        sourceSignature: row[6]
      }
    } else if (row[0] === 'ACTIVE_SYNC_COUNT') {
      state.activeSyncCount = Number(row[1])
    } else if (row[0] === 'RESYNC_CANDIDATE_COUNT') {
      state.resyncCandidateCount = Number(row[1])
    }
  }
  return state
}

function queryStorageFile(sourceFileId, includeDeleted = false) {
  const rows = parseTabRows(
    runMysqlStrict(
      `
SELECT id, name, path, size, deleted
FROM infra_file
WHERE id = ${Number(sourceFileId)}
${includeDeleted ? '' : "  AND deleted = b'0'"};
`,
      'query synced infra file'
    )
  )
  if (!rows.length) return null
  return {
    id: Number(rows[0][0]),
    name: rows[0][1],
    path: rows[0][2],
    size: Number(rows[0][3]),
    deleted: rows[0][4] !== '\0'
  }
}

function cleanupAuditFixture(fixture) {
  if (!fixture?.taskId) return
  runMysqlStrict(
    `
UPDATE dcc_controlled_file_nas_transfer_task_item item
JOIN dcc_controlled_file_nas_transfer_task task ON task.id = item.task_id
SET item.deleted = b'1'
WHERE task.audit_task_id = ${fixture.taskId}
  AND task.source_type = 'NAS_ORIGINAL_PATH_SYNC';

UPDATE dcc_controlled_file_nas_transfer_task
SET deleted = b'1'
WHERE audit_task_id = ${fixture.taskId}
  AND source_type = 'NAS_ORIGINAL_PATH_SYNC';

UPDATE dcc_nas_original_path_sync_file
SET deleted = b'1'
WHERE audit_task_id = ${fixture.taskId}
  AND sync_status = 'DELETED';

UPDATE dcc_nas_control_audit_file
SET deleted = b'1'
WHERE task_id = ${fixture.taskId};

UPDATE dcc_nas_control_audit_task
SET deleted = b'1'
WHERE id = ${fixture.taskId};
`,
    'cleanup task-owned NAS original-path sync audit fixture'
  )
}

async function deleteActiveSyncByApi(page, fixture) {
  const state = queryFixtureState(fixture)
  if (state.sync?.syncStatus !== 'ACTIVE' || !state.sync.syncFileId) return false
  await pageApi(
    page,
    `/admin-api/dcc/controlled-files/nas-control-audit/original-path-sync/${state.sync.syncFileId}`,
    { method: 'DELETE' }
  )
  return true
}

async function waitForRowWithText(dialog, fileName, text, timeout = 120000) {
  const row = dialog.locator('.el-table__row').filter({ hasText: fileName }).filter({ hasText: text }).first()
  await row.waitFor({ state: 'visible', timeout })
  return row
}

async function run() {
  ensureDir(artifactsDir)
  await assertRuntimeReady()

  let browser
  let page
  let fixture
  let evidence
  let uiDeleted = false

  try {
    browser = await chromium.launch({
      executablePath: config.browserExecutable,
      headless: true
    })
    page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
    await login(page)
    await openNasPage(page)

    const candidate = await resolveNasCandidate(page)
    fixture = createAuditFixture(candidate)
    const beforeState = queryFixtureState(fixture)
    assert.equal(beforeState.resyncCandidateCount, 1, 'fixture must start with one sync candidate')

    await page.evaluate((taskId) => {
      localStorage.setItem('int-ruoyi:nas-control-audit:last-task-id', String(taskId))
      localStorage.removeItem('int-ruoyi:nas-transfer:last-task-id')
    }, fixture.taskId)
    await openNasPage(page)

    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '统计未受控文件' }).last()
    await dialog.waitFor({ state: 'visible', timeout: 60000 })
    await dialog.getByText(`任务编号：${fixture.taskId}`).waitFor({ state: 'visible', timeout: 30000 })
    await dialog.getByText('未受控数量：1').waitFor({ state: 'visible', timeout: 30000 })
    await waitForRowWithText(dialog, candidate.fileName, '未同步', 60000)

    const beforeScreenshot = path.join(artifactsDir, 'nas-original-path-sync-single-before.png')
    await page.screenshot({ path: beforeScreenshot, fullPage: true })

    const syncResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes(`/dcc/controlled-files/nas-control-audit/${fixture.taskId}/original-path-sync`) &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await clickButton(dialog, '同步 1 个验证', 'sync one original-path file')
    const syncResponse = await syncResponsePromise
    const syncPayload = await syncResponse.json()
    assert.ok(syncResponse.ok(), `sync one returned HTTP ${syncResponse.status()}`)
    assert.ok(
      Number(syncPayload.code) === 0 || Number(syncPayload.code) === 200,
      `sync one failed with code=${syncPayload.code}, msg=${syncPayload.msg || ''}`
    )
    const syncTaskId = Number(syncPayload.data?.taskId)
    assert.ok(syncTaskId > 0, 'sync task id missing from response')

    await waitForRowWithText(dialog, candidate.fileName, '已同步', 120000)
    const afterSyncScreenshot = path.join(artifactsDir, 'nas-original-path-sync-single-after-sync.png')
    await page.screenshot({ path: afterSyncScreenshot, fullPage: true })

    const afterSyncState = queryFixtureState(fixture)
    assert.equal(afterSyncState.audit?.originalPathSyncStatus, 'ORIGINAL_PATH_ACTIVE')
    assert.equal(afterSyncState.activeSyncCount, 1)
    assert.equal(afterSyncState.resyncCandidateCount, 0)
    assert.equal(afterSyncState.sync?.normalizedRelativePath, candidate.normalizedPath)
    assert.equal(afterSyncState.sync?.fileSize, candidate.fileSize)
    assert.equal(afterSyncState.sync?.sourceSignature, candidate.sourceSignature)
    const storageFile = queryStorageFile(afterSyncState.sync.sourceFileId)
    assert.ok(storageFile, 'synced source file must exist in infra_file before delete')
    assert.equal(storageFile.name, candidate.fileName)
    assert.equal(storageFile.size, candidate.fileSize)
    assert.ok(
      storageFile.path.includes('dcc/nas-original-path-sync/'),
      `synced storage path must use original-path sync directory, got ${storageFile.path}`
    )

    const deleteResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes(
          `/dcc/controlled-files/nas-control-audit/original-path-sync/${afterSyncState.sync.syncFileId}`
        ) && response.request().method() === 'DELETE',
      { timeout: 60000 }
    )
    const syncedRow = await waitForRowWithText(dialog, candidate.fileName, '已同步', 30000)
    await clickButton(syncedRow, '移除同步记录', 'delete synced original-path file')
    const confirmDialog = page.locator('.el-message-box:visible').filter({ hasText: '移除同步记录' }).last()
    await confirmDialog.waitFor({ state: 'visible', timeout: 30000 })
    await clickButton(confirmDialog, '确认移除', 'confirm delete synced original-path file')
    const deleteResponse = await deleteResponsePromise
    const deletePayload = await deleteResponse.json()
    assert.ok(deleteResponse.ok(), `delete original-path sync returned HTTP ${deleteResponse.status()}`)
    assert.ok(
      Number(deletePayload.code) === 0 || Number(deletePayload.code) === 200,
      `delete original-path sync failed with code=${deletePayload.code}, msg=${deletePayload.msg || ''}`
    )
    uiDeleted = true

    await waitForRowWithText(dialog, candidate.fileName, '已移除', 60000)
    const afterDeleteScreenshot = path.join(artifactsDir, 'nas-original-path-sync-single-after-delete.png')
    await page.screenshot({ path: afterDeleteScreenshot, fullPage: true })

    const afterDeleteState = queryFixtureState(fixture)
    assert.equal(afterDeleteState.audit?.originalPathSyncStatus, 'ORIGINAL_PATH_DELETED')
    assert.equal(afterDeleteState.audit?.originalPathSyncFileId, null)
    assert.equal(afterDeleteState.sync?.syncStatus, 'DELETED')
    assert.equal(afterDeleteState.activeSyncCount, 0)
    assert.equal(afterDeleteState.resyncCandidateCount, 1)
    assert.equal(queryStorageFile(afterSyncState.sync.sourceFileId), null)
    const deletedStorageFile = queryStorageFile(afterSyncState.sync.sourceFileId, true)
    assert.ok(deletedStorageFile?.deleted, 'synced source file must be logically deleted after remove')

    evidence = {
      scenario: 'single NAS original-path sync E2E',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      auditTaskId: fixture.taskId,
      auditFileId: fixture.auditFileId,
      relativePath: candidate.normalizedPath,
      syncTaskId,
      syncFileId: afterSyncState.sync.syncFileId,
      sourceFileId: afterSyncState.sync.sourceFileId,
      before: {
        resyncCandidateCount: beforeState.resyncCandidateCount,
        uiStatus: '未同步'
      },
      afterSync: {
        activeSyncCount: afterSyncState.activeSyncCount,
        resyncCandidateCount: afterSyncState.resyncCandidateCount,
        auditStatus: afterSyncState.audit.originalPathSyncStatus,
        storagePath: storageFile.path,
        storageSize: storageFile.size
      },
      afterDelete: {
        activeSyncCount: afterDeleteState.activeSyncCount,
        resyncCandidateCount: afterDeleteState.resyncCandidateCount,
        auditStatus: afterDeleteState.audit.originalPathSyncStatus,
        syncStatus: afterDeleteState.sync.syncStatus,
        activeStorageFileRemoved: true,
        storageFileLogicallyDeleted: true
      },
      screenshots: {
        before: beforeScreenshot,
        afterSync: afterSyncScreenshot,
        afterDelete: afterDeleteScreenshot
      }
    }
    fs.writeFileSync(
      path.join(artifactsDir, 'nas-original-path-sync-single-e2e-evidence.json'),
      `${JSON.stringify(evidence, null, 2)}\n`,
      'utf8'
    )
    console.log(
      `PASS: single NAS original-path sync E2E verified auditTaskId=${fixture.taskId}, auditFileId=${fixture.auditFileId}, syncTaskId=${syncTaskId}`
    )
  } catch (error) {
    if (page) {
      await page
        .screenshot({
          path: path.join(artifactsDir, 'nas-original-path-sync-single-failure.png'),
          fullPage: true
        })
        .catch(() => undefined)
    }
    throw error
  } finally {
    if (page && fixture && !uiDeleted) {
      await deleteActiveSyncByApi(page, fixture)
    }
    cleanupAuditFixture(fixture)
    if (browser) {
      await browser.close()
    }
  }
}

run().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exit(1)
})
