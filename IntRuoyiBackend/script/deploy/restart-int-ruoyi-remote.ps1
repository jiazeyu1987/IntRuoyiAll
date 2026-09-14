param(
    [string]$ServerHost,
    [string]$ServerUser = 'root',
    [string]$RemoteAppDir,
    [string]$RemoteDataRoot = '/var/lib/docker/intruoyi-data/runtime-data',
    [string]$RemoteDataDiskMount = '/var/lib/docker',
    [string]$RemoteDataDiskDevice = '/dev/vdb',
    [string]$RemoteMinioContainer = '',
    [ValidateSet('backend', 'frontend', 'full', 'website')]
    [string]$Component = 'full',
    [int]$FrontendPort = 8081,
    [int]$BackendPort = 48081,
    [int]$WebsiteHostPort = 8083,
    [string]$OperationRecordPath
)

$ErrorActionPreference = 'Stop'

$RemoteMysqlContainer = 'intruoyi-mysql'
$RemoteMysqlDatabase = 'ruoyi-vue-pro'
$RemoteMysqlUser = 'root'
$RemoteMysqlPassword = '123456'
$ShowroomMediaSampleObjects = @(
    'showroom/product/cover/20260530/product-product_001-cover.png',
    'showroom/narration/20260522/company-1-zh-ruoxi.wav'
)

function Fail([string]$Message) {
    Write-Host "[FAIL] $Message" -ForegroundColor Red
    exit 1
}

function Info([string]$Message) {
    Write-Host "[INFO] $Message"
}

function Update-OperationRecord {
    param(
        [string]$Status,
        [string]$Summary
    )
    if ([string]::IsNullOrWhiteSpace($OperationRecordPath) -or -not (Test-Path -LiteralPath $OperationRecordPath)) {
        return
    }
    $json = Get-Content -LiteralPath $OperationRecordPath -Encoding UTF8 -Raw
    $record = $json | ConvertFrom-Json
    $record.status = $Status
    $record.summary = $Summary
    $payload = $record | ConvertTo-Json -Depth 10
    [System.IO.File]::WriteAllText($OperationRecordPath, $payload, [System.Text.UTF8Encoding]::new($false))
}

function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        Fail "Missing $Name command"
    }
}

function Invoke-SshCommand {
    param([string]$Command)
    & ssh -o BatchMode=yes -o ConnectTimeout=5 -o StrictHostKeyChecking=no "$ServerUser@$ServerHost" $Command
    if ($LASTEXITCODE -ne 0) {
        Fail "SSH command failed: $Command"
    }
}

function ConvertTo-ShellSingleQuotedLiteral {
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyString()]
        [string]$Value,
        [string]$Purpose = 'shell literal'
    )

    if ($Value.Contains("'")) {
        Fail "$Purpose contains an unsupported single quote: $Value"
    }
    return "'$Value'"
}

function Assert-RemoteRuntimeDataOnDataDisk {
    $command = @"
set -eu
data_disk_source=`$(findmnt -n -o SOURCE --target '$RemoteDataDiskMount' 2>/dev/null || true)
data_dir_source=`$(df -P '$RemoteAppDir/data' 2>/dev/null | awk 'NR==2 {print `$1}')
if [ "`$data_disk_source" != '$RemoteDataDiskDevice' ]; then
  echo "Expected $RemoteDataDiskMount to be mounted from $RemoteDataDiskDevice, got: `$data_disk_source" >&2
  exit 1
fi
if [ "`$data_dir_source" != '$RemoteDataDiskDevice' ]; then
  echo "Expected $RemoteAppDir/data to be stored on $RemoteDataDiskDevice via $RemoteDataRoot, got: `$data_dir_source" >&2
  exit 1
fi
"@
    Invoke-SshCommand $command
}

function Assert-RemoteShowroomMediaBucketConsistency {
    $sampleObjectArguments = ($ShowroomMediaSampleObjects | ForEach-Object { "'$_'" }) -join ' '
    $command = @"
set -eu
bucket=`$(docker exec -e MYSQL_PWD=$RemoteMysqlPassword $RemoteMysqlContainer mysql -u$RemoteMysqlUser -N -B $RemoteMysqlDatabase -e 'SELECT JSON_UNQUOTE(JSON_EXTRACT(config, CAST(0x242e6275636b6574 AS CHAR CHARACTER SET utf8mb4))) FROM infra_file_config WHERE master = 1 AND deleted = 0 LIMIT 1')
if [ "`$bucket" = "NULL" ]; then
  bucket=''
fi
if [ -z "`$bucket" ]; then
  echo "Showroom media bucket consistency check failed: missing master infra_file_config bucket." >&2
  exit 1
fi
minio_running=`$(docker inspect -f '{{.State.Running}}' $RemoteMinioContainer 2>/dev/null || true)
if [ "`$minio_running" != "true" ]; then
  echo "Showroom media bucket consistency check failed: required MinIO container $RemoteMinioContainer is not running." >&2
  exit 1
fi
for object in $sampleObjectArguments; do
  if ! docker exec $RemoteMinioContainer sh -lc "test -f '/data/`$bucket/`$object' || test -f '/data/`$bucket/`$object/xl.meta'"; then
    echo "Showroom media bucket consistency check failed: master bucket '`$bucket' is missing object '`$object' in MinIO container $RemoteMinioContainer." >&2
    exit 1
  fi
done
"@
    Invoke-SshCommand $command
}

