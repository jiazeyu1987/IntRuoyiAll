$ErrorActionPreference='Stop'
$taskErrors=$null
$taskTokens=$null
$taskAst=[System.Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'restart-int-ruoyi-local.ps1'),[ref]$taskTokens,[ref]$taskErrors)
if ($taskErrors.Count) { throw ($taskErrors | Out-String) }
foreach ($name in @('Assert-OwnedBackendProcess','Stop-OwnedBackendListener','Ensure-RequiredLocalMySqlSchema')) {
  $fn=$taskAst.Find({param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq $name},$true)
  if (-not $fn) { throw "Function missing: $name" }
  Invoke-Expression $fn.Extent.Text
}
$RuntimeDir='E:\IntRuoyi\output\runtime\int_main'
$RepoRoot='E:\IntRuoyi\IntRuoyiBackend'
$BackendPort=48081
$script:stopped=@()
$script:queryFailure=$false
$script:listeners=@()
$script:processes=@()
$script:exitBeforeStop=$false
$script:reusePid=$false
function Get-NetTCPConnection { param($State,$ErrorAction); if ($script:queryFailure) { throw 'Management query failed' }; $script:listeners }
function Get-CimInstance { param($ClassName,$Filter,$ErrorAction); if ($script:queryFailure) { throw 'Management query failed' }; if ($Filter) { if ($script:reusePid) { [pscustomobject]@{ProcessId=12345;Name='java.exe';CreationDate='different';CommandLine=$owned.CommandLine} } elseif (-not $script:exitBeforeStop) { $script:processes } } else { $script:processes } }
function Stop-Process { param($Id,[switch]$Force,$ErrorAction); $script:stopped+=@($Id); $script:listeners=@() }
function Fail($text) { throw $text }
function ExpectFailure([scriptblock]$Action,[string]$Reason) {
  $failed=$false
  try { & $Action } catch { $failed=$true }
  if (-not $failed) { throw "Expected failure: $Reason" }
}
function Check($value,$reason) { if (-not $value) { throw $reason } }
Stop-OwnedBackendListener
Check ($script:stopped.Count -eq 0) 'No listener must not stop anything'
$script:queryFailure=$true
ExpectFailure { Stop-OwnedBackendListener } 'Query failure must fail fast'
$script:queryFailure=$false
$script:listeners=@([pscustomobject]@{LocalPort=48081;OwningProcess=12345})
$owned=[pscustomobject]@{ProcessId=12345;Name='java.exe';CreationDate='2026-10-08T10:00:00';CommandLine='java -jar "E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-old.jar" --server.port=48081 --yudao.runtime-control.repo-root=E:\IntRuoyi\IntRuoyiBackend'}
$script:processes=@([pscustomobject]@{ProcessId=12345;Name='node.exe';CreationDate='2026-10-08T10:00:00';CommandLine='node other-task.js'})
ExpectFailure { Stop-OwnedBackendListener } 'Unknown owner'
Check ($script:stopped.Count -eq 0) 'Unknown process must never be stopped'
foreach ($command in @(
  $owned.CommandLine.Replace('runtime\int_main','runtime\other'),
  $owned.CommandLine.Replace('--server.port=48081','--server.port=48101'),
  $owned.CommandLine.Replace('repo-root=E:\IntRuoyi\IntRuoyiBackend','repo-root=E:\Other\IntRuoyiBackend')
)) {
  $script:processes=@([pscustomobject]@{ProcessId=12345;Name='java.exe';CreationDate=$owned.CreationDate;CommandLine=$command})
  ExpectFailure { Stop-OwnedBackendListener } 'Other profile/port/repository'
}
$script:processes=@($owned)
$script:reusePid=$true
ExpectFailure { Stop-OwnedBackendListener } 'PID reuse before stop'
Check ($script:stopped.Count -eq 0) 'PID reuse must never be stopped'
$script:reusePid=$false
Stop-OwnedBackendListener
Check ($script:stopped.Count -eq 1 -and $script:stopped[0] -eq 12345) 'Owned backend must stop only its verified PID'
$script:stopped=@(); $script:exitBeforeStop=$true; $script:listeners=@([pscustomobject]@{LocalPort=48081;OwningProcess=12345})
# A disappearing process must not produce an unverified stop; lingering listener fails safely.
ExpectFailure { Stop-OwnedBackendListener } 'Listener identity changed/lingering after exit'
Check ($script:stopped.Count -eq 0) 'Exit race must not stop a reused PID'
$script:exitBeforeStop=$false
$script:writes=0
function Invoke-LocalSqlScript { param($ScriptPath); $script:writes++ }
function Test-LocalTableExists { param($TableName); return $false }
function Test-LocalSqlProbe { param($Sql); return $false }
$RequiredLocalMySqlMigrations=@([pscustomobject]@{Name='required seed';ProbeSql='SELECT 1';ScriptPath='never.sql'})
$BackendSchemaReadOnly=$true
ExpectFailure { Ensure-RequiredLocalMySqlSchema } 'Missing seed read-only'
Check ($script:writes -eq 0) 'Read-only schema mode must never invoke migration/seed writes'
$RequiredLocalMySqlMigrations=@([pscustomobject]@{Name='required table';ProbeTable='required';ScriptPath='never.sql'})
ExpectFailure { Ensure-RequiredLocalMySqlSchema } 'Missing table read-only'
Check ($script:writes -eq 0) 'Read-only table mode must never invoke migration writes'
function Test-LocalTableExists { param($TableName); return $true }
Ensure-RequiredLocalMySqlSchema
Check ($script:writes -eq 0) 'Existing schema must pass read-only mode without writes'
$BackendSchemaReadOnly=$false
$script:probeCalls=0
function Test-LocalTableExists { param($TableName); $script:probeCalls++; return $script:probeCalls -gt 1 }
Ensure-RequiredLocalMySqlSchema
Check ($script:writes -eq 1) 'Normal schema migration behavior must remain available'
$backend=$taskAst.Find({param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Start-Backend'},$true).Extent.Text
Check ($backend.Contains('Stop-OwnedBackendListener') -and -not $backend.Contains('Stop-Port') -and -not $backend.Contains('Stop-MatchingProcesses')) 'Backend must use only verified listener stop'
Write-Output 'PASS restart-backend-safety: unknown/query/no-listener/owned/exit-race/read-only-schema; no real stop/start'
