import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const pagePath = resolve(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const apiPath = resolve(root, 'src/api/mes/pro/processpool/teamLeader.ts')

const page = readFileSync(pagePath, 'utf8')
const api = readFileSync(apiPath, 'utf8')

const assert = (condition, message) => {
  if (!condition) {
    throw new Error(message)
  }
}

const handleStage1Match = page.match(
  /const handleSimulateStage1 = async \(row: TeamLeaderActiveOrderRespVO\) => \{[\s\S]*?\n\}/
)
assert(handleStage1Match, '未找到 handleSimulateStage1')
const handleStage1 = handleStage1Match[0]

const detailEntryMatch = page.match(
  /const openActiveOrderSubmissionDetail = \(row: TeamLeaderActiveOrderRespVO\) => \{[\s\S]*?\n\}/
)
assert(detailEntryMatch, '未找到 openActiveOrderSubmissionDetail')
const detailEntry = detailEntryMatch[0]

const requestTypeMatch = api.match(
  /export interface Stage1ActiveOrderCompleteSimulationReqVO \{[\s\S]*?\n\}/
)
assert(requestTypeMatch, '未找到 Stage1ActiveOrderCompleteSimulationReqVO')
const requestType = requestTypeMatch[0]

assert(
  requestType.includes('activeOrderId: number'),
  'Stage1 请求必须使用 activeOrderId 表示当前点击的活跃订单'
)
assert(
  !requestType.includes('templateActiveOrderId'),
  'Stage1 请求不能继续暴露 templateActiveOrderId，避免把点击订单误当复制模板'
)
assert(
  handleStage1.includes('const activeOrderId = requirePositiveNumber(row.id'),
  'Stage1 点击处理必须以当前行 row.id 作为 activeOrderId'
)
assert(
  handleStage1.includes('activeOrderId'),
  'Stage1 点击处理必须把 activeOrderId 传给后端'
)
assert(
  !handleStage1.includes('templateActiveOrderId'),
  'Stage1 点击处理不能继续使用 templateActiveOrderId'
)
assert(
  !handleStage1.includes('stage1GeneratedDetailTargets.value.set'),
  'Stage1 模拟后不能记录源订单到 STAGE1 生成订单的详情跳转映射'
)
assert(
  handleStage1.includes('navigateActiveOrderSubmissionDetail(activeOrderId)'),
  'Stage1 模拟完成后必须打开当前点击活跃订单详情'
)
assert(
  !detailEntry.includes('stage1GeneratedTarget'),
  '普通详情入口不能再优先打开 Stage1 生成活跃订单'
)
assert(
  detailEntry.includes('navigateActiveOrderSubmissionDetail(sourceActiveOrderId)'),
  '普通详情入口必须打开当前行自己的活跃订单详情'
)

console.log('stage1 clicked order identity static contract passed')
