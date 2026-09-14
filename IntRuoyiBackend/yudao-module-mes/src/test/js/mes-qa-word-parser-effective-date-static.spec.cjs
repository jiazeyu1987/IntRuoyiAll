const assert = require('assert')
const fs = require('fs')
const path = require('path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const parser = fs.readFileSync(path.join(moduleRoot, 'src', 'main', 'java', 'cn', 'iocoder',
  'yudao', 'module', 'mes', 'service', 'qa', 'regulation',
  'MesQaInspectionRegulationWordParser.java'), 'utf8')

assert(/findEffectiveDateColumn\s*\(\s*header\s*\)/.test(parser),
  'Word parser must resolve revision effective-date columns through one canonical helper.')
assert(/findExactColumn\s*\(\s*row,\s*"生效日期"\s*\)/.test(parser)
  && /findExactColumn\s*\(\s*row,\s*"实施日期"\s*\)/.test(parser),
  'Word parser must accept both 生效日期 and 实施日期 as the revision effective-date header.')
assert(parser.includes('生效/实施日期'),
  'Word parser failure messages must identify that 生效日期 and 实施日期 are equivalent accepted headers.')

console.log('PASS: QA Word parser effective-date header static contract')
