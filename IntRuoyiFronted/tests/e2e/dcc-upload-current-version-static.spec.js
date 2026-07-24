const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')
const submitter = readSource('src/views/dcc/controlled-file/upload/submitter.ts')
const extractBetween = (source, startToken, endToken) => {
  const startIndex = source.indexOf(startToken)
  const endIndex = source.indexOf(endToken, startIndex + startToken.length)
  assert.ok(startIndex >= 0 && endIndex > startIndex, `无法提取 ${startToken} 到 ${endToken} 内容`)
  return source.slice(startIndex, endIndex)
}
const currentVersionLookupBlock = extractBetween(
  uploadPage,
  'const loadCurrentVersionByFileNumber',
  'const queryUploadNameSuggestions'
)

assert.equal(
  packageJson.scripts['e2e:dcc:upload-current-version:static'],
  'node tests/e2e/dcc-upload-current-version-static.spec.js',
  'package.json 必须提供受控文件上传现行版本静态契约脚本'
)

assert.match(
  workflowApi,
  /export type ControlledFileChangeType = 'NEW' \| 'REVISION' \| 'OBSOLETE'/,
  '前端 API 必须声明新建、升版、作废三种变更方式。'
)
assert.match(
  workflowApi,
  /export interface ControlledFileCurrentVersionRespVO/,
  '前端 API 必须声明同编号现行版本响应类型。'
)
for (const field of [
  'originalFileName',
  'originalFilePath',
  'sourceFileName',
  'sourceFilePath',
  'publishedFileName',
  'publishedFilePath',
  'stampedFileName',
  'stampedFilePath'
]) {
  assert.match(
    workflowApi,
    new RegExp(`${field}\\?: string \\| null`),
    `现行版本响应类型必须包含可追溯路径字段 ${field}。`
  )
}
assert.match(
  workflowApi,
  /export const getControlledFileCurrentVersion = async/,
  '前端 API 必须提供按文件编号查询现行版本的方法。'
)
assert.match(
  workflowApi,
  /url:\s*'\/dcc\/controlled-files\/current-version'/,
  '现行版本查询必须调用后端 current-version 接口。'
)
assert.match(
  workflowApi,
  /changeType:\s*ControlledFileChangeType/,
  '提交请求必须携带变更方式字段。'
)

assert.match(
  submitter,
  /changeType:\s*ControlledFileChangeType/,
  '上传草稿必须包含变更方式。'
)
assert.match(
  submitter,
  /changeType:\s*draft\.changeType/,
  '提交载荷必须把页面内部判定的变更方式传给后端。'
)

assert.doesNotMatch(uploadPage, /label="变更方式"/, '上传页不得提供变更方式选择。')
for (const changeType of ['NEW', 'REVISION', 'OBSOLETE']) {
  assert.doesNotMatch(
    uploadPage,
    new RegExp(`<el-radio-button\\s+value="${changeType}"`),
    `上传页不得允许用户手动选择 ${changeType} 变更方式。`
  )
}
assert.doesNotMatch(uploadPage, /请选择变更方式/, '上传页不得保留变更方式必填校验。')
assert.match(
  uploadPage,
  /changeType:\s*'NEW'/,
  '上传页必须保留内部默认新建变更方式。'
)
assert.match(
  uploadPage,
  /getControlledFileCurrentVersion/,
  '上传页必须调用同编号现行版本查询接口。'
)
assert.match(
  uploadPage,
  /currentVersionInfo/,
  '上传页必须维护同编号现行版本状态。'
)
assert.match(
  uploadPage,
  /watch\(\s*\(\) => formData\.fileNumber[\s\S]*loadCurrentVersionByFileNumber/,
  '上传页必须在文件编号变化时查询现行版本供用户核对。'
)
assert.doesNotMatch(
  currentVersionLookupBlock,
  /formData\.changeType\s*=\s*'REVISION'/,
  '文件编号查询不得自动判定升版，升版只能来自历史文件名称下拉选择。'
)
assert.match(
  uploadPage,
  /data-testid="dcc-upload-current-version-panel"/,
  '上传页必须展示同编号现行版本面板，便于用户核对当前有效版本。'
)
for (const label of ['原版本路径', '源文件路径', '受控文件路径']) {
  assert.match(
    uploadPage,
    new RegExp(label),
    `上传页现行版本面板必须展示${label}。`
  )
}
assert.match(
  uploadPage,
  /currentVersionInfo\.value\?\.modifying[\s\S]*已有未完成流程/,
  '上传页提交前必须在同编号修改中时直接阻断并提示。'
)

console.log('PASS: DCC upload current version static contract')
