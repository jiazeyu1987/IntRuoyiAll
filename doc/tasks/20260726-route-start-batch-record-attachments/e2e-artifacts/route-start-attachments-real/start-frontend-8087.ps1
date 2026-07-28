$ErrorActionPreference = 'Stop'

$ArtifactDir = 'E:\IntRuoyi\doc\tasks\20260726-route-start-batch-record-attachments\e2e-artifacts\route-start-attachments-real'
$FrontendRoot = 'D:\IntRuoyiWorktree\route-start-batch-record-attachments-e2e\IntRuoyiFronted'
$Stdout = Join-Path $ArtifactDir 'frontend-8087.stdout.log'
$Stderr = Join-Path $ArtifactDir 'frontend-8087.stderr.log'
$PidFile = Join-Path $ArtifactDir 'frontend-8087.pid'

$env:VITE_PORT = '8087'
$env:VITE_BASE_URL = 'http://127.0.0.1:48087'
$env:VITE_PROXY_TARGET = 'http://127.0.0.1:48087'
$env:VITE_API_URL = '/admin-api'
$env:VITE_APP_CAPTCHA_ENABLE = 'false'

$Args = @(
    'exec',
    'vite',
    '--mode',
    'env.local',
    '--host',
    '127.0.0.1',
    '--port',
    '8087',
    '--strictPort'
)

$Process = Start-Process -FilePath 'pnpm.cmd' `
    -ArgumentList $Args `
    -WorkingDirectory $FrontendRoot `
    -WindowStyle Hidden `
    -RedirectStandardOutput $Stdout `
    -RedirectStandardError $Stderr `
    -PassThru

[System.IO.File]::WriteAllText($PidFile, [string] $Process.Id, [System.Text.Encoding]::UTF8)
"PID=$($Process.Id)"
