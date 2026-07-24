const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(workspaceRoot, 'yudao-ui-admin-vue3')
const backendRoot = path.join(workspaceRoot, 'ruoyi-vue-pro')

const readText = (filePath) => fs.readFileSync(filePath, 'utf8')

const workOrderPage = readText(path.join(frontendRoot, 'src/views/mes/pro/workorder/index.vue'))
const schedulerWorkbenchSql = readText(
  path.join(backendRoot, 'sql/mysql/20260610_mes_scheduler_workbench_p7.sql')
)
const scheduleOrderSql = readText(path.join(backendRoot, 'sql/mysql/20260610_mes_schedule_order_p1.sql'))

assert(!workOrderPage.includes('????'), '生产工单页面源码不能包含问号乱码。')
assert(
  workOrderPage.includes('金蝶工单同步完成，新增'),
  '生产工单金蝶同步成功提示必须是可读中文。'
)

assert(
  schedulerWorkbenchSql.includes("'排产员工作台'"),
  '排产员工作台菜单 SQL 必须保留正确中文名称。'
)
assert(!schedulerWorkbenchSql.includes('????'), '排产员工作台菜单 SQL 不能包含问号乱码。')
assert(
  scheduleOrderSql.includes("'排产工单池'"),
  '排产工单池菜单 SQL 必须保留正确中文名称。'
)
assert(!scheduleOrderSql.includes('????'), '排产工单池菜单 SQL 不能包含问号乱码。')

console.log('PASS: MES production menu copy static contract')
