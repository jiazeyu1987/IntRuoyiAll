param(
    [ValidateSet('Check', 'Real')]
    [string]$Mode = 'Real',
    [string]$ContainerName = 'int-ruoyi-mysql',
    [string]$FrontendRoot = 'E:\IntRuoyi\IntRuoyiFronted',
    [string]$BackendJar =
        'E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar',
    [string]$BrowserPath = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$expectedUsers = [ordered]@{
    '512' = 'huzonggang'
    '659' = 'shangmengying'
    '964' = 'liuyueyue'
    '1301' = 'sunxiaoqing'
    '1520' = 'lvyujie'
    '1618' = 'zhengxiaofang'
    '910272' = 'aoteman'
}
$targetIdList = ($expectedUsers.Keys | ForEach-Object { [string]$_ }) -join ','
$temporaryUpdater = 'rrm-acm04-local-e2e'
$rrmEnvironmentKeys = @(
    'PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH',
    'RRM_FRONTEND_URL',
    'RRM_BACKEND_URL',
    'RRM_TENANT',
    'RRM_LOCAL_BASELINE_TENANT_AUTHORIZATION',
    'RRM_DATA_PREFIX',
    'RRM_PRODUCTION_EMPLOYEE_USERNAME',
    'RRM_PRODUCTION_EMPLOYEE_PASSWORD',
    'RRM_PRODUCTION_LEADER_USERNAME',
    'RRM_PRODUCTION_LEADER_PASSWORD',
    'RRM_QA_USERNAME',
    'RRM_QA_PASSWORD',
    'RRM_PQC_INSPECTOR_USERNAME',
    'RRM_PQC_INSPECTOR_PASSWORD',
    'RRM_PQC_LEADER_USERNAME',
    'RRM_PQC_LEADER_PASSWORD',
    'RRM_RELEASE_OWNER_USERNAME',
    'RRM_RELEASE_OWNER_PASSWORD',
    'RRM_UNAUTHORIZED_USERNAME',
    'RRM_UNAUTHORIZED_PASSWORD',
    'RRM_SIGNATURE_IDS_JSON',
    'RRM_PRODUCTION_ORDER_ID',
    'RRM_PRODUCTION_ORDER_CODE',
    'RRM_ROUTE_ID',
    'RRM_ROUTE_VERSION_ID',
    'RRM_ROUTE_PROCESS_ID_1',
    'RRM_ROUTE_PROCESS_ID_2',
    'RRM_TRANSFER_IDS',
    'RRM_BATCH_RECORD_REPORT_ID',
    'RRM_QA_REGULATION_VERSION_ID'
)

function Invoke-LocalMysql([string]$Sql) {
    $output = @(
        $Sql |
            docker exec -i $ContainerName sh -lc `
                'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot -N -B ruoyi-vue-pro'
    )
    if ($LASTEXITCODE -ne 0) {
        throw "Local MySQL command failed with exit code $LASTEXITCODE."
    }
    return $output
}

function Assert-CommandAvailable([string]$CommandName) {
    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw "Required command is missing: $CommandName"
    }
    return $command.Source
}

function Assert-LocalRuntime {
    if (-not (Test-Path -LiteralPath $FrontendRoot -PathType Container)) {
        throw "Frontend root is missing: $FrontendRoot"
    }
    if (-not (Test-Path -LiteralPath $BackendJar -PathType Leaf)) {
        throw "Backend runtime jar is missing: $BackendJar"
    }
    if (-not (Test-Path -LiteralPath $BrowserPath -PathType Leaf)) {
        throw "Playwright browser is missing: $BrowserPath"
    }

    $containerState = docker inspect -f '{{.State.Running}}' $ContainerName
    if ($LASTEXITCODE -ne 0 -or ([string]$containerState).Trim() -ne 'true') {
        throw "Local MySQL container is not running: $ContainerName"
    }

    $frontendResponse = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8081/' -TimeoutSec 15
    if ($frontendResponse.StatusCode -ne 200) {
        throw "Local frontend health check failed: HTTP $($frontendResponse.StatusCode)"
    }
    $backendHealth = Invoke-RestMethod -Uri 'http://127.0.0.1:48081/actuator/health' -TimeoutSec 15
    if ($backendHealth.status -ne 'UP') {
        throw "Local backend health check failed: $($backendHealth.status)"
    }

    $conflictingProcesses = @(
        Get-CimInstance Win32_Process |
            Where-Object {
                $_.ProcessId -ne $PID -and
                $_.CommandLine -and
                (
                    $_.CommandLine.Contains('role-requirement-matrix-real-flow.e2e.js') -or
                    $_.CommandLine.Contains('e2e:role-requirement-matrix:real')
                )
            }
    )
    if ($conflictingProcesses.Count -gt 0) {
        $conflictIds = ($conflictingProcesses.ProcessId | Sort-Object) -join ','
        throw "Another RRM real E2E process is running: PID $conflictIds"
    }

    [void](Assert-CommandAvailable 'docker.exe')
    [void](Assert-CommandAvailable 'java.exe')
    [void](Assert-CommandAvailable 'pnpm.cmd')
}

function Assert-RrmDatabasePrerequisites {
    $sql = @"
SELECT CONCAT('P0_SCHEMA=', COUNT(*))
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
    (TABLE_NAME = 'mes_pro_process_pool_event'
      AND COLUMN_NAME IN ('event_idempotency_key', 'recordbook_entry_id'))
    OR (TABLE_NAME = 'mes_pro_process_pool_pqc_record'
      AND COLUMN_NAME = 'production_submit_event_id')
    OR (TABLE_NAME = 'mes_pro_process_pool_quantity_fragment'
      AND COLUMN_NAME = 'production_submit_event_id')
  );
SELECT CONCAT('PRIMARY_TASK=', COUNT(*))
FROM mes_pro_task
WHERE id = 981940
  AND code = 'RRM-20260805-PRIMARY-922985'
  AND work_order_id = 980008
  AND workstation_id = 980010
  AND route_id = 922119
  AND process_id = 922985
  AND item_id = 902149
  AND quantity = 10.000000
  AND status = 0
  AND tenant_id = 1
  AND deleted = b'0';
SELECT CONCAT('PQC_SELECTED_TASK=', COUNT(*))
FROM mes_pro_task
WHERE id = 981941
  AND code = 'RRM-20260805-PQC-922987'
  AND work_order_id = 980008
  AND workstation_id = 980009
  AND route_id = 922119
  AND process_id = 922987
  AND item_id = 902149
  AND quantity = 10.000000
  AND status = 0
  AND tenant_id = 1
  AND deleted = b'0';
SELECT CONCAT('PROCESS_SCOPE=', COUNT(*))
FROM mes_pro_process_pool_team_leader_scope
WHERE leader_user_id = 1520
  AND leader_type = 'PRODUCTION'
  AND scope_type = 'PROCESS'
  AND process_id = 922985
  AND enabled = b'1'
  AND tenant_id = 1
  AND deleted = b'0';
SELECT CONCAT('WORKSTATION_SCOPE=', COUNT(*))
FROM mes_pro_process_pool_team_leader_scope
WHERE leader_user_id = 1520
  AND leader_type = 'PRODUCTION'
  AND scope_type = 'WORKSTATION'
  AND workstation_id = 980010
  AND enabled = b'1'
  AND tenant_id = 1
  AND deleted = b'0';
SELECT CONCAT('PROCESS_DEVICE=', COUNT(*))
FROM mes_pro_process_pool_team_process_device
WHERE leader_user_id = 1520
  AND process_id = 922985
  AND device_id = 41
  AND enabled = b'1'
  AND tenant_id = 1
  AND deleted = b'0';
SELECT CONCAT('EMPLOYEE_BINDING=', COUNT(*))
FROM mes_pro_process_pool_team_employee_binding
WHERE leader_user_id = 1520
  AND process_id = 922985
  AND employee_profile_id = 980022
  AND employee_user_id = 964
  AND enabled = b'1'
  AND tenant_id = 1
  AND deleted = b'0';
SELECT CONCAT('PQC_REVIEW_SCOPE=', COUNT(*))
FROM mes_pro_process_pool_team_leader_scope
WHERE leader_user_id = 512
  AND leader_type = 'PQC'
  AND scope_type = 'EMPLOYEE'
  AND employee_user_id = 914524
  AND enabled = b'1'
  AND tenant_id = 1
  AND deleted = b'0';
"@
    $values = @{}
    foreach ($line in @(Invoke-LocalMysql $sql)) {
        if ($line -match '^(?<key>[A-Z0-9_]+)=(?<value>\d+)$') {
            $values[$Matches.key] = [int]$Matches.value
        }
    }
    foreach ($required in @(
        @{ Key = 'P0_SCHEMA'; Value = 4 },
        @{ Key = 'PRIMARY_TASK'; Value = 1 },
        @{ Key = 'PQC_SELECTED_TASK'; Value = 1 },
        @{ Key = 'PROCESS_SCOPE'; Value = 1 },
        @{ Key = 'WORKSTATION_SCOPE'; Value = 1 },
        @{ Key = 'PROCESS_DEVICE'; Value = 1 },
        @{ Key = 'EMPLOYEE_BINDING'; Value = 1 },
        @{ Key = 'PQC_REVIEW_SCOPE'; Value = 1 }
    )) {
        if (-not $values.ContainsKey($required.Key) -or $values[$required.Key] -ne $required.Value) {
            throw "RRM database prerequisite failed: $($required.Key) expected $($required.Value)."
        }
    }
}

function Read-RrmAccountSnapshot {
    $sql = @"
SELECT id,
       username,
       HEX(password),
       IF(updater IS NULL, '__NULL__', HEX(updater)),
       DATE_FORMAT(update_time, '%Y-%m-%d %H:%i:%s'),
       status,
       tenant_id,
       deleted + 0
FROM system_users
WHERE id IN ($targetIdList)
ORDER BY id;
"@
    $snapshot = [System.Collections.Generic.List[object]]::new()
    foreach ($line in @(Invoke-LocalMysql $sql)) {
        $parts = $line -split "`t", 8
        if ($parts.Count -ne 8) {
            throw 'Unexpected account snapshot row shape.'
        }
        $id = [long]$parts[0]
        $idKey = [string]$id
        if (-not $expectedUsers.Contains($idKey) -or $expectedUsers[$idKey] -ne $parts[1]) {
            throw "RRM account identity mismatch for user ID $id."
        }
        if ($parts[5] -ne '0' -or $parts[6] -ne '1' -or $parts[7] -ne '0') {
            throw "RRM account state is not enabled local tenant data for user ID $id."
        }
        if ([string]::IsNullOrWhiteSpace($parts[2])) {
            throw "RRM account password snapshot is empty for user ID $id."
        }
        $snapshot.Add([pscustomobject]@{
            Id = $id
            Username = $parts[1]
            PasswordHex = $parts[2]
            UpdaterHex = if ($parts[3] -eq '__NULL__') { $null } else { $parts[3] }
            UpdateTime = $parts[4]
        })
    }
    if ($snapshot.Count -ne $expectedUsers.Count) {
        throw "Expected 7 RRM accounts, captured $($snapshot.Count)."
    }
    return @($snapshot)
}

function New-RrmTemporaryPassword {
    [byte[]]$randomBytes = New-Object byte[] 12
    $randomNumberGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $randomNumberGenerator.GetBytes($randomBytes)
    }
    finally {
        $randomNumberGenerator.Dispose()
    }
    $randomHex = [System.BitConverter]::ToString($randomBytes) -replace '-', ''
    return 'Rrm' + $randomHex + '9'
}

