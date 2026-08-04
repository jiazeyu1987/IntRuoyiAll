const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.resolve(workspaceRoot, 'IntRuoyiFronted')
const pagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)

const source = fs.readFileSync(pagePath, 'utf8')

assert.match(
  source,
  /<el-tab-pane\s+label="QA 规程"\s+name="QA"\s*\/>/,
  'The workbench must expose a QA regulation tab next to production and PQC tabs.'
)
assert.match(source, /data-qa-regulation-tab/, 'QA regulation tab must have a stable root selector.')
assert.match(
  source,
  /data-qa-regulation-pressure-pump-source/,
  'QA tab must show the pressure-pump procedure source used to initialize the draft.'
)

const qaBlockStart = source.indexOf('data-qa-regulation-tab')
assert.notEqual(qaBlockStart, -1, 'QA regulation block must exist.')
const qaBlockEnd = source.indexOf('<script', qaBlockStart)
const qaBlock = source.slice(qaBlockStart, qaBlockEnd)

for (const requiredText of [
  '按压式球囊扩充压力泵组装过程检验规程',
  'PQC-IDI-001',
  'B/0',
  '2026-01-04',
  '过程检验规程',
  'QA 负责制定 PQC 的首检、巡检、末检和检验项目规则',
  '正式保存/发布接口未接入',
  '未写入后台'
]) {
  assert.match(qaBlock, new RegExp(requiredText), `QA tab must include ${requiredText}.`)
}

for (const requiredSelector of [
  'data-qa-regulation-scope',
  'data-qa-regulation-inspection-rules',
  'data-qa-regulation-items',
  'data-qa-regulation-original-excerpt',
  'data-qa-regulation-completeness',
  'data-qa-pqc-task-preview'
]) {
  assert.match(qaBlock, new RegExp(requiredSelector), `QA tab must include ${requiredSelector}.`)
}

for (const requiredRule of ['首检', '上午巡检', '下午巡检', '末检']) {
  assert.match(qaBlock, new RegExp(requiredRule), `QA tab must configure ${requiredRule}.`)
}

for (const requiredField of [
  'inspectionMethod',
  'inspectionTool',
  'resultType',
  'standardText',
  'lowerLimit',
  'upperLimit',
  'critical',
  'failureRule'
]) {
  assert.match(
    source,
    new RegExp(requiredField),
    `QA regulation item model must retain ${requiredField}.`
  )
}

for (const requiredSourceField of [
  'sourceOriginalPage',
  'sourceOriginalItem',
  'sourceOriginalExcerpt',
  'sourceOriginalMethod'
]) {
  assert.match(
    source,
    new RegExp(requiredSourceField),
    `QA regulation item model must retain ${requiredSourceField}.`
  )
}

for (const requiredOriginalExcerpt of [
  '原文依据',
  '20atm 压力打至 20atm 应无跳压现象',
  '负压检测：抽负压-80±5kpa，不应有泄漏',
  '正常或矫正视力，在 300~700lx 的照度下',
  '推杆组件推入外套',
  '每一个检验项目均应合格'
]) {
  assert.match(
    source,
    new RegExp(requiredOriginalExcerpt.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `QA regulation item must expose source excerpt: ${requiredOriginalExcerpt}.`
  )
}

assert.doesNotMatch(
  qaBlock,
  /DCC|文件分类|受控文件|文控|controlled-file|fileTypeTaxonomy/i,
  'QA regulation tab must not be coupled to document-control classification or controlled files.'
)

console.log('PASS role-matrix QA regulation tab static contract')
