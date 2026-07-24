const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/edhr/batchExecution.ts')

const source = fs.readFileSync(pagePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

assert.ok(
  !source.includes('@click="openCurrentUserFillTask(row)"') &&
    !source.includes('>去填写<') &&
    !source.includes('const openCurrentUserFillTask = async'),
  '批次执行列表操作列不得继续提供直接“去填写”入口。'
)

assert.ok(
  source.includes('@click="openDetail(row)">编辑</el-button>') &&
    !source.includes('@click="openDetail(row)">查看详情</el-button>'),
  '批次执行列表操作列必须使用“编辑”进入批次详情页。'
)

assert.ok(
  api.includes('workTaskId?: number') &&
    api.includes('activeWorkTaskId?: number') &&
    api.includes('allowedActions?: string[]') &&
    api.includes('activeWorkTaskType?: string'),
  '批次执行任务 API 类型必须暴露打开请求 workTaskId、activeWorkTaskId、allowedActions 和 activeWorkTaskType。'
)

assert(!/mock|降级|静默跳过/.test(source), '批次执行填写人入口不得引入 mock、降级或静默跳过。')

console.log('PASS: EDHR batch execution list edit entry static contract')