function New-BcryptHash(
    [string]$TemporaryPassword,
    [string]$TemporaryWorkspace
) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $cryptoJar = Join-Path $TemporaryWorkspace 'spring-security-crypto-6.5.10.jar'
    $loggingJar = Join-Path $TemporaryWorkspace 'commons-logging-1.2.jar'
    $javaSourcePath = Join-Path $TemporaryWorkspace 'RrmBcryptHash.java'
    $archive = [System.IO.Compression.ZipFile]::OpenRead($BackendJar)
    try {
        $cryptoEntries = @(
            $archive.Entries |
                Where-Object { $_.FullName -match '^BOOT-INF/lib/spring-security-crypto-[^/]+\.jar$' }
        )
        $loggingEntries = @(
            $archive.Entries |
                Where-Object { $_.FullName -match '^BOOT-INF/lib/commons-logging-[^/]+\.jar$' }
        )
        if ($cryptoEntries.Count -ne 1 -or $loggingEntries.Count -ne 1) {
            throw 'Required Spring Security BCrypt runtime jars are missing from the backend jar.'
        }
        $cryptoEntry = $cryptoEntries[0]
        $loggingEntry = $loggingEntries[0]
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($cryptoEntry, $cryptoJar, $true)
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($loggingEntry, $loggingJar, $true)
    }
    finally {
        $archive.Dispose()
    }

    $javaSource = @'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class RrmBcryptHash {
    public static void main(String[] args) {
        String password = System.getenv("RRM_LOCAL_E2E_TEMP_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Missing temporary password");
        }
        System.out.println(new BCryptPasswordEncoder(10).encode(password));
    }
}
'@
    [System.IO.File]::WriteAllText(
        $javaSourcePath,
        $javaSource,
        [System.Text.UTF8Encoding]::new($false)
    )

    $previousTemporaryPassword = [Environment]::GetEnvironmentVariable(
        'RRM_LOCAL_E2E_TEMP_PASSWORD',
        'Process'
    )
    try {
        $env:RRM_LOCAL_E2E_TEMP_PASSWORD = $TemporaryPassword
        $javaPath = Assert-CommandAvailable 'java.exe'
        $javaOutput = @(
            & $javaPath --class-path "$cryptoJar;$loggingJar" $javaSourcePath
        )
        if ($LASTEXITCODE -ne 0) {
            throw "BCrypt helper failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        if ($null -eq $previousTemporaryPassword) {
            [Environment]::SetEnvironmentVariable('RRM_LOCAL_E2E_TEMP_PASSWORD', $null, 'Process')
        }
        else {
            $env:RRM_LOCAL_E2E_TEMP_PASSWORD = $previousTemporaryPassword
        }
    }
    $hashes = @($javaOutput | Where-Object { $_ -match '^\$2[aby]\$\d{2}\$.{53}$' })
    if ($hashes.Count -ne 1) {
        throw 'BCrypt helper did not return exactly one valid hash.'
    }
    return [string]$hashes[0]
}

