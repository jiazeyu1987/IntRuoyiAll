param(
    [ValidateSet('ApplySchema', 'Clean', 'Seed', 'Verify', 'Replay', 'ExerciseApiFlow', 'ReplayAndExercise', 'SyncSimulationDate')]
    [string]$Action = 'Verify',
    [ValidateSet('Minimal', 'Complete')]
    [string]$Scenario = 'Minimal',
    [string]$DbContainer = 'int-ruoyi-mysql',
    [string]$DbName = 'ruoyi-vue-pro',
    [string]$DbUser = 'root',
    [string]$DbPassword = '123456',
    [string]$DbHost = '127.0.0.1',
    [int]$DbPort = 23306,
    [string]$BaseUrl = 'http://127.0.0.1:48081/admin-api',
    [int]$TenantId = 1,
    [string]$AdminUsername = 'admin',
    [string]$AdminPassword = 'admin123',
    [string]$SimulationDate = '',
    [string]$PreviewStartTime = ''
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$schemaScript = Join-Path $repoRoot 'sql\mysql\mes-auto-schedule-first-loop.sql'
$script:UseDockerMySql = $false
$script:ScenarioConfig = $null

function Get-ScenarioConfig {
    switch ($Scenario) {
        'Complete' {
            return @{
                Name = 'Complete'
                CleanScript = 'sql\mysql\mes-auto-schedule-complete-demo-clean.sql'
                SeedScript = 'sql\mysql\mes-auto-schedule-complete-demo-data.sql'
                WorkOrderIds = @(900080, 900082)
                ExpectedGeneratedTaskCount = 4
                ExpectedFormalTaskCount = 4
                ExpectedDependencyCount = 2
            }
        }
        default {
            return @{
                Name = 'Minimal'
                CleanScript = 'sql\mysql\mes-auto-schedule-first-loop-demo-clean.sql'
                SeedScript = 'sql\mysql\mes-auto-schedule-first-loop-demo-data.sql'
                WorkOrderIds = @(900080)
                ExpectedGeneratedTaskCount = 1
                ExpectedFormalTaskCount = 1
                ExpectedDependencyCount = 0
            }
        }
    }
}

$script:ScenarioConfig = Get-ScenarioConfig
$cleanScript = Join-Path $repoRoot $script:ScenarioConfig.CleanScript
$seedScript = Join-Path $repoRoot $script:ScenarioConfig.SeedScript

function Assert-Exists {
    param(
        [string]$PathValue,
        [string]$Description
    )

    if (-not (Test-Path $PathValue)) {
        throw "$Description not found: $PathValue"
    }
}

function Assert-DockerContainer {
    param([string]$ContainerName)

    $container = docker ps --filter "name=$ContainerName" --format "{{.Names}}"
    if ($LASTEXITCODE -ne 0) {
        throw 'docker ps failed'
    }
    if (-not ($container -split "`r?`n" | Where-Object { $_ -eq $ContainerName })) {
        throw "Docker container is not running: $ContainerName"
    }
}

function Test-TcpPortOpen {
    param(
        [string]$HostName,
        [int]$Port
    )

    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $iar = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $iar.AsyncWaitHandle.WaitOne(2000, $false)) {
            $client.Close()
            return $false
        }
        $client.EndConnect($iar)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

function Initialize-DatabaseAccess {
    $dockerAvailable = $false
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        try {
            docker ps --format "{{.Names}}" *> $null
            if ($LASTEXITCODE -eq 0) {
                $dockerAvailable = $true
            }
        } catch {
            $dockerAvailable = $false
        }
    }

    if ($dockerAvailable) {
        try {
            Assert-DockerContainer $DbContainer
            $script:UseDockerMySql = $true
            return
        } catch {
            $script:UseDockerMySql = $false
        }
    }

    if (-not (Test-TcpPortOpen -HostName $DbHost -Port $DbPort)) {
        throw "Neither Docker container [$DbContainer] nor TCP MySQL endpoint [${DbHost}:$DbPort] is available."
    }
}

