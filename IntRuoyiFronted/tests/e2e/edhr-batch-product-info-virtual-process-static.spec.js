const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const detail = fs.readFileSync(detailPath, 'utf8').replace(/\r\n/g, '\n')

assert(
  detail.includes('const isProductInfoProcessTask = (task: EdhrBatchExecutionTaskRespVO) =>'),
  '批次详情必须识别产品信息任务，避免它被同 routeProcessId 合并到第一个工序。'
)
assert(
  detail.includes('const buildProcessTaskGroupKey = (task: EdhrBatchExecutionTaskRespVO) =>') &&
    detail.includes("isProductInfoProcessTask(task)") &&
    detail.includes("`product-info:${task.batchRecordReportId || task.id}`"),
  '左侧工序分组 key 必须把产品信息任务拆成独立 80 工序组。'
)
assert(
  detail.includes('const PRODUCT_INFO_PROCESS_SORT = 80') &&
    detail.includes("PRODUCT_INFO_PROCESS_NAME = '产品信息'"),
  '前端必须固定识别产品信息虚拟工序为 80 / 产品信息。'
)
assert(
  detail.includes('const key = buildProcessTaskGroupKey(task)'),
  'processTaskGroups 必须使用产品信息专用分组 key。'
)
assert(
  detail.includes('processName: resolveProcessTaskGroupName(task)') &&
    detail.includes('routeProcessSort: resolveProcessTaskGroupSort(task)'),
  '产品信息左侧工序名称和序号必须来自虚拟 80 工序解析。'
)

console.log('PASS: eDHR batch product info is rendered as an independent virtual process 80.')
