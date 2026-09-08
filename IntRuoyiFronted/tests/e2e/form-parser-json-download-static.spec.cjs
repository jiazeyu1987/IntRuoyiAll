const fs = require('fs')
const path = require('path')

const root = process.cwd()

const assertFile = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  if (!fs.existsSync(absolutePath)) {
    throw new Error(`Missing expected file: ${relativePath}`)
  }
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) {
    throw new Error(message || `Expected content to include: ${expected}`)
  }
}

const assertNotIncludes = (content, unexpected, message) => {
  if (content.includes(unexpected)) {
    throw new Error(message || `Expected content not to include: ${unexpected}`)
  }
}

const templateApi = assertFile('src/api/form-center/template.ts')
assertIncludes(templateApi, 'FormTemplateParseJsonRespVO')
assertIncludes(templateApi, 'parseProductionBatchRecordJson')
assertIncludes(templateApi, '/form-center/parser/production-batch-record/json')
assertIncludes(templateApi, 'request.upload<FormTemplateParseJsonRespVO>')

const parserPage = assertFile('src/views/form-center/parser/index.vue')
assertIncludes(parserPage, "name: 'FormCenterParser'")
assertIncludes(parserPage, '表单解析')
assertIncludes(parserPage, '生产批记录')
assertIncludes(parserPage, 'QA检验规程')
assertIncludes(parserPage, '过程检验记录')
assertIncludes(parserPage, 'handleProductionBatchRecord')
assertIncludes(parserPage, 'handleUnsupportedParseType')
assertIncludes(parserPage, 'accept=".doc,.docx"')
assertIncludes(parserPage, 'parseProductionBatchRecordJson')
assertIncludes(parserPage, 'download.json')
assertIncludes(parserPage, "application/json;charset=utf-8")
assertIncludes(parserPage, "'.json'")
assertIncludes(parserPage, 'JSON.stringify')
assertIncludes(parserPage, 'FormTemplateParseJsonRespVO')
assertNotIncludes(
  parserPage,
  'importTemplateDoc',
  '表单解析页只能做 parse-only JSON 下载，不得复用会创建模板版本的导入接口'
)
assertNotIncludes(parserPage, '/form-center/templates/import-doc')

const parserButtons = parserPage.match(/<el-button[\s\S]*?<\/el-button>/g) || []
const productionButton = parserButtons.find((button) => button.includes('生产批记录'))
const qaButton = parserButtons.find((button) => button.includes('QA检验规程'))
const processButton = parserButtons.find((button) => button.includes('过程检验记录'))
if (!productionButton || !productionButton.includes('handleProductionBatchRecord')) {
  throw new Error('生产批记录按钮必须打开 Word 上传解析流程')
}
for (const [label, button] of [
  ['QA检验规程', qaButton],
  ['过程检验记录', processButton]
]) {
  if (!button || !button.includes('handleUnsupportedParseType')) {
    throw new Error(`${label} 按钮当前只能提示未实现，不能调用生产批记录解析接口`)
  }
}

const sql = assertFile('../IntRuoyiBackend/sql/mysql/20260908_bpm_form_parser_menu.sql')
assertIncludes(sql, "'表单解析'")
assertIncludes(sql, "'form-center/parser'")
assertIncludes(sql, "'form-center/parser/index'")
assertIncludes(sql, "'FormCenterParser'")
assertIncludes(sql, "'form:parser:query'")
assertIncludes(sql, "'form:parser:production-batch-record'")

console.log('form parser json download static contract passed')
