param(
    [string]$ContainerName = 'int-ruoyi-mysql',
    [string]$BinlogName = 'binlog.000128',
    [long]$StartPosition = 8815139,
    [long]$StopPosition = 8840000,
    [switch]$InspectOnly,
    [string]$MysqlBinlogPath =
        'C:\Program Files\MySQL\MySQL Workbench 8.0 CE\swb\shell\libexec\mysqlsh\mysqlbinlog.exe'
)

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
$tempPath = Join-Path $env:TEMP (
    'acm04-rrm-account-recovery-' + [guid]::NewGuid().ToString('N') + '.bin'
)
$recoveryMarker = 'acm04-rrm-binlog-recovery-20260805'

function Invoke-LocalMysql([string]$Sql) {
    $result = $Sql |
        docker exec -i $ContainerName sh -lc `
            'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot -N -B ruoyi-vue-pro'
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        throw "Local MySQL command failed with exit code $exitCode."
    }
    return @($result)
}

function Read-MySqlQuotedString([string]$Literal) {
    if ($Literal.Length -lt 2 -or -not $Literal.StartsWith("'") -or -not $Literal.EndsWith("'")) {
        throw 'Expected a quoted MySQL string literal in the binlog before-image.'
    }
    return $Literal.Substring(1, $Literal.Length - 2).
        Replace("''", "'").
        Replace("\'", "'").
        Replace('\\', '\')
}

function Add-WhereBeforeImage(
    [hashtable]$Columns,
    [System.Collections.Generic.Dictionary[long, object]]$RecoveredRows
) {
    if (-not $Columns.ContainsKey(1)) {
        return
    }
    $id = [long]$Columns[1]
    $expectedUserKey = [string]$id
    if (-not $expectedUsers.Contains($expectedUserKey) -or $RecoveredRows.ContainsKey($id)) {
        return
    }
    foreach ($requiredColumn in @(2, 3, 18, 19, 20, 21)) {
        if (-not $Columns.ContainsKey($requiredColumn)) {
            throw "Binlog before-image for user $id is missing column @$requiredColumn."
        }
    }
    $username = Read-MySqlQuotedString $Columns[2]
    if ($username -ne $expectedUsers[$expectedUserKey]) {
        throw "Binlog before-image username mismatch for user $id."
    }
    if ([string]$Columns[20] -notin @('0', "b'0'") -or [string]$Columns[21] -ne '1') {
        throw "Binlog before-image tenant/deleted guard mismatch for user $id."
    }
    if (-not ([string]$Columns[3]).StartsWith("'")) {
        throw "Binlog before-image password literal is invalid for user $id."
    }
    $RecoveredRows.Add($id, [pscustomobject]@{
        Id = $id
        PasswordLiteral = [string]$Columns[3]
        UpdaterLiteral = [string]$Columns[18]
        UpdateTimeLiteral = [string]$Columns[19]
    })
}

if (-not (Test-Path -LiteralPath $MysqlBinlogPath)) {
    throw "mysqlbinlog executable is missing: $MysqlBinlogPath"
}

$recoveredRows = [System.Collections.Generic.Dictionary[long, object]]::new()
try {
    docker cp "${ContainerName}:/var/lib/mysql/$BinlogName" $tempPath | Out-Null
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $tempPath)) {
        throw "Unable to copy $BinlogName from the local MySQL container."
    }

    $originalPath = $env:PATH
    try {
        $env:PATH = (
            'C:\Program Files\MySQL\MySQL Workbench 8.0 CE;' +
            'C:\Program Files\MySQL\MySQL Workbench 8.0 CE\swb\shell\lib\Python3.13\Lib\venv\scripts\nt;' +
            'C:\Program Files\MySQL\MySQL Server 9.5\bin;' +
            $originalPath
        )
        $decodedLines = @(
            & $MysqlBinlogPath `
                --base64-output=DECODE-ROWS `
                -vv `
                "--start-position=$StartPosition" `
                "--stop-position=$StopPosition" `
                $tempPath
        )
        if ($LASTEXITCODE -ne 0) {
            throw 'mysqlbinlog failed to decode the requested local position range.'
        }
    }
    finally {
        $env:PATH = $originalPath
    }

    if ($InspectOnly) {
        $currentPosition = 0L
        $inSystemUsersUpdate = $false
        $inWhere = $false
        $whereId = $null
        $targetIdsByPosition = @{}
        $positionContext = @{}
        $recentPositions = [System.Collections.Generic.Queue[long]]::new()
        foreach ($line in $decodedLines) {
            if ($line -match '^# at (?<position>\d+)$') {
                $currentPosition = [long]$Matches.position
                $recentPositions.Enqueue($currentPosition)
                while ($recentPositions.Count -gt 6) {
                    [void]$recentPositions.Dequeue()
                }
                $inSystemUsersUpdate = $false
                $inWhere = $false
                $whereId = $null
                continue
            }
            if ($line -match '^### UPDATE `ruoyi-vue-pro`\.`system_users`$') {
                $inSystemUsersUpdate = $true
                if (-not $positionContext.ContainsKey($currentPosition)) {
                    $positionContext[$currentPosition] = @($recentPositions.ToArray())
                }
                $inWhere = $false
                $whereId = $null
                continue
            }
            if (-not $inSystemUsersUpdate) {
                continue
            }
            if ($line -eq '### WHERE') {
                $inWhere = $true
                $whereId = $null
                continue
            }
            if ($line -eq '### SET') {
                if ($null -ne $whereId -and $expectedUsers.Contains([string]$whereId)) {
                    if (-not $targetIdsByPosition.ContainsKey($currentPosition)) {
                        $targetIdsByPosition[$currentPosition] =
                            [System.Collections.Generic.HashSet[long]]::new()
                    }
                    [void]$targetIdsByPosition[$currentPosition].Add([long]$whereId)
                }
                $inWhere = $false
                continue
            }
            if ($inWhere -and $line -match '^###\s+@1=(?<id>\d+)(?:\s+/\*.*)?$') {
                $whereId = [long]$Matches.id
            }
        }
        foreach ($position in @($targetIdsByPosition.Keys | Sort-Object)) {
            $ids = @($targetIdsByPosition[$position] | Sort-Object)
            Write-Output (
                "RRM_TARGET_USER_UPDATE_POSITION=$position;" +
                "TARGET_COUNT=$($ids.Count);" +
                "TARGET_IDS=$($ids -join ',');" +
                "EVENT_CONTEXT=$(@($positionContext[$position]) -join ',')"
            )
        }
        Write-Output "RRM_TARGET_USER_UPDATE_EVENT_COUNT=$($targetIdsByPosition.Count)"
        return
    }

    $inSystemUsersUpdate = $false
    $inWhere = $false
    $whereColumns = @{}
    foreach ($line in $decodedLines) {
        if ($line -match '^### UPDATE `ruoyi-vue-pro`\.`system_users`$') {
            $inSystemUsersUpdate = $true
            $inWhere = $false
            $whereColumns = @{}
            continue
        }
        if ($line -match '^### UPDATE ' -and $line -notmatch '`system_users`$') {
            $inSystemUsersUpdate = $false
            $inWhere = $false
            $whereColumns = @{}
            continue
        }
        if (-not $inSystemUsersUpdate) {
            continue
        }
        if ($line -eq '### WHERE') {
            $inWhere = $true
            $whereColumns = @{}
            continue
        }
        if ($line -eq '### SET') {
            if ($inWhere) {
                Add-WhereBeforeImage $whereColumns $recoveredRows
            }
            $inWhere = $false
            if ($recoveredRows.Count -eq $expectedUsers.Count) {
                break
            }
            continue
        }
        if ($inWhere -and $line -match '^###\s+@(?<column>\d+)=(?<value>.*?)(?:\s+/\*.*)?$') {
            $whereColumns[[int]$Matches.column] = $Matches.value
        }
    }

    if ($recoveredRows.Count -ne $expectedUsers.Count) {
        $decodedLines |
            Where-Object { $_ -match '^# at \d+$' -or $_ -match '^### UPDATE ' } |
            Select-Object -First 40 |
            ForEach-Object { Write-Output "RRM_BINLOG_STRUCTURE=$_" }
        throw "Expected 7 RRM account before-images, recovered $($recoveredRows.Count)."
    }

    $orderedRows = @($expectedUsers.Keys | ForEach-Object { $recoveredRows[[long]$_] })
    $passwordCases = ($orderedRows | ForEach-Object {
        "WHEN $($_.Id) THEN $($_.PasswordLiteral)"
    }) -join "`n"
    $updaterCases = ($orderedRows | ForEach-Object {
        "WHEN $($_.Id) THEN $($_.UpdaterLiteral)"
    }) -join "`n"
    $updateTimeCases = ($orderedRows | ForEach-Object {
        "WHEN $($_.Id) THEN $($_.UpdateTimeLiteral)"
    }) -join "`n"
    $exactConditions = ($orderedRows | ForEach-Object {
        "(id = $($_.Id) " +
            "AND password = $($_.PasswordLiteral) " +
            "AND updater <=> $($_.UpdaterLiteral) " +
            "AND update_time <=> $($_.UpdateTimeLiteral))"
    }) -join "`nOR "

    $restoreSql = @"
