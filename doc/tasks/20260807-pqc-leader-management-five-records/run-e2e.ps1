param(
    [string]$ContainerName = 'int-ruoyi-mysql',
    [string]$FrontendRoot = 'E:\IntRuoyi\IntRuoyiFronted',
    [string]$BackendJar = 'E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar',
    [string]$BrowserPath = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$expectedUsers = [ordered]@{ '512' = 'huzonggang'; '659' = 'shangmengying'; '964' = 'liuyueyue' }
$targetIdList = ($expectedUsers.Keys | ForEach-Object { [string]$_ }) -join ','
$temporaryUpdater = 'CODX-PQC-20260807-CREDENTIAL'

function Invoke-LocalMysql([string]$Sql) {
    $output = @($Sql | docker exec -i $ContainerName sh -lc 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot -N -B ruoyi-vue-pro')
    if ($LASTEXITCODE -ne 0) { throw "Local MySQL failed with exit code $LASTEXITCODE" }
    return $output
}

function Read-AccountSnapshot {
    $sql = @"
SELECT id,username,HEX(password),IF(updater IS NULL,'__NULL__',HEX(updater)),DATE_FORMAT(update_time,'%Y-%m-%d %H:%i:%s'),status,tenant_id,deleted+0
FROM system_users WHERE id IN ($targetIdList) ORDER BY id;
"@
    $rows = @()
    foreach ($line in @(Invoke-LocalMysql $sql)) {
        $parts = $line -split "`t", 8
        if ($parts.Count -ne 8) { throw 'Unexpected account snapshot row shape' }
        $id = [string]$parts[0]
        if (-not $expectedUsers.Contains($id) -or $expectedUsers[$id] -ne $parts[1]) { throw "Account identity mismatch: $id" }
        if ($parts[5] -ne '0' -or $parts[6] -ne '1' -or $parts[7] -ne '0') { throw "Account is not enabled tenant-1 data: $id" }
        $rows += [pscustomobject]@{
            Id = [long]$parts[0]; Username = $parts[1]; PasswordHex = $parts[2]
            UpdaterHex = if ($parts[3] -eq '__NULL__') { $null } else { $parts[3] }
            UpdateTime = $parts[4]
        }
    }
    if ($rows.Count -ne 3) { throw "Expected 3 account rows, found $($rows.Count)" }
    return $rows
}

function New-TemporaryPassword {
    [byte[]]$bytes = New-Object byte[] 16
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return 'Pqc5' + ([BitConverter]::ToString($bytes) -replace '-', '') + '9'
}

function New-BcryptHash([string]$TemporaryPassword, [string]$TempDir) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $cryptoJar = Join-Path $TempDir 'spring-security-crypto.jar'
    $loggingJar = Join-Path $TempDir 'commons-logging.jar'
    $sourcePath = Join-Path $TempDir 'Pqc5BcryptHash.java'
    $archive = [System.IO.Compression.ZipFile]::OpenRead($BackendJar)
    try {
        $crypto = @($archive.Entries | Where-Object { $_.FullName -match '^BOOT-INF/lib/spring-security-crypto-[^/]+\.jar$' })
        $logging = @($archive.Entries | Where-Object { $_.FullName -match '^BOOT-INF/lib/commons-logging-[^/]+\.jar$' })
        if ($crypto.Count -ne 1 -or $logging.Count -ne 1) { throw 'BCrypt runtime jars are missing or ambiguous' }
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($crypto[0], $cryptoJar, $true)
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($logging[0], $loggingJar, $true)
    } finally { $archive.Dispose() }
    $source = @'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
class Pqc5BcryptHash {
  public static void main(String[] args) {
    String value = System.getenv("PQC5_HASH_INPUT");
    if (value == null || value.isBlank()) throw new IllegalStateException("missing hash input");
    System.out.println(new BCryptPasswordEncoder(10).encode(value));
  }
}
'@
    [IO.File]::WriteAllText($sourcePath, $source, [Text.UTF8Encoding]::new($false))
    $old = [Environment]::GetEnvironmentVariable('PQC5_HASH_INPUT', 'Process')
    try {
        $env:PQC5_HASH_INPUT = $TemporaryPassword
        $output = @(& java.exe --class-path "$cryptoJar;$loggingJar" $sourcePath)
        if ($LASTEXITCODE -ne 0) { throw 'BCrypt helper failed' }
    } finally { [Environment]::SetEnvironmentVariable('PQC5_HASH_INPUT', $old, 'Process') }
    $hash = @($output | Where-Object { $_ -match '^\$2[aby]\$\d{2}\$.{53}$' })
    if ($hash.Count -ne 1) { throw 'BCrypt helper returned an invalid result' }
    return [string]$hash[0]
}

function Get-ExactConditions([object[]]$Snapshot) {
    return ($Snapshot | ForEach-Object {
        $updater = if ($null -eq $_.UpdaterHex) { 'updater IS NULL' } else { "HEX(updater)='$($_.UpdaterHex)'" }
        "(id=$($_.Id) AND username='$($_.Username)' AND HEX(password)='$($_.PasswordHex)' AND $updater AND DATE_FORMAT(update_time,'%Y-%m-%d %H:%i:%s')='$($_.UpdateTime)')"
    }) -join ' OR '
}

function Set-TemporaryPassword([object[]]$Snapshot, [string]$Hash) {
    $conditions = Get-ExactConditions $Snapshot
    $sql = @"
START TRANSACTION;
SELECT id FROM system_users WHERE id IN ($targetIdList) ORDER BY id FOR UPDATE;
SET @before_count=(SELECT COUNT(*) FROM system_users WHERE $conditions);
SET @assert_sql=IF(@before_count=3,'SELECT 1','SELECT 1 FROM __pqc5_snapshot_mismatch');
PREPARE stmt FROM @assert_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE system_users SET password='$Hash',updater='$temporaryUpdater',update_time=NOW()
 WHERE id IN ($targetIdList) AND tenant_id=1 AND deleted=b'0';
SET @changed=ROW_COUNT();
SET @assert_sql=IF(@changed=3,'SELECT 1','SELECT 1 FROM __pqc5_update_count_mismatch');
PREPARE stmt FROM @assert_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
COMMIT;
SELECT CONCAT('PQC5_TEMP_ACCOUNT_ROWS=',@changed);
"@
    $result = @(Invoke-LocalMysql $sql)
    if (-not ($result -contains 'PQC5_TEMP_ACCOUNT_ROWS=3')) { throw 'Temporary credential update failed' }
}

function Restore-Accounts([object[]]$Snapshot) {
    $passwordCases = ($Snapshot | ForEach-Object { "WHEN $($_.Id) THEN CONVERT(UNHEX('$($_.PasswordHex)') USING utf8mb4)" }) -join "`n"
    $updaterCases = ($Snapshot | ForEach-Object {
        if ($null -eq $_.UpdaterHex) { "WHEN $($_.Id) THEN NULL" } else { "WHEN $($_.Id) THEN CONVERT(UNHEX('$($_.UpdaterHex)') USING utf8mb4)" }
    }) -join "`n"
    $timeCases = ($Snapshot | ForEach-Object { "WHEN $($_.Id) THEN '$($_.UpdateTime)'" }) -join "`n"
    $conditions = Get-ExactConditions $Snapshot
    $sql = @"
START TRANSACTION;
SELECT id FROM system_users WHERE id IN ($targetIdList) ORDER BY id FOR UPDATE;
UPDATE system_users SET password=CASE id $passwordCases END,updater=CASE id $updaterCases END,update_time=CASE id $timeCases END
 WHERE id IN ($targetIdList);
SET @exact=(SELECT COUNT(*) FROM system_users WHERE $conditions);
SET @assert_sql=IF(@exact=3,'SELECT 1','SELECT 1 FROM __pqc5_restore_mismatch');
PREPARE stmt FROM @assert_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
COMMIT;
SELECT CONCAT('PQC5_ACCOUNT_RESTORE_ROWS=',@exact);
"@
    $result = @(Invoke-LocalMysql $sql)
    if (-not ($result -contains 'PQC5_ACCOUNT_RESTORE_ROWS=3')) { throw 'Account restoration failed' }
}

$snapshot = $null
$tempDir = $null
$password = $null
$originalPasswordEnv = [Environment]::GetEnvironmentVariable('PQC5_TEMP_PASSWORD', 'Process')
$originalBrowserEnv = [Environment]::GetEnvironmentVariable('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH', 'Process')
$originalTaskIdsEnv = [Environment]::GetEnvironmentVariable('PQC5_TASK_IDS_JSON', 'Process')
$originalProductionTaskEnv = [Environment]::GetEnvironmentVariable('PQC5_PRODUCTION_TASK_ID', 'Process')
$originalRecordbookEnv = [Environment]::GetEnvironmentVariable('PQC5_RECORDBOOK_ID', 'Process')
try {
    foreach ($path in @($FrontendRoot, $BackendJar, $BrowserPath)) { if (-not (Test-Path -LiteralPath $path)) { throw "Required path is missing: $path" } }
    $conflicts = @(Get-CimInstance Win32_Process | Where-Object {
        $_.ProcessId -ne $PID -and $_.CommandLine -and
        ($_.CommandLine.Contains('role-requirement-matrix-real-flow.e2e.js') -or $_.CommandLine.Contains('run-e2e.cjs'))
    })
    if ($conflicts.Count -gt 0) { throw "Conflicting real E2E process is running: $($conflicts.ProcessId -join ',')" }
    $snapshot = Read-AccountSnapshot
    $password = New-TemporaryPassword
    $tempDir = Join-Path ([IO.Path]::GetTempPath()) ('pqc5-hash-' + [guid]::NewGuid().ToString('N'))
    [void](New-Item -ItemType Directory -Path $tempDir)
    $hash = New-BcryptHash $password $tempDir
    Set-TemporaryPassword $snapshot $hash
    $taskIds = @(Invoke-LocalMysql "SELECT id FROM mes_pqc_inspection_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807' ORDER BY round_no;")
    $productionTaskIds = @(Invoke-LocalMysql "SELECT id FROM mes_pro_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807';")
    $recordbookIds = @(Invoke-LocalMysql "SELECT id FROM mes_pro_edhr_recordbook WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807';")
    if ($taskIds.Count -ne 5 -or $productionTaskIds.Count -ne 1 -or $recordbookIds.Count -ne 1) { throw 'Task-owned fixture ids are incomplete' }
    $env:PQC5_TEMP_PASSWORD = $password
    $env:PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH = $BrowserPath
    $env:PQC5_TASK_IDS_JSON = '[' + (($taskIds | ForEach-Object { [string]$_ }) -join ',') + ']'
    $env:PQC5_PRODUCTION_TASK_ID = [string]$productionTaskIds[0]
    $env:PQC5_RECORDBOOK_ID = [string]$recordbookIds[0]
    Push-Location $FrontendRoot
    try {
        & node.exe 'E:\IntRuoyi\doc\tasks\20260807-pqc-leader-management-five-records\run-e2e.cjs'
        if ($LASTEXITCODE -ne 0) { throw "Playwright flow failed with exit code $LASTEXITCODE" }
    } finally { Pop-Location }
} finally {
    if ($null -ne $snapshot) { Restore-Accounts $snapshot }
    [Environment]::SetEnvironmentVariable('PQC5_TEMP_PASSWORD', $originalPasswordEnv, 'Process')
    [Environment]::SetEnvironmentVariable('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH', $originalBrowserEnv, 'Process')
    [Environment]::SetEnvironmentVariable('PQC5_TASK_IDS_JSON', $originalTaskIdsEnv, 'Process')
    [Environment]::SetEnvironmentVariable('PQC5_PRODUCTION_TASK_ID', $originalProductionTaskEnv, 'Process')
    [Environment]::SetEnvironmentVariable('PQC5_RECORDBOOK_ID', $originalRecordbookEnv, 'Process')
    if ($tempDir -and (Test-Path -LiteralPath $tempDir)) {
        $resolved = [IO.Path]::GetFullPath($tempDir)
        $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
        if (-not $resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) { throw "Refusing to remove non-temp path: $resolved" }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
