$ErrorActionPreference = 'Stop'

$ArtifactDir = 'E:\IntRuoyi\doc\tasks\20260726-route-start-batch-record-attachments\e2e-artifacts\route-start-attachments-real'
$BackendRoot = 'D:\IntRuoyiWorktree\route-start-batch-record-attachments-e2e\IntRuoyiBackend'
$JarPath = Join-Path $BackendRoot 'yudao-server\target\yudao-server-exec.jar'

if (-not (Test-Path -LiteralPath $JarPath)) {
    throw "Missing backend jar: $JarPath"
}

$Stdout = Join-Path $ArtifactDir 'backend-48087.stdout.log'
$Stderr = Join-Path $ArtifactDir 'backend-48087.stderr.log'
$PidFile = Join-Path $ArtifactDir 'backend-48087.pid'

$JavaArgs = @(
    '-jar',
    $JarPath,
    '--server.port=48087',
    '--spring.profiles.active=local',
    '--spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true',
    '--spring.datasource.dynamic.datasource.slave.url=jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&nullCatalogMeansCurrent=true',
    '--spring.data.redis.host=127.0.0.1',
    '--spring.data.redis.port=26379'
)

$Process = Start-Process -FilePath 'java' `
    -ArgumentList $JavaArgs `
    -WorkingDirectory $BackendRoot `
    -WindowStyle Hidden `
    -RedirectStandardOutput $Stdout `
    -RedirectStandardError $Stderr `
    -PassThru

[System.IO.File]::WriteAllText($PidFile, [string] $Process.Id, [System.Text.Encoding]::UTF8)
"PID=$($Process.Id)"
