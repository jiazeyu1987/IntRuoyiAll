Set-StrictMode -Version Latest

function Get-RemoteRuntimeLeaseShell {
    param(
        [ValidateSet('Acquire', 'Fence', 'Release')][string]$Action,
        [Parameter(Mandatory = $true)][string]$Token,
        [string]$Command = 'true',
        [string]$LockRoot = '/opt/intruoyi/runtime/.runtime-control-resource',
        [switch]$CaptureRuntimeBaseline,
        [string]$ExecutorIdentityBase64 = '',
        [string]$RestoreIsolationMarkerPath = ''
    )
    if ($Token -cnotmatch '^[0-9a-f]{32}$' -or $LockRoot -notmatch '^/[a-zA-Z0-9/_.-]+$' -or $LockRoot -match '(^|/)\.{1,2}(/|$)') {
        throw 'RUNTIME_RESOURCE_LEASE_IDENTITY_INVALID'
    }
    $header = "set -eu`nexec 9>'$LockRoot.guard'`n"
    if ($Action -eq 'Acquire') {
        $baseline = ''
        $receiptRuntimePreflight = ''
        if ($CaptureRuntimeBaseline) {
            if ($ExecutorIdentityBase64 -notmatch '^[a-zA-Z0-9+/=]+$') { throw 'RUNTIME_RESOURCE_EXECUTOR_REQUIRED' }
            if ($RestoreIsolationMarkerPath -notmatch '^/[a-zA-Z0-9/_.-]+/\.restore-stage/restore-isolation\.json$' -or $RestoreIsolationMarkerPath -match '(^|/)\.{1,2}(/|$)') {
                throw 'RUNTIME_RESOURCE_RESTORE_MARKER_CONFIG_REQUIRED'
            }
            $markerRoot = $RestoreIsolationMarkerPath.Substring(0, $RestoreIsolationMarkerPath.Length - '/.restore-stage/restore-isolation.json'.Length)
            $baseline = "set -o pipefail`n(sha256sum /opt/intruoyi/runtime/.env /opt/intruoyi/runtime/docker-compose.yml && docker inspect --format '{{.Config.Image}}' intruoyi-backend intruoyi-frontend) | sha256sum | cut -d' ' -f1 > '$LockRoot.baseline'`n" +
                "printf '%s' '$ExecutorIdentityBase64' > '$LockRoot.identity'`n"
            $baseline = "test -d '$markerRoot' || { echo RUNTIME_RESOURCE_RESTORE_MARKER_ROOT_MISSING >&2; exit 75; }`n" +
                "test ! -e '$RestoreIsolationMarkerPath' || { echo RUNTIME_RESOURCE_RESTORE_ISOLATION_ACTIVE >&2; exit 75; }`n" + $baseline
            $receiptRuntimePreflight = "flock --version >/dev/null || { echo RUNTIME_RESOURCE_FLOCK_RUNTIME_UNAVAILABLE >&2; exit 75; }`n" +
                "python3 -B -c 'import base64,contextlib,fcntl,hashlib,json,os,pathlib,re,subprocess,sys,tempfile,urllib.request; assert sys.version_info >= (3,6); assert isinstance(os.O_DIRECTORY,int)' || { echo RUNTIME_RESOURCE_RECEIPT_RUNTIME_UNAVAILABLE >&2; exit 75; }`n"
            $schemaCapture = @'
runtime_mysql_password=$(sed -n 's/^MYSQL_ROOT_PASSWORD=//p' /opt/intruoyi/runtime/.env)
test -n "$runtime_mysql_password"
docker exec -e MYSQL_PWD="$runtime_mysql_password" intruoyi-mysql mysqldump -uroot --no-data --skip-comments --skip-lock-tables --no-tablespaces --set-gtid-purged=OFF --routines --triggers --events ruoyi-vue-pro | sed -E 's/AUTO_INCREMENT=[0-9]+//g' | sha256sum | cut -d' ' -f1 > '__LOCK_ROOT__.schema'
'@
            $baseline += $schemaCapture.Replace('__LOCK_ROOT__', $LockRoot) + "`n"
        }
        return $receiptRuntimePreflight + $header + "flock -xn 9 || { echo RUNTIME_RESOURCE_BUSY >&2; exit 73; }`n" +
            "test ! -e '$LockRoot.owner' || { echo RUNTIME_RESOURCE_RECOVERY_REQUIRED >&2; exit 73; }`n" +
            "umask 077`n" + $baseline + "printf '%s' '$Token' > '$LockRoot.owner'`nprintf RUNTIME_LEASE_ACQUIRED"
    }
    $lock = if ($Action -eq 'Release') { 'flock -xn 9' } else { 'flock -sn 9' }
    $check = "$lock || { echo RUNTIME_RESOURCE_BUSY >&2; exit 73; }`n" +
        "test -f '$LockRoot.owner' && test `"`$(cat '$LockRoot.owner')`" = '$Token' || { echo RUNTIME_RESOURCE_LEASE_LOST >&2; exit 74; }`n"
    if ($Action -eq 'Release') {
        return $header + $check + "rm -- '$LockRoot.owner'"
    }
    $payload = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes(($Command -replace "`r`n", "`n")))
    # Child inherits descriptor 9: recovery/release cannot overtake this command.
    return $header + $check + "bash -c `"`$(printf '%s' '$payload' | base64 -d)`""
}

