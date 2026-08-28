const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

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

const extractPreviewActions = (source) => {
  const startNeedle = '<div v-if="selectedTemplate" class="form-template-preview__actions">'
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, 'missing form template preview actions')
  const end = source.indexOf('</div>', start)
  assert.notEqual(end, -1, 'missing form template preview actions end')
  return source.slice(start, end + '</div>'.length)
}

const templatePage = read('src/views/form-center/template/index.vue')
const templateApi = read('src/api/form-center/template.ts')
const batchRecordPage = read('src/views/mes/pro/batchrecordformlist/index.vue')
const designerWrapper = read('src/views/mes/pro/batchrecord-shared/DesignerWrapper.vue')

const formPreviewActions = extractPreviewActions(templatePage)
const openDesignerBody = extractConstFunction(templatePage, 'openDesigner')
const openSelectedTemplateWorkspaceBody = extractConstFunction(
  templatePage,
  'openSelectedTemplateWorkspace'
)
const openSelectedTemplateBody = extractConstFunction(templatePage, 'openSelectedTemplate')
const editSelectedTemplateBody = extractConstFunction(templatePage, 'editSelectedTemplate')
const openSelectedTemplateFillBody = extractConstFunction(templatePage, 'openSelectedTemplateFill')

assert.match(
  batchRecordPage,
  /@click="openDesigner\(selectedReport\.reportId,\s*'edit'\)"/,
  '基准批记录表单右侧“编辑”必须仍通过 reportId 进入 edit 设计器'
)
assert.match(
  designerWrapper,
  /reportMode\.value\s*===\s*'edit'[\s\S]*?BatchRecordReportApi\.getEditPath\(reportId\)/,
  '批记录设计器 edit 模式必须继续调用正式编辑路径'
)
assert.match(
  templateApi,
  /batchRecordReportId\??:\s*string/,
  '表单模板 API 必须暴露批记录设计器正式身份'
)

assert.match(
  formPreviewActions,
  /data-form-template-action="edit"[\s\S]*?@click="editSelectedTemplate"[\s\S]*?>\s*编辑\s*</,
  '表单模板右侧“编辑”必须绑定稳定的当前模板编辑入口'
)
assert.doesNotMatch(
  formPreviewActions,
  /@click="openDesigner\(selectedTemplate,\s*'edit'\)"/,
  '表单模板右侧“编辑”不能散落直连 openDesigner，应复用专用入口'
)
assert.match(
  editSelectedTemplateBody,
  /batchRecordReportId[\s\S]*当前模板未绑定批记录表单[\s\S]*\/mes\/pro\/batch-record-form-list[\s\S]*reportId[\s\S]*reportMode:\s*'edit'/,
  '表单模板“编辑”必须携带正式批记录报表身份并进入批记录表单设计器'
)
assert.match(
  openSelectedTemplateWorkspaceBody,
  /openDesigner\(selectedTemplate\.value,\s*templateMode\)/,
  '表单模板编辑/打开入口必须共用同一个路由切换函数'
)
assert.match(
  openDesignerBody,
  /path:\s*route\.path[\s\S]*?templateId:\s*template\.templateId[\s\S]*?versionNo:\s*template\.versionNo[\s\S]*?mode:\s*'designer'[\s\S]*?templateMode/,
  '表单模板编辑入口必须保留在当前模板路由，并携带 templateId + versionNo'
)
assert.match(
  openSelectedTemplateBody,
  /openSelectedTemplateWorkspace\('preview'\)/,
  '本次不改变表单模板“打开”的既有查看路径'
)
assert.match(
  openSelectedTemplateFillBody,
  /\/mdm\/form-center\/template\/simulate/,
  '本次不改变表单模板“填写”的既有模拟填写路径'
)

for (const forbidden of [
  'resolveSelectedTemplateBatchRecordBinding',
  "openSelectedTemplateWorkspace('edit')"
]) {
  assert.doesNotMatch(
    editSelectedTemplateBody,
    new RegExp(escapeRegExp(forbidden)),
    `表单模板编辑不得继续依赖旧的同页规则编辑入口：${forbidden}`
  )
}

console.log('PASS form-template-edit-designer-parity-static')
