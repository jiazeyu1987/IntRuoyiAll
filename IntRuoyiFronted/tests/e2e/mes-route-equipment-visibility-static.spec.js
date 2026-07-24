const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const routeFormPath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteForm.vue')
const routeProcessListPath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteProcessList.vue')

assert(fs.existsSync(routeFormPath), '工艺路线表单必须存在。')
assert(fs.existsSync(routeProcessListPath), '工艺路线组成工序表必须存在。')

const routeFormSource = fs.readFileSync(routeFormPath, 'utf8')
const routeProcessListSource = fs.readFileSync(routeProcessListPath, 'utf8')

assert(
  routeFormSource.includes('<RouteProcessList :routeId="formData.id" :form-type="formType" />'),
  '工艺路线详情必须继续通过组成工序主表承载排产资源信息，避免用户误以为设备信息已丢失。'
)
assert(
  !routeFormSource.includes('label="设备信息"'),
  '工艺路线详情不应再保留误加的设备信息页签，避免和最终职责边界冲突。'
)
assert(
  routeProcessListSource.includes('label="资源类型"'),
  '组成工序主表必须继续展示资源类型，避免排产资源信息被精简掉。'
)
assert(
  routeProcessListSource.includes('label="标准资源"'),
  '组成工序主表必须继续展示标准资源入口，方便用户查看当前路线设备/人工配置。'
)
assert(
  routeProcessListSource.includes('label="标准班次产能"'),
  '组成工序主表必须继续展示标准班次产能，避免排产能力信息丢失。'
)
assert(
  routeProcessListSource.includes('label="工作站"'),
  '组成工序主表必须继续展示工作站列，避免用户无法定位当前资源归属工位。'
)
assert(
  routeProcessListSource.includes('openMachineryDetail(') ||
    routeProcessListSource.includes('openProcessCapacityDetail('),
  '组成工序主表必须保留查看设备/产能详情的链路。'
)

console.log('PASS: MES route equipment visibility static contract')
