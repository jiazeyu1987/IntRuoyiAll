const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const approvalActions = readSource('src/views/dcc/controlled-file/detail/approval-actions.ts')
const realE2e = readSource('tests/e2e/dcc-doc-control-department-distribution-real.e2e.js')

assert.equal(
  packageJson.scripts['e2e:dcc:doc-control-department-distribution:static'],
  'node tests/e2e/dcc-doc-control-department-distribution-static.spec.js',
  'package.json 必须提供文控部门下发范围静态契约脚本'
)

assert.match(
  workflowApi,
  /export interface ControlledFileDistributionScopeVO[\s\S]*departmentId: number[\s\S]*distributionMedium: ControlledFileDistributionMedium/,
  '审批通过请求必须声明部门与下发介质明细类型。'
)
assert.match(
  workflowApi,
  /selectedDistributionScopes\?: ControlledFileDistributionScopeVO\[\]/,
  '审批通过请求必须发送部门与下发介质明细。'
)
assert.match(
  approvalActions,
  /selectedDistributionScopes\?: ControlledFileDistributionScopeVO\[\]/,
  '审批表单必须包含部门与下发介质明细。'
)
assert.doesNotMatch(
  approvalActions,
  /selectedDistributionDepartmentIds:\s*form\.selectedDistributionDepartmentIds/,
  '审批提交载荷不得继续把部门编号隐式当作电子下发范围。'
)
assert.match(
  approvalActions,
  /selectedDistributionScopes:\s*form\.selectedDistributionScopes/,
  '审批提交载荷必须发送部门与下发介质明细。'
)

assert.match(detailPage, /label="文件下发范围"/, '第四节点弹窗必须显示文件下发范围字段。')
assert.match(
  detailPage,
  /data-testid="dcc-doc-control-distribution-departments"/,
  '第四节点文件下发范围必须提供稳定测试定位。'
)
assert.match(detailPage, /el-tree-select/, '文件下发范围必须使用部门树多选控件。')
assert.match(
  detailPage,
  /selectedDistributionScopes/,
  '第四节点状态和提交载荷必须保存部门与下发介质明细。'
)
assert.match(detailPage, /PUBLIC_FOLDER/, '第四节点下发介质必须支持电子公共文件夹。')
assert.match(detailPage, /PAPER/, '第四节点下发介质必须支持纸质下发。')
assert.match(detailPage, /getSimpleDeptList\(\)/, '部门树必须来自系统真实部门精简列表。')
assert.match(
  detailPage,
  /buildDepartmentTreeOptions/,
  '页面必须把真实部门列表组织成树形下发范围选项。'
)
assert.match(
  detailPage,
  /selectedDistributionScopes\.length[\s\S]*请选择文件下发范围/,
  '第四节点审批前必须校验至少选择一个下发部门。'
)
assert.doesNotMatch(
  detailPage,
  /label="电子发放接收人"/,
  '第四节点不应继续要求文控按人员选择电子发放接收人。'
)

assert.match(
  realE2e,
  /confirmedDirectoryId/,
  '真实 E2E 必须断言第四节点审批请求包含确认后的存入路径。'
)
assert.match(
  realE2e,
  /selectedDistributionScopes/,
  '真实 E2E 必须断言第四节点审批请求包含部门与下发介质明细。'
)
assert.doesNotMatch(
  realE2e,
  /selectedDistributionDepartmentIds/,
  '真实 E2E 不得继续断言旧的部门数组字段。'
)

console.log('PASS: DCC doc-control department distribution static contract')
