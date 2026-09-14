const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const templatePage = read('src/views/form-center/template/index.vue')

const fieldValueTypeStart = templatePage.indexOf('const fieldValueType =')
const fieldValueTypeEnd = templatePage.indexOf('\nconst fieldComponentFlag =', fieldValueTypeStart)
assert.ok(
  fieldValueTypeStart >= 0 && fieldValueTypeEnd > fieldValueTypeStart,
  '必须定位表单中心填写页字段类型转换函数。'
)
const fieldValueTypeBlock = templatePage.slice(fieldValueTypeStart, fieldValueTypeEnd)

const fieldComponentStart = templatePage.indexOf('const fieldComponentFlag =')
const fieldComponentEnd = templatePage.indexOf('\nconst fieldPlaceholder =', fieldComponentStart)
assert.ok(
  fieldComponentStart >= 0 && fieldComponentEnd > fieldComponentStart,
  '必须定位表单中心填写页控件类型转换函数。'
)
const fieldComponentBlock = templatePage.slice(fieldComponentStart, fieldComponentEnd)

const fieldPlaceholderStart = templatePage.indexOf('const fieldPlaceholder =')
const fieldPlaceholderEnd = templatePage.indexOf('\nconst buildRecognizedFieldCellRules =', fieldPlaceholderStart)
assert.ok(
  fieldPlaceholderStart >= 0 && fieldPlaceholderEnd > fieldPlaceholderStart,
  '必须定位表单中心填写页占位提示转换函数。'
)
const fieldPlaceholderBlock = templatePage.slice(fieldPlaceholderStart, fieldPlaceholderEnd)

assert.match(fieldValueTypeBlock, /normalized === 'input-number'/, '数字字段必须兼容 input-number。')
assert.match(fieldValueTypeBlock, /isSignatureRecognizedFieldType\(normalized\)/, '签名字段必须兼容多种签名标识。')
assert.match(fieldValueTypeBlock, /normalized\.includes\('数字'\)|compact === '数字'/, '数字字段必须兼容中文“数字”类型。')
assert.match(fieldValueTypeBlock, /normalized\.includes\('日期'\)|compact === '日期'/, '日期字段必须兼容中文“日期”类型。')
assert.match(fieldValueTypeBlock, /normalized\.includes\('时间'\)|compact === '时间'/, '时间字段必须兼容中文“时间”类型。')
assert.match(
  templatePage,
  /const isSignatureRecognizedFieldType =[\s\S]*?normalized\.includes\('签名'\)[\s\S]*?normalized\.includes\('签字'\)/,
  '签名字段必须兼容中文“签名/签字”，不能只识别英文 signature。'
)

for (const flag of ['radio-group', 'option-group', 'single-choice', 'radioGroup', 'optionGroup', 'singleChoice', '单选']) {
  assert.ok(
    fieldComponentBlock.toLowerCase().includes(flag.toLowerCase()) || fieldComponentBlock.includes(flag),
    `表单中心填写页必须把 ${flag} 生成为正式单选控件，不能落成 input-text。`
  )
}

assert.match(
  fieldComponentBlock,
  /case 'checkbox-group':[\s\S]*case 'radio-group':[\s\S]*case 'option-group':[\s\S]*case 'single-choice':[\s\S]*return 'radio-group'/,
  '表单中心填写页的单选类识别字段必须保存 componentFlag=radio-group。'
)
assert.ok(
  fieldComponentBlock.indexOf("case 'radio-group'") <
    fieldComponentBlock.indexOf("normalized.includes('radio')"),
  '明确 radio-group 必须先于模糊文本识别。'
)

for (const flag of ['upload-file', 'upload-image', 'upload-images']) {
  assert.ok(
    fieldComponentBlock.includes(flag),
    `表单中心填写页必须保留上传类控件 ${flag}，不能落成 input-text。`
  )
}
for (const flag of ['附件', '文件', '图片']) {
  assert.ok(
    fieldComponentBlock.includes(flag),
    `表单中心填写页必须兼容中文上传类控件 ${flag}，不能落成 input-text。`
  )
}

assert.match(
  fieldPlaceholderBlock,
  /if \(componentFlag === 'signature'\) return '请签名'/,
  '签名字段必须显示签名提示，不能显示普通输入提示。'
)
assert.match(
  fieldPlaceholderBlock,
  /if \(componentFlag === 'date' \|\| componentFlag === 'datetime'\)/,
  '日期字段必须显示选择日期提示。'
)
assert.match(
  fieldPlaceholderBlock,
  /if \(componentFlag === 'radio-group' \|\| componentFlag === 'select'\)/,
  '单选组和下拉字段必须显示选择提示。'
)
assert.doesNotMatch(
  fieldPlaceholderBlock,
  /return '\?'/,
  '表单中心填写页不能用问号作为非勾选字段的统一占位提示。'
)

assert.match(
  templatePage,
  /:cell-rules="templatePreviewCellRules"[\s\S]*@signature-action="handleTemplatePreviewSignatureAction"/,
  '表单中心填写页必须继续把正式规则交给模板内填写组件渲染。'
)

console.log('PASS form-template-fill-control-rendering-static')
