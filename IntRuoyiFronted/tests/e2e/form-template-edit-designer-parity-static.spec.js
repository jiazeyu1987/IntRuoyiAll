const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

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

const templatePage = read('src/views/form-center/template/index.vue')
const templateApi = read('src/api/form-center/template.ts')
const routes = read('src/router/modules/remaining.ts')
const formTemplateDesignerWrapper = read(
  'src/views/form-center/template/components/FormTemplateDesignerWrapper.vue'
)
const sharedDesignerWrapper = read('src/views/mes/pro/batchrecord-shared/DesignerWrapper.vue')

const previewActionsStart = templatePage.indexOf(
  '<div v-if="selectedTemplate" class="form-template-preview__actions">'
)
assert.notEqual(previewActionsStart, -1, 'missing form template preview action area')
const previewActions = templatePage.slice(
  previewActionsStart,
  templatePage.indexOf('</div>', previewActionsStart) + '</div>'.length
)
const openDesignerBody = extractConstFunction(templatePage, 'openDesigner')
const editSelectedTemplateBody = extractConstFunction(templatePage, 'editSelectedTemplate')
const openSelectedTemplateWorkspaceBody = extractConstFunction(
  templatePage,
  'openSelectedTemplateWorkspace'
)

assert.match(
  templateApi,
  /designerReportId\?:\s*string/,
  '表单模板列表类型必须接收后端返回的模板虚拟 Jimu 报表 ID'
)
assert.match(
  sharedDesignerWrapper,
  /reportMode\s*=\s*computed<'preview'\s*\|\s*'edit'>/,
  '批记录表单共享 DesignerWrapper 必须继续使用 reportMode 区分打开和编辑'
)
assert.match(
  sharedDesignerWrapper,
  /designerTitle\?:\s*string[\s\S]*previewTitle\?:\s*string/,
  '共享 Jimu DesignerWrapper 必须支持表单模板自己的页面标题'
)
assert.match(
  templatePage,
  /import\s+FormTemplateDesignerWrapper\s+from\s+'\.\/components\/FormTemplateDesignerWrapper\.vue'/,
  '表单模板页必须保留表单模板自己的 Jimu 预览壳'
)
assert.doesNotMatch(
  templatePage,
  /isTemplateDesignerEditMode|templateMode/,
  '表单模板“编辑”不得再回退到 templateMode=edit 的规则面板'
)
assert.doesNotMatch(
  templatePage,
  /v-else-if="isTemplateDesignerEditMode"[\s\S]*?<FormTemplateFillConfigDialog[\s\S]*?embedded/,
  '表单模板“编辑”不得把填写配置面板冒充 Jimu 编辑器'
)
assert.doesNotMatch(
  templatePage,
  /import\s+DesignerWrapper\s+from\s+'@\/views\/mes\/pro\/batchrecord-shared\/DesignerWrapper\.vue'/,
  '表单模板页不得再从批记录表单模块导入页面壳'
)
assert.match(
  formTemplateDesignerWrapper,
  /defineOptions\(\{\s*name:\s*'FormCenterTemplateDesignerWrapper'\s*\}\)/,
  '表单模板 Jimu 壳必须保持表单中心组件身份'
)
assert.match(
  formTemplateDesignerWrapper,
  /reportMode\s*=\s*computed<'preview'\s*\|\s*'edit'>/,
  '表单模板 Jimu 预览壳仍可保留 reportMode 兼容预览链路'
)
assert.match(
  formTemplateDesignerWrapper,
  /sameOriginChromeMode\s*=\s*computed/,
  '表单模板 Jimu 壳必须复用批记录表单的同源编辑适配能力'
)
assert.match(
  formTemplateDesignerWrapper,
  /BatchRecordReportApi\.getEditPath\(reportId\)/,
  '表单模板 Jimu 壳必须像批记录表单一样通过 edit path 进入 Jimu 编辑器'
)
assert.match(
  formTemplateDesignerWrapper,
  /ensureFormTemplateReportId\(reportId\)/,
  '表单模板 Jimu 壳必须拦截非表单模板报表 ID，避免进入批记录表单模块'
)
assert.match(
  previewActions,
  /data-form-template-action="edit"[\s\S]*?@click="editSelectedTemplate"[\s\S]*?>\s*编辑\s*</,
  '表单模板右侧“编辑”必须绑定稳定的当前模板编辑入口'
)
assert.match(
  editSelectedTemplateBody,
  /openSelectedTemplateWorkspace\('edit'\)/,
  '表单模板“编辑”必须通过统一工作区函数进入 edit 模式'
)
assert.match(
  openSelectedTemplateWorkspaceBody,
  /openDesigner\(selectedTemplate\.value,\s*reportMode\)/,
  '表单模板打开和编辑必须共用同一个路由切换函数'
)
assert.match(
  openDesignerBody,
  /const\s+reportId\s*=\s*normalizeRouteQueryText\(template\.designerReportId\)/,
  '表单模板“编辑”必须使用当前模板自己的 designerReportId'
)
assert.match(
  openDesignerBody,
  /path:\s*route\.path[\s\S]*?templateId:\s*template\.templateId[\s\S]*?versionNo:\s*template\.versionNo[\s\S]*?mode:\s*'designer'[\s\S]*?reportId[\s\S]*?reportMode/,
  '表单模板编辑入口必须留在当前表单模板路由，并携带 reportId + reportMode=edit'
)
assert.doesNotMatch(
  openDesignerBody,
  /\/mes\/pro\/batch-record-form-list/,
  '表单模板“编辑”不得跳转到批记录表单列表页面'
)
assert.doesNotMatch(
  openDesignerBody,
  /if\s*\(reportMode\s*===\s*'edit'\)[\s\S]{0,600}?return/,
  '表单模板“编辑”不得再分流到非 Jimu 的本地规则面板'
)
assert.match(
  routes,
  /path:\s*'template'[\s\S]*?component:\s*\(\)\s*=>\s*import\('@\/views\/form-center\/template\/index\.vue'\)/,
  '表单模板主页面路由必须继续存在'
)

console.log('PASS form-template-edit-designer-parity-static')
