param(
    [Parameter(Mandatory = $true)][ValidateSet('inspect', 'ack')][string]$Mode,
    [Parameter(Mandatory = $true)][ValidatePattern('^[a-zA-Z0-9.-]+$')][string]$ServerHost,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$ServerUser = 'root',
    [ValidateSet('/opt/intruoyi/runtime')][string]$RemoteAppDir = '/opt/intruoyi/runtime',
    [Parameter(Mandatory = $true)][string]$ReleaseWorkflowId,
    [Parameter(Mandatory = $true)][string]$JavaOperationId,
    [Parameter(Mandatory = $true)][string]$ReleaseTag,
    [Parameter(Mandatory = $true)][string]$ExpectedPackageDigest,
    [Parameter(Mandatory = $true)][string]$ExpectedManifestDigest,
    [string]$LeaseToken = '', [string]$ExpectedReceiptDigest = '',
    [string]$ConfirmationDecisionDigest = '', [string]$ConfirmText = ''
)
$ErrorActionPreference = 'Stop'
$encoding = [System.Text.UTF8Encoding]::new($false)
[Console]::InputEncoding = $encoding
[Console]::OutputEncoding = $encoding
$OutputEncoding = $encoding
if ($Mode -eq 'ack' -and $ConfirmText -cne 'PROD') { throw 'REVIEW_PUBLISH_ACK_PROD_REQUIRED' }
Import-Module (Join-Path $PSScriptRoot 'ReviewPublishReceipt.psm1')
$command = Get-ReviewPublishReceiptRemoteCommand -Action $Mode -ServerHost $ServerHost -RemoteAppDir $RemoteAppDir `
    -ReleaseWorkflowId $ReleaseWorkflowId -JavaOperationId $JavaOperationId -ReleaseTag $ReleaseTag `
    -ExpectedPackageDigest $ExpectedPackageDigest -ExpectedManifestDigest $ExpectedManifestDigest `
    -LeaseToken $LeaseToken -ExpectedReceiptDigest $ExpectedReceiptDigest -ConfirmationDecisionDigest $ConfirmationDecisionDigest
& ssh -n -o BatchMode=yes -o ConnectTimeout=10 "$ServerUser@$ServerHost" $command
if ($LASTEXITCODE -ne 0) { throw 'REVIEW_PUBLISH_ACK_UNVERIFIED: owner must remain isolated' }
