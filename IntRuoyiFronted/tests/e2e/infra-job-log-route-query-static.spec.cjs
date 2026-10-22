const fs = require('node:fs')
const assert = require('node:assert/strict')

const source = fs.readFileSync('src/views/infra/job/logger/index.vue', 'utf8')

assert.match(source, /const \{\s*query\s*\} = useRoute\(\)/, 'job log page must keep route query ownership')
assert.match(
  source,
  /handlerName:\s*query\.handlerName/,
  'InfraJobLog must initialize handlerName from route query so ERP sync log links filter the intended job handler'
)
assert.match(
  source,
  /jobId:\s*query\.id/,
  'existing jobId deep link must remain supported'
)

console.log('PASS: infra job log route query initializes handlerName filter')
