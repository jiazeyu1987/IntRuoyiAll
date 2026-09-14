$ErrorActionPreference = 'Stop'

$script:RegistrarPath = Join-Path $PSScriptRoot '..\actions\Register-BackupOpsScheduledTasks.ps1'
$script:RegistrarSource = [System.IO.File]::ReadAllText(
    $script:RegistrarPath,
    [System.Text.UTF8Encoding]::new($false)
)

Describe 'Backup scheduled task registration contract' {
    It 'registers-the-validated-principal-with-s4u-limited' {
        $script:RegistrarSource | Should Match 'taskPrincipal\.principalId'
        $script:RegistrarSource | Should Match 'New-ScheduledTaskPrincipal'
        $script:RegistrarSource | Should Match '-LogonType\s+S4U'
        $script:RegistrarSource | Should Match '-RunLevel\s+Limited'
        $script:RegistrarSource | Should Match 'Register-ScheduledTask'
        $script:RegistrarSource | Should Match '-Principal\s+\$principal'
        $script:RegistrarSource | Should Not Match '(?i)-User(?:Id)?\s+["'']?(?:NT AUTHORITY\\)?SYSTEM\b'
        $script:RegistrarSource | Should Not Match '-RunLevel\s+Highest'
    }

    It 'blocks-missing-principal-id' {
        $script:RegistrarSource | Should Match 'taskPrincipal\.principalId is required'
        $script:RegistrarSource | Should Match 'IsNullOrWhiteSpace\(\$principalId\)'
    }

    It 'blocks-missing-batch-logon-right' {
        $script:RegistrarSource | Should Match 'Assert-BackupOpsBatchLogonRight'
        $script:RegistrarSource | Should Match 'Assert-BackupOpsBatchLogonRight\s+-PrincipalId\s+\$principalId'
    }

    It 'blocks-principal-acl-identity-mismatch' {
        $script:RegistrarSource | Should Match 'Assert-BackupOpsPrincipalAclIdentity'
        $script:RegistrarSource | Should Match 'Assert-BackupOpsPrincipalAclIdentity\s+-PrincipalId\s+\$principalId'
    }

    It 'blocks-ordinary-user-write-access' {
        $script:RegistrarSource | Should Match 'Assert-BackupOpsSecretsAcl'
        $script:RegistrarSource | Should Match '-RejectOrdinaryUserWrite'
    }

    It 'never-emits-password-or-confirmation' {
        $script:RegistrarSource | Should Not Match '(?i)-ProductionBackupConfirmText'
        $script:RegistrarSource | Should Not Match '(?i)-Password\b'
        $script:RegistrarSource | Should Match 'productionAuthorizationProof'
        $script:RegistrarSource | Should Match '(?i)masked'
    }

    It 'requires-explicit-repository-environment-in-the-scheduled-command' {
        $script:RegistrarSource | Should Match 'backup\.repositoryEnvironment'
        $script:RegistrarSource | Should Match "ValidateSet\('test',\s*'backup'\)"
        $script:RegistrarSource | Should Match '-RepositoryEnvironment\s+\$repositoryEnvironment'
        $script:RegistrarSource | Should Match "-TargetEnvironment\s+'prod'"
        $script:RegistrarSource | Should Match "'-NonInteractive'"
    }

    It 'blocks-missing-or-invalid-repository-environment' {
        $script:RegistrarSource | Should Match 'backup\.repositoryEnvironment is required'
        $script:RegistrarSource | Should Match 'Unsupported backup\.repositoryEnvironment'
    }
}
