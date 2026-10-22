const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = process.cwd()
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const page = read('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const api = read('src/api/mes/pro/edhr/nonconformanceReview.ts')

assert(
  page.includes("v-hasPermi=\"['mes:pro-production-release:pqc-reject']\"") &&
    page.includes('data-edhr-batch-action="reject"') &&
    page.includes('handleRejectClick(row)'),
  '驳回按钮必须由上市放行负责人权限控制，并绑定正式驳回 handler。'
)

assert(
  page.includes('message.confirm') &&
    page.includes('rejectDialogVisible') &&
    page.includes('signaturePassword') &&
    page.includes('nonconformanceReason'),
  '驳回必须二次确认并要求填写不合格原因和电子密码。'
)

assert(
  page.includes('rejectEdhrBatchExecutionToNonconformanceReview') &&
    page.includes('await getList()'),
  '驳回成功必须调用正式接口并刷新批次执行列表。'
)

assert(
  /export\s+interface\s+EdhrBatchExecutionRejectReqVO[\s\S]*batchExecutionId[\s\S]*nonconformanceReason[\s\S]*signaturePassword/.test(api),
  '前端 API 必须声明批次驳回请求 VO。'
)

assert(
  api.includes('/reject-batch') &&
    api.includes('rejectEdhrBatchExecutionToNonconformanceReview'),
  '前端 API 必须调用既有不合格评审驳回接口 /reject-batch。'
)

console.log('PASS: edhr-batch-reject-nonconformance-static')
