const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const component = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
const template = component.slice(
  component.indexOf('<template>'),
  component.indexOf('<script setup')
)

const includes = (content, token, message) => assert.ok(content.includes(token), message)
const notIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

includes(template, 'data-fill-config-panel="source-form"', '左侧原表单主体区域必须保留。')
includes(template, 'data-fill-config-panel="assist-preview"', '中间辅助表格主体区域必须保留。')
includes(template, 'data-fill-config-panel="mapping-control"', '右侧映射控制栏必须保留。')
includes(template, 'data-assist-grid-cell', '辅助表格可点击格子必须保留。')
includes(template, 'batch-record-cell-rules-editor__side-actions', '保存、重读、关闭能力必须移动到右侧固定操作区。')
includes(template, '@click="confirmAllRules"', '保存填写配置必须继续调用正式保存链路。')
includes(template, '@click="loadCellRules"', '重新读取必须继续调用正式读取链路。')

notIncludes(template, 'data-fill-config-actions="primary"', '顶部右侧红框操作组不得继续显示。')
notIncludes(template, 'batch-record-cell-rules-editor__top-actions', '顶部右侧操作组 DOM 不得保留。')
notIncludes(template, '<strong>原表单</strong>', '左侧原表单红框说明标题不得显示。')
notIncludes(template, '点击任意单元格只会选中规则目标', '左侧原表单红框说明文案不得显示。')
notIncludes(template, '<el-tag type="info" effect="plain">只读</el-tag>', '左侧原表单红框只读标签不得显示。')
notIncludes(template, '<strong>辅助表单预览</strong>', '中间辅助表单预览红框标题不得显示。')
notIncludes(template, '点击黄色表格单元格后', '中间辅助表单预览红框说明文案不得显示。')
notIncludes(template, '<el-tag type="warning" effect="plain">实时</el-tag>', '中间辅助表单预览红框实时标签不得显示。')

console.log('PASS edhr-fill-config-redbox-hide-static')
