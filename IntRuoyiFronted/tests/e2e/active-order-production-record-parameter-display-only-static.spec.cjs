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

function extractFunction(name) {
  const start = component.indexOf(`const ${name} = (`)
  assert(start >= 0, `missing function ${name}`)
  const next = component.indexOf('\nconst ', start + 1)
  assert(next > start, `cannot isolate function ${name}`)
  return component.slice(start, next)
}

const valueFormatter = extractFunction('formatProductionRecordParameterValue')

assert(
  valueFormatter.includes('parameter.textValue || parameter.value'),
  '生产记录表单提交值必须直接读取模拟/一线提交 payload 的 textValue/value'
)

assert(
  valueFormatter.includes('parameter.unit') && valueFormatter.includes('return `${value}${parameter.unit || \'\'}'),
  '生产记录表单只能在展示时追加单位，不能把单位写回提交值'
)

for (const forbidden of ['lowerLimit', 'upperLimit', 'standardText', 'defaultText', 'Math.', 'divide(']) {
  assert(
    !valueFormatter.includes(forbidden),
    `生产记录表单不能用 ${forbidden} 计算或推断提交值，只能显示已提交值`
  )
}

console.log('PASS: active order production record parameter display-only static contract')
