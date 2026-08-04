const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.resolve(workspaceRoot, 'IntRuoyiFronted')
const qaPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const workbenchPath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)
const routePath = path.join(frontendRoot, 'src/router/modules/remaining.ts')

assert.ok(fs.existsSync(qaPagePath), 'QA regulation must be implemented as a standalone page.')

const qaSource = fs.readFileSync(qaPagePath, 'utf8')
const workbenchSource = fs.readFileSync(workbenchPath, 'utf8')
const routeSource = fs.readFileSync(routePath, 'utf8')

assert.match(
  routeSource,
  /path:\s*'pro\/process-pool\/qa-regulation'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/mes\/pro\/processpool\/QaRegulationPage\.vue'\)[\s\S]*name:\s*'MesProProcessPoolQaRegulation'/,
  'QA regulation must have a standalone route at /mes/pro/process-pool/qa-regulation.'
)
assert.match(routeSource, /title:\s*'QA 规程配置'/, 'Standalone route must have a QA title.')
assert.match(
  routeSource,
  /permission:\s*\['mes:pro-process-pool-team-leader:query'\]/,
  'Standalone QA route must keep the existing process-pool query permission until a formal QA menu permission exists.'
)

assert.doesNotMatch(
  workbenchSource,
  /<el-tab-pane\s+label="QA 规程"\s+name="QA"\s*\/>/,
  'The production/PQC workbench must not expose QA as an internal Element tab.'
)
assert.doesNotMatch(
  workbenchSource,
  /activeLeaderTab\s*[!=]==?\s*'QA'|type\s+WorkbenchLeaderTab\s*=\s*TeamLeaderType\s*\|\s*'QA'/,
  'The production/PQC workbench must not keep QA tab branching logic.'
)
assert.doesNotMatch(
  workbenchSource,
  /data-qa-regulation-tab|data-qa-regulation-page/,
  'The QA regulation UI must not remain inside TeamLeaderWorkbenchPage.vue.'
)

assert.match(
  qaSource,
  /data-qa-regulation-page/,
  'Standalone QA regulation page must have a stable root selector.'
)
assert.match(
  qaSource,
  /data-qa-regulation-pressure-pump-source/,
  'Standalone QA page must show the pressure-pump procedure source used to initialize the draft.'
)
assert.doesNotMatch(
  qaSource,
  /<el-tabs|<el-tab-pane/,
  'Standalone QA page must not render another internal tab wrapper.'
)

for (const requiredText of [
  'QA 规程配置',
  '按压式球囊扩充压力泵组装过程检验规程',
  'PQC-IDI-001',
  'B/0',
  '2026-01-04',
  '过程检验规程',
  'QA 负责制定 PQC 的首检、巡检、末检和检验项目规则',
  '正式保存/发布接口未接入',
  '未写入后台'
]) {
  assert.match(qaSource, new RegExp(requiredText), `Standalone QA page must include ${requiredText}.`)
}

for (const requiredSelector of [
  'data-qa-regulation-scope',
  'data-qa-regulation-inspection-rules',
  'data-qa-regulation-items',
  'data-qa-regulation-original-excerpt',
  'data-qa-regulation-completeness',
  'data-qa-pqc-task-preview'
]) {
  assert.match(qaSource, new RegExp(requiredSelector), `Standalone QA page must include ${requiredSelector}.`)
}

for (const requiredRule of ['首检', '上午巡检', '下午巡检', '末检']) {
  assert.match(qaSource, new RegExp(requiredRule), `Standalone QA page must configure ${requiredRule}.`)
}

for (const requiredField of [
  'inspectionMethod',
  'inspectionTool',
  'resultType',
  'standardText',
  'lowerLimit',
  'upperLimit',
  'critical',
  'failureRule',
  'sourceOriginalPage',
  'sourceOriginalItem',
  'sourceOriginalExcerpt',
  'sourceOriginalMethod'
]) {
  assert.match(qaSource, new RegExp(requiredField), `QA regulation item model must retain ${requiredField}.`)
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
    qaSource,
    new RegExp(requiredOriginalExcerpt.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `QA regulation item must expose source excerpt: ${requiredOriginalExcerpt}.`
  )
}

assert.doesNotMatch(
  qaSource,
  /DCC|文件分类|受控文件|文控|controlled-file|fileTypeTaxonomy/i,
  'Standalone QA regulation page must not be coupled to document-control classification or controlled files.'
)

console.log('PASS role-matrix QA regulation standalone page static contract')
