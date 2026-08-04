param(
  [int] $OldPid = 20048
)

$ErrorActionPreference = 'Stop'

$runtimeDir = 'E:\IntRuoyi\output\runtime\int_main'
$oldJar = Join-Path $runtimeDir 'backend-runtime-control-20260804-dcc-upload-onlyoffice-document-url.jar'
$newJar = Join-Path $runtimeDir 'backend-runtime-control-20260804-dcc-nas-uncontrolled-import.jar'
$moduleJar = 'E:\IntRuoyi\IntRuoyiBackend\yudao-module-infra\target\yudao-module-infra-2026.04-SNAPSHOT.jar'
$stageRoot = 'E:\IntRuoyi\doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\artifacts'
$stageDir = Join-Path $stageRoot ('runtime-jar-stage-merged-' + [DateTime]::UtcNow.ToString('yyyyMMddHHmmss'))
$stageLibDir = Join-Path $stageDir 'BOOT-INF\lib'
$oldRuntimeExtractDir = Join-Path $stageDir 'old-runtime'
$currentInfraExtractDir = Join-Path $stageDir 'current-infra'
$mergedModuleJar = Join-Path $stageDir 'yudao-module-infra-2026.04-SNAPSHOT.jar'
$java = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe'
$logDir = Join-Path $runtimeDir 'logs'
$stdout = Join-Path $logDir 'dcc-nas-uncontrolled-import-stdout.log'
$stderr = Join-Path $logDir 'dcc-nas-uncontrolled-import-stderr.log'

function Get-ListeningPid {
  param([int] $Port)
  $lines = netstat.exe -ano | Select-String -Pattern (':' + $Port + '\s+.*LISTENING')
  foreach ($line in $lines) {
    $parts = $line.ToString().Trim() -split '\s+'
    if ($parts.Length -ge 5) {
      return [int] $parts[-1]
    }
  }
  return $null
}

$oldProcessLine = (jps -lv | Select-String -Pattern ('^' + $OldPid + '\s+') | Select-Object -First 1)
if (-not $oldProcessLine -or $oldProcessLine.ToString() -notlike '*backend-runtime-control-20260804-dcc-upload-onlyoffice-document-url.jar*') {
  throw "Old backend PID $OldPid does not match the expected int_main runtime jar"
}

$listenerPid = Get-ListeningPid -Port 48081
if ($listenerPid -ne $OldPid) {
  throw "Port 48081 listener PID $listenerPid does not match expected old backend PID $OldPid"
}

New-Item -ItemType Directory -Force $stageLibDir | Out-Null
New-Item -ItemType Directory -Force $oldRuntimeExtractDir | Out-Null
New-Item -ItemType Directory -Force $currentInfraExtractDir | Out-Null
New-Item -ItemType Directory -Force $logDir | Out-Null
Copy-Item -LiteralPath $oldJar -Destination $newJar -Force

Push-Location $oldRuntimeExtractDir
try {
  jar xf $oldJar BOOT-INF/lib/yudao-module-infra-2026.04-SNAPSHOT.jar
} finally {
  Pop-Location
}

Copy-Item -LiteralPath (Join-Path $oldRuntimeExtractDir 'BOOT-INF\lib\yudao-module-infra-2026.04-SNAPSHOT.jar') -Destination $mergedModuleJar -Force

$nasBrowserClasses = jar tf $moduleJar | Where-Object {
  $_ -like 'cn/iocoder/yudao/module/infra/service/file/NasBrowserServiceImpl*.class'
}
if (-not $nasBrowserClasses -or $nasBrowserClasses.Count -lt 3) {
  throw 'Rebuilt infra jar does not contain the expected NasBrowserServiceImpl classes'
}

Push-Location $currentInfraExtractDir
try {
  jar xf $moduleJar @nasBrowserClasses
} finally {
  Pop-Location
}

Push-Location $currentInfraExtractDir
try {
  jar uf $mergedModuleJar @nasBrowserClasses
} finally {
  Pop-Location
}

Copy-Item -LiteralPath $mergedModuleJar -Destination (Join-Path $stageLibDir 'yudao-module-infra-2026.04-SNAPSHOT.jar') -Force

Push-Location $stageDir
try {
  jar uf0 $newJar BOOT-INF/lib/yudao-module-infra-2026.04-SNAPSHOT.jar
} finally {
  Pop-Location
}

Stop-Process -Id $OldPid
for ($i = 0; $i -lt 30; $i++) {
  if (-not (Get-ListeningPid -Port 48081)) {
    break
  }
  Start-Sleep -Seconds 1
}

if (Get-ListeningPid -Port 48081) {
  throw 'Port 48081 is still occupied after stopping the old backend process'
}

$args = @(
  '-jar', $newJar,
  '--server.port=48081',
  '--spring.profiles.active=local',
  '--logging.file.name=E:\IntRuoyi\output\runtime\int_main\logs\yudao-server.log',
  '--yudao.runtime-control.repo-root=E:\IntRuoyi\IntRuoyiBackend',
  '--yudao.runtime-control.state-dir=E:\IntRuoyi\IntRuoyiBackend\runtime\runtime-control',
  '--yudao.runtime-control.storage-guard.log-dir=E:\IntRuoyi\output\runtime\int_main\logs'
)

$process = Start-Process -FilePath $java `
  -ArgumentList $args `
  -WorkingDirectory 'E:\IntRuoyi\IntRuoyiBackend' `
  -WindowStyle Hidden `
  -RedirectStandardOutput $stdout `
  -RedirectStandardError $stderr `
  -PassThru

Write-Output "STARTED_PID=$($process.Id)"
for ($i = 0; $i -lt 90; $i++) {
  try {
    $health = Invoke-RestMethod -Uri 'http://127.0.0.1:48081/actuator/health' -TimeoutSec 5
    if ($health.status -eq 'UP') {
      Write-Output 'HEALTH_UP'
      break
    }
  } catch {
  }
  Start-Sleep -Seconds 2
}

$listenerPid = Get-ListeningPid -Port 48081
if (-not $listenerPid) {
  throw 'Backend did not start listening on 48081'
}

Write-Output "LISTENER_PID=$listenerPid"
