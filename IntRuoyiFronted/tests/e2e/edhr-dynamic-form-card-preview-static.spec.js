const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const servicePath = path.resolve(
  repoRoot,
  '..',
  'IntRuoyiBackend',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'service',
  'pro',
  'batchrecord',
  'MesProEdhrBatchExecutionServiceImpl.java'
)

const read = (absolutePath) => fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')
const detail = read(detailPath)
const service = read(servicePath)

const extractConstBlock = (source, marker) => {
  const start = source.indexOf(marker)
  assert.notEqual(start, -1, `missing block: ${marker}`)
  const next = source.indexOf('\n\nconst ', start + marker.length)
  return next >= 0 ? source.slice(start, next) : source.slice(start)
}

const extractStyleRule = (source, selector) => {
  const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = source.match(new RegExp(`${escapedSelector}\\s*\\{[\\s\\S]*?\\}`))
  assert.ok(match, `missing style rule: ${selector}`)
  return match[0]
}

const previewGuardBlock = extractConstBlock(
  detail,
  'const shouldLoadTaskPreview = (task: EdhrBatchExecutionTaskRespVO) =>'
)
assert.ok(
  previewGuardBlock.includes('isDynamicRouteFormPreviewTask(task)'),
  '右侧动态表单卡片选中态必须加载中心预览，不得继续显示“当前节点没有可预览的批记录表单”。'
)
assert.ok(
  previewGuardBlock.includes('task.batchRecordReportId') &&
    !previewGuardBlock.includes('!task.formTemplateId') &&
    !previewGuardBlock.includes('!task.formCenterInstanceId'),
  '主生产表预览链路应保留，但动态表单不得再被 formTemplateId/formCenterInstanceId 排除。'
)

const dynamicPreviewBlock = extractConstBlock(
  detail,
  'const isDynamicRouteFormPreviewTask = (task: EdhrBatchExecutionTaskRespVO) =>'
)
assert.ok(
  dynamicPreviewBlock.includes('task.formTemplateId') &&
    dynamicPreviewBlock.includes('task.formTemplateVersionId') &&
    dynamicPreviewBlock.includes('task.formCenterInstanceId') &&
    dynamicPreviewBlock.includes('task.formBindingKey') &&
    dynamicPreviewBlock.includes('!task.batchRecordReportId'),
  '动态表单中心预览必须要求真实 formBindings / FormCenter 实例上下文，不得请求未配置任务。'
)

assert.ok(
  service.includes('buildDynamicRouteFormTaskPreview') &&
    service.includes('mergeDynamicRouteFormRulesIntoSheetLayout') &&
    service.includes('templateVersion.getJimuSchemaJson()'),
  '后端任务预览必须为动态表单走表单中心模板 JSON，不得只支持批记录报表 JSON。'
)
assert.ok(
  service.indexOf('if (isDynamicRouteFormTask(task))') <
    service.indexOf('if (StrUtil.isBlank(task.getBatchRecordReportId()))'),
  '动态表单预览必须在传统 batchRecordReportId 缺失报错前分流。'
)
assert.ok(
  service.includes('reportMapper.selectByReportId(task.getBatchRecordReportId())'),
  '主生产表预览仍应按批记录报表来源读取，不得被动态表单链路替代。'
)

const rightRailActiveCardStyle = extractStyleRule(
  detail,
  '.edhr-batch-detail__rail-process-form-item.is-active'
)
assert.ok(
  rightRailActiveCardStyle.includes('background: #fff8e6;'),
  '右侧红框内当前选中的表单卡片必须与左侧工序面板一致显示浅黄色背景。'
)
assert.ok(
  !rightRailActiveCardStyle.includes('background: #eef5ff;'),
  '右侧当前选中表单卡片不得继续使用蓝色背景，避免用户无法判断哪个表单被选中。'
)

console.log('PASS: eDHR dynamic form card loads a center read-only preview from FormCenter template context.')
