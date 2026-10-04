const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const components = [
  'src/views/dcc/controlled-file/project-attributes/ProjectAttributesFields.vue',
  'src/views/dcc/controlled-file/project-attributes/ProjectApplicationAttributes.vue',
  'src/views/dcc/controlled-file/basic-data/components/ProjectAttributeConfigurationDialog.vue',
  'src/views/dcc/controlled-file/basic-data/components/FolderTemplateLibraryEditor.vue',
  'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue',
  'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue',
  'src/views/dcc/controlled-file/basic-data/components/ProjectFolderTreePanel.vue',
  'src/views/dcc/controlled-file/basic-data/file-type-taxonomy/FileTypeCategoryMappingDialog.vue',
  'src/views/dcc/controlled-file/basic-data/file-type-taxonomy/index.vue',
  'src/views/dcc/controlled-file/basic-data/components/ProjectFolderEditor.vue'
]
for (const file of components) {
  const filename = path.join(root, file)
  const source = fs.readFileSync(filename, 'utf8')
  const result = parse(source, { filename })
  assert.deepEqual(result.errors, [], file)
  const script = compileScript(result.descriptor, { id: file })
  const template = compileTemplate({ source: result.descriptor.template.content, filename, id: file, compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [], file)
}
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const product = read(components[4])
for (const marker of ['projectLeaderUserId', 'defaultAttributes', 'folderTemplateId', 'ProjectAttributesFields', 'FolderTemplateLibraryEditor', 'validateAttributes']) assert.ok(product.includes(marker), marker)
assert.ok(!product.includes('v-model="projectProductForm.projectLeader"'), '新负责人不能从文本填写')
const project = read(components[5])
for (const marker of ['ProjectAttributeConfigurationDialog', 'defaultAttributesJson', '历史未记录', '项目 OWNER/编制权限']) assert.ok(project.includes(marker), marker)
const api = read('src/api/dcc/controlledFile/projectAttributes.ts')
for (const endpoint of ['/attributes/defaults', '/attributes/configuration', '/dcc/folder-templates', '/folders']) assert.ok(api.includes(endpoint), endpoint)
const requestPanel = read(components[1])
assert.ok(requestPanel.includes('getSnapshot') && requestPanel.includes('selectProject'))
assert.ok(requestPanel.includes('state.error') && requestPanel.includes('savedSnapshot'))
const taxonomyPage = read(components[8])
assert.ok(taxonomyPage.includes('mappingDialogRef?.open(row.id, row.name)'), '类型维护页面必须读取正式类别映射')
assert.ok(taxonomyPage.includes("['dcc:controlled-file:category:manage']"))
console.log('PASS: 10 Vue SFC script/template compiles + B API/field/permission/history integration contracts')
