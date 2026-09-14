Set-StrictMode -Version Latest

$script:BackupOpsValidationCatalog = @{
    'INTBK-1001' = 'Configuration file does not exist.'
    'INTBK-1002' = 'Configuration file content is not valid JSON.'
    'INTBK-1003' = 'Required configuration is missing.'
    'INTBK-1004' = 'User input is invalid.'
}

function New-BackupOpsValidationException {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet('INTBK-1001', 'INTBK-1002', 'INTBK-1003', 'INTBK-1004')]
        [string]$Code,

        [string]$Message = '',

        [string]$Target = '',

        [hashtable]$Context = @{}
    )

    $resolvedMessage = if ([string]::IsNullOrWhiteSpace($Message)) {
        $script:BackupOpsValidationCatalog[$Code]
    } else {
        $Message
    }

    $exception = [System.InvalidOperationException]::new("$Code $resolvedMessage")
    $exception.Data['Code'] = $Code
    $exception.Data['BackupOpsCode'] = $Code
    $exception.Data['BackupOpsStatus'] = 'blocked'
    if ($Target) {
        $exception.Data['Target'] = $Target
    }
    foreach ($key in $Context.Keys) {
        $exception.Data[$key] = $Context[$key]
    }
    return $exception
}

function Throw-BackupOpsValidationError {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet('INTBK-1001', 'INTBK-1002', 'INTBK-1003', 'INTBK-1004')]
        [string]$Code,

        [string]$Message = '',

        [string]$Target = '',

        [hashtable]$Context = @{}
    )

    throw (New-BackupOpsValidationException -Code $Code -Message $Message -Target $Target -Context $Context)
}

function Get-BackupOpsConfigValue {
    param(
        [Parameter(Mandatory = $true)]
        [object]$InputObject,

        [Parameter(Mandatory = $true)]
        [string[]]$Path
    )

    $current = $InputObject
    foreach ($segment in $Path) {
        if ($null -eq $current) {
            return $null
        }

        if ($current -is [System.Collections.IDictionary]) {
            if (-not $current.Contains($segment)) {
                return $null
            }
            $current = $current[$segment]
            continue
        }

        $property = $current.PSObject.Properties[$segment]
        if ($null -eq $property) {
            return $null
        }
        $current = $property.Value
    }

    return $current
}

function Assert-BackupOpsFileExists {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [string]$Label = 'Configuration file'
    )

    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path)) {
        Throw-BackupOpsValidationError -Code 'INTBK-1001' -Message "$Label not found: $Path" -Target $Path
    }
}

function ConvertFrom-BackupOpsJsonText {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$Text
    )

    try {
        return $Text | ConvertFrom-Json
    } catch {
        Throw-BackupOpsValidationError -Code 'INTBK-1002' -Message "Invalid JSON in ${Path}: $($_.Exception.Message)" -Target $Path
    }
}

function Assert-BackupOpsRequiredValue {
    param(
        [Parameter(Mandatory = $false)]
        [object]$Value,

        [Parameter(Mandatory = $true)]
        [string]$FieldName
    )

    if ($null -eq $Value) {
        Throw-BackupOpsValidationError -Code 'INTBK-1003' -Message "Required configuration is missing: $FieldName" -Target $FieldName
    }

    if ($Value -is [string] -and [string]::IsNullOrWhiteSpace($Value)) {
        Throw-BackupOpsValidationError -Code 'INTBK-1003' -Message "Required configuration is missing: $FieldName" -Target $FieldName
    }

    if ($Value -is [System.Collections.ICollection] -and $Value.Count -eq 0) {
        Throw-BackupOpsValidationError -Code 'INTBK-1003' -Message "Required configuration is missing: $FieldName" -Target $FieldName
    }
}

function Assert-BackupOpsSupportedMode {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Mode
    )

    $knownModes = @(
        'backup-now',
        'backup-scheduled',
        'rollback-app',
        'restore-data',
        'rehearsal'
    )

    if ($knownModes -notcontains $Mode) {
        Throw-BackupOpsValidationError -Code 'INTBK-1004' -Message "Unsupported mode: $Mode" -Target $Mode
    }
}

function Assert-BackupOpsMode {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Mode
    )

    Assert-BackupOpsSupportedMode -Mode $Mode
}

function Assert-BackupOpsUserChoice {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value,

        [Parameter(Mandatory = $true)]
        [string[]]$AllowedValues,

        [string]$FieldName = 'userChoice'
    )

    if ([string]::IsNullOrWhiteSpace($Value) -or $AllowedValues -notcontains $Value) {
        $allowed = $AllowedValues -join ', '
        Throw-BackupOpsValidationError -Code 'INTBK-1004' -Message "Invalid $FieldName. Allowed values: $allowed" -Target $FieldName
    }
}

