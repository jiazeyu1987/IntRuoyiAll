Set-StrictMode -Version Latest

$script:BackupOpsUtf8NoBom = [System.Text.UTF8Encoding]::new($false)
$script:BackupMySqlDumpArguments = @(
    '--single-transaction',
    '--routines',
    '--triggers',
    '--events',
    '--hex-blob',
    '--default-character-set=utf8mb4'
)

function Import-BackupOpsSshDependency {
    if (Get-Command -Name 'Invoke-BackupSshCommand' -ErrorAction SilentlyContinue) {
        return
    }

    $sshModulePath = Join-Path $PSScriptRoot 'SshOps.psm1'
    if (-not (Test-Path -LiteralPath $sshModulePath)) {
        throw "SSH module not found: $sshModulePath"
    }

    Import-Module $sshModulePath -Force -DisableNameChecking -ErrorAction Stop | Out-Null
}

function Merge-BackupOpsRequest {
    param(
        [Parameter(Mandatory)]
        [hashtable]$Request,
        [Parameter(Mandatory)]
        [hashtable]$Extra
    )

    $merged = @{}
    foreach ($key in $Request.Keys) {
        $merged[$key] = $Request[$key]
    }
    foreach ($key in $Extra.Keys) {
        $merged[$key] = $Extra[$key]
    }

    return $merged
}

function New-BackupOpsOperatorBlockedMessage {
    param(
        [Parameter(Mandatory)]
        [string]$Reason,
        [Parameter(Mandatory)]
        [string]$Action
    )

    return "原因：$Reason`n建议动作：$Action"
}

function New-BackupOpsMySqlException {
    param(
        [Parameter(Mandatory)]
        [ValidateSet('INTBK-3001', 'INTBK-3002', 'INTBK-3003', 'INTBK-7001')]
        [string]$Code,
        [Parameter(Mandatory)]
        [string]$Message,
        [ValidateSet('blocked', 'fail')]
        [string]$Status = 'fail'
    )

    $exception = [System.InvalidOperationException]::new($Message)
    $exception.Data['BackupOpsCode'] = $Code
    $exception.Data['BackupOpsStatus'] = $Status
    return $exception
}

function Get-BackupMySqlFieldValue {
    param(
        [Parameter(Mandatory)]
        [hashtable]$Request,
        [Parameter(Mandatory)]
        [string]$Name,
        [ValidateSet('INTBK-3001', 'INTBK-3002', 'INTBK-3003', 'INTBK-7001')]
        [string]$Code = 'INTBK-3001'
    )

    if (-not $Request.ContainsKey($Name)) {
        throw (New-BackupOpsMySqlException -Code $Code -Status 'blocked' -Message "Missing MySQL request field '$Name'.")
    }

    $value = $Request[$Name]
    if ($null -eq $value -or [string]::IsNullOrWhiteSpace([string]$value)) {
        throw (New-BackupOpsMySqlException -Code $Code -Status 'blocked' -Message "MySQL request field '$Name' cannot be empty.")
    }

    return [string]$value
}

function Get-BackupOpsConfigValueSafe {
    param(
        [Parameter(Mandatory)]
        [object]$InputObject,
        [Parameter(Mandatory)]
        [string[]]$Path
    )

    $current = $InputObject
    foreach ($segment in $Path) {
        if ($null -eq $current) {
            return $null
        }

        if ($current -is [System.Collections.IDictionary]) {
            if (-not $current.Contains($segment)) {
                return $null
            }
            $current = $current[$segment]
            continue
        }

        $property = $current.PSObject.Properties[$segment]
        if ($null -eq $property) {
            return $null
        }
        $current = $property.Value
    }

    return $current
}

function Get-BackupOpsRequiredConfigValue {
    param(
        [Parameter(Mandatory)]
        [object]$Config,
        [Parameter(Mandatory)]
        [string[]]$Path,
        [ValidateSet('INTBK-3001', 'INTBK-3002', 'INTBK-3003', 'INTBK-7001')]
        [string]$Code,
        [Parameter(Mandatory)]
        [string]$Reason,
        [Parameter(Mandatory)]
        [string]$Action
    )

    $value = Get-BackupOpsConfigValueSafe -InputObject $Config -Path $Path
    if ($null -eq $value -or ($value -is [string] -and [string]::IsNullOrWhiteSpace($value))) {
        throw (New-BackupOpsMySqlException -Code $Code -Status 'blocked' -Message (New-BackupOpsOperatorBlockedMessage -Reason $Reason -Action $Action))
    }

    return $value
}

function ConvertTo-BackupBashSingleQuotedString {
    param(
        [Parameter(Mandatory)]
        [string]$Value
    )

    return "'" + ($Value -replace "'", "'""'""'") + "'"
}

function Get-BackupOpsRootPath {
    return (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
}

function Get-BackupOpsRestoreStageRoot {
    param(
        [Parameter(Mandatory)]
        [string]$BackupId
    )

    $root = Get-BackupOpsRootPath
    return Join-Path $root ("tmp\restore-data\" + $BackupId + "\mysql")
}

function Get-BackupOpsDotEnvValue {
    param(
        [Parameter(Mandatory)]
        [string[]]$Lines,
        [Parameter(Mandatory)]
        [string]$Key
    )

    foreach ($line in $Lines) {
        if ($line -like "$Key=*") {
            return $line.Substring($Key.Length + 1)
        }
    }

    return ''
}

function Get-BackupOpsProductionSshRequest {
    param(
        [Parameter(Mandatory)]
        [object]$Config
    )

    return @{
        Host = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('servers', 'production', 'host') -Code 'INTBK-3001' -Reason '缺少正式服务器地址配置。' -Action '请在 runtime 配置中补齐 servers.production.host 后再执行 MySQL 备份或恢复。')
        User = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('ssh', 'user') -Code 'INTBK-3001' -Reason '缺少 SSH 操作用户配置。' -Action '请在 secrets 描述文件中补齐 ssh.user 后再重试。')
        KeyPath = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('auth', 'sshKeyPath') -Code 'INTBK-3001' -Reason '缺少 SSH 私钥路径配置。' -Action '请在操作机 secrets 文件中配置 auth.sshKeyPath，并确保私钥文件存在。')
        Port = if ($Config.ssh.PSObject.Properties['port']) { [int]$Config.ssh.port } else { 22 }
        KnownHostsPath = if ($Config.auth.PSObject.Properties['knownHostsPath']) { [string]$Config.auth.knownHostsPath } else { '' }
    }
}

function Get-BackupOpsTestSshRequest {
    param(
        [Parameter(Mandatory)]
        [object]$Config
    )

    $request = Get-BackupOpsProductionSshRequest -Config $Config
    $request['Host'] = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('servers', 'test', 'host') -Code 'INTBK-3002' -Reason '缺少测试服务器地址配置。' -Action '请在 runtime 配置中补齐 servers.test.host 后再执行恢复。')
    return $request
}

function Get-BackupOpsRuntimeEnvLines {
    param(
        [Parameter(Mandatory)]
        [object]$Config
    )

    Import-BackupOpsSshDependency
    $sshRequest = Get-BackupOpsProductionSshRequest -Config $Config
    $appDir = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('servers', 'production', 'appDir') -Code 'INTBK-3001' -Reason '缺少正式运行目录配置。' -Action '请在 runtime 配置中补齐 servers.production.appDir 后再执行。')
    $envPath = ($appDir.TrimEnd('/')) + '/.env'
    $command = "cat {0}" -f (ConvertTo-BackupBashSingleQuotedString -Value $envPath)
    $result = Invoke-BackupSshCommand -Request ($sshRequest + @{
        Command = $command
        TimeoutSeconds = 60
    })
    $lines = @($result.output -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($lines.Count -eq 0) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message (New-BackupOpsOperatorBlockedMessage -Reason "正式环境运行时配置为空或无法读取：$envPath" -Action '请确认正式环境 runtime/.env 存在且 SSH 账号具备读取权限后再重试。'))
    }

    return $lines
}