function New-RemoteRuntimeLease {
    param([string]$TargetHost, [string]$User, [string[]]$SshOptions = @(), [string]$RestoreIsolationMarkerPath)
    if ($TargetHost -notmatch '^[a-zA-Z0-9.-]+$' -or $User -notmatch '^[a-zA-Z0-9_-]+$') {
        throw 'RUNTIME_RESOURCE_TARGET_INVALID'
    }
    $lease = @{ TargetHost = $TargetHost; User = $User; SshOptions = $SshOptions; Token = [guid]::NewGuid().ToString('N') }
    $executor = "$([string]$env:COMPUTERNAME)|$PID|$([string]$env:USERNAME)"
    $identity = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($executor))
    $command = Get-RemoteRuntimeLeaseShell -Action Acquire -Token $lease.Token -CaptureRuntimeBaseline -ExecutorIdentityBase64 $identity -RestoreIsolationMarkerPath $RestoreIsolationMarkerPath
    # Persist candidate identity in the operation log before dispatch, including uncertain SSH outcomes.
    Write-Host "RUNTIME_RESOURCE_LEASE_TOKEN=$($lease.Token) RUNTIME_RESOURCE_HOST=$TargetHost"
    Write-Host "RUNTIME_RESOURCE_EXECUTOR_HOST=$([string]$env:COMPUTERNAME) RUNTIME_RESOURCE_EXECUTOR_PID=$PID RUNTIME_RESOURCE_EXECUTOR_IDENTITY=$([string]$env:USERNAME)"
    $previousResourceErrorPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = & ssh @SshOptions "${User}@${TargetHost}" $command 2>&1
        $resourceExitCode = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousResourceErrorPreference }
    $resourceOutput = ($output | ForEach-Object { [string]$_ }) -join "`n"
    if ($resourceExitCode -ne 0 -or $resourceOutput -ne 'RUNTIME_LEASE_ACQUIRED') {
        $knownRejections = @('RUNTIME_RESOURCE_BUSY', 'RUNTIME_RESOURCE_RECOVERY_REQUIRED',
            'RUNTIME_RESOURCE_RESTORE_MARKER_ROOT_MISSING', 'RUNTIME_RESOURCE_RESTORE_ISOLATION_ACTIVE',
            'RUNTIME_RESOURCE_RECEIPT_RUNTIME_UNAVAILABLE', 'RUNTIME_RESOURCE_FLOCK_RUNTIME_UNAVAILABLE')
        $knownRejection = @($resourceOutput -split "`r?`n" | Where-Object { $_.Trim() -in $knownRejections }).Count -gt 0
        if ($resourceExitCode -in @(73, 75) -and $knownRejection -and $resourceOutput -notmatch 'RUNTIME_LEASE_ACQUIRED') {
            Write-Host 'RELEASE_WORKFLOW_ZERO_WRITE_REJECTED=RESOURCE_ACQUIRE'
        }
        $safeResourceOutput = $resourceOutput -replace '(?i)(MYSQL_PWD=|MYSQL_ROOT_PASSWORD=)\S+', '$1<redacted>'
        Write-Host "RUNTIME_RESOURCE_ACQUIRE_EXIT_CODE=$resourceExitCode $safeResourceOutput"
        throw 'RUNTIME_RESOURCE_ACQUIRE_FAILED: occupied or uncertain; inspect persistent remote owner before retry'
    }
    return $lease
}

