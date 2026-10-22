Set-StrictMode -Version Latest

$rehearsalPath = Join-Path $PSScriptRoot '..\scripts\modules\UseCases\Rehearsal.psm1'
$dockerOpsPath = Join-Path $PSScriptRoot '..\scripts\modules\Infra\DockerOps.psm1'

Describe 'Rehearsal module dependency contract' {
    It 'defines the Bash single-quote helper in its own module scope' {
        $source = Get-Content -Raw -Encoding utf8 $rehearsalPath

        $source | Should Match 'function ConvertTo-BackupBashSingleQuotedString'
    }

    It 'exports the remote file reader used for evidence write-back' {
        $source = Get-Content -Raw -Encoding utf8 $dockerOpsPath

        $source | Should Match 'Export-ModuleMember[^\r\n]*Get-BackupOpsRemoteFileText'
    }
}
