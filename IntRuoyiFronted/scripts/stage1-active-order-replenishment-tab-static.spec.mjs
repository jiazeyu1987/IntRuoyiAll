import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const componentPath = resolve(
  root,
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const apiPath = resolve(root, 'src/api/mes/pro/processpool/teamLeader.ts')

const component = readFileSync(componentPath, 'utf8')
const api = readFileSync(apiPath, 'utf8')

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

assert(
  component.includes('label="补料单"') &&
    component.includes('name="replenishmentMaterials"') &&
    component.includes('data-team-leader-active-order-detail-replenishment-tab'),
  '工序提交详情主 tab 必须新增“补料单”'
)

assert(
  component.includes('const replenishmentMaterials = computed') &&
    component.includes('process.supplementMaterials ?? []') &&
    component.includes('sourceReplenishmentListNos') &&
    component.includes('暂无补料单物料批号'),
  '补料单 tab 必须从后端 supplementMaterials 正式集合渲染并提供空态'
)

assert(
  api.includes('TeamLeaderActiveOrderSupplementMaterialDetailRespVO') &&
    api.includes('supplementMaterials?: TeamLeaderActiveOrderSupplementMaterialDetailRespVO[]') &&
    api.includes('sourceReplenishmentListIds') &&
    api.includes('sourceReplenishmentListNos') &&
    api.includes('sourceReplenishmentListItemIds'),
  '前端 API 类型必须声明补料单集合字段和来源单据身份'
)

console.log('PASS: Stage1 active order replenishment tab static contract')