function Invoke-MySqlViaPython {
    param(
        [string]$ScriptPath,
        [string]$Query
    )

    if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
        throw 'python is required for direct TCP MySQL access'
    }

    $env:CODEX_DB_HOST = $DbHost
    $env:CODEX_DB_PORT = [string]$DbPort
    $env:CODEX_DB_NAME = $DbName
    $env:CODEX_DB_USER = $DbUser
    $env:CODEX_DB_PASSWORD = $DbPassword
    $env:CODEX_DB_SCRIPT_PATH = $ScriptPath
    $env:CODEX_DB_QUERY = $Query

    $pythonScript = @'
import os
import pathlib
import sys

import pymysql


def split_sql(text: str) -> list[str]:
    return [statement.strip() for statement in text.split(";") if statement.strip()]


conn = pymysql.connect(
    host=os.environ["CODEX_DB_HOST"],
    port=int(os.environ["CODEX_DB_PORT"]),
    user=os.environ["CODEX_DB_USER"],
    password=os.environ["CODEX_DB_PASSWORD"],
    database=os.environ["CODEX_DB_NAME"],
    charset="utf8mb4",
    autocommit=True,
)
try:
    with conn.cursor() as cur:
        script_path = os.environ.get("CODEX_DB_SCRIPT_PATH", "")
        query = os.environ.get("CODEX_DB_QUERY", "")
        if script_path:
            text = pathlib.Path(script_path).read_text(encoding="utf-8")
            for statement in split_sql(text):
                cur.execute(statement)
        elif query:
            cur.execute(query)
            rows = cur.fetchall()
            for row in rows:
                print("\t".join("" if value is None else str(value) for value in row))
        else:
            raise RuntimeError("no_sql_input")
finally:
    conn.close()
'@

    $output = $pythonScript | python -
    if ($LASTEXITCODE -ne 0) {
        throw 'Direct TCP MySQL execution failed'
    }
    return $output
}

function Invoke-MySqlScriptFile {
    param(
        [string]$ScriptPath,
        [switch]$Force
    )

    Assert-Exists $ScriptPath 'SQL script'
    if (-not $script:UseDockerMySql) {
        return Invoke-MySqlViaPython -ScriptPath $ScriptPath
    }
    $arguments = @('exec', '-i')
    if ($DbPassword) {
        $arguments += @('-e', "MYSQL_PWD=$DbPassword")
    }
    $arguments += $DbContainer
    $arguments += 'mysql'
    if ($Force) {
        $arguments += '--force'
    }
    $arguments += @("-u$DbUser", $DbName)

    Get-Content $ScriptPath -Raw | & docker @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to execute SQL script: $ScriptPath"
    }
}

function Invoke-MySqlQuery {
    param([string]$Query)

    if (-not $script:UseDockerMySql) {
        return Invoke-MySqlViaPython -Query $Query
    }

    $arguments = @('exec', '-i')
    if ($DbPassword) {
        $arguments += @('-e', "MYSQL_PWD=$DbPassword")
    }
    $arguments += $DbContainer
    $arguments += 'mysql'
    $arguments += @('-N', "-u$DbUser", $DbName, '-e', $Query)

    $output = & docker @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to execute query: $Query"
    }
    return $output
}

function Sync-SimulationDateIfRequested {
    if ([string]::IsNullOrWhiteSpace($SimulationDate)) {
        return
    }
    if ($SimulationDate -notmatch '^\d{4}-\d{2}-\d{2}$') {
        throw "SimulationDate must use yyyy-MM-dd: $SimulationDate"
    }

    Invoke-MySqlQuery @"
INSERT INTO mes_pro_schedule_calendar_simulation
    (tenant_id, simulation_date, remark, creator, updater, deleted)
VALUES
    ($TenantId, '${SimulationDate} 00:00:00', 'DEMO-SYNC', 'codex', 'codex', b'0')
ON DUPLICATE KEY UPDATE
    simulation_date = VALUES(simulation_date),
    remark = VALUES(remark),
    updater = 'codex',
    update_time = CURRENT_TIMESTAMP,
    deleted = b'0';
"@ | Out-Null
}