function Get-RrmSnapshotExactConditions([object[]]$Snapshot) {
    return ($Snapshot | ForEach-Object {
        $updaterCondition = if ($null -eq $_.UpdaterHex) {
            'updater IS NULL'
        }
        else {
            "HEX(updater) = '$($_.UpdaterHex)'"
        }
        "(id = $($_.Id) " +
            "AND username = '$($_.Username)' " +
            "AND HEX(password) = '$($_.PasswordHex)' " +
            "AND $updaterCondition " +
            "AND DATE_FORMAT(update_time, '%Y-%m-%d %H:%i:%s') = '$($_.UpdateTime)')"
    }) -join "`nOR "
}

function Set-RrmTemporaryPassword(
    [object[]]$Snapshot,
    [string]$PasswordHash
) {
    $exactConditions = Get-RrmSnapshotExactConditions $Snapshot
    $escapedHash = $PasswordHash.Replace("'", "''")
    $sql = @"
START TRANSACTION;
SELECT id
FROM system_users
WHERE id IN ($targetIdList)
ORDER BY id
FOR UPDATE;
SET @rrm_before_count = (
  SELECT COUNT(*)
  FROM system_users
  WHERE $exactConditions
);
SET @rrm_assert_sql = IF(
  @rrm_before_count = 7,
  'SELECT 1',
  'SELECT 1 FROM __rrm_account_snapshot_mismatch'
);
PREPARE rrm_assert_stmt FROM @rrm_assert_sql;
EXECUTE rrm_assert_stmt;
DEALLOCATE PREPARE rrm_assert_stmt;
UPDATE system_users
SET password = '$escapedHash',
    updater = '$temporaryUpdater',
    update_time = NOW()
WHERE id IN ($targetIdList)
  AND tenant_id = 1
  AND deleted = b'0';
SET @rrm_updated_rows = ROW_COUNT();
SET @rrm_temp_count = (
  SELECT COUNT(*)
  FROM system_users
  WHERE id IN ($targetIdList)
    AND tenant_id = 1
    AND deleted = b'0'
    AND password = '$escapedHash'
);
SET @rrm_assert_sql = IF(
  @rrm_updated_rows = 7 AND @rrm_temp_count = 7,
  'SELECT 1',
  'SELECT 1 FROM __rrm_temp_update_count_mismatch'
);
PREPARE rrm_assert_stmt FROM @rrm_assert_sql;
EXECUTE rrm_assert_stmt;
DEALLOCATE PREPARE rrm_assert_stmt;
COMMIT;
SELECT CONCAT('RRM_TEMP_UPDATE_ROWS=', @rrm_updated_rows);
"@
    $result = @(Invoke-LocalMysql $sql)
    if (-not ($result -contains 'RRM_TEMP_UPDATE_ROWS=7')) {
        throw 'RRM temporary account update did not affect exactly seven rows.'
    }
    Write-Output 'RRM_TEMP_ACCOUNT_UPDATE=PASS'
}

