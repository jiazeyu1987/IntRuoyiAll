const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../..')

function read(relativePath) {
  return fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')
}

const frontlinePqc = read(
  'main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)
const releaseReader = read(
  'main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/' +
    'MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl.java'
)

for (const [name, source] of [
  ['frontline PQC', frontlinePqc],
  ['release process inspection reader', releaseReader]
]) {
  assert.match(
    source,
    /(getDccProjectCodeId\(\)|dccProjectCodeId\(\))/,
    `${name} must consume the DCC identity frozen from the formal route-DCC binding`
  )
  assert.match(
    source,
    /(getQaRegulationVersionId\(\)|qaRegulationVersionId\(\))/,
    `${name} must consume the QA version frozen with the DCC identity`
  )
  assert.doesNotMatch(
    source,
    /dccProjectCodeMapper\.selectEnabledList\(\)/,
    `${name} must not scan DCC projects for a product-code match`
  )
  assert.doesNotMatch(
    source,
    /resolveRouteDccProject|routeItemCodes|routeProjectCodes/,
    `${name} must not derive DCC identity from route product codes`
  )
}

console.log('mes-route-dcc-runtime-source-unification-static contract PASS')
