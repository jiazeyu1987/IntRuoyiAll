param(
  [Parameter(Mandatory = $true)]
  [string]$NewRuntime,

  [Parameter(Mandatory = $true)]
  [string]$OldRuntime
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $NewRuntime)) {
  throw "New runtime jar does not exist"
}

$conn = Get-NetTCPConnection -LocalPort 48081 -State Listen | Select-Object -First 1
if (-not $conn) {
  throw "48081 is not listening before restart"
}

$oldPid = [int]$conn.OwningProcess
$oldProc = Get-CimInstance Win32_Process -Filter "ProcessId=$oldPid"
if (-not $oldProc) {
  throw "48081 owning process was not found"
}

if (-not $oldProc.CommandLine.Contains($OldRuntime)) {
  throw "48081 process is not the expected old runtime jar"
}

if (-not $oldProc.CommandLine.Contains("yudao.runtime-control.repo-root=E:\IntRuoyi\IntRuoyiBackend")) {
  throw "48081 process repo-root is not E:\IntRuoyi\IntRuoyiBackend"
}

$javaMatch = [regex]::Match($oldProc.CommandLine, '^\s*"([^"]+java\.exe)"\s+(.*)$')
if (-not $javaMatch.Success) {
  throw "Unable to parse current Java command line"
}

$javaExe = $javaMatch.Groups[1].Value
$argsLine = $javaMatch.Groups[2].Value.Replace('"' + $OldRuntime + '"', '"' + $NewRuntime + '"')
if ($argsLine -notlike ("*" + (Split-Path -Leaf $NewRuntime) + "*")) {
  throw "New runtime jar was not inserted into command line"
}

Stop-Process -Id $oldPid
Wait-Process -Id $oldPid -Timeout 30 -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
if (Get-Process -Id $oldPid -ErrorAction SilentlyContinue) {
  throw "Old backend process did not stop within 30 seconds"
}

$newProc = Start-Process -FilePath $javaExe -ArgumentList $argsLine -WindowStyle Hidden -PassThru
$deadline = (Get-Date).AddSeconds(120)
$status = $null
while ((Get-Date) -lt $deadline) {
  try {
    $health = Invoke-RestMethod -Uri 'http://127.0.0.1:48081/actuator/health' -TimeoutSec 5
    $status = $health.status
    if ($status -eq 'UP') {
      break
    }
  } catch {
    Start-Sleep -Seconds 3
  }
}

if ($status -ne 'UP') {
  throw "Backend did not become UP after restart"
}

[PSCustomObject]@{
  oldPid = $oldPid
  newPid = $newProc.Id
  runtimeJar = $NewRuntime
  backendStatus = $status
} | ConvertTo-Json -Compress