function Restore-RrmAccounts([object[]]$Snapshot) {
    if ($null -eq $Snapshot -or $Snapshot.Count -ne 7) {
        throw 'Cannot restore RRM accounts without the complete seven-row snapshot.'
    }
    $passwordCases = ($Snapshot | ForEach-Object {
        "WHEN $($_.Id) THEN CONVERT(UNHEX('$($_.PasswordHex)') USING utf8mb4)"
    }) -join "`n"
    $updaterCases = ($Snapshot | ForEach-Object {
        if ($null -eq $_.UpdaterHex) {
            "WHEN $($_.Id) THEN NULL"
        }
        else {
            "WHEN $($_.Id) THEN CONVERT(UNHEX('$($_.UpdaterHex)') USING utf8mb4)"
        }
    }) -join "`n"
    $updateTimeCases = ($Snapshot | ForEach-Object {
        "WHEN $($_.Id) THEN '$($_.UpdateTime)'"
    }) -join "`n"
    $exactConditions = Get-RrmSnapshotExactConditions $Snapshot
    $sql = @"
START TRANSACTION;
SELECT id
FROM system_users
WHERE id IN ($targetIdList)
ORDER BY id
FOR UPDATE;
SET @rrm_row_count = (
  SELECT COUNT(*)
  FROM system_users
  WHERE id IN ($targetIdList)
);
SET @rrm_assert_sql = IF(
  @rrm_row_count = 7,
  'SELECT 1',
  'SELECT 1 FROM __rrm_restore_row_count_mismatch'
);
PREPARE rrm_assert_stmt FROM @rrm_assert_sql;
EXECUTE rrm_assert_stmt;
DEALLOCATE PREPARE rrm_assert_stmt;
UPDATE system_users
SET password = CASE id
$passwordCases
END,
updater = CASE id
$updaterCases
END,
update_time = CASE id
$updateTimeCases
END
WHERE id IN ($targetIdList);
SET @rrm_restore_changed_rows = ROW_COUNT();
SET @rrm_restore_exact_rows = (
  SELECT COUNT(*)
  FROM system_users
  WHERE $exactConditions
);
SET @rrm_assert_sql = IF(
  @rrm_restore_exact_rows = 7,
  'SELECT 1',
  'SELECT 1 FROM __rrm_restore_exact_mismatch'
);
PREPARE rrm_assert_stmt FROM @rrm_assert_sql;
EXECUTE rrm_assert_stmt;
DEALLOCATE PREPARE rrm_assert_stmt;
COMMIT;
SELECT CONCAT('RRM_ACCOUNT_RESTORE_CHANGED_ROWS=', @rrm_restore_changed_rows);
SELECT CONCAT('RRM_ACCOUNT_RESTORE_EXACT_ROWS=', @rrm_restore_exact_rows);
"@
    $result = @(Invoke-LocalMysql $sql)
    if (-not ($result -contains 'RRM_ACCOUNT_RESTORE_EXACT_ROWS=7')) {
        throw 'RRM account restoration did not reproduce all seven snapshots.'
    }
    Write-Output 'RRM_ACCOUNT_RESTORE=PASS'
}

