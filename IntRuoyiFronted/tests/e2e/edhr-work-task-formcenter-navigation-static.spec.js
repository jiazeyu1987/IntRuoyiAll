const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const workTaskNavigation = readSource('src/utils/edhrWorkTaskNavigation.ts')
const batchDetailPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

assert.match(
  workTaskNavigation,
  /opened\?\.formCenterInstanceId[\s\S]*opened\?\.formTemplateId[\s\S]*EDHR_BATCH_EXECUTION_DETAIL_PATH[\s\S]*openRouteForm:\s*'1'/,
  '个人工作台打开 FormCenter 动态表单时必须回到批次详情并携带 openRouteForm=1，而不是强制要求 executionId。'
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
