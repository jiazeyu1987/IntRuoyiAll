const fs = require('fs')
const path = require('path')

const projectRoot = path.resolve(__dirname, '../..')
const dialogPath = path.join(
  projectRoot,
  'src/views/form-center/template/components/TemplateImportDialog.vue'
)
const dialog = fs.readFileSync(dialogPath, 'utf8').replace(/\r\n/g, '\n')

const assertIncludes = (source, expected, message) => {
  if (!source.includes(expected)) {
    throw new Error(message || `missing expected contract: ${expected}`)
  }
}

assertIncludes(
  dialog,
  'class="form-template-import-dialog scheme-d-form-control"',
  '导入弹窗必须使用独立布局作用域，避免上传样式污染其它弹窗'
)
assertIncludes(dialog, 'width="640px"', '导入弹窗桌面默认宽度必须为 640px')
assertIncludes(dialog, 'label-position="top"', '导入表单标签必须位于控件上方')
assertIncludes(
  dialog,
  'class="form-template-import-dialog__upload"',
  '上传控件必须使用独立满宽布局类'
)
assertIncludes(
  dialog,
  'data-testid="form-template-import-upload"',
  '上传控件必须保留稳定的真实页面定位锚点'
)

const styleMatch = dialog.match(/<style lang="scss">([\s\S]*?)<\/style>/)
if (!styleMatch) {
  throw new Error('导入弹窗缺少独立 SCSS 布局样式')
}
const styles = styleMatch[1]

for (const [expected, message] of [
  ['max-width: calc(100vw - 32px)', '导入弹窗必须保留 32px 视口安全边距'],
  ['.form-template-import-dialog__upload', '上传控件样式必须限制在当前弹窗作用域'],
  ['width: 100%', '上传控件和输入控件必须占满可用内容宽度'],
  ['overflow-wrap: anywhere', '长文件名必须在上传列表内换行'],
  ['min-width: 0', '文件名 flex 子项必须允许收缩'],
  ['@media (max-width: 600px)', '导入弹窗必须提供窄屏布局约束']
]) {
  assertIncludes(styles, expected, message)
}

for (const behaviorContract of [
  'accept=".doc,.docx"',
  ':limit="1"',
  'TemplateApi.importTemplateDoc(payload)',
  "payload.append('selectedTemplateId'",
  "message.error(resolveImportErrorMessage(error, '导入失败，请检查模板文件和识别结果'))",
  'throw error'
]) {
  assertIncludes(dialog, behaviorContract, `布局优化不得破坏正式导入契约：${behaviorContract}`)
}

console.log('form template import dialog layout static contract passed')
