param(
    [Nullable[int]]$Slot = $null,
    [string]$HostAddress = '0.0.0.0'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\branch-runtime-profile.ps1"

$repoRoot = Get-CurrentRepoRoot
$branch = Get-GitValue -RepoRoot $repoRoot -Arguments @('branch', '--show-current')
$context = Resolve-BranchRuntimeContext -RepoRoot $repoRoot -Branch $branch -RequestedSlot $Slot
$profile = $context.Profile
$ports = $context.Ports
$frontendRoot = Join-Path $repoRoot 'IntRuoyiFronted'

if (-not (Test-Path (Join-Path $frontendRoot 'package.json'))) {
    throw "Missing frontend package.json under $frontendRoot."
}

$env:VITE_PORT = [string]$ports.FrontendPort
$env:VITE_BASE_URL = "http://127.0.0.1:$($ports.BackendPort)"
$env:VITE_PROXY_TARGET = "http://127.0.0.1:$($ports.BackendPort)"
$env:VITE_API_URL = '/admin-api'
$env:VITE_APP_CAPTCHA_ENABLE = 'false'

Write-Host "Starting $($profile.Name) frontend on $($ports.FrontendPort), proxying backend $($ports.BackendPort)."
Push-Location $frontendRoot
try {
    & pnpm exec vite --mode $ports.FrontendMode --host $HostAddress --port $ports.FrontendPort --strictPort
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
