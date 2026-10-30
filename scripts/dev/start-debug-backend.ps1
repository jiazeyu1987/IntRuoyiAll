param(
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][string]$Jar,
    [Parameter(Mandatory)][int]$Port,
    [int]$DebugPort = 5005,
    [string]$Profile = 'local'
)
$ErrorActionPreference = 'Stop'
$java = Join-Path $JavaHome 'bin/java.exe'
if (!(Test-Path -LiteralPath $java)) { throw "Missing JDK: $java" }
$resolvedJar = (Resolve-Path -LiteralPath $Jar).Path
foreach ($listenPort in @($Port, $DebugPort)) {
    if (Get-NetTCPConnection -State Listen -LocalPort $listenPort -ErrorAction SilentlyContinue) {
        throw "Port $listenPort is occupied; this script never stops an existing process."
    }
}
# Foreground execution keeps logs and lifecycle owned by the calling Codex terminal.
& $java "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:$DebugPort" -jar $resolvedJar "--server.port=$Port" "--spring.profiles.active=$Profile"
if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE" }
