$ErrorActionPreference = 'Stop'

$runtimeJar = 'E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260807-121743-route-workstation-formal-binding-fix.jar'
$runtimeDir = 'E:\IntRuoyi\output\runtime\int_main'
$repoRoot = 'E:\IntRuoyi\IntRuoyiBackend'
$standardScriptPath = Join-Path $repoRoot 'script\deploy\restart-int-ruoyi-local.ps1'
$standardScript = Get-Content -Raw -Encoding utf8 -LiteralPath $standardScriptPath

function Read-StandardConstant([string] $name) {
    $pattern = '^\$' + [regex]::Escape($name) + " = '([^']+)'\r?$"
    $match = [regex]::Match($standardScript, $pattern, [Text.RegularExpressions.RegexOptions]::Multiline)
    if (-not $match.Success) {
        throw "Missing standard local runtime constant: $name"
    }
    return $match.Groups[1].Value
}

$dbPassword = Read-StandardConstant 'LocalMysqlPassword'
$hmacSecret = Read-StandardConstant 'DccSignatureEvidenceHmacSecret'
$hmacVersion = Read-StandardConstant 'DccSignatureEvidenceKeyVersion'
$listener = Get-NetTCPConnection -LocalPort 48081 -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    throw "48081 already listening: $($listener.OwningProcess -join ',')"
}

$env:DCC_ONLYOFFICE_BASE_URL = 'http://127.0.0.1:8080'
$env:DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL = 'http://host.docker.internal:48081'
$env:DCC_SIGNATURE_EVIDENCE_HMAC_SECRET = $hmacSecret
$env:DCC_SIGNATURE_EVIDENCE_KEY_VERSION = $hmacVersion
Remove-Item Env:CODEX_TEST_RUNNER_TOKEN -ErrorAction SilentlyContinue

$backendLogDir = Join-Path $runtimeDir 'logs'
New-Item -ItemType Directory -Path $backendLogDir -Force | Out-Null
$arguments = @(
    '-jar'
    $runtimeJar
    '--server.port=48081'
    '--spring.profiles.active=local'
    '--spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.2:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true'
    '--spring.datasource.dynamic.datasource.master.username=root'
    "--spring.datasource.dynamic.datasource.master.password=$dbPassword"
    '--spring.datasource.dynamic.datasource.slave.url=jdbc:mysql://127.0.0.2:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&nullCatalogMeansCurrent=true'
    '--spring.datasource.dynamic.datasource.slave.username=root'
    "--spring.datasource.dynamic.datasource.slave.password=$dbPassword"
    '--spring.data.redis.host=127.0.0.2'
    '--spring.data.redis.port=26379'
    "--logging.file.name=$(Join-Path $backendLogDir 'yudao-server.log')"
    "--yudao.runtime-control.repo-root=$repoRoot"
    "--yudao.runtime-control.state-dir=$(Join-Path $repoRoot 'runtime\runtime-control')"
    "--yudao.runtime-control.storage-guard.log-dir=$backendLogDir"
)
$outLog = Join-Path $runtimeDir 'backend-runtime-control-20260807-route-workstation-formal-binding-restart.out.log'
$errLog = Join-Path $runtimeDir 'backend-runtime-control-20260807-route-workstation-formal-binding-restart.err.log'
$process = Start-Process -FilePath (Get-Command java).Source -ArgumentList $arguments `
    -WorkingDirectory $repoRoot -WindowStyle Hidden `
    -RedirectStandardOutput $outLog -RedirectStandardError $errLog -PassThru

$deadline = (Get-Date).AddMinutes(3)
$health = $null
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    if ($process.HasExited) {
        throw "Backend exited with code $($process.ExitCode); see $errLog"
    }
    try {
        $health = Invoke-RestMethod 'http://127.0.0.1:48081/actuator/health' -TimeoutSec 3
        if ($health.status -eq 'UP') {
            break
        }
    } catch {
        $health = $null
    }
}
if ($null -eq $health -or $health.status -ne 'UP') {
    throw 'Backend health did not become UP'
}

$owner = (Get-NetTCPConnection -LocalPort 48081 -State Listen | Select-Object -First 1).OwningProcess
$runtimeProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$owner"
if (-not $runtimeProcess.CommandLine.Contains($runtimeJar)) {
    throw '48081 owner does not use the verified runtime jar'
}

[pscustomobject]@{
    Pid = $owner
    Health = $health.status
    RuntimeJar = $runtimeJar
} | Format-List
