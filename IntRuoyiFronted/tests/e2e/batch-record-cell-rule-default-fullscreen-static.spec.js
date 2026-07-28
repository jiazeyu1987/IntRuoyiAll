const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
const sharedDialog = read('src/components/Dialog/src/Dialog.vue')

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)

assertIncludes(
  sharedDialog,
  'defaultFullscreen: propTypes.bool.def(false)',
  '通用 Dialog 必须提供显式 defaultFullscreen 入参，避免业务组件操作内部状态。'
)
assertIncludes(
  sharedDialog,
  'isFullscreen.value = props.defaultFullscreen',
  'Dialog 每次打开时必须按 defaultFullscreen 初始化全屏状态。'
)
assertIncludes(
  dialog,
  ':fullscreen="true"',
  '填写配置弹窗右上角必须显示最大化/恢复按钮。'
)
assertIncludes(
  dialog,
  ':default-fullscreen="true"',
  '填写配置弹窗显示时必须默认全屏。'
)
assertIncludes(dialog, 'title="填写配置"', '默认全屏不得移除填写配置标题。')
assertIncludes(
  dialog,
  'width="calc(100vw - 32px)"',
  '默认全屏不得破坏原有近全屏宽度配置，退出全屏后仍应保持大弹窗。'
)
assertIncludes(
  dialog,
  'batch-record-cell-rules-editor__workspace',
  '默认全屏不得移除左侧预览和右侧配置工作区。'
)
assertIncludes(dialog, '保存填写配置', '默认全屏不得移除底部保存按钮。')

console.log('PASS: batch record cell rule default fullscreen static contract')
