const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const componentPath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const apiPath = path.join(frontendRoot, 'src/api/mes/pro/processpool/teamLeader.ts')

const component = fs.readFileSync(componentPath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

assert.match(
  api,
  /export interface TeamLeaderActiveOrderPqcSubmissionDetailRespVO \{[\s\S]*?scrapQuantity\?: number[\s\S]*?\n\}/,
  'PQC 提交详情类型必须暴露 scrapQuantity。'
)
assert.match(
  component,
  /<el-tab-pane[\s\S]*label="生产过程损耗报告单"[\s\S]*name="pqcLossReport"[\s\S]*data-active-order-pqc-loss-report-tab/,
  '详情页必须新增“生产过程损耗报告单”主 tab。'
)
assert.match(
  component,
  /data-active-order-pqc-loss-report-table[\s\S]*产品名称[\s\S]*型号规格[\s\S]*批号[\s\S]*生产数量/,
  '损耗报告单必须展示产品名称、型号规格、批号、生产数量。'
)
assert.match(
  component,
  /不合格日期[\s\S]*工序名称[\s\S]*不合格数量[\s\S]*不合格原因[\s\S]*处置方式[\s\S]*生产人员\/日期[\s\S]*检验人员确认日期[\s\S]*批准人\/日期/,
  '损耗报告单明细列必须符合纸质单据字段。'
)
assert.match(
  component,
  /const pqcLossReportRows = computed[\s\S]*scrapQuantity[\s\S]*Number\(submission\.scrapQuantity\)/,
  '损耗报告单必须按一线 PQC 提交 scrapQuantity 生成行。'
)
assert.doesNotMatch(
  component.match(/const pqcLossReportRows = computed[\s\S]*?\n\}\)/)?.[0] ?? '',
  /lossQuantity/,
  '损耗报告单不能从生产提交 lossQuantity 推断。'
)
assert.match(
  component,
  /暂无生产过程损耗记录/,
  '无 PQC 损耗时必须显示明确空态。'
)

console.log('PASS: active-order PQC loss report tab static contract')
