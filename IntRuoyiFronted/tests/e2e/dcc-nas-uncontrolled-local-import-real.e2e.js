const assert = require('node:assert/strict')
const fs = require('node:fs')
const http = require('node:http')
const https = require('node:https')
const os = require('node:os')
const path = require('node:path')
const { spawnSync } = require('node:child_process')
const { chromium } = require('playwright')

const checkMode = process.argv.includes('--check')
const taskRoot = path.resolve(
  __dirname,
  '..',
  '..',
  '..',
  'doc/tasks/20260802-dcc-uncontrolled-file-local-import-design'
)

const config = {
  frontendUrl: process.env.DCC_NAS_UNCONTROLLED_IMPORT_FRONTEND_URL || 'http://127.0.0.1:8081',
  backendHealthUrl:
    process.env.DCC_NAS_UNCONTROLLED_IMPORT_BACKEND_HEALTH_URL || 'http://127.0.0.1:48081/actuator/health',
  mysqlContainer: process.env.INT_RUOYI_MYSQL_CONTAINER || 'int-ruoyi-mysql',
  mysqlDatabase: process.env.INT_RUOYI_MYSQL_DATABASE || 'ruoyi-vue-pro',
  browserExecutable:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  auditTaskId: process.env.DCC_NAS_UNCONTROLLED_IMPORT_AUDIT_TASK_ID || '',
  localDir:
    process.env.DCC_NAS_UNCONTROLLED_IMPORT_LOCAL_DIR ||
    path.join(taskRoot, 'artifacts', 'local-import-full-e2e'),
  tenant: process.env.DCC_NAS_UNCONTROLLED_IMPORT_TENANT || '测试租户',
  username: process.env.DCC_NAS_UNCONTROLLED_IMPORT_USERNAME || 'aoteman',
  password: process.env.DCC_NAS_UNCONTROLLED_IMPORT_PASSWORD || '111111'
}

const requiredAuditFileColumns = [
  'task_id',
  'normalized_relative_path',
  'source_signature',
  'classification_status',
  'classification_candidates_json',
  'expected_local_relative_path',
  'download_status',
  'archive_status',
  'archive_error_code',
  'selected_import_task_id',
  'selected_import_task_item_id'
]

const requiredTaskColumns = ['audit_task_id', 'idempotency_key', 'request_hash']

const requiredTaskItemColumns = [
  'audit_file_id',
  'source_signature',
  'classification_status_snapshot',
  'classification_candidates_json_snapshot',
  'local_relative_path',
  'local_write_status',
  'archive_status',
  'archive_category_id_snapshot',
  'archive_directory_id_snapshot',
  'archive_dcc_project_code_id_snapshot',
  'archive_file_type_taxonomy_id_snapshot',
  'archive_effective_date_snapshot'
]

const sharedNasSourceFiles = [
  {
    auditFileId: 1,
    relativePath: '1. QMS documents/0 QM/2025年质量方针与目标.pdf',
    expectedLocalRelativePath: 'PTCABC/设计验证方案/2025年质量方针与目标.pdf',
    sourceSignature: '10fb1e18fedd414f64d2f0290b5995ee05f1f8b8df971a920a214548970f7d42',
    selectForFullFlow: true
  },
  {
    auditFileId: 2,
    relativePath: '1. QMS documents/0 QM/2026年质量方针与目标(1).pdf',
    expectedLocalRelativePath:
      '_未分类待处理/1. QMS documents/0 QM/2026年质量方针与目标(1).pdf',
    sourceSignature: 'e1161c9e75bdb6c7db759c7de1a8e102bce560cca7ebde0d170df55a30102648',
    selectForFullFlow: true
  },
  {
    auditFileId: 3,
    relativePath:
      '1. QMS documents/5.STM实验室规程/STM表单/RE-STM-MM-015-01（A 0）无菌方法适用性试验记录.pdf',
    expectedLocalRelativePath:
      'PTCABC/设计验证方案/RE-STM-MM-015-01（A 0）无菌方法适用性试验记录.pdf',
    sourceSignature: '99c143982a200d9ae92251a8a30d02f94a3a4d826dc30178d3e4c5bdf4079247',
    selectForFullFlow: false
  }
]

function block(blockers, message) {
  blockers.push(message)
}

