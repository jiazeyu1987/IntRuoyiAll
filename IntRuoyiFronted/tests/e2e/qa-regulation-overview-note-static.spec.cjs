const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const projectRoot = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(projectRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)

assert.ok(source.includes('data-qa-regulation-scope'), 'QA overview must retain regulation information.')
assert.ok(source.includes('data-qa-regulation-basic-form'), 'QA overview must retain its basic form.')
assert.doesNotMatch(source, /data-qa-regulation-overview-note/, 'QA overview must not render the removed notes card.')
assert.doesNotMatch(source, /data-qa-regulation-common-binding-scope/, 'QA overview must not render the removed binding scope field.')

console.log('PASS QA regulation overview removed content contract')
