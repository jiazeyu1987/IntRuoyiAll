$ErrorActionPreference = 'Stop'

$runtimeRoot = 'E:\IntRuoyi\output\runtime\int_main'
$jar = Join-Path $runtimeRoot 'backend-standard-template-e2e-20260730-2115.jar'
$stdout = Join-Path $runtimeRoot 'backend-standard-template-e2e-20260730-2115.out.log'
$stderr = Join-Path $runtimeRoot 'backend-standard-template-e2e-20260730-2115.err.log'
$java = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe'

if (-not (Test-Path -LiteralPath $jar)) {
    throw "Missing runtime jar: $jar"
}

$amp = [char]38
$dbUrl = 'jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false' +
    $amp + 'serverTimezone=Asia/Shanghai' +
    $amp + 'allowPublicKeyRetrieval=true' +
    $amp + 'rewriteBatchedStatements=true' +
    $amp + 'nullCatalogMeansCurrent=true'

Remove-Item Env:\CODEX_TEST_RUNNER_TOKEN -ErrorAction SilentlyContinue

$javaArgs = @(
    '-Xms512m',
    '-Xmx4096m',
    '-XX:+HeapDumpOnOutOfMemoryError',
    '-XX:HeapDumpPath=E:\IntRuoyi\output\runtime\int_main\heapdumps',
    '-XX:ErrorFile=E:\IntRuoyi\output\runtime\int_main\hs_err_standard_template_pid%p.log',
    '-jar',
    $jar,
    '--server.port=48081',
    '--spring.profiles.active=local',
    "--spring.datasource.dynamic.datasource.master.url=$dbUrl",
    '--spring.data.redis.host=127.0.0.1',
    '--spring.data.redis.port=26379',
    '--logging.file.name=E:\IntRuoyi\output\runtime\int_main\logs\yudao-server.log',
    '--yudao.runtime-control.repo-root=E:\IntRuoyi\IntRuoyiBackend',
    '--yudao.runtime-control.state-dir=E:\IntRuoyi\IntRuoyiBackend\runtime\runtime-control',
    '--yudao.runtime-control.storage-guard.log-dir=E:\IntRuoyi\output\runtime\int_main\logs'
)

$process = Start-Process `
    -FilePath $java `
    -ArgumentList $javaArgs `
    -WorkingDirectory 'E:\IntRuoyi\IntRuoyiBackend' `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr `
    -WindowStyle Hidden `
    -PassThru

$deadline = (Get-Date).AddSeconds(120)
$health = $null
do {
    Start-Sleep -Seconds 2
    try {
        $health = Invoke-RestMethod -Uri 'http://127.0.0.1:48081/actuator/health' -TimeoutSec 5
    } catch {
        $health = $null
    }
} while (-not $health -and (Get-Date) -lt $deadline)

if (-not $health) {
    Get-Content -LiteralPath $stderr -Encoding utf8 -Tail 60 -ErrorAction SilentlyContinue
    throw 'New int_main backend did not expose health within 120 seconds.'
}

$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $jar).Hash
[pscustomobject]@{
    Pid = $process.Id
    Jar = $jar
    Sha256 = $hash
    Health = ($health | ConvertTo-Json -Compress)
} | Format-List
