const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const batchDetailPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const executionPage = readSource('src/views/mes/pro/edhr/ExecutionPage.vue')

const extractFunctionBody = (source, signature) => {
  const start = source.indexOf(signature)
  assert.ok(start >= 0, `missing function signature: ${signature}`)
  const bodyStart = source.indexOf('{', start)
  assert.ok(bodyStart >= 0, `missing function body: ${signature}`)
  let depth = 0
  for (let index = bodyStart; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) return source.slice(bodyStart, index + 1)
    }
  }
  throw new Error(`unterminated function body: ${signature}`)
}

const extractComputedBody = (source, signature) => {
  const start = source.indexOf(signature)
  assert.ok(start >= 0, `missing computed signature: ${signature}`)
  const end = source.indexOf('\n})', start)
  assert.ok(end > start, `missing computed body end: ${signature}`)
  return source.slice(start, end + 3)
}

const taskSlotBlockerBody = extractFunctionBody(
  batchDetailPage,
  'const resolveTaskSlotBlocker = (row: EdhrBatchExecutionTaskRespVO) =>'
)
const taskAccessReasonBody = extractFunctionBody(
  batchDetailPage,
  'const normalizeTaskAccessReason = (reason?: string | null) =>'
)
const slotContextBlockersBody = extractComputedBody(
  executionPage,
  'const slotContextBlockers = computed(() =>'
)

for (const [name, body] of [
  ['batch detail task blocker', taskSlotBlockerBody],
  ['batch detail task access reason', taskAccessReasonBody],
  ['execution submit blocker', slotContextBlockersBody]
]) {
  if (name !== 'batch detail task access reason') {
    assert.ok(!body.includes('缺少权限范围'), `${name} must not expose permission scope wording to fillers`)
    assert.ok(!body.includes('permissionScopeId'), `${name} must not gate filler UI on permissionScopeId`)
  }
}

assert.ok(
  batchDetailPage.includes("const HIDDEN_FILL_ACCESS_REASON_KEYWORDS = ['待处理任务', '权限范围', 'permissionScopeId']"),
  'batch fill detail must filter task/access implementation wording before rendering disabled reasons'
)
assert.ok(
  taskAccessReasonBody.includes('HIDDEN_FILL_ACCESS_REASON_KEYWORDS.some'),
  'batch fill detail must hide internal task/access reasons instead of renaming them for fillers'
)

assert.ok(
  readSource('src/api/mes/pro/edhr/batchExecution.ts').includes('permissionScopeId?: number | null') &&
    readSource('src/api/mes/pro/feedback/index.ts').includes('permissionScopeId?: number | null'),
  'permissionScopeId must remain in API contracts for backend and permission matrix use'
)

console.log('PASS: eDHR batch fill UI hides permission scope technical blockers')
