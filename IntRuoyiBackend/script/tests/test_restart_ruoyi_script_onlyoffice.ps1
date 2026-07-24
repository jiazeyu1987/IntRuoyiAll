$ErrorActionPreference = 'Stop'

$restartScriptPath = Join-Path $PSScriptRoot '..\deploy\restart-int-ruoyi-local.ps1'
$statusScriptPath = Join-Path $PSScriptRoot '..\deploy\show-int-ruoyi-local-status.ps1'

if (-not (Test-Path -LiteralPath $restartScriptPath)) {
    throw "Missing restart script: $restartScriptPath"
}
if (-not (Test-Path -LiteralPath $statusScriptPath)) {
    throw "Missing status script: $statusScriptPath"
}

$restartSource = Get-Content -LiteralPath $restartScriptPath -Encoding UTF8 -Raw
$statusSource = Get-Content -LiteralPath $statusScriptPath -Encoding UTF8 -Raw

function Assert-Match {
    param(
        [string]$Source,
        [string]$Pattern,
        [string]$Message
    )

    if ($Source -notmatch $Pattern) {
        throw $Message
    }
}

Assert-Match $restartSource 'DCC_ONLYOFFICE_BASE_URL' 'Restart script must configure DCC_ONLYOFFICE_BASE_URL for local backend runtime.'
Assert-Match $restartSource 'http://127\.0\.0\.1:8080' 'Restart script must point local OnlyOffice base URL to 127.0.0.1:8080.'
Assert-Match $restartSource 'DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL' 'Restart script must configure DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL for local backend runtime.'
Assert-Match $restartSource 'host\.docker\.internal' 'Restart script must expose backend files to Docker OnlyOffice through host.docker.internal.'
Assert-Match $statusSource 'OnlyOffice' 'Status script must include OnlyOffice health information.'
Assert-Match $statusSource '8080' 'Status script must probe local OnlyOffice on port 8080.'

Write-Host 'restart-int-ruoyi-local OnlyOffice tests passed'
