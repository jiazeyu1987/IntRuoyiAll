const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const loginForm = read('src/views/Login/components/LoginForm.vue')
const useLogin = read('src/views/Login/components/useLogin.ts')
const batchList = read('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const batchDetail = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const workTaskBoard = read('src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue')

assert.match(
  useLogin,
  /resolveLoginErrorMessage/,
  'P0 登录错误必须有集中分类函数 resolveLoginErrorMessage。'
)
assert.match(useLogin, /后端服务不可达|租户识别失败|账号或密码错误|权限不足/, 'P0 登录错误提示必须区分后端、租户、账号密码和权限。')
assert.match(loginForm, /loginErrorMessage/, 'P0 登录页必须渲染登录错误提示区域，而不是只依赖全局 toast。')

assert.match(batchList, /作废|openVoidChangePage/, 'P1 批次列表必须提供作废变更入口。')
assert.match(batchList, /流程追踪|openFlowTraceDialog/, 'P1 批次追溯入口必须保留流程追踪能力。')
assert.match(batchList, /batchFlowTraceDialogVisible/, 'P1 批次列表必须提供流程追踪对话框。')
assert.match(batchList, /executionReviews|approvalRecords|archiveVersions|batchEvents/, 'P1 流程追踪必须使用正式批次复盘时间线字段。')

assert.match(workTaskBoard, /实际派发源|candidateSourceId|candidateSourceType/, 'P1 归档规则必须展示真实派发源字段。')
assert.match(workTaskBoard, /已保存，责任人与实际(派发源|责任源)一致/, 'P1 归档规则保存成功提示必须明确派发源一致。')

assert.match(batchDetail, /体验检查清单|uxChecklistDrawerVisible/, 'P2 批次详情填写链路必须保留页面体验检查清单能力。')
assert.match(batchDetail, /签名清晰度|文字遮挡|按钮命名|单位\/输入类型|历史记录入口/, 'P2 页面体验检查清单必须覆盖签名、遮挡、按钮、单位和历史入口。')
assert.match(batchList, /操作轨迹|历史记录|openOperationHistoryDialog/, 'P2 批次追溯入口必须保留历史记录或操作轨迹能力。')

console.log('PASS: eDHR P0/P1/P2 experience resolution static contract')
