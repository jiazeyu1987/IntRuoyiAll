Set-StrictMode -Version Latest

function Get-ReviewPublishReceiptRemoteCommand {
    param(
        [ValidateSet('prepare', 'inspect', 'ack')][string]$Action,
        [string]$ServerHost, [string]$ReleaseWorkflowId, [string]$JavaOperationId,
        [string]$ReleaseTag, [string]$ExpectedPackageDigest, [string]$ExpectedManifestDigest,
        [string]$LeaseToken = '', [string]$ExpectedReceiptDigest = '', [string]$ConfirmationDecisionDigest = '',
        [ValidateSet('/opt/intruoyi/runtime')][string]$RemoteAppDir = '/opt/intruoyi/runtime'
    )
    if ($ServerHost -notmatch '^[a-zA-Z0-9.-]+$' -or
        $ReleaseWorkflowId -notmatch '^[a-zA-Z0-9][a-zA-Z0-9._:-]{2,127}$' -or
        $JavaOperationId -notmatch '^(?:op-)?[a-z0-9-]{8,64}$' -or
        $ReleaseTag -notmatch '^[a-zA-Z0-9][a-zA-Z0-9._-]{2,127}$' -or
        $ExpectedPackageDigest -cnotmatch '^[0-9a-f]{64}$' -or $ExpectedManifestDigest -cnotmatch '^[0-9a-f]{64}$') {
        throw 'REVIEW_PUBLISH_RECEIPT_BINDING_REQUIRED'
    }
    if (($Action -ne 'inspect' -or $LeaseToken) -and $LeaseToken -cnotmatch '^[0-9a-f]{32}$') {
        throw 'REVIEW_PUBLISH_RECEIPT_OWNER_REQUIRED'
    }
    if ($Action -eq 'ack' -and ($ExpectedReceiptDigest -cnotmatch '^[0-9a-f]{64}$' -or
        $ConfirmationDecisionDigest -cnotmatch '^[0-9a-f]{64}$')) {
        throw 'REVIEW_PUBLISH_RECEIPT_DECISION_REQUIRED'
    }
    $binding = [ordered]@{ workflowId = $ReleaseWorkflowId; operationId = $JavaOperationId; releaseTag = $ReleaseTag;
        packageDigest = $ExpectedPackageDigest; manifestDigest = $ExpectedManifestDigest;
        targetEnvironment = 'backup'; targetHost = $ServerHost; runtimeDir = $RemoteAppDir }
    if ($LeaseToken) { $binding.leaseToken = $LeaseToken }
    $config = @{ action = $Action; binding = $binding; expectedReceiptDigest = $ExpectedReceiptDigest;
        confirmationDecisionDigest = $ConfirmationDecisionDigest }
    $utf8 = [System.Text.UTF8Encoding]::new($false)
    $payload = [Convert]::ToBase64String($utf8.GetBytes(($config | ConvertTo-Json -Depth 8 -Compress)))
    $program = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes((Join-Path $PSScriptRoot 'review_publish_receipt.py')))
    return "python3 -c `"`$(printf '%s' '$program' | base64 -d)`" '$payload'"
}

function Write-ReviewPublishReceipt {
    param([hashtable]$Lease, [string]$ReleaseWorkflowId, [string]$JavaOperationId,
        [string]$ReleaseTag, [string]$ExpectedPackageDigest, [string]$ExpectedManifestDigest)
    $command = Get-ReviewPublishReceiptRemoteCommand -Action prepare -ServerHost $Lease.TargetHost -LeaseToken $Lease.Token `
        -ReleaseWorkflowId $ReleaseWorkflowId -JavaOperationId $JavaOperationId -ReleaseTag $ReleaseTag `
        -ExpectedPackageDigest $ExpectedPackageDigest -ExpectedManifestDigest $ExpectedManifestDigest
    $options = $Lease.SshOptions
    & ssh @options "$($Lease.User)@$($Lease.TargetHost)" $command
    if ($LASTEXITCODE -ne 0) { throw 'REVIEW_PUBLISH_RECEIPT_WRITE_UNVERIFIED: owner retained' }
}

function Test-ReviewReceiptRuntime {
    if (-not (Get-Command wsl.exe -ErrorAction SilentlyContinue)) {
        throw 'REVIEW_RECEIPT_TEST_RUNTIME_UNAVAILABLE: WSL Ubuntu with Python3 and flock is required'
    }
    $previousProbePreference = $ErrorActionPreference
    $previousWslUtf8 = $env:WSL_UTF8
    try {
        $env:WSL_UTF8 = '1'
        $ErrorActionPreference = 'Continue'
        $probe = & wsl.exe -d Ubuntu --exec python3 -B -c "import fcntl,subprocess; subprocess.run(['flock','--version'],check=True,stdout=subprocess.DEVNULL); subprocess.run(['sha256sum','--version'],check=True,stdout=subprocess.DEVNULL); print('REVIEW_RECEIPT_TEST_RUNTIME_READY')" 2>&1
        $probeExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousProbePreference
        $env:WSL_UTF8 = $previousWslUtf8
    }
    if ($probeExitCode -ne 0 -or @($probe | ForEach-Object { ([string]$_).Trim() }) -notcontains 'REVIEW_RECEIPT_TEST_RUNTIME_READY') {
        throw 'REVIEW_RECEIPT_TEST_RUNTIME_UNAVAILABLE: WSL Ubuntu Python3/fcntl/flock/sha256sum probe failed'
    }
    Write-Host 'REVIEW_RECEIPT_TEST_RUNTIME_READY'
}

Export-ModuleMember -Function Get-ReviewPublishReceiptRemoteCommand, Write-ReviewPublishReceipt, Test-ReviewReceiptRuntime
