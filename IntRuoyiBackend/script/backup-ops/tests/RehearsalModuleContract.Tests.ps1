Set-StrictMode -Version Latest

$rehearsalPath = Join-Path $PSScriptRoot '..\scripts\modules\UseCases\Rehearsal.psm1'

Describe 'Rehearsal module dependency contract' {
    It 'defines the Bash single-quote helper in its own module scope' {
        $source = Get-Content -Raw -Encoding utf8 $rehearsalPath

        $source | Should Match 'function ConvertTo-BackupBashSingleQuotedString'
    }
}
