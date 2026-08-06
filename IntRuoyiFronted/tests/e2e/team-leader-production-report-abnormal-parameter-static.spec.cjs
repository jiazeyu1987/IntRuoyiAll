const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const panel = readUtf8('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

assert.match(panel, /resolveProductionParameterStatus/, 'input panel must compute parameter status against configured limits')
assert.match(panel, /is-parameter-out-of-range/, 'input panel must mark out-of-range numbers with the shared red class')
assert.match(panel, /data-parameter-status/, 'input panel must expose parameter status for tests and accessibility')
assert.match(panel, /ABOVE_UPPER|BELOW_LOWER/, 'input panel must distinguish the abnormal direction')
assert.doesNotMatch(
  panel,
  /:disabled="[^"]*resolveProductionParameterStatus/,
  'out-of-range parameter values must not disable normal submit'
)

assert.match(page, /is-parameter-out-of-range/, 'team-leader report must reuse the shared red abnormal parameter class')
assert.match(page, /data-parameter-status/, 'team-leader report must render the saved abnormal direction')
assert.match(page, /ABOVE_UPPER|BELOW_LOWER/, 'team-leader report must expose abnormal direction from the saved payload')
assert.match(page, /aria-label=.*参数异常|参数异常.*aria-label/, 'team-leader report must provide an accessible abnormal parameter cue')

console.log('PASS: abnormal production parameters are red but still submittable')