function Get-BackupOpsMySqlRootPassword {
    param(
        [Parameter(Mandatory)]
        [object]$Config
    )

    $envLines = Get-BackupOpsRuntimeEnvLines -Config $Config
    $password = Get-BackupOpsDotEnvValue -Lines $envLines -Key 'MYSQL_ROOT_PASSWORD'
    if ([string]::IsNullOrWhiteSpace($password)) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message (New-BackupOpsOperatorBlockedMessage -Reason '正式环境 runtime/.env 中缺少 MYSQL_ROOT_PASSWORD。' -Action '请先补齐正式环境 MySQL 凭据来源，再执行备份或恢复。'))
    }

    return $password
}

function Get-BackupOpsMySqlDumpFileName {
    param(
        [Parameter(Mandatory)]
        [string]$DatabaseName
    )

    return "$DatabaseName.sql.gz"
}

function ConvertFrom-BackupOpsMySqlPayloadProof {
    param(
        [Parameter(Mandatory)]
        [string]$Output
    )

    $match = [regex]::Match($Output, '(?i)([0-9a-f]{64})(?:\s+|\\t)([1-9][0-9]*)')
    if (-not $match.Success) {
        return $null
    }

    return [pscustomobject]([ordered]@{
            sha256 = $match.Groups[1].Value.ToLowerInvariant()
            size = [long]$match.Groups[2].Value
        })
}

function Assert-BackupOpsMySqlRestoreTestHost {
    param(
        [Parameter(Mandatory)]
        [string]$Host,
        [Parameter(Mandatory)]
        [string]$Label
    )

    if ($Host -eq '172.30.30.57') {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3002' -Status 'blocked' -Message (New-BackupOpsOperatorBlockedMessage -Reason "MySQL 恢复${Label}禁止指向正式服务器 172.30.30.57。" -Action '停止恢复，修正 TargetEnvironment 和运行配置，恢复目标只能是测试服务器 172.30.30.58。'))
    }
    if ($Host -ne '172.30.30.58') {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3002' -Status 'blocked' -Message (New-BackupOpsOperatorBlockedMessage -Reason "MySQL 恢复${Label}无法证明为测试服务器 172.30.30.58：$Host" -Action '请显式使用 targetEnvironment=test，并确认目标主机和备份仓库主机均为 172.30.30.58。'))
    }
}

function Get-BackupOpsMySqlExpectedBackupSourceHost {
    param(
        [Parameter(Mandatory)]
        [string]$Environment
    )

    switch ($Environment) {
        'production' { return '172.30.30.57' }
        'prod' { return '172.30.30.57' }
        'test' { return '172.30.30.58' }
        'backup' { return '172.30.30.59' }
        default { return '' }
    }
}

function Assert-BackupOpsMySqlBackupSourceHost {
    param(
        [Parameter(Mandatory)]
        [string]$Environment,
        [Parameter(Mandatory)]
        [string]$Host
    )

    $expectedHost = Get-BackupOpsMySqlExpectedBackupSourceHost -Environment $Environment
    if ([string]::IsNullOrWhiteSpace($expectedHost)) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message (New-BackupOpsOperatorBlockedMessage -Reason "MySQL 备份源环境未知：$Environment。" -Action '请显式使用 test、backup、prod 或 production 之一后再执行备份。'))
    }
    if ($Host -ne $expectedHost) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message (New-BackupOpsOperatorBlockedMessage -Reason "MySQL 备份源主机与目标环境不匹配：environment=$Environment host=$Host expected=$expectedHost。" -Action '请修正 TargetEnvironment 与运行配置，确保备份源主机可被明确证明。'))
    }
}

function Assert-BackupOpsMySqlBackupRepositoryHost {
    param(
        [Parameter(Mandatory)]
        [string]$Host
    )

    if ($Host -ne '172.30.30.58') {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message (New-BackupOpsOperatorBlockedMessage -Reason "MySQL 备份仓库主机必须是测试服务器 172.30.30.58：$Host。" -Action '请确认 BackupPackage 仓库仍由测试服务器挂载的 /mnt/nas/Backup/BackupPackage 承载。'))
    }
}

function Assert-BackupMySqlRemoteBackupPackageDumpPath {
    param(
        [Parameter(Mandatory)]
        [string]$Path
    )

    $normalized = $Path.Replace('\', '/')
    if (-not $normalized.StartsWith('/mnt/nas/Backup/BackupPackage/', [System.StringComparison]::Ordinal)) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3002' -Status 'blocked' -Message (New-BackupOpsOperatorBlockedMessage -Reason "MySQL 恢复 dump 不在测试服 BackupPackage 目录下：$Path" -Action '请确认恢复点来自测试服务器 /mnt/nas/Backup/BackupPackage 后再执行恢复。'))
    }
    if ($normalized -match '(^|/)\.\.($|/)') {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3002' -Status 'blocked' -Message (New-BackupOpsOperatorBlockedMessage -Reason "MySQL 恢复 dump 路径包含非法上级目录引用：$Path" -Action '请使用 BackupPackage 下的合法备份点路径。'))
    }
    if ($normalized -like '*/ReleasePackage/*') {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3002' -Status 'blocked' -Message (New-BackupOpsOperatorBlockedMessage -Reason "MySQL 恢复 dump 指向 ReleasePackage：$Path" -Action '数据恢复只能读取 BackupPackage 下的数据备份。'))
    }
}

function Get-BackupMySqlRemoteDumpDirectory {
    param(
        [Parameter(Mandatory)]
        [string]$Path
    )

    $normalized = $Path.Replace('\', '/')
    $lastSlash = $normalized.LastIndexOf('/')
    if ($lastSlash -le 0) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "MySQL remote dump path is invalid: $Path")
    }
    return $normalized.Substring(0, $lastSlash)
}

function New-BackupMySqlDumpCommandSpec {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [hashtable]$Request
    )

    $containerName = Get-BackupMySqlFieldValue -Request $Request -Name 'ContainerName'
    $databaseName = Get-BackupMySqlFieldValue -Request $Request -Name 'DatabaseName'
    $rootPassword = Get-BackupMySqlFieldValue -Request $Request -Name 'RootPassword'

    $escapedPassword = ConvertTo-BackupBashSingleQuotedString -Value $rootPassword
    $escapedDatabase = ConvertTo-BackupBashSingleQuotedString -Value $databaseName
    $dumpArgsText = $script:BackupMySqlDumpArguments -join ' '
    $dumpCommand = "docker exec {0} mysqldump {1} -uroot -p{2} --databases {3} | gzip -c" -f $containerName, $dumpArgsText, $escapedPassword, $escapedDatabase

    if ($Request.ContainsKey('RemoteDumpPath') -and -not [string]::IsNullOrWhiteSpace([string]$Request['RemoteDumpPath'])) {
        $remoteDumpPath = Get-BackupMySqlFieldValue -Request $Request -Name 'RemoteDumpPath'
        Assert-BackupMySqlRemoteBackupPackageDumpPath -Path $remoteDumpPath
        $remoteDumpDirectory = Get-BackupMySqlRemoteDumpDirectory -Path $remoteDumpPath
        $escapedRemoteDumpDirectory = ConvertTo-BackupBashSingleQuotedString -Value $remoteDumpDirectory
        $escapedRemoteDumpPath = ConvertTo-BackupBashSingleQuotedString -Value $remoteDumpPath
        $remoteChecksumPath = $remoteDumpPath + '.sha256'
        $escapedRemoteChecksumPath = ConvertTo-BackupBashSingleQuotedString -Value $remoteChecksumPath
        $remoteScript = "set -euo pipefail; mkdir -p $escapedRemoteDumpDirectory; $dumpCommand > $escapedRemoteDumpPath; test -s $escapedRemoteDumpPath; gzip -t $escapedRemoteDumpPath; sha256sum $escapedRemoteDumpPath | cut -d ' ' -f1 > $escapedRemoteChecksumPath; grep -Eq '^[0-9a-fA-F]{64}$' $escapedRemoteChecksumPath"
        return [pscustomobject]([ordered]@{
                tool = 'mysqldump'
                databaseName = $databaseName
                container = $containerName
                arguments = @($script:BackupMySqlDumpArguments)
                outputPath = ''
                remoteDumpPath = $remoteDumpPath
                remoteChecksumPath = $remoteChecksumPath
                remoteCommand = "bash -lc {0}" -f (ConvertTo-BackupBashSingleQuotedString -Value $remoteScript)
                commandPreview = "docker exec $containerName mysqldump $dumpArgsText -uroot -p<hidden> --databases $databaseName | gzip -c > $remoteDumpPath"
            })
    }

    $outputPath = Get-BackupMySqlFieldValue -Request $Request -Name 'OutputPath'

    return [pscustomobject]([ordered]@{
            tool = 'mysqldump'
            databaseName = $databaseName
            container = $containerName
            arguments = @($script:BackupMySqlDumpArguments)
            outputPath = $outputPath
            remoteDumpPath = ''
            remoteCommand = $dumpCommand
            commandPreview = "docker exec $containerName mysqldump $dumpArgsText -uroot -p<hidden> --databases $databaseName | gzip -c"
        })
}