function Resolve-UnoccupiedSignatureIds {
    $sql = @"
WITH RECURSIVE candidate_ids AS (
  SELECT 99009100 AS signature_id
  UNION ALL
  SELECT signature_id + 1
  FROM candidate_ids
  WHERE signature_id < 99009199
),
occupied_ids AS (
  SELECT signature_id FROM mes_pro_process_pool_event
    WHERE tenant_id = 1 AND deleted = b'0'
  UNION
  SELECT revision_signature_id FROM mes_pro_process_pool_event_revision
    WHERE tenant_id = 1 AND deleted = b'0'
  UNION
  SELECT signature_id FROM mes_pro_process_pool_pqc_record
    WHERE tenant_id = 1 AND deleted = b'0'
  UNION
  SELECT reviewer_signature_id FROM mes_pro_process_pool_review_copy
    WHERE tenant_id = 1 AND deleted = b'0'
  UNION
  SELECT review_signature_id FROM mes_pro_process_pool_submission_review
    WHERE tenant_id = 1 AND deleted = b'0' AND review_signature_id IS NOT NULL
  UNION
  SELECT id FROM mes_pro_batch_record_execution_signature
    WHERE tenant_id = 1 AND deleted = b'0'
  UNION
  SELECT id FROM mes_pro_edhr_batch_execution_signature
    WHERE tenant_id = 1 AND deleted = b'0'
  UNION
  SELECT id FROM bpm_approval_signature_record
    WHERE tenant_id = 1 AND deleted = b'0'
  UNION
  SELECT id FROM dcc_controlled_file_signature
    WHERE tenant_id = 1 AND deleted = b'0'
  UNION
  SELECT id FROM dcc_electronic_signature_image
    WHERE tenant_id = 1 AND deleted = b'0'
  UNION
  SELECT id FROM showroom_change_request_signature
    WHERE tenant_id = 1 AND deleted = b'0'
)
SELECT candidate_ids.signature_id
FROM candidate_ids
LEFT JOIN occupied_ids ON occupied_ids.signature_id = candidate_ids.signature_id
WHERE occupied_ids.signature_id IS NULL
ORDER BY candidate_ids.signature_id
LIMIT 12;
"@
    $candidateIds = @(
        Invoke-LocalMysql $sql |
            ForEach-Object { [long]([string]$_).Trim() }
    )
    if ($candidateIds.Count -ne 12) {
        throw "Expected 12 unoccupied task-owned signature IDs, found $($candidateIds.Count)."
    }
    return [ordered]@{
        productionEmployee = $candidateIds[0]
        productionExtra1 = $candidateIds[1]
        productionExtra2 = $candidateIds[2]
        productionExtra3 = $candidateIds[3]
        pqcInspector = $candidateIds[4]
        pqcExtra1 = $candidateIds[5]
        pqcExtra2 = $candidateIds[6]
        pqcExtra3 = $candidateIds[7]
        productionLeader = $candidateIds[8]
        pqcLeader = $candidateIds[9]
        qa = $candidateIds[10]
        releaseOwner = $candidateIds[11]
    }
}

