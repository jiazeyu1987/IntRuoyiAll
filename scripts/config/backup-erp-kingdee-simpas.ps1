[CmdletBinding()]
param(
    [ValidateSet('Backup', 'Restore', 'Verify')]
    [string]$Mode = 'Backup',
    [string]$SnapshotPath = (Join-Path $PSScriptRoot '..\..\config\recovery\erp-kingdee-simpas.local.aes.json'),
    [string]$KeyPath = (Join-Path $env:LOCALAPPDATA 'IntRuoyi\secrets\erp-kingdee-simpas-backup.key')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$container = 'int-ruoyi-mysql'
$database = 'ruoyi-vue-pro'
$configKeys = @(
    'yudao.erp.kingdee.config',
    'yudao.erp.kingdee.connection.active',
    'yudao.erp.kingdee.connection.production'
)

function Invoke-LocalMySql {
    param([Parameter(Mandatory = $true)][string]$Sql)

    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = 'docker'
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.UseShellExecute = $false
    foreach ($argument in @('exec', '-i', $container, 'sh', '-lc',
            'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 -N -B ruoyi-vue-pro')) {
        [void]$startInfo.ArgumentList.Add($argument)
    }
    $process = [Diagnostics.Process]::Start($startInfo)
    $process.StandardInput.Write($Sql)
    $process.StandardInput.Close()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        throw "Local MySQL command failed: $stderr"
    }
    return $stdout.Trim()
}

function Get-OrCreateKey {
    $keyDirectory = Split-Path -Parent $KeyPath
    if (-not (Test-Path -LiteralPath $keyDirectory)) {
        New-Item -ItemType Directory -Force -Path $keyDirectory | Out-Null
    }
    if (-not (Test-Path -LiteralPath $KeyPath)) {
        $key = New-Object byte[] 32
        [Security.Cryptography.RandomNumberGenerator]::Fill($key)
        [IO.File]::WriteAllBytes($KeyPath, $key)
    }
    $key = [IO.File]::ReadAllBytes($KeyPath)
    if ($key.Length -ne 32) {
        throw 'ERP recovery key must contain exactly 32 bytes.'
    }
    return $key
}

function Get-ConfigRecords {
    $quotedKeys = ($configKeys | ForEach-Object { "'$_'" }) -join ', '
    $sql = @"
SELECT REPLACE(TO_BASE64(JSON_OBJECT(
  'id', id, 'category', category, 'type', type, 'name', name,
  'configKey', config_key, 'value', value, 'visible', CAST(visible AS UNSIGNED),
  'remark', remark, 'creator', creator, 'updater', updater, 'deleted', CAST(deleted AS UNSIGNED)
)), CHAR(10), '')
FROM infra_config
WHERE config_key IN ($quotedKeys) AND deleted = b'0'
ORDER BY config_key;
"@
    $result = Invoke-LocalMySql -Sql $sql
    $records = @($result -split "`r?`n" | Where-Object { $_ } | ForEach-Object {
        [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_)) | ConvertFrom-Json
    })
    Assert-ConfigRecords -Records $records
    return $records
}

function Assert-ConfigRecords {
    param([Parameter(Mandatory = $true)][object[]]$Records)

    if ($Records.Count -ne $configKeys.Count) {
        throw "Expected $($configKeys.Count) ERP config records, found $($Records.Count)."
    }
    $actualKeys = @($Records | ForEach-Object { [string]$_.configKey } | Sort-Object)
    $expectedKeys = @($configKeys | Sort-Object)
    if ((Compare-Object $actualKeys $expectedKeys)) {
        throw 'ERP config snapshot keys do not match the required set.'
    }
}

function Protect-Payload {
    param([Parameter(Mandatory = $true)][byte[]]$Plaintext, [Parameter(Mandatory = $true)][byte[]]$Key)

    $nonce = New-Object byte[] 12
    $tag = New-Object byte[] 16
    $ciphertext = New-Object byte[] $Plaintext.Length
    [Security.Cryptography.RandomNumberGenerator]::Fill($nonce)
    $aes = [Security.Cryptography.AesGcm]::new($Key, 16)
    try {
        $aes.Encrypt($nonce, $Plaintext, $ciphertext, $tag)
    } finally {
        $aes.Dispose()
    }
    return [ordered]@{
        version = 1
        algorithm = 'AES-256-GCM'
        nonce = [Convert]::ToBase64String($nonce)
        tag = [Convert]::ToBase64String($tag)
        ciphertext = [Convert]::ToBase64String($ciphertext)
    }
}