function Complete-RemoteRuntimeLease {
    param([Parameter(Mandatory = $true)][hashtable]$Lease)
    $options = $Lease.SshOptions
    & ssh @options "$($Lease.User)@$($Lease.TargetHost)" (Get-RemoteRuntimeLeaseShell -Action Release -Token $Lease.Token)
    if ($LASTEXITCODE -ne 0) { throw 'RUNTIME_RESOURCE_RELEASE_FAILED: retain recovery isolation' }
}

function Send-RemoteRuntimeLeaseFile {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Lease,
        [Parameter(Mandatory = $true)][string]$LocalPath,
        [Parameter(Mandatory = $true)][string]$RemotePath,
        [switch]$Recursive,
        [int]$TimeoutSeconds = 3600
    )
    if ($RemotePath -notmatch '^/[a-zA-Z0-9/_.-]+$') { throw 'RUNTIME_RESOURCE_UPLOAD_PATH_INVALID' }
    $item = Get-Item -LiteralPath $LocalPath -ErrorAction Stop
    $temporaryArchive = $null
    $stream = $null
    $process = $null
    try {
        $uploadPath = $item.FullName
        if ($item.PSIsContainer) {
            if (-not $Recursive) { throw 'RUNTIME_RESOURCE_DIRECTORY_REQUIRES_RECURSIVE' }
            $temporaryArchive = Join-Path ([System.IO.Path]::GetTempPath()) ('runtime-upload-' + [guid]::NewGuid().ToString('N') + '.tar')
            & tar -cf $temporaryArchive -C $item.Parent.FullName $item.Name
            if ($LASTEXITCODE -ne 0) { throw 'RUNTIME_RESOURCE_UPLOAD_ARCHIVE_FAILED' }
            $uploadPath = $temporaryArchive
            $remoteWrite = "test -d '$RemotePath'; tar -xf - -C '$RemotePath'"
        } else {
            if ($item.Name -notmatch '^[a-zA-Z0-9_.-]+$') { throw 'RUNTIME_RESOURCE_UPLOAD_NAME_INVALID' }
            $remoteWrite = "destination='$RemotePath'; if test -d '$RemotePath'; then destination='$($RemotePath.TrimEnd('/'))/$($item.Name)'; fi; cat > `"`$destination`""
        }
        # stdin and the flock descriptor belong to the SAME remote writer process.
        # Connection loss cannot detach an unfenced scp writer.
        $command = Get-RemoteRuntimeLeaseShell -Action Fence -Token $Lease.Token -Command $remoteWrite
        $arguments = @($Lease.SshOptions | Where-Object { $_ -ne '-n' }) + @("$($Lease.User)@$($Lease.TargetHost)", $command)
        $start = [System.Diagnostics.ProcessStartInfo]::new()
        $start.FileName = 'ssh'
        $start.UseShellExecute = $false
        $start.CreateNoWindow = $true
        $start.RedirectStandardInput = $true
        $start.RedirectStandardOutput = $true
        $start.RedirectStandardError = $true
        $start.Arguments = (@($arguments | ForEach-Object { '"' + ($_ -replace '(\\*)"', '$1$1\"' -replace '(\\+)$', '$1$1') + '"' }) -join ' ')
        $process = [System.Diagnostics.Process]::Start($start)
        $errors = $process.StandardError.ReadToEndAsync()
        $output = $process.StandardOutput.ReadToEndAsync()
        $stream = [System.IO.File]::OpenRead($uploadPath)
        $copy = $stream.CopyToAsync($process.StandardInput.BaseStream)
        if (-not $copy.Wait($TimeoutSeconds * 1000)) { throw 'RUNTIME_RESOURCE_UPLOAD_TIMEOUT: recovery required' }
        $process.StandardInput.Close()
        if (-not $process.WaitForExit($TimeoutSeconds * 1000) -or $process.ExitCode -ne 0) {
            throw 'RUNTIME_RESOURCE_UPLOAD_FAILED: recovery required'
        }
        return @{ ExitCode = 0; StdOut = $output.Result; StdErr = $errors.Result }
    } finally {
        if ($null -ne $process) {
            if (-not $process.HasExited) { $process.Kill(); $process.WaitForExit() }
            $process.Dispose()
        }
        if ($null -ne $stream) { $stream.Dispose() }
        if ($null -ne $temporaryArchive -and (Test-Path -LiteralPath $temporaryArchive)) {
            Remove-Item -LiteralPath $temporaryArchive -Force
        }
    }
}

Export-ModuleMember -Function Get-RemoteRuntimeLeaseShell, New-RemoteRuntimeLease, Complete-RemoteRuntimeLease, Send-RemoteRuntimeLeaseFile
