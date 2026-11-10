const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')

test('missing predecessor evidence is a definite failure with guidance and no successful approval', async () => {
  class DccTaskActionError extends Error {
    constructor(message, code) { super(message); this.code = code }
  }
  const model = {}
  const code = ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/detail/approval-actions.ts', 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  new Function('exports', 'require', code)(model, id => {
    if (id.endsWith('/workflow')) return {
      DccTaskActionError, isControlledFileTaskPasswordInvalidError: () => false,
      approveControlledFileTask: async () => { throw new DccTaskActionError('evidence prerequisite is missing', 1080000087) }
    }
    if (id === '../shared/lifecycle') return {}
    throw Error(id)
  })
  const result = await model.submitDccApprovalAction({ fileId: '70', taskId: 'obsolete-task', action: 'approve',
    form: { password: 'unit-only', reason: '' } })
  assert.equal(result.success, false)
  assert.equal(result.resultUnconfirmed, false)
  assert.match(result.inlineError, /本次审批未推进/)
  assert.match(result.inlineError, /本轮各节点签名及会签指派记录/)
  assert.equal(result.response, undefined)
})
