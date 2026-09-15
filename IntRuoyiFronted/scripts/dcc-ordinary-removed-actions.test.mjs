import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'
import test from 'node:test'

const source = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/detail/index.vue', import.meta.url)), 'utf8')
const browser = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/browser/index.vue', import.meta.url)), 'utf8')
const browserPresentation = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/browser/presentation.ts', import.meta.url)), 'utf8')
const lifecycle = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/shared/lifecycle.ts', import.meta.url)), 'utf8')

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

test('ordinary browser publish entry follows backend action projection instead of ready status', () => {
  const publishTag = [...browser.matchAll(/<el-button\b(?:[^>"']|"[^"]*"|'[^']*')*>/g)]
    .map(m => m[0])
    .find(tag => tag.includes('dcc-controlled-browser-publish'))
  assert.ok(publishTag, 'browser publish entry exists for action-projected external review cases')
  assert.match(
    publishTag,
    /v-if="getBrowserRowActionState\(getSelectedVersion\(row\)\)\.canPublish"/,
    'browser publish entry must be hidden unless backend action projection allows PUBLISH'
  )
  assert.doesNotMatch(
    publishTag,
    /status === 'READY_TO_PUBLISH'/,
    'browser publish entry must not render from READY_TO_PUBLISH status alone'
  )
  assert.match(
    browserPresentation,
    /canPublish:\s*isDccControlledFileActionAllowed\(row,\s*'PUBLISH'\)/,
    'browser row action state must expose PUBLISH from backend actionProjection'
  )
})

test('controlled-file visible lifecycle wording uses effective-state labels', () => {
  for (const retired of ['工作稿', '工作版本', '现行', '待发布', '发布处理中', '发布失败', '发布申请']) {
    assert.doesNotMatch(source, new RegExp(retired), `detail page must not show retired DCC wording ${retired}`)
    assert.doesNotMatch(lifecycle, new RegExp(retired), `lifecycle labels must not show retired DCC wording ${retired}`)
  }
  assert.match(lifecycle, /ACTIVE', label: '当前有效'/)
  assert.match(lifecycle, /READY_TO_PUBLISH', label: '待生效处理'/)
  assert.match(lifecycle, /FINALIZING', label: '生效处理中'/)
})
