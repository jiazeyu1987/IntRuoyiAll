const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const executionPagePath = path.join(
  repoRoot,
  'src/views/mes/pro/edhr/ExecutionPage.vue'
)
const cellLinkApiPath = path.join(
  repoRoot,
  'src/api/mes/pro/batchrecordcelllink/index.ts'
)

const executionPage = fs.readFileSync(executionPagePath, 'utf8')
const cellLinkApi = fs.readFileSync(cellLinkApiPath, 'utf8')

assert(
  /getPrefill:\s*async\s*\(\s*targetExecutionId:\s*number,\s*workTaskId\?:\s*number\s*\)/.test(cellLinkApi),
  'BatchRecordCellLinkApi.getPrefill 必须接收可选 workTaskId，避免填写任务打开时误走配置查询权限。'
)

assert(
  /params:\s*\{\s*targetExecutionId,\s*workTaskId\s*\}/.test(cellLinkApi),
  'BatchRecordCellLinkApi.getPrefill 必须把 workTaskId 传给后端做工作任务级校验。'
)

assert(
  /BatchRecordCellLinkApi\.getPrefill\(currentExecutionId,\s*workTaskId\.value\)/.test(executionPage),
  'eDHR 填写页加载跨表单带入时必须携带当前 workTaskId。'
)

assert(
  /const hasFieldAuditUpdatePermission = computed\(\s*\(\) =>[\s\S]*hasPermission\(\[FIELD_AUDIT_UPDATE_PERMISSION\]\)[\s\S]*workTaskId\.value !== undefined\s*&&\s*hasExecutionUpdatePermission\.value[\s\S]*hasGoldenFingerPermission\.value[\s\S]*\)/.test(executionPage),
  '填写任务保存字段变更应保留“当前 workTaskId + execution:update”的动态任务权限，并额外允许金手指测试权限。'
)

assert(
  !/v-hasPermi="\['mes:pro-batch-record-execution:field-audit-update'\]"[\s\S]{0,160}class="edhr-fill-workspace__primary-action"/.test(executionPage),
  '填写工作区保存按钮不能被静态 field-audit-update 权限指令移除，应由页面 gate 展示明确错误。'
)

console.log('PASS edhr-fill-workspace-worktask-permission-static')
