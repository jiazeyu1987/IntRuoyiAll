param([Parameter(Mandatory=$true)][ValidateSet('RED','GREEN','REGRESSION')][string]$Phase,
      [ValidatePattern('^[a-zA-Z0-9-]+$')][string]$RunId = '1')
$ErrorActionPreference = 'Stop'
$g03TaskPath = [System.IO.Path]::GetFullPath($PSScriptRoot)
$g03RepoPath = [System.IO.Path]::GetFullPath((Join-Path $g03TaskPath '../../..'))
$g03RunName = $Phase.ToLowerInvariant() + '-' + $RunId
$g03ClassPath = Join-Path $g03TaskPath ('private-junit/' + $g03RunName + '-classes')
if (Test-Path -LiteralPath $g03ClassPath) { throw ('Phase output already exists, do not reuse stale classes: ' + $g03ClassPath) }
New-Item -ItemType Directory -Path $g03ClassPath -Force | Out-Null
$g03MavenRepository = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository'
$g03Artifacts = @(
    'com/fasterxml/jackson/core/jackson-databind/2.17.2/jackson-databind-2.17.2.jar',
    'com/fasterxml/jackson/core/jackson-core/2.17.2/jackson-core-2.17.2.jar',
    'com/fasterxml/jackson/core/jackson-annotations/2.17.2/jackson-annotations-2.17.2.jar',
    'org/junit/jupiter/junit-jupiter-api/5.12.2/junit-jupiter-api-5.12.2.jar',
    'org/junit/jupiter/junit-jupiter-engine/5.12.2/junit-jupiter-engine-5.12.2.jar',
    'org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar',
    'org/junit/platform/junit-platform-engine/1.12.2/junit-platform-engine-1.12.2.jar',
    'org/junit/platform/junit-platform-commons/1.12.2/junit-platform-commons-1.12.2.jar',
    'org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar',
    'org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar'
)
$g03JarPaths = foreach ($g03Artifact in $g03Artifacts) {
    $g03JarPath = Join-Path $g03MavenRepository $g03Artifact
    if (-not (Test-Path -LiteralPath $g03JarPath)) { throw ('Required third-party dependency missing: ' + $g03JarPath) }
    $g03JarPath
}
$g03ClasspathValue = $g03JarPaths -join [System.IO.Path]::PathSeparator
$g03JavaBase = Join-Path $g03RepoPath 'IntRuoyiBackend/yudao-module-dcc/src'
$g03Sources = @(
    (Join-Path $g03JavaBase 'main/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationCustomMatrix.java'),
    (Join-Path $g03JavaBase 'main/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationCustomMatrixService.java'),
    (Join-Path $g03JavaBase 'test/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationCustomMatrixResolverTest.java'),
    (Join-Path $g03TaskPath 'PrivateJUnitRunner.java')
)
$g03Fingerprints = @($g03Sources + $g03JarPaths) | ForEach-Object {
    $g03SourceHash = Get-FileHash -LiteralPath $_ -Algorithm SHA256
    $g03SourceHash.Path + ' ' + $g03SourceHash.Hash
}
$g03EvidenceBase = Join-Path $g03TaskPath ('private-junit/' + $g03RunName)
[System.IO.File]::WriteAllLines($g03EvidenceBase + '-sources.txt', $g03Fingerprints, [System.Text.UTF8Encoding]::new($false))
& javac --release 17 -encoding UTF-8 -classpath $g03ClasspathValue -d $g03ClassPath @g03Sources 2>&1 | Tee-Object -FilePath ($g03EvidenceBase + '-compile.txt')
$g03CompileExit = $LASTEXITCODE
if ($g03CompileExit -ne 0) { throw ('Compilation failed, this is not a behavioral RED: ' + $g03CompileExit) }
& java '-Dfile.encoding=UTF-8' -classpath ($g03ClassPath + [System.IO.Path]::PathSeparator + $g03ClasspathValue) PrivateJUnitRunner 2>&1 | Tee-Object -FilePath ($g03EvidenceBase + '-result.txt')
$g03JUnitExit = $LASTEXITCODE
for ($g03Index = 0; $g03Index -lt $g03Sources.Length; $g03Index++) {
    $g03CurrentHash = Get-FileHash -LiteralPath $g03Sources[$g03Index] -Algorithm SHA256
    if (($g03CurrentHash.Path + ' ' + $g03CurrentHash.Hash) -ne $g03Fingerprints[$g03Index]) { throw ('Source drift during run: ' + $g03Sources[$g03Index]) }
}
[System.IO.File]::WriteAllText($g03EvidenceBase + '-exit.txt', $g03JUnitExit.ToString(), [System.Text.UTF8Encoding]::new($false))
Write-Output ('G03 private ' + $Phase + ': compile=0; JUnit=' + $g03JUnitExit + '; SOURCE_STABLE; output=' + $g03ClassPath)
exit $g03JUnitExit