START TRANSACTION;
UPDATE system_users
SET updater = '$recoveryMarker',
    update_time = NOW()
WHERE id IN ($targetIdList)
  AND tenant_id = 1
  AND deleted = b'0';
SELECT ROW_COUNT();

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
WHERE id IN ($targetIdList)
  AND tenant_id = 1
  AND deleted = b'0'
  AND updater = '$recoveryMarker';
SELECT ROW_COUNT();

SELECT COUNT(*)
FROM system_users
WHERE tenant_id = 1
  AND deleted = b'0'
  AND (
$exactConditions
  );
COMMIT;
"@
    $restoreResult = @(Invoke-LocalMysql $restoreSql)
    if ($restoreResult.Count -lt 3) {
        throw 'Local MySQL recovery did not return all required row-count assertions.'
    }
    $markedRows = [int]([string]$restoreResult[-3]).Trim()
    $restoredRows = [int]([string]$restoreResult[-2]).Trim()
    $exactRows = [int]([string]$restoreResult[-1]).Trim()
    if ($markedRows -ne 7 -or $restoredRows -ne 7 -or $exactRows -ne 7) {
        throw "RRM account recovery assertions failed: marked=$markedRows restored=$restoredRows exact=$exactRows."
    }

    Write-Output 'RRM_RECOVERY_MARK_ROWS=7'
    Write-Output 'RRM_RESTORE_ROWS=7'
    Write-Output 'RRM_RESTORE_EXACT_ROWS=7'
}
finally {
    if (Test-Path -LiteralPath $tempPath) {
        [System.IO.File]::Delete($tempPath)
    }
    if (Test-Path -LiteralPath $tempPath) {
        throw "Credential-bearing temporary binlog copy was not deleted: $tempPath"
    }
    Write-Output 'RRM_BINLOG_TEMP_COPY_REMOVED=PASS'
}
