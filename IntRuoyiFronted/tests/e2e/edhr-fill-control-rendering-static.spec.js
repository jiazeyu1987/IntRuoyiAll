const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const editableForm = read('src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue')
const readonlyForm = read('src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue')
const ruleSupport = read('src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts')
const actionFormPanel = read('src/views/form-center/business-action/ActionFormPanel.vue')
const runtimeSnapshotSupport = fs
  .readFileSync(
    path.join(
      root,
      '..',
      'IntRuoyiBackend',
      'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordRuntimeSnapshotSupport.java'
    ),
    'utf8'
  )
  .replace(/\r\n/g, '\n')
const batchExecutionService = fs
  .readFileSync(
    path.join(
      root,
      '..',
      'IntRuoyiBackend',
      'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'
    ),
    'utf8'
  )
  .replace(/\r\n/g, '\n')

const editableLayoutStart = editableForm.indexOf('const layout = computed')
const editableLayoutEnd = editableForm.indexOf('\nconst normalizedRules =', editableLayoutStart)
assert.ok(
  editableLayoutStart >= 0 && editableLayoutEnd > editableLayoutStart,
  '必须定位模板内填写组件布局解析 computed。'
)
const editableLayoutBlock = editableForm.slice(editableLayoutStart, editableLayoutEnd)

assert.doesNotMatch(
  editableLayoutBlock,
  /parseError\.value\s*=/,
  '模板内填写组件不能在 computed 布局解析里写 parseError，否则进入填写页可能反复更新并卡死。'
)

const readonlyLayoutStart = readonlyForm.indexOf('const layout = computed')
const readonlyLayoutEnd = readonlyForm.indexOf('\nconst hasRenderableRows =', readonlyLayoutStart)
assert.ok(
  readonlyLayoutStart >= 0 && readonlyLayoutEnd > readonlyLayoutStart,
  '必须定位只读预览组件布局解析 computed。'
)
const readonlyLayoutBlock = readonlyForm.slice(readonlyLayoutStart, readonlyLayoutEnd)

const readonlyCellValueMapStart = readonlyForm.indexOf('const cellValueMap = computed')
const readonlyCellValueMapEnd = readonlyForm.indexOf('\nconst signatureCellMarkers =', readonlyCellValueMapStart)
assert.ok(
  readonlyCellValueMapStart >= 0 && readonlyCellValueMapEnd > readonlyCellValueMapStart,
  '必须定位只读预览组件单元格值解析 computed。'
)
const readonlyCellValueMapBlock = readonlyForm.slice(readonlyCellValueMapStart, readonlyCellValueMapEnd)

for (const block of [readonlyLayoutBlock, readonlyCellValueMapBlock]) {
  assert.doesNotMatch(
    block,
    /parseError\.value\s*=/,
    '只读预览组件不能在 computed 派生逻辑里写 parseError，否则左右预览同步时可能反复更新并卡死。'
  )
}

const snapshotKindStart = executionPage.indexOf('const resolveSnapshotComponentKind =')
const snapshotKindEnd = executionPage.indexOf('\nconst resolveSnapshotDefaultValue =', snapshotKindStart)
assert.ok(snapshotKindStart >= 0 && snapshotKindEnd > snapshotKindStart, '必须定位填写页字段控件类型归一化函数。')
const snapshotKindBlock = executionPage.slice(snapshotKindStart, snapshotKindEnd)

const snapshotOptionsStart = executionPage.indexOf('const resolveSnapshotFieldOptions =')
const snapshotOptionsEnd = executionPage.indexOf('\nconst resolveSnapshotAttachmentRule =', snapshotOptionsStart)
assert.ok(
  snapshotOptionsStart >= 0 && snapshotOptionsEnd > snapshotOptionsStart,
  '必须定位填写页字段选项归一化函数。'
)
const snapshotOptionsBlock = executionPage.slice(snapshotOptionsStart, snapshotOptionsEnd)

const readonlyKindStart = readonlyForm.indexOf('const resolveReadonlyComponentKind =')
const readonlyKindEnd = readonlyForm.indexOf('\nconst resolveReadonlyRuleContext =', readonlyKindStart)
assert.ok(readonlyKindStart >= 0 && readonlyKindEnd > readonlyKindStart, '必须定位只读预览控件类型归一化函数。')
const readonlyKindBlock = readonlyForm.slice(readonlyKindStart, readonlyKindEnd)

