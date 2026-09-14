$ErrorActionPreference = 'Stop'

$deployRoot = Join-Path $PSScriptRoot '..\deploy'
$restartScriptPath = Join-Path $deployRoot 'restart-int-ruoyi-local.ps1'
$publishScriptPath = Join-Path $deployRoot 'publish-int-ruoyi.ps1'
$composePath = Join-Path $deployRoot 'int-ruoyi-test\docker-compose.yml'
$legacyDockerComposePath = Join-Path $PSScriptRoot '..\docker\docker-compose.yml'
$legacyDockerEnvPath = Join-Path $PSScriptRoot '..\docker\docker.env'
$legacyDockerHowToPath = Join-Path $PSScriptRoot '..\docker\Docker-HOWTO.md'
$devYamlPath = Join-Path $PSScriptRoot '..\..\yudao-server\src\main\resources\application-dev.yaml'
$localYamlPath = Join-Path $PSScriptRoot '..\..\yudao-server\src\main\resources\application-local.yaml'

foreach ($path in @($restartScriptPath, $publishScriptPath, $composePath, $legacyDockerComposePath,
        $legacyDockerEnvPath, $legacyDockerHowToPath, $devYamlPath, $localYamlPath)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Missing required file: $path"
    }
}

$sources = @(
    @{ Label = 'local restart script'; Source = Get-Content -LiteralPath $restartScriptPath -Encoding UTF8 -Raw },
    @{ Label = 'publish script'; Source = Get-Content -LiteralPath $publishScriptPath -Encoding UTF8 -Raw },
    @{ Label = 'test compose'; Source = Get-Content -LiteralPath $composePath -Encoding UTF8 -Raw },
    @{ Label = 'legacy Docker compose'; Source = Get-Content -LiteralPath $legacyDockerComposePath -Encoding UTF8 -Raw },
    @{ Label = 'legacy docker.env'; Source = Get-Content -LiteralPath $legacyDockerEnvPath -Encoding UTF8 -Raw },
    @{ Label = 'legacy Docker HOWTO'; Source = Get-Content -LiteralPath $legacyDockerHowToPath -Encoding UTF8 -Raw },
    @{ Label = 'application-dev.yaml'; Source = Get-Content -LiteralPath $devYamlPath -Encoding UTF8 -Raw },
    @{ Label = 'application-local.yaml'; Source = Get-Content -LiteralPath $localYamlPath -Encoding UTF8 -Raw }
)

function Assert-NotMatch {
    param(
        [string]$Source,
        [string]$Pattern,
        [string]$Message
    )

    if ($Source -match $Pattern) {
        throw $Message
    }
}

$removedDownloadSecretPrefix = (@('DCC_DOWNLOAD', 'ENCRYPTION') -join '_') + '_'
$removedDownloadSecretNames = @(
    'CURRENT_KEY_VERSION',
    'KEYRING',
    'POLICY_VERSION',
    'KEY_ID',
    'BASE64_KEY',
    'ARTIFACT_DIRECTORY'
) | ForEach-Object { $removedDownloadSecretPrefix + $_ }
$removedDownloadPropertyPattern = 'yudao\.dcc\.download\.' + 'encryption'
$removedDownloadYamlPattern = 'download:\s*\r?\n\s+' + 'encryption:'
$removedDownloadArtifactArg = 'DccDownload' + 'EncryptionArtifactDirectory'
$removedDownloadFallbackMarker = 'DCC_HARDCODED_DOWNLOAD_' + 'ENCRYPTION'

foreach ($item in $sources) {
    foreach ($name in $removedDownloadSecretNames) {
        Assert-NotMatch $item.Source $name "$($item.Label) must not reference removed DCC download secret env $name."
    }

    Assert-NotMatch $item.Source $removedDownloadPropertyPattern "$($item.Label) must not bind removed DCC direct-download secret properties."
    Assert-NotMatch $item.Source $removedDownloadYamlPattern "$($item.Label) must not keep the removed DCC direct-download secret YAML block."
}

Assert-NotMatch $sources[0].Source 'Require-EnvironmentVariable\s+\$requiredEnv' 'Local restart script must not require removed DCC direct-download secret env before backend startup.'
Assert-NotMatch $sources[0].Source $removedDownloadArtifactArg 'Local restart script must not pass a removed DCC direct-download secret artifact directory to Java.'
Assert-NotMatch $sources[1].Source $removedDownloadFallbackMarker 'Publish script must not keep hardcoded removed DCC direct-download secret fallback values.'

Write-Host 'DCC direct download runtime config tests passed'