function New-BackupMySqlRestoreCommandSpec {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [hashtable]$Request
    )

    $containerName = Get-BackupMySqlFieldValue -Request $Request -Name 'ContainerName' -Code 'INTBK-3002'
    $databaseName = Get-BackupMySqlFieldValue -Request $Request -Name 'DatabaseName' -Code 'INTBK-3002'
    $remoteDumpPath = Get-BackupMySqlFieldValue -Request $Request -Name 'RemoteDumpPath' -Code 'INTBK-3002'
    $rootPassword = Get-BackupMySqlFieldValue -Request $Request -Name 'RootPassword' -Code 'INTBK-3002'

    Assert-BackupMySqlRemoteBackupPackageDumpPath -Path $remoteDumpPath

    $escapedPassword = ConvertTo-BackupBashSingleQuotedString -Value $rootPassword
    $escapedRemoteDump = ConvertTo-BackupBashSingleQuotedString -Value $remoteDumpPath
    $escapedRemoteChecksum = ConvertTo-BackupBashSingleQuotedString -Value ($remoteDumpPath + '.sha256')
    $integrityCommand = 'set -euo pipefail; test -s {0}; test -s {1}; gzip -t {0}; expected=$(tr -d ''[:space:]'' < {1}); actual=$(sha256sum {0} | cut -d '' '' -f1); test "$actual" = "$expected"' -f $escapedRemoteDump, $escapedRemoteChecksum
    $resetSqlArgs = @(
        ConvertTo-BackupBashSingleQuotedString -Value "DROP DATABASE IF EXISTS ``$databaseName``;"
        ConvertTo-BackupBashSingleQuotedString -Value "CREATE DATABASE ``$databaseName`` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
    ) -join ' '

    return [pscustomobject]([ordered]@{
            container = $containerName
            databaseName = $databaseName
            remoteDumpPath = $remoteDumpPath
            integrityCommand = $integrityCommand
            resetCommand = "printf '%s\n' $resetSqlArgs | docker exec -i $containerName mysql -uroot -p$escapedPassword"
            importCommand = "gzip -dc $escapedRemoteDump | docker exec -i $containerName mysql -uroot -p$escapedPassword"
            connectivityCommand = "docker exec $containerName mysqladmin -uroot -p$escapedPassword ping --silent"
            commandPreview = @(
                "printf '%s\n' '<reset database sql>' | docker exec -i $containerName mysql -uroot -p<hidden>"
                "gzip -dc $remoteDumpPath | docker exec -i $containerName mysql -uroot -p<hidden>"
            )
        })
}

function Test-BackupMySqlConnectivity {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [hashtable]$Request,
        [switch]$PlanOnly
    )

    Import-BackupOpsSshDependency
    $containerName = Get-BackupMySqlFieldValue -Request $Request -Name 'ContainerName' -Code 'INTBK-3003'
    $rootPassword = Get-BackupMySqlFieldValue -Request $Request -Name 'RootPassword' -Code 'INTBK-3003'
    $command = "docker exec {0} mysqladmin -uroot -p{1} ping --silent" -f $containerName, (ConvertTo-BackupBashSingleQuotedString -Value $rootPassword)
    $plan = [pscustomobject]([ordered]@{
            operation = 'mysql-health-check'
            host = $Request['Host']
            user = $Request['User']
            container = $containerName
            command = $command
            commandPreview = "docker exec $containerName mysqladmin -uroot -p<hidden> ping --silent"
        })
    if ($PlanOnly) {
        return $plan
    }

    try {
        $result = Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $Request -Extra @{
            Command = $command
            TimeoutSeconds = 60
        })
        return [pscustomobject]([ordered]@{
                operation = 'mysql-health-check'
                status = 'success'
                code = 'INTBK-0000'
                host = $Request['Host']
                user = $Request['User']
                container = $containerName
                output = $result.output
            })
    } catch {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3003' -Status 'fail' -Message "MySQL connectivity check failed for container '$containerName'. $($_.Exception.Message)")
    }
}

function Export-BackupMySqlDump {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [hashtable]$Request,
        [switch]$PlanOnly
    )

    Import-BackupOpsSshDependency
    $commandSpec = New-BackupMySqlDumpCommandSpec -Request $Request
    if ($PlanOnly) {
        return $commandSpec
    }

    if (-not [string]::IsNullOrWhiteSpace([string]$commandSpec.outputPath)) {
        $outputParent = Split-Path -Parent $commandSpec.outputPath
        if (-not [string]::IsNullOrWhiteSpace($outputParent)) {
            [void][System.IO.Directory]::CreateDirectory($outputParent)
        }
    }

    try {
        Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $Request -Extra @{
                Command = $commandSpec.remoteCommand
                OutputPath = $commandSpec.outputPath
                TimeoutSeconds = 7200
            }) | Out-Null
    } catch {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'fail' -Message "MySQL dump export failed for database '$($commandSpec.databaseName)'. $($_.Exception.Message)")
    }

    return [pscustomobject]([ordered]@{
            operation = 'mysql-dump'
            status = 'success'
            code = 'INTBK-0000'
            databaseName = $commandSpec.databaseName
            container = $commandSpec.container
            outputPath = $commandSpec.outputPath
            remoteDumpPath = $commandSpec.remoteDumpPath
        })
}

function Import-BackupMySqlDump {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [hashtable]$Request,
        [switch]$PlanOnly
    )

    Import-BackupOpsSshDependency
    $restoreSpec = New-BackupMySqlRestoreCommandSpec -Request $Request
    if ($PlanOnly) {
        return $restoreSpec
    }

    try {
        Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $Request -Extra @{
            Command = $restoreSpec.integrityCommand
            TimeoutSeconds = 60
        }) | Out-Null
        Test-BackupMySqlConnectivity -Request $Request | Out-Null
        Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $Request -Extra @{
            Command = $restoreSpec.resetCommand
            TimeoutSeconds = 300
        }) | Out-Null
        Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $Request -Extra @{
            Command = $restoreSpec.importCommand
            TimeoutSeconds = 7200
        }) | Out-Null
    } catch {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3002' -Status 'fail' -Message "MySQL dump import failed for database '$($restoreSpec.databaseName)'. $($_.Exception.Message)")
    }

    return [pscustomobject]([ordered]@{
            operation = 'mysql-restore'
            status = 'success'
            code = 'INTBK-0000'
            databaseName = $restoreSpec.databaseName
            container = $restoreSpec.container
            remoteDumpPath = $restoreSpec.remoteDumpPath
        })
}

function Get-BackupOpsMySqlCurrentPosition {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Config,
        [Parameter(Mandatory = $true)]
        [object]$LogSession
    )

    Import-BackupOpsSshDependency
    $sshRequest = Get-BackupOpsProductionSshRequest -Config $Config
    $containerName = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('containers', 'mysql') -Code 'INTBK-3001' -Reason '缺少 MySQL 容器名配置。' -Action '请补齐 containers.mysql。')
    $rootPassword = Get-BackupOpsMySqlRootPassword -Config $Config
    $query = 'SELECT @@global.log_bin, @@global.binlog_format, @@global.binlog_expire_logs_seconds; SHOW MASTER STATUS;'
    $command = 'docker exec {0} mysql -uroot -p{1} -N -B -e {2}' -f $containerName,
        (ConvertTo-BackupBashSingleQuotedString -Value $rootPassword),
        (ConvertTo-BackupBashSingleQuotedString -Value $query)
    $result = Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $sshRequest -Extra @{
        Command = $command
        TimeoutSeconds = 60
    })
    $lines = @($result.output -split '\r?\n' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($lines.Count -lt 2) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message 'MySQL binlog preflight output is incomplete.')
    }
    $settings = $lines[0] -split ([char]9)
    $position = $lines[1] -split ([char]9)
    if ($settings.Count -lt 3 -or $position.Count -lt 2) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message 'MySQL binlog preflight output format is invalid.')
    }
    if ([string]$settings[0] -notin @('1', 'ON', 'on', 'TRUE', 'true')) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message 'MySQL incremental backup requires log_bin=ON.')
    }
    if (([string]$settings[1]).ToUpperInvariant() -ne 'ROW') {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "MySQL incremental backup requires binlog_format=ROW; actual=$($settings[1]).")
    }
    Write-BackupOpsLog -Session $LogSession -Message "MySQL binlog position resolved: $($position[0]):$($position[1])."
    return [pscustomobject]([ordered]@{
        file = [string]$position[0]
        position = [long]$position[1]
        retentionSeconds = [long]$settings[2]
        format = ([string]$settings[1]).ToUpperInvariant()
    })
}