function Unprotect-Payload {
    param([Parameter(Mandatory = $true)][object]$Envelope, [Parameter(Mandatory = $true)][byte[]]$Key)

    if ($Envelope.version -ne 1 -or $Envelope.algorithm -ne 'AES-256-GCM') {
        throw 'Unsupported ERP config recovery snapshot format.'
    }
    $nonce = [Convert]::FromBase64String([string]$Envelope.nonce)
    $tag = [Convert]::FromBase64String([string]$Envelope.tag)
    $ciphertext = [Convert]::FromBase64String([string]$Envelope.ciphertext)
    $plaintext = New-Object byte[] $ciphertext.Length
    $aes = [Security.Cryptography.AesGcm]::new($Key, 16)
    try {
        $aes.Decrypt($nonce, $ciphertext, $tag, $plaintext)
    } finally {
        $aes.Dispose()
    }
    return [Text.Encoding]::UTF8.GetString($plaintext) | ConvertFrom-Json
}

function Get-SnapshotRecords {
    if (-not (Test-Path -LiteralPath $SnapshotPath)) {
        throw "ERP recovery snapshot does not exist: $SnapshotPath"
    }
    $envelope = Get-Content -LiteralPath $SnapshotPath -Raw -Encoding utf8 | ConvertFrom-Json
    $payload = Unprotect-Payload -Envelope $envelope -Key (Get-OrCreateKey)
    $records = @($payload.records)
    Assert-ConfigRecords -Records $records
    return $records
}

function Convert-ToSqlValue {
    param([AllowNull()][string]$Value)
    if ($null -eq $Value) {
        return 'NULL'
    }
    return "FROM_BASE64('$([Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Value)))')"
}

function Get-RecordsFingerprint {
    param([Parameter(Mandatory = $true)][object[]]$Records)

    $canonical = @($Records | Sort-Object configKey | ForEach-Object {
        [ordered]@{
            id = [string]$_.id
            category = [string]$_.category
            type = [string]$_.type
            name = [string]$_.name
            configKey = [string]$_.configKey
            value = [string]$_.value
            visible = [string]$_.visible
            remark = [string]$_.remark
            creator = [string]$_.creator
            updater = [string]$_.updater
            deleted = [string]$_.deleted
        } | ConvertTo-Json -Compress
    }) -join "`n"
    $bytes = [Text.Encoding]::UTF8.GetBytes($canonical)
    return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes))
}

if ($Mode -eq 'Backup') {
    $records = Get-ConfigRecords
    $payload = [ordered]@{
        createdAt = [DateTime]::UtcNow.ToString('o')
        records = $records
    }
    $plaintext = [Text.Encoding]::UTF8.GetBytes(($payload | ConvertTo-Json -Depth 8 -Compress))
    $snapshotDirectory = Split-Path -Parent $SnapshotPath
    if (-not (Test-Path -LiteralPath $snapshotDirectory)) {
        New-Item -ItemType Directory -Force -Path $snapshotDirectory | Out-Null
    }
    (Protect-Payload -Plaintext $plaintext -Key (Get-OrCreateKey) | ConvertTo-Json -Depth 4) |
            Set-Content -LiteralPath $SnapshotPath -Encoding utf8
    Write-Output "ERP config backup created: $SnapshotPath"
    exit 0
}

$records = Get-SnapshotRecords
if ($Mode -eq 'Verify') {
    Write-Output 'ERP config recovery snapshot verified.'
    exit 0
}

$values = foreach ($record in $records) {
    "($($record.id), $(Convert-ToSqlValue $record.category), $($record.type), " +
            "$(Convert-ToSqlValue $record.name), $(Convert-ToSqlValue $record.configKey), " +
            "$(Convert-ToSqlValue $record.value), b'$($record.visible)', $(Convert-ToSqlValue $record.remark), " +
            "$(Convert-ToSqlValue $record.creator), NOW(), $(Convert-ToSqlValue $record.updater), NOW(), b'$($record.deleted)')"
}
$quotedKeys = ($configKeys | ForEach-Object { "'$_'" }) -join ', '
$restoreSql = @"
START TRANSACTION;
DELETE FROM infra_config WHERE config_key IN ($quotedKeys);
INSERT INTO infra_config (id, category, type, name, config_key, value, visible, remark, creator, create_time, updater, update_time, deleted)
VALUES
$($values -join ",`n");
COMMIT;
"@
Invoke-LocalMySql -Sql $restoreSql | Out-Null
$restored = Get-ConfigRecords
if ((Get-RecordsFingerprint $restored) -ne (Get-RecordsFingerprint $records)) {
    throw 'ERP config restore verification failed.'
}
Write-Output 'ERP config recovery snapshot restored and verified.'