const readonlyRuleContextStart = readonlyForm.indexOf('const resolveReadonlyRuleContext =')
const readonlyRuleContextEnd = readonlyForm.indexOf('\nfunction formatAttachmentRule', readonlyRuleContextStart)
assert.ok(
  readonlyRuleContextStart >= 0 && readonlyRuleContextEnd > readonlyRuleContextStart,
  '必须定位只读预览规则上下文构建函数。'
)
const readonlyRuleContextBlock = readonlyForm.slice(readonlyRuleContextStart, readonlyRuleContextEnd)

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
  /const resolveSnapshotComponentTypeText =[\s\S]*?field\.componentFlag[\s\S]*?readSnapshotNestedString\(field, 'edhrCellRule', 'componentFlag'\)[\s\S]*?readSnapshotNestedString\(field, 'fillForm', 'componentFlag'\)[\s\S]*?field\.fieldType[\s\S]*?field\.componentKind[\s\S]*?field\.component[\s\S]*?\.join\(' '\)/,
  '填写页控件类型优先级必须是正式 componentFlag / edhrCellRule / fillForm，并兼容 fieldType/componentKind，再到通用 component。'
)
assert.match(
  snapshotKindBlock,
  /compactType\s*=[\s\S]*?replace\(\s*\/\[\\s_-\]\+\/g/,
  '填写页控件归一化必须压缩空格、下划线和中划线，兼容 radioGroup / optionGroup / singleChoice。'
)
assert.match(
  executionPage,
  /const readSnapshotNestedRecord =[\s\S]*?readSnapshotRecord\(field\[sourceKey\]\)\?\.\[valueKey\]/,
  '填写页必须能读取 edhrCellRule/fillForm 下的嵌套 constraints，不能只读顶层字段。'
)
assert.match(
  executionPage,
  /const resolveSnapshotValueTypeText =[\s\S]*?field\.valueType[\s\S]*?readSnapshotNestedString\(field, 'edhrCellRule', 'valueType'\)[\s\S]*?readSnapshotNestedString\(field, 'fillForm', 'valueType'\)/,
  '填写页值类型必须兼容 edhrCellRule/fillForm.valueType，日期、数字、签名不能因缺顶层 valueType 退成文本。'
)
assert.match(
  snapshotKindBlock,
  /const fieldConstraints = resolveSnapshotFieldConstraints\(field\)[\s\S]*String\(fieldConstraints\.selectionMode/,
  '填写页控件归一化必须读取规则 constraints.selectionMode/options，单选规则不能因 options 不在顶层退成文本。'
)
assert.match(
  snapshotOptionsBlock,
  /field\.options[\s\S]*fieldConstraints\.options[\s\S]*readSnapshotRecord\(field\.edhrCellRule\)\?\.options[\s\S]*readSnapshotRecord\(field\.fillForm\)\?\.options/,
  '填写页选项必须合并顶层、constraints、edhrCellRule 和 fillForm 来源，不能让 radio/select 无选项。'
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

for (const flag of ['radio-group', 'option-group', 'single-choice', 'radiogroup', 'optiongroup', 'singlechoice', '单选']) {
  assert.ok(snapshotKindBlock.includes(flag), `填写页必须识别 ${flag} 控件标识。`)
  assert.ok(sharedKindBlock.includes(flag), `原表格内联填写必须识别 ${flag} 控件标识。`)
  assert.ok(readonlyKindBlock.includes(flag), `只读预览必须识别 ${flag} 控件标识，不能把选项格显示成普通文本。`)
}

for (const flag of ['电子签名', '签名', '签字', '数字', '日期', '时间']) {
  assert.ok(snapshotKindBlock.includes(flag), `填写页必须兼容中文字段类型 ${flag}。`)
  assert.ok(sharedKindBlock.includes(flag), `原表格内联填写必须兼容中文字段类型 ${flag}。`)
  assert.ok(readonlyKindBlock.includes(flag), `只读预览必须兼容中文字段类型 ${flag}。`)
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
assert.match(
  readonlyKindBlock,
  /lowerComponent\.includes\('radio-group'\)[\s\S]*?lowerComponent\.includes\('radio'\)[\s\S]*?lowerComponent\.includes\('option-group'\)[\s\S]*?lowerComponent\.includes\('single-choice'\)[\s\S]*?return 'radio'/,
  '只读预览 radio-group/option-group/single-choice 必须归一为 radio 控件。'
)

assert.ok(
  snapshotKindBlock.indexOf("rawType.includes('radio')") < snapshotKindBlock.indexOf("rawType.includes('select')"),
  'radio 识别必须早于 select/options 兜底，避免单选框被渲染为下拉框。'
)
assert.ok(
  readonlyKindBlock.indexOf("lowerComponent.includes('radio-group')") <
    readonlyKindBlock.indexOf("lowerComponent.includes('select')"),
  '只读预览 radio 识别必须早于 select/options 兜底。'
)
assert.ok(
  sharedKindBlock.indexOf("rawComponent.includes('signature')") <
    sharedKindBlock.indexOf('return templateSimulationComponentMap'),
  'componentFlag=signature 必须早于 valueType 兜底进入签名控件。'
)

assert.match(
  readonlyRuleContextBlock,
  /const options = cleanedSelectOptions\(constraints\.options \|\| fillForm\.options \|\| rule\.options\)[\s\S]*?componentKind: resolveReadonlyComponentKind\([\s\S]*?options[\s\S]*?format:[\s\S]*?options/,
  '只读预览规则上下文必须保留正式 options，radio/select 不能只剩文本占位。'
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
assert.match(
  executionPage,
  /const rawValueType = resolveSnapshotValueTypeText\(field\)/,
  '填写页保存值类型必须从统一 valueType 解析函数读取，避免嵌套正式类型被忽略。'
)

assert.match(
  executionPage,
  /const signatureMarker = resolveSignatureCellMarker\(rowIndex, columnIndex\)[\s\S]*const componentKind = resolveSnapshotComponentKind\(field, signatureMarker\)/,
  '签名位必须先读取正式签名标记，再归一 componentKind，避免只有 edhrSignature 的格子落成文本输入。'
)
assert.match(
  snapshotKindBlock,
  /marker\?\.enabled[\s\S]*return 'signature'/,
  '执行快照 componentKind 归一必须把 edhrSignature marker 作为签名控件来源。'
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
for (const flag of ['radiogroup', 'optiongroup', 'singlechoice', '单选', '电子签名', '签名', '签字', '数字', '日期', '时间']) {
  assert.ok(
    actionFieldComponentBlock.includes(flag),
    `动态表单填写面板必须兼容识别字段 ${flag}，不能落为 input-text。`
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

const runtimeFieldBlock =
  runtimeSnapshotSupport.match(/private JSONArray extractSnapshotFields\(JSONObject root\) \{[\s\S]*?\n    \}/)?.[0] || ''
assert.ok(runtimeFieldBlock, '必须定位批记录执行快照字段输出函数。')
assert.match(
  runtimeFieldBlock,
  /String componentFlag = MesProBatchRecordCellRuleSupport\.defaultComponentFlag[\s\S]*field\.put\("component", componentFlag\)[\s\S]*field\.put\("componentFlag", componentFlag\)/,
  '后端执行快照必须把正式 componentFlag 输出到顶层，前端不应只依赖通用 component=Input。'
)

const dynamicValueTypeStart = batchExecutionService.indexOf('private String dynamicRouteFormRecognizedFieldValueType')
const dynamicValueTypeEnd = batchExecutionService.indexOf('\n    private String dynamicRouteFormRecognizedFieldComponentFlag', dynamicValueTypeStart)
assert.ok(
  dynamicValueTypeStart >= 0 && dynamicValueTypeEnd > dynamicValueTypeStart,
  '必须定位动态路线表单识别字段值类型函数。'
)
const dynamicValueTypeBlock = batchExecutionService.slice(dynamicValueTypeStart, dynamicValueTypeEnd)
const dynamicComponentStart = batchExecutionService.indexOf('private String dynamicRouteFormRecognizedFieldComponentFlag')
const dynamicComponentEnd = batchExecutionService.indexOf('\n    private String mergeDynamicRouteFormRulesIntoSheetLayout', dynamicComponentStart)
assert.ok(
  dynamicComponentStart >= 0 && dynamicComponentEnd > dynamicComponentStart,
  '必须定位动态路线表单识别字段控件类型函数。'
)
const dynamicComponentBlock = batchExecutionService.slice(dynamicComponentStart, dynamicComponentEnd)

assert.match(dynamicValueTypeBlock, /case "input-number"/, '动态路线表单 number/input-number 都必须输出 NUMBER。')
assert.match(dynamicValueTypeBlock, /case "date-time"/, '动态路线表单 date-time 必须输出 DATETIME。')
assert.match(dynamicValueTypeBlock, /isDynamicRouteFormSignatureFieldType\(normalized\)/, '动态路线表单签名别名必须输出 SIGNATURE。')
for (const flag of ['数字', '日期', '时间']) {
  assert.ok(
    dynamicValueTypeBlock.includes(`case "${flag}"`),
    `动态路线表单后端必须兼容中文值类型 ${flag}。`
  )
}
assert.match(
  dynamicValueTypeBlock,
  /isDynamicRouteFormSignatureFieldType\(normalized,\s*compact\)/,
  '动态路线表单后端签名别名必须同时兼容横线、驼峰和中文签名。'
)
for (const flag of ['radio-group', 'option-group', 'single-choice', 'radiogroup', 'optiongroup', 'singlechoice', '单选']) {
  assert.ok(
    dynamicComponentBlock.includes(`case "${flag}"`),
    `动态路线表单后端必须把 ${flag} 保留为 radio-group 控件。`
  )
}
for (const flag of ['select', 'dropdown', 'upload-file', 'upload-image', 'upload-images', '下拉', '选择', '附件', '文件', '图片']) {
  assert.ok(
    dynamicComponentBlock.includes(`case "${flag}"`),
    `动态路线表单后端必须保留 ${flag} 控件，不能退成 input-text。`
  )
}
assert.match(
  batchExecutionService,
  /dynamicRouteFormRecognizedFieldPlaceholder\(field\)/,
  '动态路线表单后端必须按字段类型生成填写提示，不能把非 checkbox 统一写成问号。'
)

console.log('PASS: eDHR fill controls render by formal component flags')
