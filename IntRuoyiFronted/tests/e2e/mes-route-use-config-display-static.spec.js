const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')

assert(fs.existsSync(pagePath), '工艺路线用途配置组件必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(pageSource.includes('应用工作台默认值'), '排产用途配置弹窗必须提供应用工作台默认值入口。')
assert(pageSource.includes('applyWorkbenchScheduleDefaults'), '排产用途配置必须实现应用工作台默认值逻辑。')

const summaryStart = pageSource.indexOf('class="route/flow-config__summary"')
const summaryEnd = pageSource.indexOf('<el-alert', summaryStart)
assert(summaryStart >= 0 && summaryEnd > summaryStart, '配置弹窗摘要区域必须存在。')
const summarySource = pageSource.slice(summaryStart, summaryEnd)

assert(
  !summarySource.includes('{{ useTypeLabel }}'),
  '配置弹窗摘要区域不得继续显示用途标签。'
)
assert(
  !pageSource.includes('route/flow-config__preview'),
  '配置弹窗不得继续显示启用工序预览条。'
)
assert(
  !/<el-table-column\s+v-if="useType === 'SCHEDULE'"\s+label="日历规则"/.test(pageSource),
  '排产用途配置表格不得继续显示日历规则列。'
)
assert(!pageSource.includes('loadCalendarRuleOptions()'), '隐藏日历规则列后不得继续加载日历规则选项。')

assert(!pageSource.includes('label="当前用途启用"'), '排产用途配置表格不得继续显示“当前用途启用”。')
assert(pageSource.includes('label="启用"'), '排产用途配置表格必须显示“启用”列。')
assert(!pageSource.includes('label="有限小时产能"'), '产能模式选项不得继续显示“有限小时产能”。')
assert(!pageSource.includes('label="无限公式产能"'), '产能模式选项不得继续显示“无限公式产能”。')
assert(pageSource.includes('label="有限"'), '产能模式选项必须显示“有限”。')
assert(pageSource.includes('label="无限"'), '产能模式选项必须显示“无限”。')

const hourlyColumnStart = pageSource.indexOf('label="产能(h)"')
const shiftHoursColumnStart = pageSource.indexOf('label="班次小时"')
const standardShiftCapacityColumnStart = pageSource.indexOf('label="标准班次产能"')
const formulaTimeColumnStart = pageSource.indexOf('label="1000产品制作时间(h)"')
assert(hourlyColumnStart >= 0, '排产用途配置表格必须显示产能(h)列。')
assert(!pageSource.includes('label="小时产能"'), '排产用途配置表格不得继续显示“小时产能”列。')
assert(shiftHoursColumnStart > hourlyColumnStart, '产能(h)列后必须显示班次小时列。')
assert(standardShiftCapacityColumnStart > shiftHoursColumnStart, '班次小时列后必须显示标准班次产能列。')
assert(formulaTimeColumnStart > standardShiftCapacityColumnStart, '标准班次产能列后必须显示 1000 产品制作时间列。')
const hourlyColumnSource = pageSource.slice(hourlyColumnStart, shiftHoursColumnStart)
assert(hourlyColumnSource.includes(':precision="0"'), '产能(h)输入必须限制为整数。')
assert(!hourlyColumnSource.includes(':precision="6"'), '产能(h)输入不得继续保留 6 位小数。')
assert(!hourlyColumnSource.includes(':precision="2"'), '产能(h)输入不得继续使用 2 位小数。')
assert(pageSource.includes('positiveInteger(item.hourlyCapacity)'), '保存有限产能时必须校验产能(h)为正整数。')
assert(!pageSource.includes('positiveNumber(item.hourlyCapacity)'), '保存有限产能时不得继续只校验正数。')
assert(pageSource.includes('产能(h)必须是大于 0 的整数'), '正整数校验失败时必须暴露产能(h)错误提示。')
assert(pageSource.includes('calculateStandardShiftCapacity(scope.row)'), '标准班次产能必须按每小时产能和班次小时实时计算。')
assert(pageSource.includes('formatIntegerCapacity'), '标准班次产能必须按整数展示。')

assert(!pageSource.includes('label="系数"'), '排产用途配置表格不得继续显示独立“系数”列。')
assert(!pageSource.includes('label="固定值"'), '排产用途配置表格不得继续显示独立“固定值”列。')
assert(!pageSource.includes('label="公式 a"'), '排产用途配置表格不得继续显示“公式 a”。')
assert(!pageSource.includes('label="公式 b(分钟)"'), '排产用途配置表格不得继续显示“公式 b(分钟)”。')

const formulaTimeColumnEnd = pageSource.indexOf('label="夜班"', formulaTimeColumnStart)
assert(formulaTimeColumnEnd > formulaTimeColumnStart, '1000 产品制作时间列必须位于夜班列之前。')
const formulaTimeColumnSource = pageSource.slice(formulaTimeColumnStart, formulaTimeColumnEnd)
assert.match(
  formulaTimeColumnSource,
  /scope\.row\.capacityMode === 'INFINITE_FORMULA'[\s\S]*openFormulaTimeDialog\(scope\.row\)/,
  '1000 产品制作时间列必须仅在无限公式产能模式下可点击打开弹框。'
)
assert.match(formulaTimeColumnSource, /--/, '有限小时产能模式下必须显示 --。')
assert.match(formulaTimeColumnSource, /buildFormulaTimeLabel\(scope\.row\)/, '公式模式必须通过统一方法生成显示文本。')
assert(pageSource.includes("'未设置'"), '公式参数缺失时必须显示未设置。')

assert(pageSource.includes('formulaTimeDialogVisible'), '页面必须包含 1000 产品制作时间配置弹框状态。')
assert(pageSource.includes('title="1000产品制作时间配置"'), '弹框标题必须是 1000产品制作时间配置。')
assert(pageSource.includes('label="系数 a(小时/件)"'), '弹框必须按小时展示系数 a 输入。')
assert(pageSource.includes('label="固定值 b(小时)"'), '弹框必须按小时展示固定值 b 输入。')
assert(pageSource.includes('1000 * a + b'), '弹框必须展示 1000 * a + b 公式。')
assert(pageSource.includes('formulaTimeResultHours'), '弹框必须实时计算 1000 产品制作时间小时结果。')
assert(pageSource.includes('submitFormulaTimeDialog'), '弹框必须通过确认动作更新当前行本地数据。')

assert.match(pageSource, /\/\s*60/, '读取后端分钟口径 a/b 时必须转换为小时展示。')
assert.match(pageSource, /\*\s*60/, '保存到后端分钟口径 a/b 时必须由小时转换为分钟。')

console.log('PASS: MES route flow config display static contract')
