const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const processPage = read('src/views/mes/pro/process/index.vue')
const processApi = read('src/api/mes/pro/process/index.ts')
const batchTemplatePage = read('src/views/mes/pro/batchrecordformlist/index.vue')
const batchReportApi = read('src/api/mes/pro/batchrecordreport/index.ts')

assert.ok(
  processApi.includes('batchRecordForms?: ProProcessBatchRecordFormLinkVO[]'),
  '工序 API 类型必须提供批记录表单结构化链接字段。'
)
assert.ok(
  !processApi.includes('ProProcessFillerLinkVO') &&
    !processApi.includes('productionFillers') &&
    !processApi.includes('qualityFillers') &&
    !processApi.includes('equipmentFillers'),
  '工序 API 类型必须移除生产、质量、设备填写人结构化链接字段。'
)
assert.ok(
  processPage.includes('openBatchRecordForm') &&
    processPage.includes('/mes/pro/batch-record-form-list'),
  '工序设置红框列必须保留批记录表单点击跳转入口。'
)
assert.ok(
  processPage.includes('scope.row.batchRecordForms'),
  '工序设置红框列必须消费批记录表单结构化字段，不得只展示名称字符串。'
)
assert.ok(
  batchReportApi.includes('reportId?: string') &&
    batchTemplatePage.includes('route.query.reportId') &&
    batchTemplatePage.includes('selectedReportId.value = reportId.trim()'),
  '批记录模板页必须支持 reportId 查询参数并精确定位目标表单。'
)

for (const legacyToken of ['openFillerTarget', '/system/role/permission-role', '/system/dept', '/system/user']) {
  assert.ok(!processPage.includes(legacyToken), `工序设置列表必须移除旧填写人跳转入口：${legacyToken}`)
}

console.log('mes-pro-process-batch-record-click-through-static passed')
