const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const component = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
const template = component.slice(
  component.indexOf('<template>'),
  component.indexOf('<script setup')
)

const includes = (content, token, message) => assert.ok(content.includes(token), message)
const notIncludes = (content, token, message) => assert.ok(!content.includes(token), message)
const matches = (content, pattern, message) => assert.ok(pattern.test(content), message)

includes(
  template,
  'data-fill-config-current-form="name-version"',
  '红框位置必须渲染当前表单名称和版本的独立区域。'
)
matches(
  template,
  /data-fill-config-current-form="name-version"[\s\S]*{{\s*currentFormTitleLabel\s*}}/,
  '当前表单标题必须由专门的 currentFormTitleLabel 渲染。'
)
includes(
  component,
  "type ReportLike = Pick<BatchRecordReportVO, 'reportId' | 'reportName' | 'batchRecordName' | 'versionNo'>",
  '填写配置弹窗必须把正式 versionNo 纳入当前表单上下文类型。'
)
matches(
  component,
  /const currentFormTitleLabel = computed\(\(\) => \{[\s\S]*props\.report\?\.reportName[\s\S]*props\.report\?\.batchRecordName[\s\S]*props\.report\?\.versionNo[\s\S]*return versionNo \? `\$\{formName\} \/ \$\{versionNo\}` : formName[\s\S]*\}\)/,
  '当前表单标题必须使用 reportName 优先、batchRecordName 兜底，并与 versionNo 组合。'
)
notIncludes(
  component,
  'currentFormTitleLabel = computed(() => props.navigationLabel',
  '当前表单标题不得复用同产品同版本导航标签。'
)
notIncludes(
  component,
  'currentFormTitleLabel = computed(() => navigationDisplayLabel',
  '当前表单标题不得从导航标签推导。'
)
notIncludes(
  component,
  'currentFormTitleLabel = computed(() => formBindings',
  '当前表单标题不得使用表单槽位 formBindings 作为替代来源。'
)
includes(
  component,
  '.batch-record-cell-rules-editor__current-form',
  '当前表单标题必须有稳定样式类控制宽度和省略，避免顶栏文字挤压导航按钮。'
)
includes(
  component,
  'text-overflow: ellipsis;',
  '当前表单标题样式必须允许长表单名省略。'
)

console.log('PASS edhr-fill-config-current-form-title-static')