function Save-RrmEnvironment {
    $snapshot = @{}
    foreach ($key in $rrmEnvironmentKeys) {
        $snapshot[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
    }
    return $snapshot
}

function Set-RrmEnvironment(
    [string]$TemporaryPassword,
    [System.Collections.IDictionary]$SignatureIds
) {
    $env:PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH = $BrowserPath
    $env:RRM_FRONTEND_URL = 'http://127.0.0.1:8081'
    $env:RRM_BACKEND_URL = 'http://127.0.0.1:48081'
    $env:RRM_TENANT = -join ([char[]](0x828B, 0x9053, 0x6E90, 0x7801))
    $env:RRM_LOCAL_BASELINE_TENANT_AUTHORIZATION = 'USER_APPROVED_YUDAO_SOURCE_20260802'
    $env:RRM_DATA_PREFIX = 'RRM-20260801-'
    $env:RRM_PRODUCTION_EMPLOYEE_USERNAME = 'liuyueyue'
    $env:RRM_PRODUCTION_EMPLOYEE_PASSWORD = $TemporaryPassword
    $env:RRM_PRODUCTION_LEADER_USERNAME = 'lvyujie'
    $env:RRM_PRODUCTION_LEADER_PASSWORD = $TemporaryPassword
    $env:RRM_QA_USERNAME = 'sunxiaoqing'
    $env:RRM_QA_PASSWORD = $TemporaryPassword
    $env:RRM_PQC_INSPECTOR_USERNAME = 'shangmengying'
    $env:RRM_PQC_INSPECTOR_PASSWORD = $TemporaryPassword
    $env:RRM_PQC_LEADER_USERNAME = 'huzonggang'
    $env:RRM_PQC_LEADER_PASSWORD = $TemporaryPassword
    $env:RRM_RELEASE_OWNER_USERNAME = 'zhengxiaofang'
    $env:RRM_RELEASE_OWNER_PASSWORD = $TemporaryPassword
    $env:RRM_UNAUTHORIZED_USERNAME = 'aoteman'
    $env:RRM_UNAUTHORIZED_PASSWORD = $TemporaryPassword
    $env:RRM_SIGNATURE_IDS_JSON = $SignatureIds | ConvertTo-Json -Compress
    $env:RRM_PRODUCTION_ORDER_ID = '980008'
    $env:RRM_PRODUCTION_ORDER_CODE = 'RRM-20260801-PP-MO-001'
    $env:RRM_ROUTE_ID = '922119'
    $env:RRM_ROUTE_VERSION_ID = '448'
    $env:RRM_ROUTE_PROCESS_ID_1 = '928609'
    $env:RRM_ROUTE_PROCESS_ID_2 = '928610'
    $env:RRM_TRANSFER_IDS = '1,2'
    $env:RRM_BATCH_RECORD_REPORT_ID = '1d05410f1d3140c5b8aa6786887ae69c'
    $env:RRM_QA_REGULATION_VERSION_ID = '6'
}

function Clear-RrmEnvironment([hashtable]$OriginalEnvironment) {
    foreach ($key in $rrmEnvironmentKeys) {
        $value = $OriginalEnvironment[$key]
        [Environment]::SetEnvironmentVariable($key, $value, 'Process')
    }
}

function Remove-TemporaryWorkspace([string]$TemporaryWorkspace) {
    if ([string]::IsNullOrWhiteSpace($TemporaryWorkspace) -or
        -not (Test-Path -LiteralPath $TemporaryWorkspace)) {
        return
    }
    $tempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).
        TrimEnd([System.IO.Path]::DirectorySeparatorChar) +
        [System.IO.Path]::DirectorySeparatorChar
    $resolvedWorkspace = [System.IO.Path]::GetFullPath($TemporaryWorkspace)
    if (-not $resolvedWorkspace.StartsWith(
        $tempRoot,
        [System.StringComparison]::OrdinalIgnoreCase
    )) {
        throw "Refusing to remove a non-temporary workspace: $resolvedWorkspace"
    }
    Remove-Item -LiteralPath $resolvedWorkspace -Recurse -Force
    if (Test-Path -LiteralPath $resolvedWorkspace) {
        throw "Temporary BCrypt workspace was not removed: $resolvedWorkspace"
    }
}

