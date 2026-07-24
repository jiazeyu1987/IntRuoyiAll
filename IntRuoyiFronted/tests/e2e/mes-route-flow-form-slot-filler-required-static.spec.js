const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)
const routeFormContent = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFormContent.vue'),
  'utf8'
)

const validatorMatch = component.match(
  /const validateRecordBindingCandidateSource = \(binding: RouteFlowRecordBinding\) => \{[\s\S]*?\n\}/
)
assert.ok(validatorMatch, 'RouteFlowGraphDesigner must define validateRecordBindingCandidateSource')
const validator = validatorMatch[0]

assert.ok(
  !/!sourceType\s*&&\s*sourceIds\.length\s*===\s*0[\s\S]*?return/.test(validator),
  '选择表单后不能允许 candidateSourceType 和 candidateSourceIds 同时为空并继续保存。'
)
assert.match(
  validator,
  /if \(!sourceType\)[\s\S]*throw new Error\(`\$\{displayName\}填写人配置缺少来源。`\)/,
  '缺少填写人来源时必须在前端保存前给出明确错误。'
)
assert.match(
  validator,
  /if \(sourceIds\.length !== 1\)[\s\S]*throw new Error\(`\$\{displayName\}填写人配置必须选择一个人员或一个权限角色。`\)/,
  '缺少人员或角色 ID 时必须在前端保存前给出明确错误。'
)
assert.match(
  component,
  /buildFormBindingSaveRows[\s\S]*\.filter\(\(binding\) => Boolean\(binding\.formTemplateId\)\)[\s\S]*validateRecordBindingCandidateSource\(binding\)[\s\S]*candidateSourceType: binding\.candidateSourceType[\s\S]*candidateSourceIds: binding\.candidateSourceIds/,
  '保存 formBindings payload 前必须校验填写人配置，并把已校验的来源和 ID 写入 payload。'
)
assert.match(
  routeFormContent,
  /@request-submit="handleSubmitRequest"/,
  '流转关系图保存按钮必须走受控提交处理器，避免校验失败冒泡成页面错误。'
)
assert.match(
  routeFormContent,
  /const handleSubmitRequest = async \(\) => \{[\s\S]*try \{[\s\S]*await submitForm\(\)[\s\S]*\} catch \(error\) \{[\s\S]*message\.error\(resolveRouteOperationErrorMessage\(error, '保存工艺路线失败'\)\)[\s\S]*\}/,
  '受控提交处理器必须显示错误并收敛异常，不能让填写人校验失败产生 unhandled pageerror。'
)

console.log('mes-route-flow-form-slot-filler-required-static PASS')
