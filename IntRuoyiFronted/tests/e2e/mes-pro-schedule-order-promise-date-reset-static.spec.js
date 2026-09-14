const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

assert(fs.existsSync(pagePath), '排产工单页面必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(
  /<el-button[\s\S]*v-hasPermi="\['mes:pro-schedule-order:update'\]"[\s\S]*@click="openPromiseDateDialog\(row\)"[\s\S]*>\s*交期\s*<\/el-button>/.test(
    pageSource
  ),
  '排产工单行操作必须保留受 update 权限保护的两字“交期”入口。'
)
assert(
  pageSource.includes('<Dialog v-model="promiseDateDialogVisible" title="设置承诺交期"'),
  '设置交期必须使用独立的“设置承诺交期”弹窗。'
)
assert(pageSource.includes('promiseDateForm.promiseDate'), '设置交期弹窗必须维护新的承诺交期表单值。')
assert(pageSource.includes('promiseDateForm.reason'), '设置交期弹窗必须要求填写修改原因。')
assert(
  pageSource.includes('const openPromiseDateDialog = (row: MesProScheduleOrderVO) =>'),
  '设置交期入口必须有独立打开逻辑。'
)
assert(
  pageSource.includes('const submitPromiseDateReset = async () =>'),
  '设置交期弹窗必须有独立提交逻辑。'
)
assert(
  pageSource.includes("message.warning('承诺交期不能为空')") &&
    pageSource.includes("message.warning('修改原因不能为空')"),
  '设置交期提交必须 fail fast 校验承诺交期和修改原因。'
)
assert(
  /MesProScheduleOrderApi\.updateScheduleOrder\(\{[\s\S]*id: promiseDateForm\.id[\s\S]*promiseDate: promiseDateForm\.promiseDate[\s\S]*priorityNo[\s\S]*remark[\s\S]*reason: promiseDateForm\.reason/.test(
    pageSource
  ),
  '设置交期必须复用排产工单更新接口，并保留原优先级和备注。'
)
assert(pageSource.includes("message.success('承诺交期已更新')"), '设置交期成功后必须提示承诺交期已更新。')
assert(pageSource.includes('await getScheduleOrderList()'), '设置交期成功后必须刷新排产工单列表。')

console.log('PASS: MES schedule order promise date reset static contract')