function Assert-BackupOpsConfiguration {
    param(
        [Parameter(Mandatory = $true)]
        [object]$ConfigObject,

        [Parameter(Mandatory = $true)]
        [object]$SecretsObject
    )

    $requiredConfigPaths = @(
        @('schemaVersion'),
        @('environment'),
        @('servers', 'production', 'host'),
        @('servers', 'production', 'appDir'),
        @('servers', 'test', 'host'),
        @('servers', 'test', 'backupRoot'),
        @('servers', 'test', 'rehearsalRoot'),
        @('servers', 'test', 'rehearsalBackendPort'),
        @('servers', 'test', 'rehearsalFrontendPort'),
        @('containers', 'mysql'),
        @('containers', 'redis'),
        @('containers', 'backend'),
        @('containers', 'frontend'),
        @('tools', 'minioClientImage'),
        @('tools', 'archiveImage'),
        @('backup', 'fullSchedule'),
        @('backup', 'incrementalSchedule'),
        @('backup', 'repositoryEnvironment'),
        @('backup', 'maxFreshnessHours'),
        @('backup', 'localWorkspaceRoot'),
        @('backup', 'keepDaysRemote'),
        @('backup', 'keepDaysLocal'),
        @('backup', 'mysqlDatabase'),
        @('backup', 'mysqlBackupMode'),
        @('backup', 'exclusiveWriterMode'),
        @('backup', 'objectBucket'),
        @('rehearsal', 'schedule'),
        @('rehearsal', 'runtimeNamePrefix'),
        @('rehearsal', 'bucket'),
        @('rehearsal', 'validation', 'tenantId'),
        @('rehearsal', 'validation', 'fileConfigId'),
        @('rehearsal', 'validation', 'sampleFilePath'),
        @('console', 'title'),
        @('console', 'logRoot'),
        @('notify', 'enabled'),
        @('notify', 'channel')
    )

    foreach ($path in $requiredConfigPaths) {
        $value = Get-BackupOpsConfigValue -InputObject $ConfigObject -Path $path
        Assert-BackupOpsRequiredValue -Value $value -FieldName ($path -join '.')
    }

    $requiredSecretPaths = @(
        @('ssh', 'user'),
        @('auth', 'sshKeyPath'),
        @('rehearsal', 'tenantName'),
        @('rehearsal', 'username'),
        @('rehearsal', 'password')
    )

    foreach ($path in $requiredSecretPaths) {
        $value = Get-BackupOpsConfigValue -InputObject $SecretsObject -Path $path
        Assert-BackupOpsRequiredValue -Value $value -FieldName ($path -join '.')
    }

    $keepDaysRemote = [int](Get-BackupOpsConfigValue -InputObject $ConfigObject -Path @('backup', 'keepDaysRemote'))
    $keepDaysLocal = [int](Get-BackupOpsConfigValue -InputObject $ConfigObject -Path @('backup', 'keepDaysLocal'))
    if ($keepDaysRemote -lt $keepDaysLocal) {
        Throw-BackupOpsValidationError -Code 'INTBK-1003' -Message 'backup.keepDaysRemote must be greater than or equal to backup.keepDaysLocal.' -Target 'backup.keepDaysRemote'
    }

    if ([string]$ConfigObject.schemaVersion -ne 'v2-minimal') {
        Throw-BackupOpsValidationError -Code 'INTBK-1003' -Message 'schemaVersion must be v2-minimal.' -Target 'schemaVersion'
    }

    $fullSchedule = [string](Get-BackupOpsConfigValue -InputObject $ConfigObject -Path @('backup', 'fullSchedule'))
    if ($fullSchedule -notmatch '^(?:MON|TUE|WED|THU|FRI|SAT|SUN)\s(?:[01]\d|2[0-3]):[0-5]\d$') {
        Throw-BackupOpsValidationError -Code 'INTBK-1003' -Message 'backup.fullSchedule must use WEEKDAY HH:mm format.' -Target 'backup.fullSchedule'
    }

    $incrementalSchedule = [string](Get-BackupOpsConfigValue -InputObject $ConfigObject -Path @('backup', 'incrementalSchedule'))
    if ($incrementalSchedule -notmatch '^(?:[01]\d|2[0-3]):[0-5]\d$') {
        Throw-BackupOpsValidationError -Code 'INTBK-1003' -Message 'backup.incrementalSchedule must use HH:mm format.' -Target 'backup.incrementalSchedule'
    }
    $repositoryEnvironment = [string](Get-BackupOpsConfigValue -InputObject $ConfigObject -Path @('backup', 'repositoryEnvironment'))
    if ($repositoryEnvironment -ne 'test') {
        Throw-BackupOpsValidationError -Code 'INTBK-1003' -Message 'backup.repositoryEnvironment must be test in v2-minimal.' -Target 'backup.repositoryEnvironment'
    }
    if ([int]$ConfigObject.backup.maxFreshnessHours -le 0) {
        Throw-BackupOpsValidationError -Code 'INTBK-1003' -Message 'backup.maxFreshnessHours must be positive.' -Target 'backup.maxFreshnessHours'
    }
    if ([string]$ConfigObject.backup.mysqlBackupMode -ne 'binlog') {
        Throw-BackupOpsValidationError -Code 'INTBK-1003' -Message 'backup.mysqlBackupMode must be binlog for v2-minimal.' -Target 'backup.mysqlBackupMode'
    }
    if ([string]$ConfigObject.backup.exclusiveWriterMode -ne 'backend-container-stop') {
        Throw-BackupOpsValidationError -Code 'INTBK-1003' -Message 'backup.exclusiveWriterMode must be backend-container-stop for v2-minimal.' -Target 'backup.exclusiveWriterMode'
    }

    $rehearsalSchedule = [string](Get-BackupOpsConfigValue -InputObject $ConfigObject -Path @('rehearsal', 'schedule'))
    if ($rehearsalSchedule -notmatch '^(?:MON|TUE|WED|THU|FRI|SAT|SUN)\s(?:[01]\d|2[0-3]):[0-5]\d$') {
        Throw-BackupOpsValidationError -Code 'INTBK-1003' -Message 'rehearsal.schedule must use DDD HH:mm, for example SUN 02:00.' -Target 'rehearsal.schedule'
    }
}

Export-ModuleMember -Function @(
    'Assert-BackupOpsConfiguration',
    'Assert-BackupOpsFileExists',
    'Assert-BackupOpsMode',
    'Assert-BackupOpsRequiredValue',
    'Assert-BackupOpsSupportedMode',
    'Assert-BackupOpsUserChoice',
    'ConvertFrom-BackupOpsJsonText',
    'Get-BackupOpsConfigValue',
    'New-BackupOpsValidationException',
    'Throw-BackupOpsValidationError'
)
