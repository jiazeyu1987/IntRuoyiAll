const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const panelPath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const apiPath = path.join(frontendRoot, 'src/api/mes/pro/processpool/teamLeader.ts')
const backendRoot = path.resolve(frontendRoot, '../IntRuoyiBackend')
const backendModelPath = path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java'
)
const backendVoPath = path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java'
)
const backendServicePath = path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java'
)

const read = (filePath) => fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n')

const panel = read(panelPath)
const api = read(apiPath)
const backendModel = read(backendModelPath)
const backendVo = read(backendVoPath)
const backendService = read(backendServicePath)

const materialsTabStart = panel.indexOf('name="materials"')
const materialsTabEnd = panel.indexOf('</el-tab-pane>', materialsTabStart)
assert.ok(materialsTabStart >= 0 && materialsTabEnd > materialsTabStart, '领料单页签必须存在。')
const materialsTab = panel.slice(materialsTabStart, materialsTabEnd)

assert.doesNotMatch(materialsTab, /<el-table-column label="领料单"/, '领料单页签不得继续渲染物料行级领料单列。')

for (const label of ['生产领料单号', '单据状态', '单据日期', '生产订单编号']) {
  assert.doesNotMatch(materialsTab, new RegExp(`<el-table-column label="${label}"`), `${label}不得继续作为明细表列。`)
  assert.match(materialsTab, new RegExp(`>${label}：<`), `${label}必须在单据表头中展示。`)
}
assert.match(materialsTab, /pickListDocuments/, '领料单页签必须按正式领料单分组渲染。')
assert.match(materialsTab, /data-active-order-pick-list-document/, '领料单单据分组必须有稳定页面标识。')
assert.match(materialsTab, /<el-table-column label="物料编码"/, '单据明细表必须保留物料明细字段。')

assert.match(api, /sourcePickListDocuments:\s*TeamLeaderActiveOrderPickListDocumentRespVO\[\]/, '前端 API 类型必须承载逐单领料单表头资料。')
assert.match(backendModel, /List<SourcePickListDocument> sourcePickListDocuments/, '后端详情模型必须承载逐单领料单表头资料。')
assert.match(backendVo, /List<SourcePickListDocument> sourcePickListDocuments/, '后端详情 VO 必须返回逐单领料单表头资料。')

assert.match(
  backendService,
  /ErpKingdeeProductionPickListMapper[\s\S]*selectBatchIds[\s\S]*getDocumentStatus/,
  '后端必须从正式 ERP 领料单表头按来源 ID读取单据状态。'
)
assert.match(
  backendService,
  /getBillNo[\s\S]*getBillDate[\s\S]*getProductionOrderNos|setSourcePickListDocuments/,
  '后端必须映射单据日期和生产订单编号。'
)

console.log('PASS: active-order pick-list basic columns static contract')
