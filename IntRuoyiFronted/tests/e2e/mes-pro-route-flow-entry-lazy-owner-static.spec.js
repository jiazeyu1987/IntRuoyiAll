const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/mes/pro/route/RouteFormContent.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

const openStart = source.indexOf('const open = async')
const submitStart = source.indexOf('const submitForm', openStart)
assert.notEqual(openStart, -1, 'RouteFormContent 必须保留 open 方法。')
assert.notEqual(submitStart, -1, 'RouteFormContent 必须在 open 方法后保留 submitForm 方法。')
const openSource = source.slice(openStart, submitStart)

assert.doesNotMatch(
  openSource,
  /const\s+ownerCandidatesPromise\s*=\s*loadOwnerLeaderCandidates\(\)/,
  '进入工艺流程/组成工序/关联产品页签时不得无条件启动负责人候选人加载。'
)

assert.doesNotMatch(
  openSource,
  /await\s+ownerCandidatesPromise/,
  '非基础信息页签首次进入不得等待负责人候选人接口。'
)

assert.match(
  openSource,
  /if\s*\(\s*id\s*&&\s*initialTab\s*===\s*'basic'\s*\)\s*\{[\s\S]*await\s+ensureOwnerLeaderCandidatesLoaded\(\)[\s\S]*\}/,
  '只有首次进入基础信息页签时，open 才应预加载负责人候选人。'
)

assert.match(
  source,
  /const ensureOwnerLeaderCandidatesLoaded = async \(\) => \{[\s\S]*loadOwnerLeaderCandidates\(\)/,
  '负责人候选人加载必须有可复用的按需加载入口。'
)

const fetchStart = source.indexOf('const fetchOwnerSuggestions = async')
const selectStart = source.indexOf('const handleOwnerCandidateSelect', fetchStart)
assert.notEqual(fetchStart, -1, '负责人自动补全 fetchOwnerSuggestions 必须支持异步按需加载。')
assert.notEqual(selectStart, -1, 'RouteFormContent 必须在 fetchOwnerSuggestions 后保留 handleOwnerCandidateSelect。')
const fetchSource = source.slice(fetchStart, selectStart)
assert.match(
  fetchSource,
  /await\s+ensureOwnerLeaderCandidatesLoaded\(\)/,
  '用户首次使用负责人自动补全时必须按需加载负责人候选人。'
)

console.log('PASS: mes pro route flow entry lazy owner static contract')
