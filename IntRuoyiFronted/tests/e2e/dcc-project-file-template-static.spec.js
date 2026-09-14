const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const projectCodeApi = readSource('src/api/dcc/controlledFile/projectCodes.ts')
const projectCodePage = readSource(
  'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'
)
const templateEditor = readSource(
  'src/views/dcc/controlled-file/basic-data/components/ProjectFileTemplateEditor.vue'
)
const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:project-file-template:static'],
  'node tests/e2e/dcc-project-file-template-static.spec.js',
  'package.json must expose the project file template static contract'
)

for (const token of [
  'DccProjectFileTemplateRespVO',
  'DccProjectFileTemplateItemRespVO',
  'DccProjectFileTemplateSaveReqVO',
  'getProjectCodeFileTemplate',
  'replaceProjectCodeFileTemplate',
  '/dcc/project-codes/${projectCodeId}/file-template'
]) {
  assert.ok(projectCodeApi.includes(token), `project-code API must expose ${token}`)
}

for (const token of [
  'ProjectFileTemplateEditor',
  '项目文件模板',
  '编辑模板',
  'data-testid="dcc-project-file-template-summary"',
  'data-testid="dcc-project-file-template-edit"',
  'loadProjectFileTemplateForDetail',
  'projectFileTemplateItems'
]) {
  assert.ok(projectCodePage.includes(token), `project-code detail must expose ${token}`)
}

for (const token of [
  'data-testid="dcc-project-file-template-dialog"',
  'data-testid="dcc-project-file-template-add-item"',
  'data-testid="dcc-project-file-template-row"',
  'replaceProjectCodeFileTemplate',
  'fileTypeTaxonomyId',
  'fileName',
  '项目文件模板不能为空',
  '文件分类至少选择到第三级',
  '模板中存在重复的文件分类和文件名称'
]) {
  assert.ok(templateEditor.includes(token), `template editor must expose ${token}`)
}

for (const token of [
  'projectFileTemplateItems',
  'projectFileTemplateLoading',
  'projectFileTemplateError',
  'selectedProjectTemplateStageId',
  'selectedProjectTemplateTypeId',
  'selectedProjectTemplateItemId',
  'projectTemplateStageOptions',
  'projectTemplateTypeOptions',
  'projectTemplateFileOptions',
  'loadProjectFileTemplate',
  'resetProjectFileTemplateSelection',
  'handleProjectTemplateStageChange',
  'handleProjectTemplateTypeChange',
  'handleProjectTemplateFileSelect',
  'label="阶段"',
  'label="文件类型"',
  '项目文件模板未配置',
  'formData.fileTypeTaxonomyId = templateItem.fileTypeTaxonomyId',
  'formData.fileName = templateItem.fileName'
]) {
  assert.ok(uploadPage.includes(token), `upload page must use project template token ${token}`)
}

assert.match(
  uploadPage,
  /const handleProjectCodeChange = async \(\) => \{[\s\S]*resetProjectFileTemplateSelection\(\)[\s\S]*await Promise\.all\([\s\S]*loadProjectFileTemplate\(formData\.dccProjectCodeId\)/,
  'changing a project must clear old template selections before loading the selected project template'
)

assert.ok(
  !uploadPage.includes('getFileTypeTaxonomyUploadOptions') &&
  !uploadPage.includes('projectFileTemplateItems.value.length ?') &&
    !uploadPage.includes('projectFileTemplateItems.value.length || fileTypeTaxonomies.value'),
  'upload must not fall back to the global taxonomy when the project template is empty'
)

console.log('PASS: DCC project file template static contract')
