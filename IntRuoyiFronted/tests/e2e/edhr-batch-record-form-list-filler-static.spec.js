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