function validateStaticConfig(blockers) {
  if (!/^[A-Za-z0-9_-]+$/.test(config.mysqlDatabase)) {
    block(blockers, `INT_RUOYI_MYSQL_DATABASE contains unsupported characters: ${config.mysqlDatabase}`)
  }
  if (!/^\d+$/.test(config.auditTaskId || '')) {
    block(blockers, `DCC_NAS_UNCONTROLLED_IMPORT_AUDIT_TASK_ID must be numeric: ${config.auditTaskId || '<missing>'}`)
  }
  if (!fs.existsSync(config.browserExecutable)) {
    block(blockers, `browser executable missing: ${config.browserExecutable}`)
  }
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

async function validateRuntime(blockers) {
  const runtime = {
    frontendOk: false,
    backendOk: false
  }

  try {
    const frontend = await requestText(config.frontendUrl)
    if (frontend.statusCode !== 200) {
      block(blockers, `frontend URL ${config.frontendUrl} returned HTTP ${frontend.statusCode}`)
    } else {
      runtime.frontendOk = true
    }
  } catch (error) {
    block(blockers, `frontend URL ${config.frontendUrl} is not reachable: ${error.message}`)
  }

  try {
    const health = await requestText(config.backendHealthUrl)
    if (health.statusCode !== 200 || !health.body.includes('"status":"UP"')) {
      block(blockers, `backend health ${config.backendHealthUrl} is not UP: HTTP ${health.statusCode}`)
    } else {
      runtime.backendOk = true
    }
  } catch (error) {
    block(blockers, `backend health ${config.backendHealthUrl} is not reachable: ${error.message}`)
  }

  return runtime
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
  const result = runMysql(sql)
  if (result.status !== 0) {
    throw new Error(`${label} failed: ${result.stderr.trim() || result.stdout.trim()}`)
  }
  return result.stdout
}

function sqlString(value) {
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`
}

function validateSchema(blockers) {
  if (!/^[A-Za-z0-9_-]+$/.test(config.mysqlDatabase)) {
    return
  }

  const tables = [
    'dcc_nas_control_audit_file',
    'dcc_controlled_file_nas_transfer_task',
    'dcc_controlled_file_nas_transfer_task_item'
  ]
  const columns = [
    ...requiredAuditFileColumns.map((column) => `dcc_nas_control_audit_file.${column}`),
    ...requiredTaskColumns.map((column) => `dcc_controlled_file_nas_transfer_task.${column}`),
    ...requiredTaskItemColumns.map((column) => `dcc_controlled_file_nas_transfer_task_item.${column}`)
  ]

  const sql = `
SELECT CONCAT('TABLE:', TABLE_NAME)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (${tables.map((table) => `'${table}'`).join(',')});
SELECT CONCAT('COLUMN:', TABLE_NAME, '.', COLUMN_NAME)
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND CONCAT(TABLE_NAME, '.', COLUMN_NAME) IN (${columns.map((column) => `'${column}'`).join(',')});
`

  const result = runMysql(sql)
  if (result.status !== 0) {
    block(blockers, `runtime MySQL schema probe failed for ${config.mysqlContainer}/${config.mysqlDatabase}: ${result.stderr.trim() || result.stdout.trim()}`)
    return
  }

  const output = new Set(result.stdout.split(/\r?\n/).filter(Boolean))
  for (const table of tables) {
    if (!output.has(`TABLE:${table}`)) {
      block(blockers, `runtime schema missing table ${table}`)
    }
  }
  for (const column of columns) {
    if (!output.has(`COLUMN:${column}`)) {
      block(blockers, `runtime schema missing column ${column}`)
    }
  }
}

function validateAuditSample(blockers) {
  if (!config.auditTaskId || !/^\d+$/.test(config.auditTaskId)) {
    block(blockers, 'missing numeric DCC_NAS_UNCONTROLLED_IMPORT_AUDIT_TASK_ID for task-owned completed audit sample data')
    return
  }

  const sql = `
SELECT CONCAT('MATCHED:', COUNT(*))
FROM dcc_nas_control_audit_file
WHERE task_id = ${config.auditTaskId}
  AND classification_status = 'MATCHED'
  AND expected_local_relative_path IS NOT NULL
  AND deleted = b'0';
SELECT CONCAT('PENDING:', COUNT(*))
FROM dcc_nas_control_audit_file
WHERE task_id = ${config.auditTaskId}
  AND classification_status IN ('UNCLASSIFIED_PENDING', 'AMBIGUOUS')
  AND deleted = b'0';
SELECT CONCAT('PENDING_PATH:', COUNT(*))
FROM dcc_nas_control_audit_file
WHERE task_id = ${config.auditTaskId}
  AND classification_status IN ('UNCLASSIFIED_PENDING', 'AMBIGUOUS')
  AND expected_local_relative_path REGEXP '^_未分类待处理/'
  AND deleted = b'0';
SELECT CONCAT('ARCHIVE_BLOCK:', COUNT(*))
FROM dcc_nas_control_audit_file
WHERE task_id = ${config.auditTaskId}
  AND archive_error_code = 'ARCHIVE_METADATA_REQUIRED'
  AND deleted = b'0';
`

  const result = runMysql(sql)
  if (result.status !== 0) {
    block(blockers, `task-owned audit sample probe failed for task ${config.auditTaskId}: ${result.stderr.trim() || result.stdout.trim()}`)
    return
  }

  const counts = Object.fromEntries(
    result.stdout
      .split(/\r?\n/)
      .filter(Boolean)
      .map((line) => {
        const [key, value] = line.split(':')
        return [key, Number(value)]
      })
  )
  if (!counts.MATCHED) {
    block(blockers, `audit task ${config.auditTaskId} has no MATCHED file with expected local relative path`)
  }
  if (!counts.PENDING) {
    block(blockers, `audit task ${config.auditTaskId} has no UNCLASSIFIED_PENDING or AMBIGUOUS file for "unclassified/pending" verification`)
  }
  if (!counts.PENDING_PATH) {
    block(blockers, `audit task ${config.auditTaskId} has no UNCLASSIFIED_PENDING or AMBIGUOUS file with _未分类待处理 expected local path`)
  }
  if (!counts.ARCHIVE_BLOCK) {
    block(blockers, `audit task ${config.auditTaskId} has no ARCHIVE_METADATA_REQUIRED blocker row`)
  }
}

async function validateBrowserCapabilities(blockers) {
  if (!fs.existsSync(config.browserExecutable)) {
    return
  }

  let browser
  try {
    browser = await chromium.launch({
      executablePath: config.browserExecutable,
      headless: true
    })
    const page = await browser.newPage()
    await page.goto(config.frontendUrl, { waitUntil: 'domcontentloaded', timeout: 10000 })
    const capabilities = await page.evaluate(() => ({
      secureContext: window.isSecureContext,
      showDirectoryPicker: typeof window.showDirectoryPicker === 'function',
      directoryHandle: typeof window.FileSystemDirectoryHandle?.prototype?.getFileHandle === 'function',
      fileHandle: typeof window.FileSystemFileHandle?.prototype?.createWritable === 'function'
    }))
    if (!capabilities.secureContext) {
      block(blockers, `${config.frontendUrl} is not a secure context for File System Access API`)
    }
    if (!capabilities.showDirectoryPicker) {
      block(blockers, 'browser does not expose showDirectoryPicker')
    }
    if (!capabilities.directoryHandle) {
      block(blockers, 'browser does not expose FileSystemDirectoryHandle.getFileHandle')
    }
    if (!capabilities.fileHandle) {
      block(blockers, 'browser does not expose FileSystemFileHandle.createWritable')
    }
  } catch (error) {
    block(blockers, `browser File System Access probe failed: ${error.message}`)
  } finally {
    if (browser) {
      await browser.close()
    }
  }
}

function validateRelativePath(relativePath) {
  const normalized = String(relativePath || '').replace(/\\/g, '/')
  if (
    !normalized ||
    normalized.startsWith('/') ||
    normalized.endsWith('/') ||
    /^[A-Za-z]:/.test(normalized) ||
    normalized.split('/').some((segment) => !segment || segment === '.' || segment === '..')
  ) {
    throw new Error(`unsafe relative path: ${relativePath}`)
  }
  return normalized
}

function safeJoin(root, relativePath) {
  const safeRelativePath = validateRelativePath(relativePath)
  const target = path.resolve(root, ...safeRelativePath.split('/'))
  const normalizedRoot = path.resolve(root)
  if (target !== normalizedRoot && !target.startsWith(normalizedRoot + path.sep)) {
    throw new Error(`local target path escapes root: ${relativePath}`)
  }
  return target
}

function verifySharedNasSourceFiles() {
  for (const file of sharedNasSourceFiles) {
    validateRelativePath(file.relativePath)
  }

  const script = String.raw`
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$payload = [Console]::In.ReadToEnd() | ConvertFrom-Json
$sql = "SELECT config_key, value FROM infra_config WHERE config_key LIKE 'infra.nas.%' AND deleted=b'0' ORDER BY config_key;"
$mysqlCommand = 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 -N -B "' + $payload.mysqlDatabase + '"'
$configRows = $sql | docker exec -i $payload.mysqlContainer sh -lc $mysqlCommand
if ($LASTEXITCODE -ne 0) {
  throw "NAS config query failed"
}
$nasConfig = @{}
foreach ($line in ($configRows -split '\r?\n')) {
  if ([string]::IsNullOrWhiteSpace($line)) { continue }
  $parts = $line -split ([char]9), 2
  if ($parts.Length -eq 2) {
    $nasConfig[$parts[0]] = $parts[1]
  }
}
foreach ($requiredKey in @('infra.nas.server','infra.nas.share','infra.nas.username','infra.nas.password')) {
  if ([string]::IsNullOrWhiteSpace($nasConfig[$requiredKey])) {
    throw "NAS config missing $requiredKey"
  }
}
$root = "\\" + $nasConfig['infra.nas.server'] + "\" + $nasConfig['infra.nas.share']
$domain = $nasConfig['infra.nas.domain']
$credentialUser = if ([string]::IsNullOrWhiteSpace($domain)) { $nasConfig['infra.nas.username'] } else { $domain + "\" + $nasConfig['infra.nas.username'] }
$securePassword = [System.Security.SecureString]::new()
foreach ($char in ([string]$nasConfig['infra.nas.password']).ToCharArray()) {
  $securePassword.AppendChar($char)
}
$securePassword.MakeReadOnly()
$credential = [System.Management.Automation.PSCredential]::new($credentialUser, $securePassword)
$driveName = @('Z','Y','X','W','V','U','T') | Where-Object { -not (Get-PSDrive -Name $_ -ErrorAction SilentlyContinue) } | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($driveName)) {
  throw "No free temporary PSDrive letter for NAS sample setup"
}
New-PSDrive -Name $driveName -PSProvider FileSystem -Root $root -Credential $credential -Scope Script | Out-Null
try {
  $driveRoot = [System.IO.Path]::GetFullPath($driveName + ":\")
  foreach ($file in $payload.files) {
    $relativePath = [string]$file.relativePath
    if ($relativePath.Contains('\') -or $relativePath.StartsWith('/') -or $relativePath.Contains('..')) {
      throw "Unsafe NAS sample relative path: $relativePath"
    }
    $target = Join-Path ($driveName + ":\") ($relativePath -replace '/', [System.IO.Path]::DirectorySeparatorChar)
    $targetFull = [System.IO.Path]::GetFullPath($target)
    if (-not $targetFull.StartsWith($driveRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
      throw "NAS sample target escaped drive root: $relativePath"
    }
    if (-not [System.IO.File]::Exists($targetFull)) {
      throw "Existing NAS source file missing: $relativePath"
    }
    $item = Get-Item -LiteralPath $targetFull
    if ($item.Length -le 0) {
      throw "Existing NAS source file is empty: $relativePath"
    }
    $stream = [System.IO.File]::Open($targetFull, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
    try {
      $buffer = [byte[]]::new([Math]::Min(4, [int]$item.Length))
      [void]$stream.Read($buffer, 0, $buffer.Length)
    } finally {
      $stream.Dispose()
    }
    Write-Output ("NAS_EXISTING_FILE_READY" + [char]9 + $relativePath + [char]9 + $item.Length)
  }
} finally {
  Remove-PSDrive -Name $driveName -Force -ErrorAction SilentlyContinue
}
`

  const result = spawnSync(
    'powershell',
    ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', script],
    {
      encoding: 'utf8',
      input: JSON.stringify({
        mysqlContainer: config.mysqlContainer,
        mysqlDatabase: config.mysqlDatabase,
        files: sharedNasSourceFiles
      }),
      maxBuffer: 10 * 1024 * 1024
    }
  )
  if (result.status !== 0) {
    throw new Error(`verifySharedNasSourceFiles failed: ${result.stderr.trim() || result.stdout.trim()}`)
  }
  const readyLines = result.stdout.split(/\r?\n/).filter((line) => line.startsWith('NAS_EXISTING_FILE_READY\t'))
  assert.equal(readyLines.length, sharedNasSourceFiles.length, 'all selected existing NAS files must be readable')
  return readyLines
}

function syncTaskOwnedFixtureToExistingNasFiles() {
  const matched = sharedNasSourceFiles[0]
  const pending = sharedNasSourceFiles[1]
  const archiveBlock = sharedNasSourceFiles[2]
  const sql = `
UPDATE dcc_nas_control_audit_file
SET normalized_relative_path = ${sqlString(matched.relativePath)},
    file_name = ${sqlString(path.basename(matched.relativePath))},
    source_signature = ${sqlString(matched.sourceSignature)},
    expected_local_relative_path = ${sqlString(matched.expectedLocalRelativePath)},
    classification_status = 'MATCHED',
    download_status = 'NOT_SELECTED',
    archive_status = 'NOT_STARTED',
    archive_error_code = NULL,
    selected_import_task_id = NULL,
    selected_import_task_item_id = NULL,
    local_relative_path = NULL,
    local_write_error_code = NULL,
    local_write_error = NULL,
    controlled_file_id = NULL
WHERE task_id = ${config.auditTaskId}
  AND id = ${matched.auditFileId}
  AND deleted = b'0';

UPDATE dcc_nas_control_audit_file
SET normalized_relative_path = ${sqlString(pending.relativePath)},
    file_name = ${sqlString(path.basename(pending.relativePath))},
    source_signature = ${sqlString(pending.sourceSignature)},
    expected_local_relative_path = ${sqlString(pending.expectedLocalRelativePath)},
    classification_status = 'UNCLASSIFIED_PENDING',
    download_status = 'NOT_SELECTED',
    archive_status = 'PENDING_MANUAL_REVIEW',
    archive_error_code = NULL,
    selected_import_task_id = NULL,
    selected_import_task_item_id = NULL,
    local_relative_path = NULL,
    local_write_error_code = NULL,
    local_write_error = NULL,
    controlled_file_id = NULL
WHERE task_id = ${config.auditTaskId}
  AND id = ${pending.auditFileId}
  AND deleted = b'0';

UPDATE dcc_nas_control_audit_file
SET normalized_relative_path = ${sqlString(archiveBlock.relativePath)},
    file_name = ${sqlString(path.basename(archiveBlock.relativePath))},
    source_signature = ${sqlString(archiveBlock.sourceSignature)},
    expected_local_relative_path = ${sqlString(archiveBlock.expectedLocalRelativePath)},
    classification_status = 'MATCHED',
    download_status = 'LOCAL_WRITTEN',
    archive_status = 'FAILED',
    archive_error_code = 'ARCHIVE_METADATA_REQUIRED',
    selected_import_task_id = NULL,
    selected_import_task_item_id = NULL,
    local_relative_path = ${sqlString(archiveBlock.expectedLocalRelativePath)},
    local_write_error_code = NULL,
    local_write_error = NULL,
    controlled_file_id = NULL
WHERE task_id = ${config.auditTaskId}
  AND id = ${archiveBlock.auditFileId}
  AND deleted = b'0';
`
  runMysqlStrict(sql, 'sync task-owned audit fixture to existing NAS source files')
}

function resetTaskOwnedFixture() {
  const selectablePaths = sharedNasSourceFiles
    .filter((file) => file.selectForFullFlow)
    .map((file) => sqlString(file.relativePath))
    .join(',')
  const sql = `
DELETE item
FROM dcc_controlled_file_nas_transfer_task_item item
JOIN dcc_controlled_file_nas_transfer_task task ON task.id = item.task_id
WHERE task.audit_task_id = ${config.auditTaskId}
  AND task.source_type = 'NAS_UNCONTROLLED_IMPORT'
  AND task.deleted = b'0';

DELETE FROM dcc_controlled_file_nas_transfer_task
WHERE audit_task_id = ${config.auditTaskId}
  AND source_type = 'NAS_UNCONTROLLED_IMPORT'
  AND deleted = b'0';

UPDATE dcc_nas_control_audit_file
SET download_status = 'NOT_SELECTED',
    selected_import_task_id = NULL,
    selected_import_task_item_id = NULL,
    local_relative_path = NULL,
    local_write_error_code = NULL,
    local_write_error = NULL,
    controlled_file_id = NULL,
    archive_status = CASE
      WHEN classification_status = 'MATCHED' THEN 'NOT_STARTED'
      ELSE 'PENDING_MANUAL_REVIEW'
    END,
    archive_error_code = NULL,
    archive_error = NULL
WHERE task_id = ${config.auditTaskId}
  AND normalized_relative_path IN (${selectablePaths})
  AND deleted = b'0';
`
  runMysqlStrict(sql, 'reset task-owned uncontrolled import fixture')
}

function queryAuditRows() {
  const sql = `
SELECT id,
       normalized_relative_path,
       classification_status,
       expected_local_relative_path,
       download_status,
       archive_status,
       IFNULL(archive_error_code, ''),
       IFNULL(controlled_file_id, ''),
       IFNULL(local_relative_path, ''),
       IFNULL(selected_import_task_id, ''),
       IFNULL(selected_import_task_item_id, '')
FROM dcc_nas_control_audit_file
WHERE task_id = ${config.auditTaskId}
  AND deleted = b'0'
ORDER BY id;
`
  return runMysqlStrict(sql, 'query audit rows')
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => {
      const [
        id,
        normalizedRelativePath,
        classificationStatus,
        expectedLocalRelativePath,
        downloadStatus,
        archiveStatus,
        archiveErrorCode,
        controlledFileId,
        localRelativePath,
        selectedImportTaskId,
        selectedImportTaskItemId
      ] = line.split('\t')
      return {
        id: Number(id),
        normalizedRelativePath,
        classificationStatus,
        expectedLocalRelativePath,
        downloadStatus,
        archiveStatus,
        archiveErrorCode,
        controlledFileId,
        localRelativePath,
        selectedImportTaskId,
        selectedImportTaskItemId
      }
    })
}

function queryImportSummary() {
  const sql = `
SELECT CONCAT('TASK_COUNT:', COUNT(*))
FROM dcc_controlled_file_nas_transfer_task
WHERE audit_task_id = ${config.auditTaskId}
  AND source_type = 'NAS_UNCONTROLLED_IMPORT'
  AND deleted = b'0';
SELECT CONCAT('LOCAL_WRITTEN_ITEMS:', COUNT(*))
FROM dcc_controlled_file_nas_transfer_task_item item
JOIN dcc_controlled_file_nas_transfer_task task ON task.id = item.task_id
WHERE task.audit_task_id = ${config.auditTaskId}
  AND task.source_type = 'NAS_UNCONTROLLED_IMPORT'
  AND item.local_write_status = 'LOCAL_WRITTEN'
  AND item.deleted = b'0';
SELECT CONCAT('ACTIVE_SOURCE_COUNT:', COUNT(*))
FROM dcc_controlled_file_nas_source
WHERE source_type = 'NAS_UNCONTROLLED_IMPORT'
  AND normalized_relative_path IN (${sharedNasSourceFiles.map((file) => sqlString(file.relativePath)).join(',')})
  AND deleted = b'0';
`
  return Object.fromEntries(
    runMysqlStrict(sql, 'query import summary')
      .split(/\r?\n/)
      .filter(Boolean)
      .map((line) => {
        const [key, value] = line.split(':')
        return [key, Number(value)]
      })
  )
}

function assertFinalDatabaseState() {
  const rows = queryAuditRows()
  const byPath = new Map(rows.map((row) => [row.normalizedRelativePath, row]))
  const matched = byPath.get(sharedNasSourceFiles[0].relativePath)
  const pending = byPath.get(sharedNasSourceFiles[1].relativePath)
  const archiveBlock = byPath.get(sharedNasSourceFiles[2].relativePath)

  assert.ok(matched, 'matched audit row must exist')
  assert.equal(matched.downloadStatus, 'LOCAL_WRITTEN')
  assert.equal(matched.archiveStatus, 'FAILED')
  assert.equal(matched.archiveErrorCode, 'ARCHIVE_METADATA_REQUIRED')
  assert.equal(matched.controlledFileId, '')
  assert.equal(matched.localRelativePath, matched.expectedLocalRelativePath)
  assert.ok(matched.selectedImportTaskId, 'matched row must bind selected import task')
  assert.ok(matched.selectedImportTaskItemId, 'matched row must bind selected import task item')

  assert.ok(pending, 'pending audit row must exist')
  assert.equal(pending.classificationStatus, 'UNCLASSIFIED_PENDING')
  assert.equal(pending.downloadStatus, 'LOCAL_WRITTEN')
  assert.equal(pending.archiveStatus, 'PENDING_MANUAL_REVIEW')
  assert.equal(pending.archiveErrorCode, '')
  assert.equal(pending.controlledFileId, '')
  assert.equal(pending.localRelativePath, pending.expectedLocalRelativePath)
  assert.ok(pending.expectedLocalRelativePath.startsWith('_未分类待处理/'))
  assert.ok(pending.selectedImportTaskId, 'pending row must bind selected import task')
  assert.ok(pending.selectedImportTaskItemId, 'pending row must bind selected import task item')
  assert.equal(matched.selectedImportTaskId, pending.selectedImportTaskId, 'both selected rows should share one import task')

  assert.ok(archiveBlock, 'metadata blocker audit row must exist')
  assert.equal(archiveBlock.downloadStatus, 'LOCAL_WRITTEN')
  assert.equal(archiveBlock.archiveStatus, 'FAILED')
  assert.equal(archiveBlock.archiveErrorCode, 'ARCHIVE_METADATA_REQUIRED')

  const summary = queryImportSummary()
  assert.equal(summary.TASK_COUNT, 1, 'full E2E should create one task-owned uncontrolled import task')
  assert.equal(summary.LOCAL_WRITTEN_ITEMS, 2, 'full E2E should locally write the matched and pending rows')
  assert.equal(summary.ACTIVE_SOURCE_COUNT, 0, 'pending/metadata-blocked rows must not create ACTIVE NAS sources')

  return { rows, summary }
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(500)
}

async function login(page) {
  const targetPath = '/system/nas'
  await page.goto(`${config.frontendUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.frontendUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  }

  const usernameInput = form.locator('input.el-input__inner:not([role="combobox"]):visible').first()
  await usernameInput.fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(
    loginPayload && (loginPayload.code === 0 || loginPayload.code === 200),
    `login failed for ${config.tenant}/${config.username}: ${JSON.stringify(loginPayload)}`
  )
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function installDirectoryPickerHarness(page) {
  fs.rmSync(config.localDir, { recursive: true, force: true })
  fs.mkdirSync(config.localDir, { recursive: true })

  await page.exposeBinding('__dccNasUncontrolledImportCommitLocalFile', async (source, payload) => {
    const target = safeJoin(config.localDir, payload.relativePath)
    fs.mkdirSync(path.dirname(target), { recursive: true })
    fs.writeFileSync(target, Buffer.from(payload.contentBase64, 'base64'))
    return { relativePath: payload.relativePath, byteLength: fs.statSync(target).size }
  })

  await page.addInitScript(() => {
    window.__dccNasUncontrolledImportHarnessEvents = []
    const pushHarnessEvent = (event) => {
      window.__dccNasUncontrolledImportHarnessEvents.push({
        ...event,
        at: Date.now()
      })
    }
    const toBase64 = (buffer) => {
      const bytes = new Uint8Array(buffer)
      let binary = ''
      for (let index = 0; index < bytes.length; index += 1) {
        binary += String.fromCharCode(bytes[index])
      }
      return btoa(binary)
    }
    const normalizeSegment = (segment) => String(segment || '').replace(/[\\/]+/g, '')
    const createDirectoryHandle = (segments = []) => ({
      kind: 'directory',
      name: segments.length ? segments[segments.length - 1] : 'codex-local-root',
      async getDirectoryHandle(name, options = {}) {
        const safeName = normalizeSegment(name)
        pushHarnessEvent({ type: 'getDirectoryHandle', path: [...segments, safeName].join('/'), create: !!options.create })
        return createDirectoryHandle([...segments, safeName])
      },
      async getFileHandle(name, options = {}) {
        const safeName = normalizeSegment(name)
        const relativePath = [...segments, safeName].join('/')
        pushHarnessEvent({ type: 'getFileHandle', path: relativePath, create: !!options.create })
        return {
          kind: 'file',
          name: safeName,
          async createWritable() {
            pushHarnessEvent({ type: 'createWritable', path: relativePath })
            const chunks = []
            return {
              async write(chunk) {
                let buffer
                if (chunk instanceof Blob) {
                  buffer = await chunk.arrayBuffer()
                } else if (chunk instanceof ArrayBuffer) {
                  buffer = chunk
                } else if (ArrayBuffer.isView(chunk)) {
                  buffer = chunk.buffer
                } else if (typeof chunk === 'string') {
                  buffer = new TextEncoder().encode(chunk).buffer
                } else {
                  throw new Error('Unsupported write chunk type')
                }
                chunks.push(new Uint8Array(buffer))
                pushHarnessEvent({ type: 'write', path: relativePath, byteLength: buffer.byteLength })
              },
              async close() {
                const totalLength = chunks.reduce((sum, chunk) => sum + chunk.length, 0)
                const merged = new Uint8Array(totalLength)
                let offset = 0
                for (const chunk of chunks) {
                  merged.set(chunk, offset)
                  offset += chunk.length
                }
                await window.__dccNasUncontrolledImportCommitLocalFile({
                  relativePath,
                  contentBase64: toBase64(merged.buffer)
                })
                pushHarnessEvent({ type: 'close', path: relativePath, byteLength: totalLength })
              }
            }
          }
        }
      }
    })
    window.showDirectoryPicker = async () => {
      pushHarnessEvent({ type: 'showDirectoryPicker', path: '' })
      return createDirectoryHandle([])
    }
  })
}

async function runFullPageFlow() {
  const nasReadyLines = verifySharedNasSourceFiles()
  syncTaskOwnedFixtureToExistingNasFiles()
  resetTaskOwnedFixture()

  const browser = await chromium.launch({
    executablePath: config.browserExecutable,
    headless: true
  })
  const requestEvents = []
  try {
    const page = await browser.newPage()
    await installDirectoryPickerHarness(page)
    page.on('request', (request) => {
      const url = request.url()
      if (url.includes('/admin-api/dcc/controlled-files/nas-control-audit/') && url.includes('/import-selected')) {
        requestEvents.push({ type: 'import-selected', method: request.method(), url })
      }
      if (url.includes('/admin-api/dcc/controlled-files/nas-uncontrolled-import/') && url.includes('/content')) {
        requestEvents.push({ type: 'content', method: request.method(), url })
      }
      if (url.includes('/admin-api/dcc/controlled-files/nas-uncontrolled-import/') && url.includes('/local-write-result')) {
        requestEvents.push({ type: 'local-write-result', method: request.method(), url })
      }
    })

    await login(page)
    await page.evaluate((taskId) => {
      localStorage.setItem('int-ruoyi:nas-control-audit:last-task-id', String(taskId))
    }, config.auditTaskId)
    await page.goto(`${config.frontendUrl}/system/nas`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await settle(page)

    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '统计未受控文件' }).first()
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    await dialog.getByText('未受控文件下载与归类').waitFor({ state: 'visible', timeout: 30000 })
    await dialog.getByText('Design-Drawing-MATCHED.pdf').waitFor({ state: 'visible', timeout: 30000 })
    await dialog.getByText('Needs-Manual-Project.pdf').waitFor({ state: 'visible', timeout: 30000 })
    await dialog.getByText('_未分类待处理').waitFor({ state: 'visible', timeout: 30000 })

    for (const file of sharedNasSourceFiles.filter((item) => item.selectForFullFlow)) {
      const row = dialog
        .locator('.el-table__body-wrapper tbody tr')
        .filter({ hasText: file.relativePath })
        .first()
      await row.waitFor({ state: 'visible', timeout: 30000 })
      await row.locator('.el-checkbox__input').first().click()
    }

    const downloadButton = dialog.getByRole('button', { name: '下载选中文件到本地并归类' }).first()
    await assertEventually(async () => {
      assert.equal(await downloadButton.isEnabled(), true, 'download selected button must be enabled')
    }, 10000)
    await downloadButton.click()

    await assertEventually(() => {
      const state = assertFinalDatabaseState()
      assert.equal(state.summary.LOCAL_WRITTEN_ITEMS, 2)
    }, 60000)

    await loadAndAssertLocalFiles()

    const harnessEvents = await page.evaluate(() => window.__dccNasUncontrolledImportHarnessEvents || [])
    for (const requiredEvent of ['showDirectoryPicker', 'getDirectoryHandle', 'getFileHandle', 'createWritable', 'write', 'close']) {
      assert.ok(
        harnessEvents.some((event) => event.type === requiredEvent),
        `directory picker harness event missing: ${requiredEvent}`
      )
    }
    const eventTypes = requestEvents.map((event) => event.type)
    assert.equal(eventTypes.filter((type) => type === 'import-selected').length, 1, 'page must create exactly one import-selected task')
    assert.equal(eventTypes.filter((type) => type === 'content').length, 2, 'page must download exactly two selected source files')
    assert.equal(eventTypes.filter((type) => type === 'local-write-result').length, 2, 'page must report exactly two local-write results')
    assert.ok(
      eventTypes.indexOf('import-selected') < eventTypes.indexOf('content') &&
        eventTypes.indexOf('content') < eventTypes.indexOf('local-write-result'),
      'request order must be import-selected -> content -> local-write-result'
    )

    const finalState = assertFinalDatabaseState()
    return {
      nasReadyLines,
      localDir: config.localDir,
      harnessEvents,
      requestEvents,
      finalState
    }
  } finally {
    await browser.close()
  }
}

async function assertEventually(assertion, timeoutMs) {
  const startedAt = Date.now()
  let lastError
  while (Date.now() - startedAt < timeoutMs) {
    try {
      return await assertion()
    } catch (error) {
      lastError = error
      await new Promise((resolve) => setTimeout(resolve, 750))
    }
  }
  throw lastError
}

async function loadAndAssertLocalFiles() {
  for (const file of sharedNasSourceFiles.filter((item) => item.selectForFullFlow)) {
    const target = safeJoin(config.localDir, file.expectedLocalRelativePath)
    assert.ok(fs.existsSync(target), `local file missing: ${file.expectedLocalRelativePath}`)
    const content = fs.readFileSync(target)
    assert.ok(content.length > 0, `local file is empty: ${file.expectedLocalRelativePath}`)
    assert.equal(content.subarray(0, 4).toString('utf8'), '%PDF')
  }
}

async function main() {
  const blockers = []
  validateStaticConfig(blockers)
  const runtime = await validateRuntime(blockers)
  validateSchema(blockers)
  validateAuditSample(blockers)
  if (runtime.frontendOk) {
    await validateBrowserCapabilities(blockers)
  }

  if (blockers.length) {
    console.error('DCC NAS uncontrolled local import real E2E BLOCKED')
    for (const blocker of blockers) {
      console.error(`- ${blocker}`)
    }
    process.exit(1)
  }

  if (checkMode) {
    console.log('DCC_NAS_UNCONTROLLED_LOCAL_IMPORT_REAL_CHECK_PASS')
    return
  }

  const result = await runFullPageFlow()
  const summaryPath = path.join(config.localDir, 'dcc-nas-uncontrolled-local-import-full-summary.json')
  fs.writeFileSync(
    summaryPath,
    JSON.stringify(
      {
        localDir: result.localDir,
        nasReadyCount: result.nasReadyLines.length,
        requestEvents: result.requestEvents.map((event) => ({ type: event.type, method: event.method })),
        harnessEvents: result.harnessEvents.map((event) => ({
          type: event.type,
          path: event.path,
          byteLength: event.byteLength
        })),
        databaseSummary: result.finalState.summary
      },
      null,
      2
    ),
    'utf8'
  )
  console.log('DCC_NAS_UNCONTROLLED_LOCAL_IMPORT_REAL_E2E_PASS')
  console.log(`LOCAL_DIR=${result.localDir}`)
  console.log(`SUMMARY=${summaryPath}`)
}

main().catch((error) => {
  console.error(`DCC NAS uncontrolled local import real E2E failed: ${error.message}`)
  process.exit(1)
})