function Invoke-RrmPnpm([string]$ScriptName) {
    $pnpmPath = Assert-CommandAvailable 'pnpm.cmd'
    Push-Location $FrontendRoot
    $originalErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $pnpmPath $ScriptName 2>&1 | ForEach-Object { Write-Host $_ }
        $pnpmExitCode = $LASTEXITCODE
        return $pnpmExitCode
    }
    finally {
        $ErrorActionPreference = $originalErrorActionPreference
        Pop-Location
    }
}

$accountSnapshot = $null
$temporaryWorkspace = $null
$originalEnvironment = Save-RrmEnvironment
$resultExitCode = 1
$mutex = [System.Threading.Mutex]::new(
    $false,
    'Local\IntRuoyi-RRM-Real-E2E-20260805'
)
$mutexOwned = $false
$primaryError = $null

try {
    $mutexOwned = $mutex.WaitOne(0)
    if (-not $mutexOwned) {
        throw 'Another local RRM safety wrapper owns the execution mutex.'
    }

    Assert-LocalRuntime
    Assert-RrmDatabasePrerequisites
    $accountSnapshot = @(Read-RrmAccountSnapshot)
    $signatureIds = Resolve-UnoccupiedSignatureIds
    $temporaryPassword = New-RrmTemporaryPassword
    $temporaryWorkspace = Join-Path (
        [System.IO.Path]::GetTempPath()
    ) (
        'acm04-rrm-bcrypt-' + [guid]::NewGuid().ToString('N')
    )
    [void][System.IO.Directory]::CreateDirectory($temporaryWorkspace)
    $passwordHash = New-BcryptHash `
        -TemporaryPassword $temporaryPassword `
        -TemporaryWorkspace $temporaryWorkspace

    Set-RrmTemporaryPassword -Snapshot $accountSnapshot -PasswordHash $passwordHash
    Set-RrmEnvironment -TemporaryPassword $temporaryPassword -SignatureIds $signatureIds

    $checkExitCode = Invoke-RrmPnpm 'e2e:role-requirement-matrix:real:check'
    if ($checkExitCode -ne 0) {
        $resultExitCode = $checkExitCode
    }
    elseif ($Mode -eq 'Check') {
        $resultExitCode = 0
    }
    else {
        $resultExitCode = Invoke-RrmPnpm 'e2e:role-requirement-matrix:real'
    }
}
catch {
    $primaryError = $_
    $resultExitCode = 1
}
finally {
    $restoreError = $null
    try {
        if ($null -ne $accountSnapshot) {
            Restore-RrmAccounts -Snapshot $accountSnapshot
        }
    }
    catch {
        $restoreError = $_
        $resultExitCode = 1
    }
    finally {
        Clear-RrmEnvironment -OriginalEnvironment $originalEnvironment
        Remove-TemporaryWorkspace -TemporaryWorkspace $temporaryWorkspace
        if ($mutexOwned) {
            $mutex.ReleaseMutex()
        }
        $mutex.Dispose()
    }
    if ($null -ne $restoreError) {
        throw $restoreError
    }
}

if ($null -ne $primaryError) {
    throw $primaryError
}
exit $resultExitCode
