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
for (const marker of ['ProjectAttributeConfigurationDialog', 'defaultAttributesJson', '历史未记录']) assert.ok(project.includes(marker), marker)
// 权限契约检查实际操作入口，避免说明文案调整造成误报。
const configurationButton = project.match(/<el-button\b[^>]*@click="projectAttributeDialogRef\?\.open\(selectedProjectCode\.id\)"[^>]*>/)?.[0]
assert.ok(configurationButton, '项目属性配置入口必须打开当前项目')
assert.match(configurationButton, /v-if="selectedProjectCode\?\.id"/, '项目属性配置必须要求有效项目')
assert.match(configurationButton, /v-hasPermi="\['dcc:project-code:update'\]"/, '项目属性配置入口必须要求更新权限')
const configurationDialog = read(components[2])
const configurationSaveButton = configurationDialog.match(/<el-button\b[^>]*@click="save"[^>]*>/)?.[0]
assert.ok(configurationSaveButton, '项目属性配置必须提供保存入口')
assert.match(configurationSaveButton, /v-hasPermi="\['dcc:project-code:update'\]"/, '项目属性配置保存必须要求更新权限')
const api = read('src/api/dcc/controlledFile/projectAttributes.ts')
for (const endpoint of ['/attributes/defaults', '/attributes/configuration', '/dcc/folder-templates', '/folders']) assert.ok(api.includes(endpoint), endpoint)
const requestPanel = read(components[1])
assert.ok(requestPanel.includes('getSnapshot') && requestPanel.includes('selectProject'))
assert.ok(requestPanel.includes('state.error') && requestPanel.includes('savedSnapshot'))
const taxonomyPage = read(components[8])
assert.ok(taxonomyPage.includes('mappingDialogRef?.open(row.id, row.name)'), '类型维护页面必须读取正式类别映射')
assert.ok(taxonomyPage.includes("['dcc:controlled-file:category:manage']"))
console.log('PASS: 10 Vue SFC script/template compiles + B API/field/permission/history integration contracts')
