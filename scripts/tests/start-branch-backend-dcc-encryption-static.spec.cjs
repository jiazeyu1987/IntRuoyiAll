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

const inspectedSources = [
  ['branch backend start script', script],
  ['local Spring profile', localYaml],
  ['dev Spring profile', devYaml]
]

const removedDownloadSecretPrefix = ['DCC_DOWNLOAD', 'ENCRYPTION'].join('_') + '_'
const forbiddenRuntimeVariables = [
  'CURRENT_KEY_VERSION',
  'KEYRING',
  'POLICY_VERSION',
  'KEY_ID',
  'BASE64_KEY',
  'ARTIFACT_DIRECTORY'
].map((suffix) => `${removedDownloadSecretPrefix}${suffix}`)
const removedDownloadPropertyPattern = new RegExp([
  'yudao\\.dcc\\.download\\.',
  'encryption'
].join(''))
const removedDownloadYamlPattern = new RegExp(['download:\\n\\s+', 'encryption:'].join(''))
const removedDownloadEnvironmentPattern = new RegExp([
  'Get-RequiredRuntimeEnvironmentValue[\\s\\S]*download secret',
  ['dccDownload', 'EncryptionEnvironment'].join(''),
  'requiredDccDownloadSecretEnvironmentNames'
].join('|'), 'i')
const removedDownloadEnvInjectionPattern = new RegExp(
  ['Set-Item\\s+-Path\\s+\\$environmentPath\\s+-Value\\s+\\$dccDownload', 'EncryptionEnvironment\\[\\$name\\]'].join('')
)

const removedDownloadEnvMessage = (label, envName) =>
  `${label} must not reference removed DCC download secret env ${envName}`
const removedDownloadPropertyMessage = (label) =>
  `${label} must not bind removed DCC direct-download secret properties.`
const removedDownloadLoaderMessage =
  'Backend start script must not load or inject removed DCC direct-download secret values.'
const removedDownloadInjectionMessage =
  'Backend start script must not inject removed DCC direct-download secret values into Java process env.'

assert.match(
  script,
  /--spring\.profiles\.active=local/,
  'Backend start script must still start the local Spring profile.'
)

for (const [label, source] of inspectedSources) {
  for (const envName of forbiddenRuntimeVariables) {
    assert.ok(!source.includes(envName), removedDownloadEnvMessage(label, envName))
  }
  assert.doesNotMatch(
    source,
    removedDownloadPropertyPattern,
    removedDownloadPropertyMessage(label)
  )
  assert.doesNotMatch(source, removedDownloadYamlPattern, removedDownloadPropertyMessage(label))
}

assert.doesNotMatch(
  script,
  removedDownloadEnvironmentPattern,
  removedDownloadLoaderMessage
)
assert.doesNotMatch(
  script,
  removedDownloadEnvInjectionPattern,
  removedDownloadInjectionMessage
)

console.log('PASS: start-branch-backend DCC direct download runtime contract')