function Get-BackupOpsMySqlBinaryLogs {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Config
    )

    Import-BackupOpsSshDependency
    $sshRequest = Get-BackupOpsProductionSshRequest -Config $Config
    $containerName = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('containers', 'mysql') -Code 'INTBK-3001' -Reason '缺少 MySQL 容器名配置。' -Action '请补齐 containers.mysql。')
    $rootPassword = Get-BackupOpsMySqlRootPassword -Config $Config
    $command = 'docker exec {0} mysql -uroot -p{1} -N -B -e {2}' -f $containerName,
        (ConvertTo-BackupBashSingleQuotedString -Value $rootPassword),
        (ConvertTo-BackupBashSingleQuotedString -Value 'SHOW BINARY LOGS;')
    $result = Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $sshRequest -Extra @{
        Command = $command
        TimeoutSeconds = 60
    })
    return @($result.output -split '\r?\n' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object {
        $parts = $_ -split ([char]9)
        [pscustomobject]@{ file = [string]$parts[0]; size = if ($parts.Count -gt 1) { [long]$parts[1] } else { 0L } }
    })
}

function Assert-BackupOpsMySqlParentPayloadIntegrity {
    param(
        [Parameter(Mandatory = $true)][hashtable]$SshRequest,
        [Parameter(Mandatory = $true)][string]$BackupPointPath,
        [Parameter(Mandatory = $true)][object]$Manifest
    )

    $commands = [System.Collections.Generic.List[string]]::new()
    $escapedPoint = ConvertTo-BackupBashSingleQuotedString -Value $BackupPointPath
    $commands.Add("cd $escapedPoint")
    $commands.Add('test -s manifest/checksums.txt')
    $commands.Add('sha256sum -c manifest/checksums.txt >/dev/null')
    $manifestKind = if ($Manifest.PSObject.Properties['backupKind']) { [string]$Manifest.backupKind } else { '' }
    $mysqlEvidence = if ($Manifest.PSObject.Properties['mysqlEvidence']) { $Manifest.mysqlEvidence } else { $null }
    if ($manifestKind -notin @('FULL', 'INCREMENTAL') -or $null -eq $mysqlEvidence) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "MySQL chain payload evidence is missing: $BackupPointPath.")
    }
    $payloads = @(
        if ($manifestKind -eq 'FULL') {
            if (-not $mysqlEvidence.PSObject.Properties['dumpPath'] -or -not $mysqlEvidence.PSObject.Properties['sha256']) {
                throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "MySQL FULL payload evidence is incomplete: $BackupPointPath.")
            }
            [pscustomobject]@{ path = [string]$mysqlEvidence.dumpPath; sha256 = [string]$mysqlEvidence.sha256 }
        } else {
            if (-not $mysqlEvidence.PSObject.Properties['segments']) {
                throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "MySQL INCREMENTAL payload evidence is incomplete: $BackupPointPath.")
            }
            @($mysqlEvidence.segments)
        }
    )
    if ($payloads.Count -eq 0) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "MySQL incremental parent has no verifiable payload: $BackupPointPath.")
    }
    foreach ($payload in $payloads) {
        $relativePath = ([string]$payload.path).Replace('\', '/')
        $expectedSha256 = ([string]$payload.sha256).ToLowerInvariant()
        if ($relativePath -notmatch '^mysql/[A-Za-z0-9._/-]+\.gz$' -or $relativePath.Contains('..') -or
            $expectedSha256 -notmatch '^[0-9a-f]{64}$') {
            throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "MySQL incremental parent payload metadata is invalid: $relativePath.")
        }
        $escapedPath = ConvertTo-BackupBashSingleQuotedString -Value $relativePath
        $escapedSha = ConvertTo-BackupBashSingleQuotedString -Value $expectedSha256
        $commands.Add("test -s $escapedPath")
        $commands.Add("gzip -t $escapedPath")
        $commands.Add(('actual=$(sha256sum {0} | cut -d '' '' -f1); test "$actual" = {1}' -f $escapedPath, $escapedSha))
    }
    Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $SshRequest -Extra @{
        Command = "bash -lc {0}" -f (ConvertTo-BackupBashSingleQuotedString -Value ($commands -join '; '))
        TimeoutSeconds = 7200
    }) | Out-Null
}

function Get-BackupOpsPreviousMySqlChainPoint {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Config,
        [Parameter(Mandatory = $true)]
        [string]$CurrentBackupId
    )

    Import-BackupOpsSshDependency
    $backupPointsRoot = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('servers', 'test', 'backupPointsRoot') -Code 'INTBK-3001' -Reason '缺少测试服务器备份点根目录配置。' -Action '请补齐 servers.test.backupPointsRoot。')
    $testRequest = Get-BackupOpsTestSshRequest -Config $Config
    $escapedRoot = ConvertTo-BackupBashSingleQuotedString -Value $backupPointsRoot
    $listResult = Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $testRequest -Extra @{
        Command = "find $escapedRoot -mindepth 1 -maxdepth 1 -regextype posix-extended -type d -regex '.*/[0-9]{8}-[0-9]{6}$' | sort -r"
        TimeoutSeconds = 300
    })
    $candidatePaths = @($listResult.output -split '\r?\n' | Where-Object {
        -not [string]::IsNullOrWhiteSpace($_) -and -not $_.TrimEnd('/').EndsWith("/$CurrentBackupId")
    })
    if ($candidatePaths.Count -eq 0) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message 'MySQL incremental backup requires a latest successful backup point, but none exists.')
    }

    $path = [string]$candidatePaths[0]
    try {
        $manifestResult = Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $testRequest -Extra @{
            Command = "cat {0}" -f (ConvertTo-BackupBashSingleQuotedString -Value ($path.TrimEnd('/') + '/manifest/manifest.json'))
            TimeoutSeconds = 60
        })
        $manifest = $manifestResult.output | ConvertFrom-Json -ErrorAction Stop
    } catch {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "MySQL incremental latest successful backup point manifest is unreadable: $path. $($_.Exception.Message)")
    }
    $kind = if ($manifest.PSObject.Properties['backupKind']) { [string]$manifest.backupKind } else { '' }
    $expectedBackupId = ($path.TrimEnd('/') -split '/')[-1]
    $baseBackupId = if ($manifest.PSObject.Properties['baseBackupId']) { [string]$manifest.baseBackupId } else { '' }
    $mysqlEvidence = if ($manifest.PSObject.Properties['mysqlEvidence']) { $manifest.mysqlEvidence } else { $null }
    $endFile = if ($null -ne $mysqlEvidence -and $mysqlEvidence.PSObject.Properties['endPosition'] -and $null -ne $mysqlEvidence.endPosition) { [string]$mysqlEvidence.endPosition.file } else { '' }
    $endPosition = if ($null -ne $mysqlEvidence -and $mysqlEvidence.PSObject.Properties['endPosition'] -and $null -ne $mysqlEvidence.endPosition) { [long]$mysqlEvidence.endPosition.position } else { 0L }
    $sourceEnvironment = if ($manifest.PSObject.Properties['sourceEnvironment']) { [string]$manifest.sourceEnvironment } else { '' }
    $sourceHost = if ($manifest.PSObject.Properties['sourceHost']) { [string]$manifest.sourceHost } else { '' }
    $expectedSourceEnvironment = if ($Config.PSObject.Properties['environment']) { [string]$Config.environment } else { 'production' }
    $expectedSourceHost = if ($expectedSourceEnvironment -eq 'test') { [string]$Config.servers.test.host } else { [string]$Config.servers.production.host }
    $repositoryEnvironment = if ($manifest.PSObject.Properties['repositoryEnvironment']) { [string]$manifest.repositoryEnvironment } else { '' }
    $repositoryHost = if ($manifest.PSObject.Properties['repositoryHost']) { [string]$manifest.repositoryHost } else { '' }
    $recoverySetStatus = if ($manifest.PSObject.Properties['recoverySet'] -and $null -ne $manifest.recoverySet -and $manifest.recoverySet.PSObject.Properties['status']) { [string]$manifest.recoverySet.status } else { '' }
    if ([string]$manifest.status -ne 'success' -or [string]$manifest.backupId -ne $expectedBackupId -or $kind -notin @('FULL', 'INCREMENTAL') -or
        [string]::IsNullOrWhiteSpace($baseBackupId) -or [string]::IsNullOrWhiteSpace($endFile) -or $endPosition -le 0 -or
        $sourceEnvironment -ne $expectedSourceEnvironment -or $sourceHost -ne $expectedSourceHost -or
        $repositoryEnvironment -ne 'test' -or $repositoryHost -ne '172.30.30.58' -or $recoverySetStatus -ne 'COMPLETE') {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "MySQL incremental latest successful backup point is not a valid chain parent: $path.")
    }
    return [pscustomobject]([ordered]@{
        backupId = [string]$manifest.backupId
        baseBackupId = $baseBackupId
        endFile = $endFile
        endPosition = $endPosition
    })
}

