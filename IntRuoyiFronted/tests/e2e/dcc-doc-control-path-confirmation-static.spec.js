const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const approvalActions = readSource('src/views/dcc/controlled-file/detail/approval-actions.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:doc-control-path-confirmation:static'],
  'node tests/e2e/dcc-doc-control-path-confirmation-static.spec.js',
  'package.json 必须提供文控存入路径确认静态契约脚本'
)

assert.match(
  workflowApi,
  /confirmedDirectoryId\?: number/,
  '审批通过请求必须支持文控确认的最终存入目录编号。'
)
assert.match(
  approvalActions,
  /confirmedDirectoryId\?: number/,
  '审批表单必须包含文控确认的最终存入目录编号。'
)
assert.match(
  approvalActions,
  /confirmedDirectoryId:\s*form\.confirmedDirectoryId/,
  '审批提交载荷必须发送文控确认的最终存入目录编号。'
)
assert.match(
  detailPage,
  /getControlledFileUploadDirectoryTree/,
  '第四节点存入路径确认必须使用类别绑定目录树。'
)
assert.match(detailPage, /label="存入路径确认"/, '第四节点弹窗必须显示存入路径确认字段。')
assert.match(
  detailPage,
  /data-testid="dcc-doc-control-confirmed-directory"/,
  '第四节点存入路径确认必须提供稳定测试定位。'
)
assert.match(
  detailPage,
  /confirmedDirectoryId/,
  '第四节点状态和提交载荷必须保存最终存入目录编号。'
)
assert.match(
  detailPage,
  /请选择存入路径/,
  '第四节点审批前必须校验已选择存入路径。'
)

console.log('PASS: DCC doc-control path confirmation static contract')
