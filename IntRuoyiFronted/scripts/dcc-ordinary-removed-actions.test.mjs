import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'
import test from 'node:test'

const source = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/detail/index.vue', import.meta.url)), 'utf8')

test('ordinary approval does not render legacy mutation buttons; external review retains its controls', () => {
  for (const action of ['return', 'transfer', 'sign']) {
    const tags = [...source.matchAll(/<el-button\b(?:[^>"']|"[^"]*"|'[^']*')*>/g)].map(m => m[0])
      .filter(tag => tag.includes(`openTaskActionDialog('${action}')`))
    assert.ok(tags.length > 0, `external review ${action} control exists`)
    for (const tag of tags) {
    const condition = tag.match(/v-if="([^"]+)"/)?.[1]
    assert.ok(condition)
    const context = { isExternalReviewProcess: false, isReturnedApplicantTask: false, returnTargetOptions: [1] }
    assert.equal(vm.runInNewContext(condition, context), false, `${action} must be hidden for ordinary DCC`)
    assert.equal(vm.runInNewContext(condition, { ...context, isExternalReviewProcess: true }), true)
    }
  }
})

test('generic BPM hides route mutations for ordinary DCC including form-center tasks', () => {
  const page = readFileSync(fileURLToPath(new URL('../src/views/bpm/processInstance/detail/ProcessInstanceOperationButton.vue', import.meta.url)), 'utf8')
  const body = page.match(/const isShowButton = \(btnType: OperationButtonType\): boolean => \{([\s\S]*?)\n\}/)?.[1]
  assert.ok(body)
  for (const key of ['dcc-controlled-file-approval', 'other-process']) {
    for (const action of ['TRANSFER', 'DELEGATE', 'ADD_SIGN', 'RETURN', 'APPROVE']) {
      const context = {
        props: { processInstance: { processDefinition: { key } } },
        CONTROLLED_FILE_PROCESS_DEFINITION_KEY: 'dcc-controlled-file-approval',
        OperationButtonType: Object.fromEntries(['TRANSFER', 'DELEGATE', 'ADD_SIGN', 'RETURN', 'APPROVE'].map(x => [x, x])),
        runningTask: { value: { buttonsSetting: { [action]: { enable: true } } } },
        btnType: action
      }
      const allowed = vm.runInNewContext(`(() => {${body}})()`, context)
      assert.equal(allowed, key !== 'dcc-controlled-file-approval' || action === 'APPROVE', `${key}/${action}`)
    }
  }
})