function Write-BackupOpsMySqlEvidenceJson {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [object]$Value
    )
    [System.IO.Directory]::CreateDirectory((Split-Path -Parent $Path)) | Out-Null
    [System.IO.File]::WriteAllText($Path, ($Value | ConvertTo-Json -Depth 8), [System.Text.UTF8Encoding]::new($false))
}

function Export-BackupOpsMySqlBinlogIncrement {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Config,
        [Parameter(Mandatory = $true)]
        [object]$Workspace,
        [Parameter(Mandatory = $true)]
        [object]$LogSession
    )

    Import-BackupOpsSshDependency
    $sshRequest = Get-BackupOpsProductionSshRequest -Config $Config
    $sourceEnvironment = if ($Config.PSObject.Properties['environment']) { [string]$Config.environment } else { 'production' }
    Assert-BackupOpsMySqlBackupSourceHost -Environment $sourceEnvironment -Host ([string]$sshRequest.Host)
    $containerName = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('containers', 'mysql') -Code 'INTBK-3001' -Reason '缺少 MySQL 容器名配置。' -Action '请补齐 containers.mysql。')
    $databaseName = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('backup', 'mysqlDatabase') -Code 'INTBK-3001' -Reason '缺少 MySQL 数据库名配置。' -Action '请补齐 backup.mysqlDatabase。')
    $rootPassword = Get-BackupOpsMySqlRootPassword -Config $Config
    $backupPointsRoot = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('servers', 'test', 'backupPointsRoot') -Code 'INTBK-3001' -Reason '缺少测试服务器备份点根目录配置。' -Action '请补齐 servers.test.backupPointsRoot。')
    $previous = Get-BackupOpsPreviousMySqlChainPoint -Config $Config -CurrentBackupId $Workspace.BackupId
    Assert-BackupOpsExistingMySqlChainIntegrity -Config $Config -BaseBackupId $previous.baseBackupId -TargetBackupId $previous.backupId | Out-Null
    $current = Get-BackupOpsMySqlCurrentPosition -Config $Config -LogSession $LogSession
    $logs = @(Get-BackupOpsMySqlBinaryLogs -Config $Config)
    $startIndex = -1
    $endIndex = -1
    for ($index = 0; $index -lt $logs.Count; $index++) {
        if ([string]$logs[$index].file -eq [string]$previous.endFile) {
            $startIndex = $index
        }
        if ([string]$logs[$index].file -eq [string]$current.file) {
            $endIndex = $index
        }
    }
    if ($startIndex -lt 0 -or $endIndex -lt $startIndex) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "MySQL binlog chain is broken: $($previous.endFile) -> $($current.file).")
    }

    $remoteDir = ($backupPointsRoot.TrimEnd('/')) + "/$($Workspace.BackupId).creating/mysql/binlog"
    $segments = [System.Collections.Generic.List[object]]::new()
    for ($index = $startIndex; $index -le $endIndex; $index++) {
        $file = [string]$logs[$index].file
        $startPosition = if ($index -eq $startIndex) { [long]$previous.endPosition } else { 4L }
        $stopPosition = if ($index -eq $endIndex) { [long]$current.position } else { [long]$logs[$index].size }
        if ($stopPosition -lt $startPosition) {
            throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "MySQL binlog position is reversed for $file.")
        }
        $fileName = "$file.$startPosition-$stopPosition.sql.gz"
        $remotePath = "$remoteDir/$fileName"
        $checksumPath = "$remotePath.sha256"
        $mysqlbinlog = 'mysqlbinlog --verify-binlog-checksum --read-from-remote-server --host=127.0.0.1 --user=root --password={0} --start-position={1} --stop-position={2} --database={3} {4}' -f
            (ConvertTo-BackupBashSingleQuotedString -Value $rootPassword), $startPosition, $stopPosition,
            (ConvertTo-BackupBashSingleQuotedString -Value $databaseName),
            (ConvertTo-BackupBashSingleQuotedString -Value $file)
        $remoteScript = 'set -euo pipefail; mkdir -p {0}; docker exec {1} sh -lc {2} | gzip -c > {3}; test -s {3}; gzip -t {3}; sha256sum {3} | cut -d '' '' -f1 > {4}; printf ''%s\t%s\n'' "$(cat {4})" "$(wc -c < {3})"' -f
            (ConvertTo-BackupBashSingleQuotedString -Value $remoteDir), $containerName,
            (ConvertTo-BackupBashSingleQuotedString -Value $mysqlbinlog),
            (ConvertTo-BackupBashSingleQuotedString -Value $remotePath),
            (ConvertTo-BackupBashSingleQuotedString -Value $checksumPath)
        $exportResult = Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $sshRequest -Extra @{
            Command = "bash -lc {0}" -f (ConvertTo-BackupBashSingleQuotedString -Value $remoteScript)
            TimeoutSeconds = 7200
        })
        $proof = ConvertFrom-BackupOpsMySqlPayloadProof -Output ([string]$exportResult.output)
        if ($null -eq $proof) {
            throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "MySQL binlog segment proof is invalid for $file.")
        }
        $segments.Add([pscustomobject]([ordered]@{
            binlogFile = $file
            path = "mysql/binlog/$fileName"
            checksumPath = "mysql/binlog/$fileName.sha256"
            startPosition = $startPosition
            stopPosition = $stopPosition
            sha256 = $proof.sha256
            size = $proof.size
        })) | Out-Null
    }
    $evidence = [pscustomobject]([ordered]@{
        schemaVersion = 'mysql-binlog-segment-v1'
        status = 'exported'
        baseBackupId = [string]$previous.baseBackupId
        parentBackupId = [string]$previous.backupId
        startPosition = [pscustomobject]@{ file = [string]$previous.endFile; position = [long]$previous.endPosition }
        endPosition = [pscustomobject]@{ file = [string]$current.file; position = [long]$current.position }
        retentionSeconds = [long]$current.retentionSeconds
        segments = @($segments)
        replayStatus = 'not-run'
    })
    $evidencePath = Join-Path $Workspace.MySqlPath 'binlog-segment-manifest.json'
    Write-BackupOpsMySqlEvidenceJson -Path $evidencePath -Value $evidence
    Write-BackupOpsLog -Session $LogSession -Message "MySQL binlog increment exported: $($previous.backupId) -> $($Workspace.BackupId)."
    return $evidence
}

function Get-BackupOpsRemoteBackupManifest {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Config,
        [Parameter(Mandatory = $true)]
        [string]$BackupId
    )

    $backupPointsRoot = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('servers', 'test', 'backupPointsRoot') -Code 'INTBK-3001' -Reason '缺少测试服务器备份点根目录配置。' -Action '请补齐 servers.test.backupPointsRoot。')
    $testRequest = Get-BackupOpsTestSshRequest -Config $Config
    $manifestPath = "$($backupPointsRoot.TrimEnd('/'))/$BackupId/manifest/manifest.json"
    try {
        $result = Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $testRequest -Extra @{
            Command = "cat {0}" -f (ConvertTo-BackupBashSingleQuotedString -Value $manifestPath)
            TimeoutSeconds = 60
        })
        return $result.output | ConvertFrom-Json
    } catch {
        throw (New-BackupOpsMySqlException -Code 'INTBK-7001' -Status 'blocked' -Message "MySQL restore chain manifest cannot be read: $BackupId.")
    }
}

