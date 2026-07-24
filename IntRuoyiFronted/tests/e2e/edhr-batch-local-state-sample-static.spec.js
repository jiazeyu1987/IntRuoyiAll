const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const listPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'),
  'utf8'
)
const apiFile = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/edhr/batchExecution.ts'),
  'utf8'
)

assert.match(
  apiFile,
  /export type EdhrLocalStateSampleState\s*=/,
  '批次执行 API 必须声明本地状态样本枚举类型。'
)

assert.match(
  apiFile,
  /createEdhrLocalStateSample/,
  '批次执行 API 必须暴露 createEdhrLocalStateSample 调用函数。'
)

assert.match(
  apiFile,
  /\/local-state-sample/,
  '本地状态样本必须调用后端 /local-state-sample 接口。'
)

assert.match(
  listPage,
  /import\.meta\.env\.DEV/,
  '临时状态样本入口必须只在 DEV 环境显示。'
)

assert.match(
  listPage,
  /v-hasPermi="\['mes:pro-edhr-batch-execution:create'\]"/,
  '临时状态样本入口必须复用批次执行创建权限。'
)

assert.match(
  listPage,
  /LOCAL_STATE_SAMPLE/,
  '临时状态样本入口必须带 LOCAL_STATE_SAMPLE 标记，便于后续清理。'
)

assert.match(
  listPage,
  /会写入芋道源码\/admin 当前租户/,
  '点击样本按钮前必须明确提示会写入芋道源码/admin 当前租户。'
)

assert.match(
  listPage,
  /sampleState: sample\.state/,
  '创建成功后跳转详情页必须携带 sampleState 查询参数。'
)

assert.match(
  listPage,
  /release: '1'/,
  '创建成功后跳转详情页必须携带 release=1 查询参数。'
)

for (const [state, label] of [
  ['CLOSE', '关闭批次样本'],
  ['PRECHECK', '放行预检样本'],
  ['RELEASE_APPROVAL', '放行审批样本'],
  ['ARCHIVE', '归档打印样本'],
  ['ARCHIVED', '已归档样本'],
  ['QUALITY_TERMINAL', '质量终态样本']
]) {
  assert.match(listPage, new RegExp(`state: '${state}'`), `列表页必须包含 ${state} 样本状态。`)
  assert.match(listPage, new RegExp(label), `列表页必须展示“${label}”按钮文案。`)
}

assert.match(
  listPage,
  /message\.error\(resolveErrorMessage\(error, '本地状态样本创建失败。'\)\)/,
  '样本创建失败必须 toast 暴露后端错误，不能空 catch 或假成功。'
)

console.log('PASS edhr batch local state sample static contract')
