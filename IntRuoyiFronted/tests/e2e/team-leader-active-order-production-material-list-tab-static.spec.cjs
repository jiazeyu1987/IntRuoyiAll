const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const detailPage = read('src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue')
const detailPanel = read(
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const teamLeaderApi = read('src/api/mes/pro/processpool/teamLeader.ts')
const productionMaterialListStart = detailPanel.indexOf('label="生产用料清单"')
const workOrderTabStart = detailPanel.indexOf('label="生产工单"', productionMaterialListStart)
assert.ok(productionMaterialListStart >= 0, '详情面板必须新增“生产用料清单”主 tab。')
assert.ok(
  workOrderTabStart > productionMaterialListStart,
  '生产用料清单 tab 必须位于生产工单 tab 之前。'
)
const productionMaterialListTab = detailPanel.slice(productionMaterialListStart, workOrderTabStart)

assert.match(
  teamLeaderApi,
  /getTeamLeaderActiveOrderProductionMaterialLists\s*=\s*async[\s\S]*request\.get<ErpProductionMaterialListVO\[\]>\([\s\S]*\/mes\/pro\/process-pool\/team-leader\/active-order\/production-material-lists[\s\S]*params:\s*\{\s*activeOrderId\s*\}/,
  '前端必须使用按活跃订单限定归属的 MES 正式生产用料清单只读接口。'
)

assert.match(
  detailPage,
  /loadProductionMaterialLists\s*=\s*async\s*\(activeOrderId:[\s\S]*getTeamLeaderActiveOrderProductionMaterialLists\(activeOrderId\)/,
  '详情页必须以当前活跃订单身份查询完整正式用料清单，由服务端解析工单归属。'
)

assert.doesNotMatch(
  detailPage,
  /ErpProductionMaterialListApi|productionOrderNo/,
  '详情页不得继续依赖 ERP 通用分页查询或客户端工单编码。'
)

assert.match(
  detailPage,
  /productionMaterialLists[\s\S]*ActiveOrderSubmissionDetailPanel/,
  '详情页必须把正式生产用料清单明细传给展示面板。'
)

assert.match(
  productionMaterialListTab,
  /label="生产用料清单"[\s\S]*data-team-leader-active-order-detail-production-material-list-tab/,
  '详情面板必须新增“生产用料清单”主 tab。'
)

assert.match(
  productionMaterialListTab,
  /productionMaterialListDocuments[\s\S]*sourceBillNo/,
  '生产用料清单展示必须按单据编号分组，支持同一生产订单多张单据。'
)

for (const label of [
  '生产订单号',
  '生产车间',
  '单据编号',
  '产品代码',
  '产品名称',
  '规格型号',
  '单位',
  '生产数量',
  '生产组织',
  '序号',
  '物料编码',
  '物料名称',
  '应发数量',
  '实际用量',
  '需求日期',
  '仓库',
  '发料方式',
  '创建人',
  '创建日期',
  '审核人',
  '审核日期'
]) {
  assert.match(
    productionMaterialListTab,
    new RegExp(label),
    `生产用料清单纸质样式必须显示 ${label}。`
  )
}

assert.doesNotMatch(
  productionMaterialListTab,
  /pickListMaterials|replenishmentMaterials/,
  '生产用料清单 tab 不得使用领料单或补料单集合冒充。'
)

console.log('PASS team-leader-active-order-production-material-list-tab-static')