function Assert-BackupOpsExistingMySqlChainIntegrity {
    param(
        [Parameter(Mandatory = $true)][object]$Config,
        [Parameter(Mandatory = $true)][string]$BaseBackupId,
        [Parameter(Mandatory = $true)][string]$TargetBackupId
    )

    $testRequest = Get-BackupOpsTestSshRequest -Config $Config
    $backupPointsRoot = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('servers', 'test', 'backupPointsRoot') -Code 'INTBK-3001' -Reason '缺少测试服务器备份点根目录配置。' -Action '请补齐 servers.test.backupPointsRoot。')
    $expectedSourceEnvironment = if ($Config.PSObject.Properties['environment']) { [string]$Config.environment } else { 'production' }
    $expectedSourceHost = if ($expectedSourceEnvironment -eq 'test') { [string]$Config.servers.test.host } else { [string]$Config.servers.production.host }
    $currentBackupId = $TargetBackupId
    $visited = @{}
    for ($depth = 0; $depth -lt 100; $depth++) {
        if ($visited.ContainsKey($currentBackupId)) {
            throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "Existing MySQL chain contains a cycle at $currentBackupId.")
        }
        $visited[$currentBackupId] = $true
        $manifest = Get-BackupOpsRemoteBackupManifest -Config $Config -BackupId $currentBackupId
        $kind = if ($manifest.PSObject.Properties['backupKind']) { [string]$manifest.backupKind } else { '' }
        $parentBackupId = if ($manifest.PSObject.Properties['parentBackupId']) { [string]$manifest.parentBackupId } else { '' }
        $sourceEnvironment = if ($manifest.PSObject.Properties['sourceEnvironment']) { [string]$manifest.sourceEnvironment } else { '' }
        $sourceHost = if ($manifest.PSObject.Properties['sourceHost']) { [string]$manifest.sourceHost } else { '' }
        $repositoryEnvironment = if ($manifest.PSObject.Properties['repositoryEnvironment']) { [string]$manifest.repositoryEnvironment } else { '' }
        $repositoryHost = if ($manifest.PSObject.Properties['repositoryHost']) { [string]$manifest.repositoryHost } else { '' }
        $recoverySetStatus = if ($manifest.PSObject.Properties['recoverySet'] -and $null -ne $manifest.recoverySet -and $manifest.recoverySet.PSObject.Properties['status']) { [string]$manifest.recoverySet.status } else { '' }
        $manifestStatus = if ($manifest.PSObject.Properties['status']) { [string]$manifest.status } else { '' }
        $manifestBackupId = if ($manifest.PSObject.Properties['backupId']) { [string]$manifest.backupId } else { '' }
        $manifestBaseBackupId = if ($manifest.PSObject.Properties['baseBackupId']) { [string]$manifest.baseBackupId } else { '' }
        if ($manifestStatus -ne 'success' -or $manifestBackupId -ne $currentBackupId -or
            $manifestBaseBackupId -ne $BaseBackupId -or $sourceEnvironment -ne $expectedSourceEnvironment -or
            $sourceHost -ne $expectedSourceHost -or $repositoryEnvironment -ne 'test' -or
            $repositoryHost -ne '172.30.30.58' -or $recoverySetStatus -ne 'COMPLETE') {
            throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "Existing MySQL chain manifest is invalid at $currentBackupId.")
        }
        if ($currentBackupId -eq $BaseBackupId) {
            if ($kind -ne 'FULL' -or -not [string]::IsNullOrWhiteSpace($parentBackupId)) {
                throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "Existing MySQL chain baseline is invalid: $BaseBackupId.")
            }
        } elseif ($kind -ne 'INCREMENTAL' -or [string]::IsNullOrWhiteSpace($parentBackupId)) {
            throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message "Existing MySQL chain parent is invalid at $currentBackupId.")
        }
        Assert-BackupOpsMySqlParentPayloadIntegrity -SshRequest $testRequest -BackupPointPath ($backupPointsRoot.TrimEnd('/') + "/$currentBackupId") -Manifest $manifest
        if ($currentBackupId -eq $BaseBackupId) {
            return [pscustomobject]@{ status = 'passed'; pointCount = $visited.Count }
        }
        $currentBackupId = $parentBackupId
    }
    throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message 'Existing MySQL chain exceeds 100 points.')
}

function Resolve-BackupOpsMySqlRestoreChain {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Config,
        [Parameter(Mandatory = $true)]
        [string]$BaseBackupId,
        [Parameter(Mandatory = $true)]
        [string]$TargetBackupId
    )

    $reverseChain = [System.Collections.Generic.List[object]]::new()
    $visited = @{}
    $currentBackupId = $TargetBackupId
    for ($depth = 0; $depth -lt 100; $depth++) {
        if ($visited.ContainsKey($currentBackupId)) {
            throw (New-BackupOpsMySqlException -Code 'INTBK-7001' -Status 'blocked' -Message "MySQL restore chain contains a cycle at $currentBackupId.")
        }
        $visited[$currentBackupId] = $true
        $manifest = Get-BackupOpsRemoteBackupManifest -Config $Config -BackupId $currentBackupId
        if ([string]$manifest.backupId -ne $currentBackupId -or [string]$manifest.baseBackupId -ne $BaseBackupId) {
            throw (New-BackupOpsMySqlException -Code 'INTBK-7001' -Status 'blocked' -Message "MySQL restore chain identity mismatch at $currentBackupId.")
        }
        if ($currentBackupId -eq $BaseBackupId) {
            if ([string]$manifest.backupKind -ne 'FULL') {
                throw (New-BackupOpsMySqlException -Code 'INTBK-7001' -Status 'blocked' -Message "MySQL restore chain base is not FULL: $BaseBackupId.")
            }
            $items = @($reverseChain)
            [array]::Reverse($items)
            return $items
        }
        if ([string]$manifest.backupKind -ne 'INCREMENTAL' -or [string]::IsNullOrWhiteSpace([string]$manifest.parentBackupId)) {
            throw (New-BackupOpsMySqlException -Code 'INTBK-7001' -Status 'blocked' -Message "MySQL incremental chain parent is missing at $currentBackupId.")
        }
        $reverseChain.Add([pscustomobject]@{
            backupId = $currentBackupId
            parentBackupId = [string]$manifest.parentBackupId
            baseBackupId = [string]$manifest.baseBackupId
            mysqlEvidence = $manifest.mysqlEvidence
        }) | Out-Null
        $currentBackupId = [string]$manifest.parentBackupId
    }
    throw (New-BackupOpsMySqlException -Code 'INTBK-7001' -Status 'blocked' -Message 'MySQL restore chain exceeds 100 segments.')
}

function Import-BackupOpsMySqlBinlogSegment {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Config,
        [Parameter(Mandatory = $true)]
        [string]$BackupId,
        [Parameter(Mandatory = $true)]
        [object]$Segment,
        [Parameter(Mandatory = $true)]
        [object]$LogSession
    )

    Test-BackupOpsMySqlBinlogSegmentIntegrity -Config $Config -BackupId $BackupId -Segment $Segment -LogSession $LogSession | Out-Null
    $relativePath = ([string]$Segment.path).Replace('\', '/')
    $backupPointsRoot = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('servers', 'test', 'backupPointsRoot') -Code 'INTBK-7001' -Reason '缺少测试服务器备份点根目录配置。' -Action '请补齐 servers.test.backupPointsRoot。')
    $remotePath = "$($backupPointsRoot.TrimEnd('/'))/$BackupId/$relativePath"
    $sshRequest = Get-BackupOpsProductionSshRequest -Config $Config
    $containerName = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('containers', 'mysql') -Code 'INTBK-7001' -Reason '缺少 MySQL 容器名配置。' -Action '请补齐 containers.mysql。')
    $rootPassword = Get-BackupOpsMySqlRootPassword -Config $Config
    $escapedPath = ConvertTo-BackupBashSingleQuotedString -Value $remotePath
    Write-BackupOpsLog -Session $LogSession -Message "Replaying MySQL binlog segment $BackupId/$relativePath."
    $importCommand = 'set -euo pipefail; gzip -dc {0} | docker exec -i {1} mysql -uroot -p{2}' -f
        $escapedPath, $containerName, (ConvertTo-BackupBashSingleQuotedString -Value $rootPassword)
    Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $sshRequest -Extra @{
        Command = "bash -lc {0}" -f (ConvertTo-BackupBashSingleQuotedString -Value $importCommand)
        TimeoutSeconds = 7200
    }) | Out-Null
}

