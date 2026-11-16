param(
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][string[]]$ClassNames,
    [Parameter(Mandatory)][string[]]$ClassFiles,
    [int]$DebugPort = 5005
)
$ErrorActionPreference = 'Stop'
if ($ClassNames.Count -ne $ClassFiles.Count -or $ClassNames.Count -eq 0) { throw 'Provide matching class names and compiled files.' }
$java = Join-Path $JavaHome 'bin/java.exe'
if (!(Test-Path -LiteralPath $java)) { throw "Missing JDK: $java" }
$arguments = @('--add-modules', 'jdk.jdi', (Join-Path $PSScriptRoot 'HotSwap.java'), "$DebugPort")
for ($i = 0; $i -lt $ClassNames.Count; $i++) {
    $arguments += $ClassNames[$i]
    $arguments += (Resolve-Path -LiteralPath $ClassFiles[$i]).Path
}
& $java @arguments
if ($LASTEXITCODE -ne 0) { throw "HotSwap failed with code $LASTEXITCODE; no restart or fallback was attempted." }
