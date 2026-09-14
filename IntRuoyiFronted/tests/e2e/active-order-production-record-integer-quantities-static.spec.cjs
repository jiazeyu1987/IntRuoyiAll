const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const repoRoot = resolve(__dirname, '../../..')
const component = readFileSync(
  resolve(
    repoRoot,
    'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
  ),
  'utf8'
)

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

assert(
  component.includes('formatIntegerQuantity'),
  '生产记录表单必须使用整数数量格式化函数'
)

assert(
  /const formatIntegerQuantity[\s\S]*return Number\.isFinite\(parsed\) \? String\(Math\.round\(parsed\)\) : String\(value\)/.test(component),
  '数量相关展示必须四舍五入为整数文本，不能保留三位小数'
)

assert(
  !/const formatTraceQuantity[\s\S]*\.toFixed\(3\)/.test(component),
  '生产记录详情不能继续使用 toFixed(3) 展示数量'
)

assert(
  /const normalizeProductionRecordLossQuantity[\s\S]*return 0/.test(component),
  '生产记录损耗数量缺失时必须归一化为 0'
)

assert(
  /totalQuantity:\s*sumProductionRecordQuantities\(outputQuantity,\s*lossQuantity\)/.test(component),
  '生产记录总数量必须等于生产数量加损耗数量'
)

assert(
  /const lossQuantity = normalizeProductionRecordLossQuantity\(material\.lossQuantity\)/.test(
    component
  ) &&
    /lossQuantity:\s*0/.test(component),
  '物料行和事件行缺失损耗都必须显示 0，而不是横杠'
)

console.log('PASS: active order production record integer quantities static contract')
