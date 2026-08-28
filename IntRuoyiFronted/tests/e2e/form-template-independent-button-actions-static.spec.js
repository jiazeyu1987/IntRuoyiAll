const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const templatePage = fs.readFileSync(
  path.join(repoRoot, 'src/views/form-center/template/index.vue'),
  'utf8'
)
const templateApi = fs.readFileSync(
  path.join(repoRoot, 'src/api/form-center/template.ts'),
  'utf8'
)

const extractConstFunction = (source, name) => {
  const start = source.indexOf(`const ${name} =`)
  assert.notEqual(start, -1, `missing function ${name}`)
  const braceStart = source.indexOf('{', start)
  assert.notEqual(braceStart, -1, `missing function body ${name}`)
  let depth = 0
  for (let index = braceStart; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(start, index + 1)
      }
    }
  }
  throw new Error(`unterminated function ${name}`)
}

const openSelectedTemplateBody = extractConstFunction(templatePage, 'openSelectedTemplate')
const fillSelectedTemplateBody = extractConstFunction(templatePage, 'openSelectedTemplateFill')
const openDesignerBody = extractConstFunction(templatePage, 'openDesigner')

assert.match(
  openSelectedTemplateBody,
  /openSelectedTemplateWorkspace\('preview'\)/,
  '表单模板“打开”必须进入当前模板自身只读工作区'
)
assert.match(
  templatePage,
  /<el-button[\s\S]*?canUseTemplateInteractiveAction\(selectedTemplate\)[\s\S]*?data-form-template-action="edit"[\s\S]*?@click="editSelectedTemplate"[\s\S]*?>[\s\S]*?编辑\s*<\/el-button>/,
  '表单模板“编辑”必须从右侧操作区通过统一入口进入当前模板自身 Jimu 编辑器'
)
assert.match(
  fillSelectedTemplateBody,
  /\/mdm\/form-center\/template\/simulate[\s\S]*templateId[\s\S]*versionNo/,
  '表单模板“填写”必须跳转当前模板自身模拟填写工作区'
)

for (const forbidden of [
  'resolveSelectedTemplateBatchRecordBinding',
  'openSelectedTemplateDesigner',
  '当前模板未绑定批记录表单',
  "/mes/pro/batch-record-form-list",
  "/mes/pro/feedback/edhr-batch-execution/template-simulate"
]) {
  assert.doesNotMatch(templatePage, new RegExp(forbidden), `表单模板三按钮不得依赖批记录链路：${forbidden}`)
}

for (const field of [
  'batchRecordReportId',
  'batchRecordReportName',
  'batchRecordName',
  'batchRecordVersionNo',
  'batchRecordFormSlotType',
  'batchRecordBindingStatus',
  'batchRecordBindingError'
]) {
  assert.doesNotMatch(templateApi, new RegExp(`\\b${field}\\??:`), `表单模板 API 类型不得包含 ${field}`)
}

assert.match(templatePage, /isDesignerMode/)
assert.doesNotMatch(
  templatePage,
  /isTemplateDesignerEditMode|templateMode/,
  '表单模板“编辑”不得再回退到 templateMode=edit 的规则面板'
)
assert.match(
  openDesignerBody,
  /const\s+reportId\s*=\s*normalizeRouteQueryText\(template\.designerReportId\)[\s\S]*?reportId[\s\S]*?reportMode/,
  '表单模板“编辑”必须使用当前模板 designerReportId 和 reportMode=edit 进入 Jimu 编辑器'
)
assert.match(templatePage, /isTemplateSimulationMode/)
assert.match(templatePage, /getTemplateVersion/)
assert.match(templatePage, /form-template-route-workspace/)
assert.doesNotMatch(
  openDesignerBody,
  /if\s*\(reportMode\s*===\s*'edit'\)[\s\S]{0,600}?return/,
  '表单模板“编辑”不得再分流到非 Jimu 的本地规则面板'
)

console.log('PASS form-template-independent-button-actions-static')
