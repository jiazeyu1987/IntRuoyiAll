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
const simulatePage = read('src/views/form-center/template/FormTemplateSimulatePage.vue')
const routes = read('src/router/modules/remaining.ts')
const designerWrapper = read('src/views/form-center/template/components/FormTemplateDesignerWrapper.vue')

const formPreviewActions = templatePage.slice(
  templatePage.indexOf('<div v-if="selectedTemplate" class="form-template-preview__actions">'),
  templatePage.indexOf('</div>', templatePage.indexOf('<div v-if="selectedTemplate" class="form-template-preview__actions">')) +
    '</div>'.length
)
const openSelectedTemplateWorkspaceBody = extractConstFunction(
  templatePage,
  'openSelectedTemplateWorkspace'
)
const openSelectedTemplateBody = extractConstFunction(templatePage, 'openSelectedTemplate')
const editSelectedTemplateBody = extractConstFunction(templatePage, 'editSelectedTemplate')
const openSelectedTemplateFillBody = extractConstFunction(templatePage, 'openSelectedTemplateFill')
const openDesignerBody = extractConstFunction(templatePage, 'openDesigner')

assert.match(
  designerWrapper,
  /defineOptions\(\{\s*name:\s*'FormCenterTemplateDesignerWrapper'\s*\}\)/,
  '表单模板必须提供独立 DesignerWrapper 组件'
)
assert.match(
  templatePage,
  /<FormTemplateDesignerWrapper[\s\S]*?v-if="isDesignerMode\s*&&\s*templateDesignerMode\s*===\s*'preview'"/,
  '表单模板必须像批记录管理一样通过独立 DesignerWrapper 切换同页设计工作区'
)
assert.match(
  templatePage,
  /const\s+isDesignerMode\s*=\s*computed\(\(\)\s*=>\s*route\.query\.mode\s*===\s*'designer'\)/,
  '表单模板必须复用批记录管理的 designer 页面模式'
)
assert.match(
  templatePage,
  /<FormTemplateDesignerWrapper[\s\S]*?v-if="isDesignerMode\s*&&\s*templateDesignerMode\s*===\s*'edit'"/,
  '表单模板编辑必须继续使用同页 DesignerWrapper，不得跳到批记录表单页面'
)
assert.match(
  templatePage,
  /const\s+templateDesignerMode\s*=\s*computed<'preview'\s*\|\s*'edit'>/,
  '表单模板 DesignerWrapper 必须区分 preview 和 edit'
)
assert.match(
  formPreviewActions,
  /data-form-template-action="edit"[\s\S]*?@click="editSelectedTemplate"[\s\S]*?>\s*编辑\s*</,
  '表单模板右侧“编辑”必须绑定稳定的当前模板编辑入口'
)
assert.match(
  editSelectedTemplateBody,
  /openSelectedTemplateWorkspace\('edit'\)/,
  '表单模板“编辑”必须进入当前模板自身规则编辑工作区'
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
assert.match(
  routes,
  /path:\s*'form-center\/template\/simulate'[\s\S]*?@\/views\/form-center\/template\/FormTemplateSimulatePage\.vue[\s\S]*?activeMenu:\s*'\/mdm\/form-center\/template'/,
  '表单中心必须注册隐藏的独立模拟填写路由'
)
assert.match(
  simulatePage,
  /<FormTemplateIndex\s+simulation-only\s*\/>[\s\S]*?import\s+FormTemplateIndex\s+from\s+'\.\/index\.vue'/,
  '独立模拟填写页面必须复用表单模板自身运行态，不得复制批记录数据链路'
)
assert.doesNotMatch(templatePage, /batchRecordReportId|\/mes\/pro\/batch-record-form-list/, '表单模板编辑不得继续依赖批记录链路')

console.log('PASS form-template-edit-designer-parity-static')
