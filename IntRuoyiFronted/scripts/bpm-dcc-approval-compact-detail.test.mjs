import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const repoRoot = path.resolve(import.meta.dirname, '..')
const bpmDetailPath = path.join(repoRoot, 'src/views/bpm/processInstance/detail/index.vue')
const source = fs.readFileSync(bpmDetailPath, 'utf8')

assert.match(
  source,
  /data-testid="bpm-dcc-approval-compact-summary"/,
  'BPM DCC 审批详情必须提供审核人专用摘要卡。'
)
assert.match(source, /审核内容/, '摘要卡必须明确说明审核内容。')
assert.match(source, /当前步骤/, '摘要卡必须明确展示当前步骤。')
assert.match(source, /当前处理人/, '摘要卡必须明确展示当前处理人。')
assert.match(
  source,
  /isDccControlledFileCustomForm/,
  'BPM 详情必须识别 DCC 受控文件自定义业务表单。'
)
assert.match(
  source,
  /<BusinessFormComponent\s+v-else[\s\S]*:id="processInstance\.businessKey"/,
  '完整业务表单组件只能在非 DCC 审批摘要场景挂载。'
)
assert.doesNotMatch(
  source,
  /<BusinessFormComponent\s+:id="processInstance\.businessKey"\s*\/>/,
  'BPM 详情不得再无条件挂载完整 DCC 详情组件。'
)
assert.match(
  source,
  /openDccControlledFileApprovalDetail/,
  '摘要卡必须保留进入正式文控审批处理页的入口。'
)
assert.match(
  source,
  /getControlledFile/,
  '摘要卡应只读取受控文件基础审核信息，而不是挂载完整详情页面。'
)

console.log('PASS: BPM DCC approval compact detail static contract')