function Test-BackupOpsMySqlBinlogSegmentIntegrity {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Config,
        [Parameter(Mandatory = $true)]
        [string]$BackupId,
        [Parameter(Mandatory = $true)]
        [object]$Segment,
        [Parameter(Mandatory = $true)]
        [object]$LogSession
    )

    $relativePath = ([string]$Segment.path).Replace('\', '/')
    if ($relativePath -notmatch '^mysql/binlog/[A-Za-z0-9._-]+\.sql\.gz$' -or $relativePath.Contains('..')) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-7001' -Status 'blocked' -Message "MySQL binlog segment path is invalid: $relativePath.")
    }
    $expectedSha256 = ([string]$Segment.sha256).ToLowerInvariant()
    if ($expectedSha256 -notmatch '^[0-9a-f]{64}$') {
        throw (New-BackupOpsMySqlException -Code 'INTBK-7001' -Status 'blocked' -Message "MySQL binlog segment SHA-256 is invalid: $relativePath.")
    }
    $backupPointsRoot = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('servers', 'test', 'backupPointsRoot') -Code 'INTBK-7001' -Reason '缺少测试服务器备份点根目录配置。' -Action '请补齐 servers.test.backupPointsRoot。')
    $remotePath = "$($backupPointsRoot.TrimEnd('/'))/$BackupId/$relativePath"
    $sshRequest = Get-BackupOpsProductionSshRequest -Config $Config
    $escapedPath = ConvertTo-BackupBashSingleQuotedString -Value $remotePath
    $integrityCommand = 'set -euo pipefail; test -s {0}; gzip -t {0}; actual=$(sha256sum {0} | cut -d '' '' -f1); test "$actual" = {1}' -f
        $escapedPath, (ConvertTo-BackupBashSingleQuotedString -Value $expectedSha256)
    Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $sshRequest -Extra @{
        Command = "bash -lc {0}" -f (ConvertTo-BackupBashSingleQuotedString -Value $integrityCommand)
        TimeoutSeconds = 300
    }) | Out-Null
    Write-BackupOpsLog -Session $LogSession -Message "Verified MySQL binlog segment $BackupId/$relativePath."
    return [pscustomobject]@{ backupId = $BackupId; path = $relativePath; status = 'passed' }
}

function Test-BackupOpsMySqlRestoreChainIntegrity {
    param(
        [Parameter(Mandatory = $true)][object]$Config,
        [Parameter(Mandatory = $true)][string]$BaseBackupId,
        [Parameter(Mandatory = $true)][string]$TargetBackupId,
        [Parameter(Mandatory = $true)][object]$LogSession
    )

    $chain = @(Resolve-BackupOpsMySqlRestoreChain -Config $Config -BaseBackupId $BaseBackupId -TargetBackupId $TargetBackupId)
    $expectedParent = $BaseBackupId
    foreach ($point in $chain) {
        if ([string]$point.parentBackupId -ne $expectedParent -or [string]$point.baseBackupId -ne $BaseBackupId -or
            $null -eq $point.mysqlEvidence -or [string]$point.mysqlEvidence.schemaVersion -ne 'mysql-binlog-segment-v1') {
            throw (New-BackupOpsMySqlException -Code 'INTBK-7001' -Status 'blocked' -Message "MySQL restore chain integrity metadata is invalid at $($point.backupId).")
        }
        foreach ($segment in @($point.mysqlEvidence.segments)) {
            Test-BackupOpsMySqlBinlogSegmentIntegrity -Config $Config -BackupId ([string]$point.backupId) -Segment $segment -LogSession $LogSession | Out-Null
        }
        $expectedParent = [string]$point.backupId
    }
    return [pscustomobject]@{ status = 'passed'; incrementalPointCount = $chain.Count }
}

function Replay-BackupOpsMySqlBinlogChain {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Config,
        [Parameter(Mandatory = $true)]
        [string]$BaseBackupId,
        [Parameter(Mandatory = $true)]
        [string]$TargetBackupId,
        [Parameter(Mandatory = $true)]
        [object]$LogSession
    )

    $chain = @(Resolve-BackupOpsMySqlRestoreChain -Config $Config -BaseBackupId $BaseBackupId -TargetBackupId $TargetBackupId)
    $expectedParent = $BaseBackupId
    foreach ($point in $chain) {
        if ([string]$point.parentBackupId -ne $expectedParent -or [string]$point.baseBackupId -ne $BaseBackupId) {
            throw (New-BackupOpsMySqlException -Code 'INTBK-7001' -Status 'blocked' -Message "MySQL restore chain parent mismatch at $($point.backupId).")
        }
        if ($null -eq $point.mysqlEvidence -or [string]$point.mysqlEvidence.schemaVersion -ne 'mysql-binlog-segment-v1') {
            throw (New-BackupOpsMySqlException -Code 'INTBK-7001' -Status 'blocked' -Message "MySQL binlog evidence is invalid at $($point.backupId).")
        }
        foreach ($segment in @($point.mysqlEvidence.segments)) {
            Import-BackupOpsMySqlBinlogSegment -Config $Config -BackupId ([string]$point.backupId) -Segment $segment -LogSession $LogSession
        }
        $expectedParent = [string]$point.backupId
    }
    return [pscustomobject]@{
        replayStatus = 'passed'
        baseBackupId = $BaseBackupId
        targetBackupId = $TargetBackupId
        incrementalPointCount = $chain.Count
    }
}

