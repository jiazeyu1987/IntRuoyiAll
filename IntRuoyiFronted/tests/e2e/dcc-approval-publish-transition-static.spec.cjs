const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/detail/index.vue'), 'utf8')
const start = source.indexOf('const actionDialogSubmitFlowText = computed(')
const end = source.indexOf('const getStageRouteSnapshot =', start)
assert.ok(start >= 0 && end > start, '审批提交后流转说明必须存在')
const expression = source.slice(start, end).replace('const actionDialogSubmitFlowText =', '')

for (const stage of ['审核会签', '文控批准']) {
  test(`${stage}应明确独立文控发布`, () => {
    const message = vm.runInNewContext(expression, {
      computed: (getter) => getter(),
      actionDialog: { mode: 'approve' },
      currentStageLabel: { value: stage }
    })
    assert.match(message, /待文控发布/)
    assert.doesNotMatch(message, /批准后文件发布为 ACTIVE/)
  })
}
