const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const componentPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const component = fs.readFileSync(componentPath, 'utf8')

assert.match(
  component,
  /<el-tab-pane[\s\S]{0,120}label="生产表单"[\s\S]{0,80}name="productionSubmissions"/,
  '生产主页签必须显示“生产表单”，并保留 productionSubmissions 身份。'
)
assert.match(
  component,
  /<el-tab-pane[\s\S]{0,120}label="过程检表单"[\s\S]{0,80}name="pqcSubmissions"/,
  'PQC主页签必须显示“过程检表单”，并保留 pqcSubmissions 身份。'
)
assert.doesNotMatch(
  component,
  /label="生产提交"\s*name="productionSubmissions"/,
  '生产主页签不得继续显示旧文案。'
)
assert.doesNotMatch(
  component,
  /label="PQC提交"\s*name="pqcSubmissions"/,
  'PQC主页签不得继续显示旧文案。'
)

console.log('active-order submission tab labels static contract passed')