function Export-BackupOpsMySqlDump {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Config,
        [Parameter(Mandatory = $true)]
        [object]$Workspace,
        [Parameter(Mandatory = $true)]
        [object]$LogSession
    )

    Import-BackupOpsSshDependency
    $sshRequest = Get-BackupOpsProductionSshRequest -Config $Config
    $testSshRequest = Get-BackupOpsTestSshRequest -Config $Config
    $targetEnvironment = if ($Config.PSObject.Properties['environment']) { [string]$Config.environment } else { 'production' }
    Assert-BackupOpsMySqlBackupSourceHost -Environment $targetEnvironment -Host ([string]$sshRequest.Host)
    Assert-BackupOpsMySqlBackupRepositoryHost -Host ([string]$testSshRequest.Host)
    $containerName = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('containers', 'mysql') -Code 'INTBK-3001' -Reason '缺少 MySQL 容器名配置。' -Action '请在 runtime 配置中补齐 containers.mysql 后再执行备份。')
    $databaseName = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('backup', 'mysqlDatabase') -Code 'INTBK-3001' -Reason '缺少 MySQL 数据库名配置。' -Action '请在 runtime 配置中补齐 backup.mysqlDatabase 后再执行备份。')
    $backupPointsRoot = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('servers', 'test', 'backupPointsRoot') -Code 'INTBK-3001' -Reason '缺少测试服务器备份根目录配置。' -Action '请在 runtime 配置中补齐 servers.test.backupPointsRoot 后再执行备份。')
    $rootPassword = Get-BackupOpsMySqlRootPassword -Config $Config
    $dumpFileName = Get-BackupOpsMySqlDumpFileName -DatabaseName $databaseName
    $remoteDumpPath = ($backupPointsRoot.TrimEnd('/')) + "/$($Workspace.BackupId).creating/mysql/$dumpFileName"
    Assert-BackupMySqlRemoteBackupPackageDumpPath -Path $remoteDumpPath
    if ($Workspace.PSObject.Properties['RemoteMySqlDumpPath']) {
        $Workspace.RemoteMySqlDumpPath = $remoteDumpPath
    } else {
        $Workspace | Add-Member -MemberType NoteProperty -Name 'RemoteMySqlDumpPath' -Value $remoteDumpPath
    }

    $position = Get-BackupOpsMySqlCurrentPosition -Config $Config -LogSession $LogSession
    Write-BackupOpsLog -Session $LogSession -Message "Checking production MySQL connectivity on $($sshRequest.Host) for container $containerName."
    Test-BackupMySqlConnectivity -Request (Merge-BackupOpsRequest -Request $sshRequest -Extra @{
            ContainerName = $containerName
            DatabaseName = $databaseName
            RootPassword = $rootPassword
        }) | Out-Null

    Write-BackupOpsLog -Session $LogSession -Message "Exporting MySQL dump from $($sshRequest.Host) directly to test BackupPackage path $remoteDumpPath using docker exec mysqldump."
    $result = Export-BackupMySqlDump -Request (Merge-BackupOpsRequest -Request $sshRequest -Extra @{
            ContainerName = $containerName
            DatabaseName = $databaseName
            RootPassword = $rootPassword
            RemoteDumpPath = $remoteDumpPath
        })
    Write-BackupOpsLog -Session $LogSession -Message "MySQL dump exported successfully: $remoteDumpPath"
    $proofResult = Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $sshRequest -Extra @{
        Command = 'printf ''%s\t%s\n'' "$(cat {0})" "$(wc -c < {1})"' -f
            (ConvertTo-BackupBashSingleQuotedString -Value ($remoteDumpPath + '.sha256')),
            (ConvertTo-BackupBashSingleQuotedString -Value $remoteDumpPath)
        TimeoutSeconds = 60
    })
    $proof = ConvertFrom-BackupOpsMySqlPayloadProof -Output ([string]$proofResult.output)
    if ($null -eq $proof) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3001' -Status 'blocked' -Message 'MySQL full dump integrity proof is invalid.')
    }
    $evidence = [pscustomobject]([ordered]@{
        schemaVersion = 'mysql-full-dump-v1'
        status = 'exported'
        dumpPath = "mysql/$dumpFileName"
        checksumPath = "mysql/$dumpFileName.sha256"
        sha256 = $proof.sha256
        size = $proof.size
        endPosition = [pscustomobject]@{ file = [string]$position.file; position = [long]$position.position }
    })
    Write-BackupOpsMySqlEvidenceJson -Path (Join-Path $Workspace.MySqlPath 'full-dump-manifest.json') -Value $evidence
    return $result
}

function Import-BackupOpsMySqlDump {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Config,
        [Parameter(Mandatory = $true)]
        [string]$BackupId,
        [Parameter(Mandatory = $true)]
        [object]$LogSession
    )

    Import-BackupOpsSshDependency
    $databaseName = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('backup', 'mysqlDatabase') -Code 'INTBK-3002' -Reason '缺少 MySQL 数据库名配置。' -Action '请在 runtime 配置中补齐 backup.mysqlDatabase 后再执行恢复。')
    $containerName = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('containers', 'mysql') -Code 'INTBK-3002' -Reason '缺少 MySQL 容器名配置。' -Action '请在 runtime 配置中补齐 containers.mysql 后再执行恢复。')
    $backupPointsRoot = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('servers', 'test', 'backupPointsRoot') -Code 'INTBK-3002' -Reason '缺少测试服务器备份点根目录配置。' -Action '请在 runtime 配置中补齐 servers.test.backupPointsRoot 后再执行恢复。')
    $dumpFileName = Get-BackupOpsMySqlDumpFileName -DatabaseName $databaseName
    $testDumpPath = ($backupPointsRoot.TrimEnd('/')) + "/$BackupId/mysql/$dumpFileName"
    Assert-BackupMySqlRemoteBackupPackageDumpPath -Path $testDumpPath
    $testSshRequest = Get-BackupOpsTestSshRequest -Config $Config
    $productionSshRequest = Get-BackupOpsProductionSshRequest -Config $Config

    Assert-BackupOpsMySqlRestoreTestHost -Host ([string]$testSshRequest.Host) -Label '备份仓库主机'
    Assert-BackupOpsMySqlRestoreTestHost -Host ([string]$productionSshRequest.Host) -Label '目标主机'

    if ([string]$productionSshRequest.Host -ne [string]$testSshRequest.Host) {
        throw (New-BackupOpsMySqlException -Code 'INTBK-3002' -Status 'blocked' -Message (New-BackupOpsOperatorBlockedMessage -Reason "MySQL 恢复目标主机 $($productionSshRequest.Host) 与测试备份仓库主机 $($testSshRequest.Host) 不一致，无法证明恢复动作只作用于测试服务器。" -Action '请显式使用 targetEnvironment=test 并确认运行配置把恢复目标固定到 172.30.30.58。'))
    }

    $rootPassword = Get-BackupOpsMySqlRootPassword -Config $Config

    Write-BackupOpsLog -Session $LogSession -Message "Checking production MySQL connectivity on $($productionSshRequest.Host) before import."
    Test-BackupMySqlConnectivity -Request (Merge-BackupOpsRequest -Request $productionSshRequest -Extra @{
            ContainerName = $containerName
            DatabaseName = $databaseName
            RootPassword = $rootPassword
        }) | Out-Null

    Write-BackupOpsLog -Session $LogSession -Message "Importing MySQL restore point $BackupId from test BackupPackage path $testDumpPath into target container $containerName via docker exec mysql."
    $result = Import-BackupMySqlDump -Request (Merge-BackupOpsRequest -Request $productionSshRequest -Extra @{
            ContainerName = $containerName
            DatabaseName = $databaseName
            RootPassword = $rootPassword
            RemoteDumpPath = $testDumpPath
        })
    Write-BackupOpsLog -Session $LogSession -Message "MySQL restore point $BackupId imported successfully from $testDumpPath."
    return $result
}

function Test-BackupOpsMySqlDumpIntegrity {
    param(
        [Parameter(Mandatory = $true)][object]$Config,
        [Parameter(Mandatory = $true)][string]$BackupId,
        [Parameter(Mandatory = $true)][object]$LogSession
    )

    Import-BackupOpsSshDependency
    $databaseName = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('backup', 'mysqlDatabase') -Code 'INTBK-3002' -Reason '缺少 MySQL 数据库名配置。' -Action '请补齐 backup.mysqlDatabase。')
    $containerName = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('containers', 'mysql') -Code 'INTBK-3002' -Reason '缺少 MySQL 容器名配置。' -Action '请补齐 containers.mysql。')
    $backupPointsRoot = [string](Get-BackupOpsRequiredConfigValue -Config $Config -Path @('servers', 'test', 'backupPointsRoot') -Code 'INTBK-3002' -Reason '缺少测试服务器备份点根目录配置。' -Action '请补齐 servers.test.backupPointsRoot。')
    $remoteDumpPath = ($backupPointsRoot.TrimEnd('/')) + "/$BackupId/mysql/$(Get-BackupOpsMySqlDumpFileName -DatabaseName $databaseName)"
    $sshRequest = Get-BackupOpsProductionSshRequest -Config $Config
    $spec = New-BackupMySqlRestoreCommandSpec -Request (Merge-BackupOpsRequest -Request $sshRequest -Extra @{
        ContainerName = $containerName
        DatabaseName = $databaseName
        RootPassword = 'unused-for-integrity-check'
        RemoteDumpPath = $remoteDumpPath
    })
    Invoke-BackupSshCommand -Request (Merge-BackupOpsRequest -Request $sshRequest -Extra @{
        Command = $spec.integrityCommand
        TimeoutSeconds = 300
    }) | Out-Null
    Write-BackupOpsLog -Session $LogSession -Message "Verified MySQL FULL payload for $BackupId before rehearsal reset."
    return [pscustomobject]@{ backupId = $BackupId; status = 'passed' }
}

Export-ModuleMember -Function New-BackupMySqlDumpCommandSpec, New-BackupMySqlRestoreCommandSpec, Test-BackupMySqlConnectivity, Export-BackupMySqlDump, Import-BackupMySqlDump, ConvertFrom-BackupOpsMySqlPayloadProof, Export-BackupOpsMySqlDump, Export-BackupOpsMySqlBinlogIncrement, Test-BackupOpsMySqlDumpIntegrity, Test-BackupOpsMySqlRestoreChainIntegrity, Replay-BackupOpsMySqlBinlogChain, Import-BackupOpsMySqlDump
