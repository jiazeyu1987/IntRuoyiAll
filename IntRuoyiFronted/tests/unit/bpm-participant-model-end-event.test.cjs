const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')
const source = fs.readFileSync('src/views/bpm/model/index.vue', 'utf8')
const body = source.slice(source.indexOf('const buildApprovalParticipantSimpleModel ='), source.indexOf('const resolveParticipantConfigName ='))
for (const hasApprover of [true, false]) {
  test(`explicit end event with approver=${hasApprover}`, () => {
    const reviewer = { id: 'reviewer' }
    const approver = hasApprover ? { id: 'approver' } : undefined
    const context = { NodeType: { END_EVENT_NODE: 1, START_USER_NODE: 10 }, participantConfigForm: {}, buildParticipantSectionNode: key => key === 'reviewers' ? reviewer : approver, START_USER_BUTTON_SETTING: [] }
    vm.createContext(context)
    vm.runInContext(ts.transpile(body + '\nglobalThis.result = buildApprovalParticipantSimpleModel()', { target: ts.ScriptTarget.ES2020 }), context)
    assert.equal(context.result.childNode, reviewer)
    if (hasApprover) assert.equal(reviewer.childNode, approver)
    const end = hasApprover ? approver.childNode : reviewer.childNode
    assert.equal(end.type, 1)
    assert.equal(end.id, 'EndEvent')
    assert.equal(end.childNode, undefined)
  })
}
