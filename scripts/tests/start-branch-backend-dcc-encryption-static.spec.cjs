const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const script = fs
  .readFileSync(path.join(repoRoot, 'scripts/runtime/start-branch-backend.ps1'), 'utf8')
  .replace(/\r\n/g, '\n')
const localYaml = fs
  .readFileSync(path.join(repoRoot, 'IntRuoyiBackend/yudao-server/src/main/resources/application-local.yaml'), 'utf8')
  .replace(/\r\n/g, '\n')
const devYaml = fs
  .readFileSync(path.join(repoRoot, 'IntRuoyiBackend/yudao-server/src/main/resources/application-dev.yaml'), 'utf8')
  .replace(/\r\n/g, '\n')

const requiredMappings = [
  'DCC_DOWNLOAD_ENCRYPTION_POLICY_VERSION',
  'DCC_DOWNLOAD_ENCRYPTION_KEY_ID',
  'DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY',
  'DCC_DOWNLOAD_ENCRYPTION_ARTIFACT_DIRECTORY'
]

assert.match(
  script,
  /function\s+Get-RequiredRuntimeEnvironmentValue\b/,
  'Backend start script must fail fast through a required runtime environment resolver.'
)
assert.match(
  script,
  /throw "Missing required runtime environment variable: \$Name\./,
  'Backend start script must fail fast when required DCC download encryption config is absent.'
)

for (const envName of requiredMappings) {
  assert.ok(script.includes(envName), `Missing required environment variable lookup: ${envName}`)
}

assert.match(
  script,
  /Set-Item\s+-Path\s+\$environmentPath\s+-Value\s+\$dccDownloadEncryptionEnvironment\[\$name\]/,
  'DCC download encryption values must be injected into the Java child process environment.'
)
assert.match(
  script,
  /Remove-Item\s+-Path\s+\$environmentPath\s+-ErrorAction\s+SilentlyContinue/,
  'Backend start script must remove task-injected process environment values when they were originally absent.'
)
assert.match(
  script,
  /Set-Item\s+-Path\s+\$environmentPath\s+-Value\s+\$previousDccDownloadEncryptionEnvironment\[\$name\]/,
  'Backend start script must restore prior process environment values after Java exits.'
)
assert.doesNotMatch(
  script,
  /Write-Host[\s\S]*(DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY|base64-key)/,
  'Backend start script must not print the DCC download encryption key material.'
)
assert.doesNotMatch(
  script,
  /--yudao\.dcc\.download\.encryption\.(policy-version|key-id|base64-key|artifact-directory)=/,
  'Backend start script must not expose DCC download encryption values through Java command-line arguments.'
)
assert.doesNotMatch(
  script,
  /artifact-directory=.*(tmp|temp|default)/i,
  'Backend start script must not invent a default artifact directory.'
)

for (const [label, yaml] of [['local', localYaml], ['dev', devYaml]]) {
  assert.match(
    yaml,
    /download:\n\s+encryption:\n\s+policy-version:\s+\$\{DCC_DOWNLOAD_ENCRYPTION_POLICY_VERSION\}\n\s+key-id:\s+\$\{DCC_DOWNLOAD_ENCRYPTION_KEY_ID\}\n\s+base64-key:\s+\$\{DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY\}\n\s+artifact-directory:\s+\$\{DCC_DOWNLOAD_ENCRYPTION_ARTIFACT_DIRECTORY\}/,
    `${label} profile must bind artifact-directory under yudao.dcc.download.encryption.`
  )
}

console.log('PASS: start-branch-backend DCC download encryption runtime environment contract')
