Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Assert-BackupOpsTestTrue {
    param(
        [Parameter(Mandatory = $true)]
        [bool]$Condition,
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Write-BackupOpsLog {
    param($Session, $Level, $Message)
}

function New-TestConfig {
    return [pscustomobject]@{
        environment = 'production'
        servers     = [pscustomobject]@{
            production = [pscustomobject]@{
                host   = '127.0.0.1'
                appDir = '/opt/intruoyi/runtime'
            }
        }
        backup      = [pscustomobject]@{
            mysqlDatabase = 'ruoyi-vue-pro'
            objectBucket = 'yudao'
        }
    }
}

function New-TestWorkspace {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root
    )

    $backupRoot = Join-Path $Root '20260526-220000'
    $deployPath = Join-Path $backupRoot 'deploy'
    $manifestPath = Join-Path $backupRoot 'manifest'
    [void][System.IO.Directory]::CreateDirectory($deployPath)
    [void][System.IO.Directory]::CreateDirectory($manifestPath)
    return [pscustomobject]@{
        BackupId     = '20260526-220000'
        ImageTag     = 'release-current'
        DeployPath   = $deployPath
        ManifestPath = $manifestPath
    }
}

function Write-TestRuntimeEnv {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Workspace,
        [Parameter(Mandatory = $true)]
        [string]$Content
    )

    $runtimeEnvPath = Join-Path $Workspace.DeployPath 'runtime.env'
    [System.IO.File]::WriteAllText($runtimeEnvPath, $Content, [System.Text.UTF8Encoding]::new($false))
}

$backupRoot = Resolve-Path (Join-Path $PSScriptRoot '..\backup-ops')
$reportModule = Join-Path $backupRoot 'scripts\modules\Infra\ReportOps.psm1'
$fileModule = Join-Path $backupRoot 'scripts\modules\Infra\FileOps.psm1'
Import-Module $reportModule -Force -DisableNameChecking
Import-Module $fileModule -Force -DisableNameChecking

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('backup-ops-manifest-ports-' + [System.Guid]::NewGuid().ToString('N'))
[void][System.IO.Directory]::CreateDirectory($tempRoot)
try {
    $config = New-TestConfig
    $session = [pscustomobject]@{ startedAt = [System.DateTimeOffset]::Parse('2026-05-26T22:00:00+08:00') }

    $successWorkspace = New-TestWorkspace -Root (Join-Path $tempRoot 'success')
    Write-TestRuntimeEnv -Workspace $successWorkspace -Content "IMAGE_TAG=release-current`nBACKEND_HOST_PORT=49123`nFRONTEND_HOST_PORT=18099`n"
    $manifestPath = New-BackupOpsManifest -Config $config -Workspace $successWorkspace -BackupType 'manual' -LogSession $session
    $manifest = [System.IO.File]::ReadAllText($manifestPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
    Assert-BackupOpsTestTrue -Condition ($manifest.deploy.backendPort -eq 49123) -Message 'manifest backendPort must come from runtime.env.'
    Assert-BackupOpsTestTrue -Condition ($manifest.deploy.frontendPort -eq 18099) -Message 'manifest frontendPort must come from runtime.env.'

    $missingWorkspace = New-TestWorkspace -Root (Join-Path $tempRoot 'missing')
    Write-TestRuntimeEnv -Workspace $missingWorkspace -Content "IMAGE_TAG=release-current`nFRONTEND_HOST_PORT=18099`n"
    $blocked = $false
    try {
        New-BackupOpsManifest -Config $config -Workspace $missingWorkspace -BackupType 'manual' -LogSession $session | Out-Null
    }
    catch {
        $blocked = ([string]$_.Exception.Data['BackupOpsStatus']) -eq 'blocked' -and $_.Exception.Message.Contains('BACKEND_HOST_PORT')
    }
    Assert-BackupOpsTestTrue -Condition $blocked -Message 'missing BACKEND_HOST_PORT must fail fast with blocked status.'

    Write-Host 'PASS: backup ops manifest ports'
}
finally {
    $tempBase = [System.IO.Path]::GetTempPath().TrimEnd('\')
    $resolvedTemp = [System.IO.Path]::GetFullPath($tempRoot)
    if ($resolvedTemp.StartsWith($tempBase, [System.StringComparison]::OrdinalIgnoreCase) -and (Split-Path -Leaf $resolvedTemp).StartsWith('backup-ops-manifest-ports-', [System.StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $resolvedTemp -Recurse -Force -ErrorAction SilentlyContinue
    }
}
