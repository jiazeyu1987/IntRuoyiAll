const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const viewSource = fs.readFileSync(path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'), 'utf8')
const helperSource = fs.readFileSync(path.join(__dirname, 'frontlineTemplate.ts'), 'utf8')

assert.match(helperSource, /resolveFrontlineContextKey/, 'helper must derive state key from employee/process/template.')
assert.match(
  helperSource,
  /actualEmployeeId.*routeProcessId.*processId.*templateCode/s,
  'context key must include employee, route process, process, and template code.'
)
assert.match(
  helperSource,
  /resetFrontlineTemplateDraftForContext/,
  'helper must reset template draft when context changes.'
)
assert.match(
  viewSource,
  /watch\(\s*frontlineContextKey/s,
  'UI must watch employee/process/template context changes.'
)
assert.match(
  viewSource,
  /resetFrontlineTemplateDraftForContext/,
  'UI must clear old template values after employee or process switch.'
)
assert.match(
  helperSource,
  /fieldValues:\s*buildAllowedFieldValues/,
  'payload must be rebuilt from the current template allowed fields.'
)
assert.doesNotMatch(
  helperSource,
  /Math\.(min|max)|minimum|maximum|minValue|maxValue/,
  'frontend template helper must not clamp raw out-of-limit values.'
)
