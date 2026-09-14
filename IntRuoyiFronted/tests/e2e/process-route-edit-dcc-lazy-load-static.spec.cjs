const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const source = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFormContent.vue'),
  'utf8'
)
const editPage = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteEditPage.vue'),
  'utf8'
)

const extractFunction = (name, nextName) => {
  const start = source.indexOf(`const ${name} =`)
  const end = source.indexOf(`const ${nextName} =`, start + 1)
  assert.ok(start >= 0 && end > start, `must find ${name}`)
  return source.slice(start, end)
}

const open = extractFunction('open', 'assertRouteCandidateVersionWritable')
const handleRouteTabChange = extractFunction('handleRouteTabChange', 'loadDccProjectBinding')

assert.match(
  editPage,
  /\['basic', 'flow', 'product', 'dcc'\]\.includes\(tab\)/,
  'Route edit deep links must allow the DCC tab as an initial tab'
)
const loadDccProjectData = extractFunction('loadDccProjectData', 'loadDccProjectBinding')
assert.match(
  open,
  /const shouldLoadDccProjectData = activeTab\.value === 'dcc'[\s\S]*if \(shouldLoadDccProjectData\) \{[\s\S]*await loadDccProjectData\(\)/,
  'opening directly to the DCC tab must load the route binding without waiting for a manual tab-change event'
)
assert.match(
  handleRouteTabChange,
  /tabName === 'dcc'[\s\S]*loadDccProjectData\(\)/,
  'DCC data must load only when the DCC tab becomes active'
)
assert.match(
  loadDccProjectData,
  /await loadDccProjectBinding\(formData\.value\.id\)[\s\S]*await loadDccProjectCodeOptions\(''\)/,
  'DCC tab must load both the formal route binding and project-code options'
)
assert.match(
  loadDccProjectData,
  /dccProjectBindingError\.value[\s\S]*message\.error/,
  'DCC load failure must remain explicit in the DCC tab and user message'
)
assert.match(
  source,
  /v-if="dccProjectBindingError"[\s\S]*:title="dccProjectBindingError"[\s\S]*type="error"/,
  'DCC tab must render its own blocking load error'
)

console.log('PASS: process-route-edit-dcc-lazy-load-static')
