$ErrorActionPreference = 'Stop'
$launcherPath = Join-Path $PSScriptRoot '..\runtime\start-branch-backend.ps1'
$tokens = $null
$parseErrors = $null
$ast = [System.Management.Automation.Language.Parser]::ParseFile($launcherPath, [ref]$tokens, [ref]$parseErrors)
if ($parseErrors.Count) { throw 'Launcher parse failed' }
$function = $ast.Find({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Import-IntAuthServiceToken' }, $true)
. ([scriptblock]::Create($function.Extent.Text))
$previous = $env:INTERNAL_SERVICE_TOKEN
$fixture = Join-Path ([System.IO.Path]::GetTempPath()) ([guid]::NewGuid().ToString() + '.env')
try {
    [System.IO.File]::WriteAllText($fixture, "OTHER_KEY=untouched`nINTERNAL_SERVICE_TOKEN='test-value=with-equals'`n")
    Import-IntAuthServiceToken -Path $fixture
    if ($env:INTERNAL_SERVICE_TOKEN -ne 'test-value=with-equals') { throw 'Quoted token parsing failed' }
    foreach ($invalid in @('OTHER_KEY=present', 'INTERNAL_SERVICE_TOKEN=', "INTERNAL_SERVICE_TOKEN=a`nINTERNAL_SERVICE_TOKEN=b")) {
        [System.IO.File]::WriteAllText($fixture, $invalid)
        $rejected = $false
        try { Import-IntAuthServiceToken -Path $fixture } catch { $rejected = $true }
        if (-not $rejected) { throw 'Invalid credential configuration was accepted' }
        if ($env:INTERNAL_SERVICE_TOKEN -ne 'test-value=with-equals') { throw 'Rejected configuration changed the environment' }
    }
    Write-Output 'PASS: explicit IntAuth credential loading, missing/empty/duplicate rejection; no credential output'
} finally {
    $env:INTERNAL_SERVICE_TOKEN = $previous
    Remove-Item -LiteralPath $fixture -ErrorAction SilentlyContinue
}
