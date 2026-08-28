const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const editableForm = read('src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue')
const ruleSupport = read('src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts')
const actionFormPanel = read('src/views/form-center/business-action/ActionFormPanel.vue')

const snapshotKindStart = executionPage.indexOf('const resolveSnapshotComponentKind =')
const snapshotKindEnd = executionPage.indexOf('\nconst resolveSnapshotDefaultValue =', snapshotKindStart)
assert.ok(snapshotKindStart >= 0 && snapshotKindEnd > snapshotKindStart, '必须定位填写页字段控件类型归一化函数。')
const snapshotKindBlock = executionPage.slice(snapshotKindStart, snapshotKindEnd)

const sharedKindStart = ruleSupport.indexOf('const resolveTemplateSimulationComponentKind =')
const sharedKindEnd = ruleSupport.indexOf('\nexport const buildTemplateSimulationField =', sharedKindStart)
assert.ok(sharedKindStart >= 0 && sharedKindEnd > sharedKindStart, '必须定位原表格内联填写控件类型归一化函数。')
const sharedKindBlock = ruleSupport.slice(sharedKindStart, sharedKindEnd)

assert.match(
  snapshotKindBlock,
  /const rawType = resolveSnapshotComponentTypeText\(field\)/,
  '填写页必须先汇总正式 componentFlag / edhrCellRule 控件身份，不能让 component=Input 把签名、单选等控件遮蔽。'
)
assert.match(
  executionPage,
  /const resolveSnapshotComponentTypeText =[\s\S]*?field\.componentFlag[\s\S]*?readSnapshotNestedString\(field, 'edhrCellRule', 'componentFlag'\)[\s\S]*?readSnapshotNestedString\(field, 'fillForm', 'componentFlag'\)[\s\S]*?field\.component[\s\S]*?\.join\(' '\)/,
  '填写页控件类型优先级必须是正式 componentFlag / edhrCellRule / fillForm，再到通用 component。'
)

assert.match(
  executionPage,
  /\|\s*'radio'/,
  '填写页字段类型集合必须包含 radio，不能让 radio-group 落到普通文本。'
)
assert.match(
  ruleSupport,
  /\|\s*'radio'/,
  '原表格内联填写字段类型集合必须包含 radio。'
)

for (const flag of ['radio-group', 'option-group', 'single-choice']) {
  assert.ok(snapshotKindBlock.includes(flag), `填写页必须识别 ${flag} 控件标识。`)
  assert.ok(sharedKindBlock.includes(flag), `原表格内联填写必须识别 ${flag} 控件标识。`)
}

assert.match(
  snapshotKindBlock,
  /rawType\.includes\('radio'\)[\s\S]*?rawType\.includes\('option-group'\)[\s\S]*?rawType\.includes\('single-choice'\)[\s\S]*?return 'radio'/,
  '填写页 radio-group/option-group/single-choice 必须归一为 radio 控件。'
)
assert.match(
  sharedKindBlock,
  /rawComponent\.includes\('radio'\)[\s\S]*?rawComponent\.includes\('option-group'\)[\s\S]*?rawComponent\.includes\('single-choice'\)[\s\S]*?return 'radio'/,
  '原表格内联填写 radio-group/option-group/single-choice 必须归一为 radio 控件。'
)

assert.ok(
  snapshotKindBlock.indexOf("rawType.includes('radio')") < snapshotKindBlock.indexOf("rawType.includes('select')"),
  'radio 识别必须早于 select/options 兜底，避免单选框被渲染为下拉框。'
)
assert.ok(
  sharedKindBlock.indexOf("rawComponent.includes('signature')") <
    sharedKindBlock.indexOf('return templateSimulationComponentMap'),
  'componentFlag=signature 必须早于 valueType 兜底进入签名控件。'
)

for (const source of [executionPage, editableForm]) {
  assert.match(source, /componentKind === 'radio'[\s\S]*?<el-radio-group/, '填写表格必须为 radio 提供 el-radio-group 渲染分支。')
  assert.match(source, /v-for="option in [^"]*options/, 'radio 控件必须读取正式 options 渲染选项。')
}

assert.match(
  executionPage,
  /if \(componentKind === 'radio'\)[\s\S]*return 'STRING'/,
  'radio 字段保存值必须保持 STRING，不能被当成 BOOLEAN 或普通文本猜测。'
)

const actionFieldComponentStart = actionFormPanel.indexOf('const fieldComponentFlag =')
const actionFieldComponentEnd = actionFormPanel.indexOf('\nconst buildRecognizedFieldCellRules =', actionFieldComponentStart)
assert.ok(
  actionFieldComponentStart >= 0 && actionFieldComponentEnd > actionFieldComponentStart,
  '必须定位动态表单填写面板识别字段控件转换函数。'
)
const actionFieldComponentBlock = actionFormPanel.slice(actionFieldComponentStart, actionFieldComponentEnd)

for (const flag of ['checkbox-group', 'radio-group', 'option-group', 'single-choice']) {
  assert.ok(
    actionFieldComponentBlock.includes(flag),
    `动态表单填写面板必须把识别字段 ${flag} 转成正式单选控件，不能落为 input-text。`
  )
}

assert.match(
  actionFieldComponentBlock,
  /case 'checkbox-group':[\s\S]*case 'radio-group':[\s\S]*case 'option-group':[\s\S]*case 'single-choice':[\s\S]*return 'radio-group'/,
  '动态表单填写面板的识别字段单选组必须保存 componentFlag=radio-group。'
)

assert.match(
  actionFormPanel,
  /placeholder: fieldPlaceholder\(field\)/,
  '动态表单填写面板必须按字段类型生成占位提示，不能只把非 checkbox 全部当文本输入。'
)

console.log('PASS: eDHR fill controls render by formal component flags')
