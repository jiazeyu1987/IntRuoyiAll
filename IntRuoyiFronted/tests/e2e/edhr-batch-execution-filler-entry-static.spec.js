const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/edhr/batchExecution.ts')
const realE2ePath = path.resolve(process.cwd(), 'tests/e2e/edhr-batch-execution-real-flow.e2e.js')

const source = fs.readFileSync(pagePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')
const realE2e = fs.readFileSync(realE2ePath, 'utf8')

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

assert.ok(
  realE2e.includes('function queryLocalDatabase') && realE2e.includes('function resolveDatabaseFixture'),
  '真实批次执行 E2E 必须从本地数据库读取授权夹具，不得要求人工注入工单、批次或签名环境变量。'
)

assert.ok(
  realE2e.includes('JOIN mes_pro_edhr_work_task wt') &&
    realE2e.includes("wt.task_type IN ('FILL', 'REWORK')") &&
    realE2e.includes('wt.assignee_user_id =') &&
    realE2e.includes('NOT EXISTS') &&
    realE2e.includes('previous_task.status NOT IN (40, 45)'),
  'Real batch execution E2E database fixture must select only current-user assigned FILL/REWORK tasks whose sequence gate is open.'
)

assert.doesNotMatch(
  realE2e,
  /const REQUIRED_ENV|EDHR_BATCH_E2E_PASSWORD|EDHR_BATCH_E2E_WORK_ORDER_ID|EDHR_BATCH_E2E_BATCH_CODE|EDHR_BATCH_E2E_FIRST_FIELD_VALUE|EDHR_BATCH_E2E_CLOSE_PASSWORD/,
  '真实批次执行 E2E 不得再声明必需 EDHR_BATCH_E2E_* 数据环境变量。'
)

assert.ok(
  realE2e.includes("envValue('EDHR_BATCH_E2E_BASE_URL')") &&
    realE2e.includes("envValue('EDHR_BATCH_E2E_BACKEND_URL')") &&
    realE2e.includes('validateLocalRuntimePair'),
  '真实批次执行 E2E 必须显式校验 worktree 前后端本机 URL 配对，不能只覆盖前端或静默回退到 8081/48081。'
)

assert.ok(
  realE2e.includes("const accessToken = readCacheValue('ACCESS_TOKEN')") &&
    realE2e.includes("headers.Authorization = `Bearer ${accessToken}`") &&
    realE2e.includes("headers['tenant-id'] = String(tenantId)"),
  '真实批次执行 E2E 的执行详情只读核验必须复用浏览器登录态 Authorization 和 tenant-id。'
)

console.log('PASS: EDHR batch execution list edit entry static contract')
