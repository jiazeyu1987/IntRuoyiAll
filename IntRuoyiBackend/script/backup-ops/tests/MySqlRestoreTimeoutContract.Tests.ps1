$ErrorActionPreference = 'Stop'

Describe 'MySQL restore integrity timeout' {
    It 'allows large dump integrity verification to exceed the short SSH timeout' {
        $source = Get-Content -Raw -Encoding utf8 (Join-Path $PSScriptRoot '..\scripts\modules\Infra\MySqlOps.psm1')

        $source | Should Match 'Command = \$restoreSpec\.integrityCommand\s+TimeoutSeconds = 7200'
    }
}