function Assert-RemoteOnlyOfficePublicFileBaseUrlReachable {
    $remoteAppDirLiteral = ConvertTo-ShellSingleQuotedLiteral -Value $RemoteAppDir -Purpose 'remote app directory'
    $command = @"
set -eu
cd $remoteAppDirLiteral
if [ ! -f .env ]; then
  echo "Missing remote runtime env file: $RemoteAppDir/.env" >&2
  exit 1
fi
public_file_base_url=`$(awk -F= '`$1 == "DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL" { sub(/^[^=]*=/, ""); print; found = 1; exit } END { if (found != 1) exit 2 }' .env)
if [ -z "`$public_file_base_url" ]; then
  echo "DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL is blank; remote OnlyOffice preview requires an explicit backend file URL." >&2
  exit 1
fi
case "`$public_file_base_url" in
  *host.docker.internal*)
    echo "DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL must not use host.docker.internal; remote OnlyOffice containers must reach the backend through the compose service name backend." >&2
    exit 1
    ;;
esac
health_url="`$(printf '%s' "`$public_file_base_url" | sed 's#/*`$##')/actuator/health"
if ! docker inspect intruoyi-onlyoffice >/dev/null 2>&1; then
  echo "Missing intruoyi-onlyoffice container; cannot verify OnlyOffice document-server file download path." >&2
  exit 1
fi
docker exec intruoyi-onlyoffice curl -fsS --connect-timeout 5 "`$health_url" >/dev/null
"@
    Invoke-SshCommand $command
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                Info "$Url returned HTTP $($response.StatusCode)"
                return
            }
        } catch {
        }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $deadline)

    Fail "$Url did not become ready within $TimeoutSeconds seconds"
}

if ([string]::IsNullOrWhiteSpace($ServerHost)) {
    Fail 'Missing ServerHost'
}

if ([string]::IsNullOrWhiteSpace($RemoteAppDir)) {
    Fail 'Missing RemoteAppDir'
}

Require-Command 'ssh'
Invoke-SshCommand "if [ -d '$RemoteAppDir' ]; then true; else echo 'Missing remote runtime dir: $RemoteAppDir' >&2; exit 1; fi"
Assert-RemoteRuntimeDataOnDataDisk
Invoke-SshCommand "command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1"

$serviceNames = switch ($Component) {
    'backend' { 'backend' }
    'frontend' { 'frontend' }
    'website' { 'website' }
    'full' { 'backend frontend' }
}

Update-OperationRecord -Status 'running' -Summary "Restarting remote $Component on $ServerHost"
Info "Restarting remote runtime on $ServerHost component $Component"
if ($Component -eq 'backend' -or $Component -eq 'full') {
    if ([string]::IsNullOrWhiteSpace($RemoteMinioContainer)) {
        Fail 'Missing -RemoteMinioContainer; remote backend restart requires an explicit MinIO container.'
    }
    Assert-RemoteShowroomMediaBucketConsistency
    Assert-RemoteOnlyOfficePublicFileBaseUrlReachable
}
if ($Component -eq 'website') {
    Invoke-SshCommand "cd '$RemoteAppDir' && docker compose restart website"
} else {
    Invoke-SshCommand "cd '$RemoteAppDir' && docker compose restart $serviceNames"
}

if ($Component -eq 'backend' -or $Component -eq 'full') {
    Wait-HttpOk -Url "http://${ServerHost}:$BackendPort/actuator/health" -TimeoutSeconds 180
}
if ($Component -eq 'frontend' -or $Component -eq 'full') {
    Wait-HttpOk -Url "http://${ServerHost}:$FrontendPort/" -TimeoutSeconds 180
}
if ($Component -eq 'website') {
    Wait-HttpOk -Url "http://${ServerHost}:$WebsiteHostPort/" -TimeoutSeconds 180
}

Write-Host ''
Write-Host 'Restart completed.'
Write-Host "Frontend: http://${ServerHost}:$FrontendPort"
Write-Host "Backend health: http://${ServerHost}:$BackendPort/actuator/health"
Write-Host "Website: http://${ServerHost}:$WebsiteHostPort"
Update-OperationRecord -Status 'succeeded' -Summary "Restart completed for remote $Component on $ServerHost"
