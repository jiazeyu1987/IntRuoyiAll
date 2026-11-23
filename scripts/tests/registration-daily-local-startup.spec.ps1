$path = Join-Path $PSScriptRoot '../../IntRuoyiBackend/script/deploy/restart-int-ruoyi-local.ps1'
$tokens = $null
$errors = $null
$ast = [Management.Automation.Language.Parser]::ParseFile((Resolve-Path $path), [ref]$tokens, [ref]$errors)
if ($errors.Count) { throw ($errors | Out-String) }
$source = [IO.File]::ReadAllText((Resolve-Path $path))
if ($source -notmatch 'RegistrationCertificateDailyAutoRun is restricted') { throw 'Missing runtime boundary' }
# Evaluate only the existing conditional argument expression; never execute deployment.
foreach ($enabled in @($false, $true)) {
    $RegistrationCertificateDailyAutoRun = $enabled
    $expression = '$' + '(if ($RegistrationCertificateDailyAutoRun) { ''  "--yudao.local-job-control.quartz-auto-run-handler-whitelist=kingdeeProductionOrderSyncJob,kingdeeBomSyncJob,registrationCertificateReminderDailyJob"'' })'
    if (-not $source.Contains($expression)) { throw 'Startup argument expression changed; inspect contract' }
    $value = Invoke-Expression $expression
    if ($enabled -and $value -notmatch 'registrationCertificateReminderDailyJob') { throw 'Missing registration handler' }
    if (-not $enabled -and $null -ne $value) { throw 'Default unexpectedly overrides whitelist' }
}
'PASS: startup script parses; opt-in only adds registration handler; default unchanged.'
