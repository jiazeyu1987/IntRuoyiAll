const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const workTaskNavigation = readSource('src/utils/edhrWorkTaskNavigation.ts')
const batchDetailPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

assert.match(
  workTaskNavigation,
  /shouldOpenRouteFormDrawer[\s\S]*formSlotType[\s\S]*EDHR_FORM_SLOT_TYPE_MAIN/,
  '统一工作任务导航必须区分 MAIN 正式批记录表单和动态表单槽位，不能只因存在 FormCenter 实例就跳过执行页辅助模式。'
)

assert.match(
  workTaskNavigation,
  /if\s*\(\s*shouldOpenRouteFormDrawer\(opened\)\s*\)[\s\S]*EDHR_BATCH_EXECUTION_DETAIL_PATH[\s\S]*openRouteForm:\s*'1'/,
  '只有非 MAIN 的 FormCenter 动态表单槽位才回到批次详情并携带 openRouteForm=1。'
)

assert.match(
  workTaskNavigation,
  /if\s*\(!executionId\)[\s\S]*throw new Error\('填写任务尚未生成执行记录，无法进入填写工作区。'\)/,
  '仅普通批记录打开缺少 executionId 时才 fail fast。'
)

assert.match(
  batchDetailPage,
  /const autoOpenRouteFormFromRoute\s*=\s*async\s*\(\)[\s\S]*route\.query\.openRouteForm[\s\S]*handleOpenTask\(routeQueryTask\)/,
  '批次详情页必须识别 openRouteForm=1，并通过正式 handleOpenTask 打开动态表单抽屉。'
)

console.log('PASS: eDHR work task FormCenter navigation static contract')
