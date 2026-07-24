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

$restartSource = Get-Content -LiteralPath $restartScriptPath -Encoding UTF8 -Raw
$publishSource = Get-Content -LiteralPath $publishScriptPath -Encoding UTF8 -Raw
$composeSource = Get-Content -LiteralPath $composePath -Encoding UTF8 -Raw
$legacyDockerCompose = Get-Content -LiteralPath $legacyDockerComposePath -Encoding UTF8 -Raw
$legacyDockerEnv = Get-Content -LiteralPath $legacyDockerEnvPath -Encoding UTF8 -Raw
$legacyDockerHowTo = Get-Content -LiteralPath $legacyDockerHowToPath -Encoding UTF8 -Raw
$devYaml = Get-Content -LiteralPath $devYamlPath -Encoding UTF8 -Raw
$localYaml = Get-Content -LiteralPath $localYamlPath -Encoding UTF8 -Raw

function Assert-Match {
    param(
        [string]$Source,
        [string]$Pattern,
        [string]$Message
    )

    if ($Source -notmatch $Pattern) {
        throw $Message
    }
}

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

foreach ($name in @(
    'DCC_DOWNLOAD_ENCRYPTION_POLICY_VERSION',
    'DCC_DOWNLOAD_ENCRYPTION_KEY_ID',
    'DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY',
    'DCC_DOWNLOAD_ENCRYPTION_ARTIFACT_DIRECTORY'
)) {
    $placeholderPattern = '\$\{' + $name + '\}'
    $defaultPattern = '\$\{' + $name + ':'
    Assert-Match $restartSource $name "Local restart script must require $name before backend startup."
    Assert-Match $publishSource $name "Unified publish script must require and write $name."
    Assert-Match $composeSource $name "Test compose must pass $name into backend runtime."
    Assert-Match $legacyDockerCompose $name "Legacy Docker compose must fail fast when $name is missing."
    Assert-Match $legacyDockerEnv ('(?m)^' + $name + '=') "Legacy docker.env must expose $name as an explicit required placeholder."
    Assert-Match $legacyDockerHowTo $name "Legacy Docker HOWTO must document required $name."
    Assert-Match $devYaml $placeholderPattern "application-dev.yaml must bind $name without a default."
    Assert-Match $localYaml $placeholderPattern "application-local.yaml must bind $name without a default."
    Assert-NotMatch $devYaml $defaultPattern "application-dev.yaml must not define a default for $name."
    Assert-NotMatch $localYaml $defaultPattern "application-local.yaml must not define a default for $name."
}

Assert-Match $restartSource 'Require-EnvironmentVariable' 'Local restart script must fail fast on missing DCC download encryption env.'
Assert-Match $publishSource 'Missing DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY' 'Publish script must fail fast before writing an incomplete runtime env.'
Assert-Match $composeSource '--yudao\.dcc\.download\.encryption\.base64-key=\$\{DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY\}' 'Compose must pass the AES key through Spring configuration.'
Assert-Match $legacyDockerCompose '\$\{DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY:\?DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY is required\}' 'Legacy Docker compose must use compose-required syntax for the AES key.'
Assert-Match $legacyDockerHowTo 'docker\.env is required' 'Legacy Docker HOWTO must no longer claim docker.env is optional.'

Write-Host 'DCC download encryption runtime config tests passed'
