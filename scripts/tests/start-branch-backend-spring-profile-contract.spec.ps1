$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$taskRepoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$launcherPath = Join-Path $taskRepoRoot 'scripts\runtime\start-branch-backend.ps1'
$tokens = $null
$parseErrors = $null
$ast = [System.Management.Automation.Language.Parser]::ParseFile($launcherPath, [ref]$tokens, [ref]$parseErrors)
if ($parseErrors.Count -ne 0) { throw 'Formal launcher PowerShell parse failed' }
$statements = @($ast.EndBlock.Statements)
$dotSource = @($statements | Where-Object { $_.Extent.Text -eq '. "$PSScriptRoot\branch-runtime-profile.ps1"' })
$javaCall = @($statements | Where-Object { $_.Extent.Text -eq '& java @javaArgs' })
$exitStatement = @($statements | Where-Object { $_.Extent.Text -eq 'exit $LASTEXITCODE' })
if ($dotSource.Count -ne 1 -or $javaCall.Count -ne 1 -or $exitStatement.Count -ne 1) { throw 'Unexpected formal launcher external endpoints' }
$body = @($statements | Where-Object { $_ -notin $dotSource -and $_ -notin $javaCall -and $_ -notin $exitStatement } | ForEach-Object { $_.Extent.Text }) -join "`n"
$actualLauncher = [scriptblock]::Create($ast.ParamBlock.Extent.Text + "`n" + $body + "`n" + '[pscustomobject]@{ JavaArgs = @($javaArgs); ResolvedBranch = $branch }')

# Explicit offline authority hosts: no actual Git, listener, jar, Java or service operation.
$script:testBranch = 'int_qms'
$script:testPort = 48061
$script:requestedSlot = $null
function Get-CurrentRepoRoot { return $taskRepoRoot }
function Get-GitValue {
    param([string]$RepoRoot, [string[]]$Arguments)
    if ($RepoRoot -ne $taskRepoRoot -or ($Arguments -join '|') -ne 'branch|--show-current') { throw 'Current branch authority contract changed' }
    return $script:testBranch
}
function Resolve-BranchRuntimeContext {
    param([string]$RepoRoot, [string]$Branch, [Nullable[int]]$RequestedSlot)
    if ($RepoRoot -ne $taskRepoRoot -or $Branch -ne $script:testBranch) { throw 'Runtime context authority contract changed' }
    $script:requestedSlot = $RequestedSlot
    return [pscustomobject]@{ Profile = [pscustomobject]@{ Name = $Branch }; Ports = [pscustomobject]@{ BackendPort = $script:testPort } }
}
function Get-NetTCPConnection { param($LocalPort, $State, $ErrorAction) return @() }
function Test-Path {
    param([string]$Path)
    if ($Path -ne (Join-Path $taskRepoRoot 'IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar')) { throw 'Executable jar source changed' }
    return $true
}
function java { throw 'Real Java must not execute in contract test' }
function mvn.cmd { throw 'Real Maven must not execute in contract test' }
function Assert-TaskEqual($Actual, $Expected, [string]$Message) {
    if ($Actual -ne $Expected) { throw ($Message + ': expected ' + $Expected + ', actual ' + $Actual) }
}

$result = & $actualLauncher
Assert-TaskEqual (@($result.JavaArgs | Where-Object { $_ -like '--spring.profiles.active=*' }) -join '|') '--spring.profiles.active=local' 'Default local profile retained'
Assert-TaskEqual (@($result.JavaArgs | Where-Object { $_ -like '--server.port=*' }) -join '|') '--server.port=48061' 'Current branch port retained'
Write-Output 'PASS default local and current branch port'

$result = & $actualLauncher -SpringProfile 'dcc-local-development' -ExtraArgs @('--dcc.test-fixture=true') -Slot 3
Assert-TaskEqual (@($result.JavaArgs | Where-Object { $_ -like '--spring.profiles.active=*' }) -join '|') '--spring.profiles.active=dcc-local-development' 'Explicit single Spring profile'
Assert-TaskEqual (@($result.JavaArgs | Where-Object { $_ -like '--server.port=*' }) -join '|') '--server.port=48061' 'Spring profile does not replace runtime context port'
Assert-TaskEqual $script:requestedSlot 3 'Slot forwarded unchanged to authority'
Assert-TaskEqual $result.JavaArgs[-1] '--dcc.test-fixture=true' 'Existing ExtraArgs retained'
Write-Output 'PASS explicit development profile with existing slot and ExtraArgs'

foreach ($case in @(@('int_main', 48081), @('int_main_d', 48101), @('int_batch', 48041), @('int_shedule', 48021))) {
    $script:testBranch = $case[0]
    $script:testPort = $case[1]
    $result = & $actualLauncher
    Assert-TaskEqual $result.ResolvedBranch $case[0] 'Original branch authority retained'
    Assert-TaskEqual (@($result.JavaArgs | Where-Object { $_ -like '--server.port=*' }) -join '|') ('--server.port=' + $case[1]) 'Existing context port retained'
    Assert-TaskEqual (@($result.JavaArgs | Where-Object { $_ -like '--spring.profiles.active=*' }) -join '|') '--spring.profiles.active=local' 'All existing branches keep local default'
}
Write-Output 'PASS other branch context ports and original local defaults'

foreach ($value in @('', ' ', 'local,dcc-local-development', 'local dcc-local-development', 'local;dcc-local-development')) {
    $rejected = $false
    try { $null = & $actualLauncher -SpringProfile $value }
    catch [System.Management.Automation.ParameterBindingException] {
        if ($_.FullyQualifiedErrorId -notlike '*ParameterArgumentValidationError*') { throw }
        $rejected = $true
    }
    Assert-TaskEqual $rejected $true 'Invalid profile rejected by actual parameter binder'
}
Write-Output 'PASS blank and multiple-profile inputs rejected'
Write-Output 'PASS actual PowerShell launcher parameter construction; no Git, Java, Maven, listener, service or database execution'
