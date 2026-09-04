const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const router = read('src/router/modules/remaining.ts')
const productionPage = read('src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue')
const pqcPage = read('src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue')
const api = read('src/api/mes/pro/processpool/teamLeader.ts')

assert.doesNotMatch(
  router,
  /path:\s*'pro\/process-pool\/team-leader'[\s\S]*?name:\s*'MesProProcessPoolTeamLeader'/,
  'The obsolete team-leader page route must be removed.'
)
assert.equal(
  exists('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  true,
  'The production and PQC pages must retain their shared implementation.'
)
assert.match(
  productionPage,
  /<TeamLeaderWorkbenchPage[\s\S]*leader-type="PRODUCTION"[\s\S]*show-production-module-tabs/,
  'The production leader page must continue to render the shared content in PRODUCTION mode.'
)
assert.match(
  pqcPage,
  /<TeamLeaderWorkbenchPage[\s\S]*leader-type="PQC"[\s\S]*show-pqc-module-tabs/,
  'The PQC leader page must continue to render the shared content in PQC mode.'
)
assert.doesNotMatch(
  router,
  /component:\s*\(\)\s*=>\s*import\('@\/views\/mes\/pro\/processpool\/TeamLeaderWorkbenchPage\.vue'\)/,
  'The shared implementation must not remain directly routable.'
)
assert.match(
  api,
  /url:\s*'\/mes\/pro\/process-pool\/team-leader\/submission\/page'/,
  'Deleting the frontend page must not delete the formal backend API namespace.'
)

console.log('PASS: obsolete team-leader route and page removed')
