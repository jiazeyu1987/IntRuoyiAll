const fs = require('fs')
const path = require('path')

const root = process.cwd()

const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const assertFile = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  if (!fs.existsSync(absolutePath)) {
    throw new Error(`Missing expected file: ${relativePath}`)
  }
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) {
    throw new Error(message || `Expected content to include: ${expected}`)
  }
}

const instanceApi = assertFile('src/api/form-center/instance.ts')
assertIncludes(instanceApi, 'PENDING_EFFECT', 'instance API must expose the pending-effect runtime state')
assertIncludes(instanceApi, '/snapshots', 'instance API must expose immutable snapshot query')
assertIncludes(instanceApi, '/effects/pending', 'instance API must expose pending effect query')
assertIncludes(instanceApi, '/effects/${instanceId}/retry', 'instance API must expose effect retry')
assertIncludes(instanceApi, 'FormInstanceSnapshotVO')
assertIncludes(instanceApi, 'FormEffectExecutionVO')

const effectPage = assertFile('src/views/form-center/effect/index.vue')
assertIncludes(effectPage, 'getPendingEffects')
assertIncludes(effectPage, 'retryEffect')
assertIncludes(effectPage, 'UnifiedListTemplate')
assertIncludes(effectPage, '生效失败待处理')
assertIncludes(effectPage, '失败原因')
assertIncludes(effectPage, '重试')
assertIncludes(effectPage, 'form:effect:retry')

const actionPanel = assertFile('src/views/form-center/business-action/ActionFormPanel.vue')
assertIncludes(actionPanel, 'getInstanceSnapshots')
assertIncludes(actionPanel, '快照')
assertIncludes(actionPanel, 'snapshotVersion')
assertIncludes(actionPanel, 'PENDING_EFFECT')
assertIncludes(actionPanel, 'EFFECT_FAILED_PENDING')

const routes = assertFile('src/router/modules/remaining.ts')
assertIncludes(routes, '/approval-center/manager/form-center/effect')
assertIncludes(routes, "permission: ['form:effect:query']")
assertIncludes(routes, "ApprovalCenterFormCenterEffect")

const permissionGuard = assertFile('src/permission.ts')
if (permissionGuard.includes('{ ...to, replace: true }')) {
  throw new Error(
    'Dynamic route reload must not pass the normalized RouteLocation object back to next(); ' +
      'nested form-center routes can fail with vue-router No match and leave the app loading.'
  )
}
assertIncludes(
  permissionGuard,
  'const nextData = to.path === redirect ? { path: redirect, query, replace: true } : { path: redirect, query }',
  'Dynamic route reload must preserve path/query without spreading matched route records.'
)

for (const file of [
  'src/api/form-center/instance.ts',
  'src/views/form-center/effect/index.vue',
  'src/views/form-center/business-action/ActionFormPanel.vue',
  'src/router/modules/remaining.ts',
  'src/permission.ts'
]) {
  const content = read(file)
  if (/catch\s*\([^)]*\)\s*\{\s*\}/.test(content) || /catch\s*\{\s*\}/.test(content)) {
    throw new Error(`Empty catch is not allowed in ${file}`)
  }
}

console.log('form-center effect pending static contract passed')
