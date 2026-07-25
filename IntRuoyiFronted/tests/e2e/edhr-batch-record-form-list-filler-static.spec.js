const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const page = read('src/views/mes/pro/batchrecordformlist/index.vue')
const api = read('src/api/mes/pro/edhr/processFormPermissionRule.ts')
const processPage = read('src/views/mes/pro/process/index.vue')

const reportNameColumn = page.indexOf('label="表单名称"')
const fillerColumn = page.indexOf('label="填写人"')
const formSlotTypeColumn = page.indexOf('label="类型"')

assert(reportNameColumn >= 0, '批记录表单列表必须保留“表单名称”列')
assert(fillerColumn > reportNameColumn, '“填写人”列必须放在“表单名称”之后')
assert(formSlotTypeColumn > fillerColumn, '“填写人”列必须放在“类型”之前，即截图黄色框位置')

assert(
  page.includes('openBatchRecordFormPermissionDialog'),
  '批记录表单列表页必须提供行级填写人配置入口'
)
assert(
  page.includes('批记录表单填写人设置') && page.includes('填写人来源') && page.includes('个人') && page.includes('角色'),
  '填写人设置弹窗必须支持个人和角色'
)
assert(
  page.includes('batch-record-form-permission-filler-field') &&
    page.includes('batch-record-form-permission-filler-control'),
  '填写人设置弹窗必须给“填写人”选择框单独布局类，避免被三等分网格截断'
)
assert(
  /\.batch-record-form-permission-rule\s*\{[\s\S]*grid-template-columns:\s*minmax\(180px,\s*0\.85fr\)\s+minmax\(280px,\s*1\.4fr\)\s+minmax\(220px,\s*1fr\)/.test(page),
  '填写人设置弹窗中间“填写人”列必须比来源和完成策略更宽，完整展示角色/人员名称'
)
assert(
  /\.batch-record-form-permission-filler-control\s*:deep\(\.el-select__tags-text\)\s*\{[\s\S]*max-width:\s*none;/.test(page),
  '填写人设置弹窗选中标签文本不得继续使用 Element Plus 默认省略宽度'
)
assert(
  page.includes('EdhrProcessFormPermissionRuleApi.getByReport') &&
    page.includes('EdhrProcessFormPermissionRuleApi.saveByReport'),
  '批记录表单列表页必须调用表单维度填写人 API，而不是工艺路线配置 API'
)
assert(
  !page.includes('保存填写人前请先完成批记录路线绑定') &&
    !page.includes(':disabled="permissionTarget.permissionRule?.affectedRouteBindingCount === 0"'),
  '批记录表单列表页填写人保存必须只绑定表单，不得因未绑定工艺路线而提示或禁用保存'
)
assert(
  api.includes('getByReport') && api.includes('/get-by-report') &&
    api.includes('saveByReport') && api.includes('/save-by-report'),
  'processFormPermissionRule API 必须暴露表单维度 get/save 方法'
)
for (const removedLabel of ['生产填写人', '质量填写人', '设备填写人']) {
  assert(
    !processPage.includes(removedLabel),
    `工序设置页不得继续展示${removedLabel}，填写人必须在批记录表单页配置`
  )
}

console.log('PASS: eDHR batch record form list filler static contract')
