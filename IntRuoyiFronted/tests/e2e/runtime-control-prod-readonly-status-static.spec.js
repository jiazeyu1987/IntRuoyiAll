const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

function assertNotContains(source, forbidden, label) {
  if (source.includes(forbidden)) {
    throw new Error(`forbidden ${label}: ${forbidden}`)
  }
}

const api = readUtf8('src/api/infra/runtimeControl/index.ts')
const page = readUtf8('src/views/infra/runtime-control/index.vue')
const probePanel = readUtf8('src/views/infra/runtime-control/components/OpsProbeStatusPanel.vue')
const shared = readUtf8('src/views/infra/runtime-control/components/shared.ts')

assertContains(page, "{ key: 'prod', label: 'Production' }", 'production environment matrix column')
assertContains(page, 'statusOf(environment.key, component.key)?.status', 'status from backend overview')
assertContains(page, 'statusOf(environment.key, component.key)?.url', 'status URL rendering')
assertContains(page, 'currentReleaseTagText(environment.key)', 'production release tag rendering')
assertContains(page, 'canRestart(environment.key, component.key)', 'restart gate uses status action flag')
assertContains(page, 'Boolean(statusOf(environment, component)?.actionEnabled)', 'restart disabled when backend marks action unavailable')
assertContains(page, 'OpsProbeStatusPanel', 'probe panel component')
assertContains(page, '@run="runProbes"', 'probe run event binding')
assertContains(api, '/infra/runtime-control/probes/run', 'run probe API')
assertContains(api, '/infra/runtime-control/probes/latest', 'latest probe API')
assertContains(probePanel, '执行探针', 'manual probe trigger')
assertContains(probePanel, '目标地址', 'probe target URL column')
assertContains(shared, "prod: 'Production'", 'production probe environment label')
assertNotContains(page, "environment.key !== 'prod' && statusOf", 'production status visibility guard')

console.log('PASS: production readonly status and probe UI contracts stay visible')