function Show-VerifySummary {
    $workOrderIdCsv = ($script:ScenarioConfig.WorkOrderIds -join ',')
    $taskCount = (Invoke-MySqlQuery "SELECT COUNT(*) FROM mes_pro_task WHERE work_order_id IN ($workOrderIdCsv);").Trim()
    $dependencyCount = (Invoke-MySqlQuery @"
SELECT COUNT(*)
FROM mes_pro_task_dependency
WHERE source_task_id IN (SELECT id FROM mes_pro_task WHERE work_order_id IN ($workOrderIdCsv))
   OR target_task_id IN (SELECT id FROM mes_pro_task WHERE work_order_id IN ($workOrderIdCsv));
"@).Trim()

    Write-Host "Scenario: $($script:ScenarioConfig.Name)"
    foreach ($workOrderId in $script:ScenarioConfig.WorkOrderIds) {
        $workOrderLine = Invoke-MySqlQuery "SELECT CONCAT(id, ' | ', code, ' | ', status, ' | ', type, ' | tenant=', tenant_id, ' | qtyScheduled=', COALESCE(quantity_scheduled, 0)) FROM mes_pro_work_order WHERE id = $workOrderId;"
        $taskLines = Invoke-MySqlQuery "SELECT CONCAT(id, ' | ', code, ' | ', start_time, ' | ', end_time, ' | ', remark) FROM mes_pro_task WHERE work_order_id = $workOrderId ORDER BY id;"
        Write-Host "Demo work order [$workOrderId]:"
        if ($workOrderLine) {
            $workOrderLine | ForEach-Object { Write-Host "  $_" }
        } else {
            Write-Host '  <missing>'
        }

        if ($taskLines) {
            Write-Host '  Formal tasks:'
            $taskLines | ForEach-Object { Write-Host "    $_" }
        } else {
            Write-Host '  Formal tasks: <none>'
        }
    }

    Write-Host "Formal task count: $taskCount"
    Write-Host "Dependency count: $dependencyCount"
}

function Resolve-PreviewStartTime {
    if (-not [string]::IsNullOrWhiteSpace($PreviewStartTime)) {
        return $PreviewStartTime
    }

    $simDate = (Invoke-MySqlQuery @"
SELECT COALESCE(
    (
        SELECT DATE_FORMAT(simulation_date, '%Y-%m-%d')
        FROM mes_pro_schedule_calendar_simulation
        WHERE deleted = b'0' AND tenant_id = $TenantId
        ORDER BY id DESC
        LIMIT 1
    ),
    DATE_FORMAT(CURDATE(), '%Y-%m-%d')
);
"@).Trim()
    if (-not $simDate) {
        throw 'Unable to resolve simulation date for preview start time.'
    }
    return "$simDate 08:00:00"
}

function Invoke-ApiJson {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers,
        [object]$Body = $null
    )

    $params = @{
        Method = $Method
        Uri = $Uri
        Headers = $Headers
        ContentType = 'application/json'
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
    }

    return Invoke-RestMethod @params
}

function Get-AccessToken {
    $headers = @{
        'tenant-id' = "$TenantId"
    }
    $body = @{
        username = $AdminUsername
        password = $AdminPassword
    }
    $response = Invoke-ApiJson -Method 'Post' -Uri "$BaseUrl/system/auth/login" -Headers $headers -Body $body
    if ($response.code -ne 0 -or -not $response.data.accessToken) {
        throw "Login failed: $($response | ConvertTo-Json -Depth 10 -Compress)"
    }
    return $response.data.accessToken
}

function Invoke-AutoScheduleApiFlow {
    $token = Get-AccessToken
    $headers = @{
        'tenant-id' = "$TenantId"
        'Authorization' = "Bearer $token"
    }
    $resolvedPreviewStartTime = Resolve-PreviewStartTime
    $payload = @{
        workOrderIds = @($script:ScenarioConfig.WorkOrderIds)
        startTime = $resolvedPreviewStartTime
        capacityMode = 'PLANNED'
        preserveManualLockedTasks = $true
    }

    $preview = Invoke-ApiJson -Method 'Post' -Uri "$BaseUrl/mes/pro/auto-schedule/preview" -Headers $headers -Body $payload
    if ($preview.code -ne 0) {
        throw "Preview failed: $($preview | ConvertTo-Json -Depth 10 -Compress)"
    }
    if (-not $preview.data.previewOnly) {
        throw 'Preview response did not mark previewOnly=true'
    }
    if ($preview.data.summary.blockingIssueCount -ne 0) {
        throw "Preview returned blocking issues: $($preview.data.summary.blockingIssueCount)"
    }
    if ($preview.data.summary.generatedTaskCount -lt $script:ScenarioConfig.ExpectedGeneratedTaskCount) {
        throw "Preview generated task count was too small: $($preview.data.summary.generatedTaskCount)"
    }
    if (@($preview.data.links).Count -lt $script:ScenarioConfig.ExpectedDependencyCount) {
        throw "Preview dependency count was too small: $(@($preview.data.links).Count)"
    }
    $calendarContextToken = [string]$preview.data.calendarContextToken
    if ([string]::IsNullOrWhiteSpace($calendarContextToken)) {
        throw 'Preview response did not include calendarContextToken'
    }

    $applyPayload = @{
        workOrderIds = @($script:ScenarioConfig.WorkOrderIds)
        startTime = $resolvedPreviewStartTime
        capacityMode = 'PLANNED'
        preserveManualLockedTasks = $true
        calendarContextToken = $calendarContextToken
    }

    $apply = Invoke-ApiJson -Method 'Post' -Uri "$BaseUrl/mes/pro/auto-schedule/apply" -Headers $headers -Body $applyPayload
    if ($apply.code -ne 0) {
        throw "Apply failed: $($apply | ConvertTo-Json -Depth 10 -Compress)"
    }
    if (-not $apply.data.applied) {
        throw 'Apply response did not mark applied=true'
    }
    if (@($apply.data.createdTaskIds).Count -lt $script:ScenarioConfig.ExpectedGeneratedTaskCount) {
        throw "Apply created task count was too small: $(@($apply.data.createdTaskIds).Count)"
    }

    $queryPairs = @()
    foreach ($workOrderId in $script:ScenarioConfig.WorkOrderIds) {
        $queryPairs += "workOrderIds=$workOrderId"
    }
    $dependencyResp = Invoke-ApiJson -Method 'Get' -Uri ("$BaseUrl/mes/pro/auto-schedule/dependencies?" + ($queryPairs -join '&')) -Headers $headers
    if ($dependencyResp.code -ne 0) {
        throw "Dependency query failed: $($dependencyResp | ConvertTo-Json -Depth 10 -Compress)"
    }
    if (@($dependencyResp.data).Count -lt $script:ScenarioConfig.ExpectedDependencyCount) {
        throw "Dependency query count was too small: $(@($dependencyResp.data).Count)"
    }

    Write-Host 'Preview/apply API flow passed.'
}

Assert-Exists $schemaScript 'Schema SQL'
Assert-Exists $cleanScript 'Demo cleanup SQL'
Assert-Exists $seedScript 'Demo seed SQL'
Initialize-DatabaseAccess

switch ($Action) {
    'ApplySchema' {
        Invoke-MySqlScriptFile -ScriptPath $schemaScript -Force
        Write-Host 'Schema applied.'
    }
    'Clean' {
        Invoke-MySqlScriptFile -ScriptPath $cleanScript
        Write-Host 'Demo data cleaned.'
    }
    'Seed' {
        Sync-SimulationDateIfRequested
        Invoke-MySqlScriptFile -ScriptPath $seedScript
        Write-Host 'Demo data seeded.'
    }
    'Verify' {
        Show-VerifySummary
    }
    'SyncSimulationDate' {
        if ([string]::IsNullOrWhiteSpace($SimulationDate)) {
            throw 'SimulationDate is required when Action=SyncSimulationDate'
        }
        Sync-SimulationDateIfRequested
        Write-Host 'Simulation date synced.'
    }
    'Replay' {
        Invoke-MySqlScriptFile -ScriptPath $schemaScript -Force
        Sync-SimulationDateIfRequested
        Invoke-MySqlScriptFile -ScriptPath $cleanScript
        Invoke-MySqlScriptFile -ScriptPath $seedScript
        Show-VerifySummary
    }
    'ExerciseApiFlow' {
        Sync-SimulationDateIfRequested
        Invoke-AutoScheduleApiFlow
        Show-VerifySummary
    }
    'ReplayAndExercise' {
        Invoke-MySqlScriptFile -ScriptPath $schemaScript -Force
        Sync-SimulationDateIfRequested
        Invoke-MySqlScriptFile -ScriptPath $cleanScript
        Invoke-MySqlScriptFile -ScriptPath $seedScript
        Invoke-AutoScheduleApiFlow
        Show-VerifySummary
    }
}
