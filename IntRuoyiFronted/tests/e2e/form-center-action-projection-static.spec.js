const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const projectionPath = path.join(root, 'src/api/form-center/actionProjection.ts')
const actionPanelPath = path.join(root, 'src/views/form-center/business-action/ActionFormPanel.vue')

assert.ok(fs.existsSync(projectionPath), 'M6 requires shared form-center action projection helper.')

const projectionSource = fs.readFileSync(projectionPath, 'utf8')
const actionPanelSource = fs.readFileSync(actionPanelPath, 'utf8')

for (const token of [
  'ControlledActionProjectionVO',
  'resolveControlledActionProjection',
  'buildProjectionMissingState',
  'assertProjectionAvailable',
  'EFFECT_FAILED_PENDING'
]) {
  assert.ok(projectionSource.includes(token), `actionProjection.ts must expose ${token}.`)
}

assert.match(
  projectionSource,
  /projectionMissing:\s*true[\s\S]*动作投影缺失|动作投影缺失[\s\S]*projectionMissing:\s*true/,
  'Missing backend projection must fail visibly instead of allowing a controlled action.'
)

assert.match(
  projectionSource,
  /allowed\s*===\s*true|projection\.allowed/,
  'Shared projection state must be driven by backend allowed/projection fields.'
)

assert.doesNotMatch(
  projectionSource,
  /status\s*===\s*['"`][^'"`]+['"`][\s\S]*allowed:\s*true/,
  'Shared projection helper must not allow actions from frontend status text or local lifecycle guesses.'
)

assert.ok(
  actionPanelSource.includes('resolveProjectionErrorMessage'),
  'Business action panel must use shared projection-visible error wording.'
)

console.log('form-center action projection static contract passed')
