const assert = require('assert')
const fs = require('fs')
const path = require('path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const read = (...segments) => fs.readFileSync(path.join(moduleRoot, ...segments), 'utf8')

const parser = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'service', 'qa', 'regulation', 'MesQaInspectionRegulationWordParser.java')
const importService = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'service', 'qa', 'regulation', 'MesQaInspectionRegulationWordImportService.java')
const migration = fs.readFileSync(path.resolve(moduleRoot, '..', 'sql', 'mysql',
  '20260909_mes_qa_common_regulation_product_binding.sql'), 'utf8')

assert(/parseInspectionItems\s*\(\s*document,\s*header\.regulationName\(\)\s*\)/.test(parser),
  'Common packaging Word parser must use the regulation name as process name when a 6-column table has no process column.')
assert(/findToolColumn\s*\(\s*row\s*\)/.test(parser)
  && /findExactColumn\s*\(\s*row,\s*"检具"\s*\)/.test(parser),
  'Common packaging Word parser must accept 检具 as the inspection tool header.')
assert(/findSamplingColumn\s*\(\s*row\s*\)/.test(parser)
  && /findExactColumn\s*\(\s*row,\s*"检验规则"\s*\)/.test(parser),
  'Common packaging Word parser must accept 检验规则 as the sampling-plan header.')
assert(/processNameFromRegulation/.test(parser),
  'Common packaging Word parser must model the 6-column table separately from the product-QA 8-column table.')
assert(/previousItemName/.test(parser)
  && /previousSamplingPlanText/.test(parser)
  && /previousInspectionTool/.test(parser),
  'Common packaging Word parser must inherit item/tool/sampling fields for blank continuation rows.')
assert(/patrolRatios\.size\(\)\s*>\s*1/.test(parser)
  && /new SamplingRule\(firstQuantity,\s*patrolRatio\)/.test(parser),
  'Sampling parser must allow first-inspection-only rows while still rejecting ambiguous AQL values.')
assert(!/applicableTypes\.add\("PATROL"\);\s*if\s*\(finalInspectionApplicable\)/.test(importService),
  'Word import must not unconditionally add PATROL when a row has no AQL patrol ratio.')
assert(/parsed\.patrolInspectionRatio\(\)\s*!=\s*null[\s\S]*applicableTypes\.add\("PATROL"\)/.test(importService),
  'Word import must add PATROL only when the parsed row has an AQL patrol ratio.')
assert(/DROP INDEX `uk_mes_qa_regulation_dcc_project`/.test(migration),
  'Common QA migration must remove the legacy DCC-project unique key that blocks common and product QA sharing one DCC project.')

console.log('PASS: common packaging Word table static contract')
