const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8').replace(/\r\n/g, '\n')

const tabs = read('src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue')
const history = read('src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue')
const routes = read('src/router/modules/remaining.ts')

assert.match(tabs, /label="历史追溯"\s+name="history"/, '批记录页签必须显示历史追溯入口。')
assert.match(tabs, /history:\s*'\/mes\/pro\/feedback\/edhr-batch-history'/, '历史追溯页签必须导航到正式历史页面。')
assert.match(routes, /path:\s*'pro\/feedback\/edhr-batch-history'/, '路由必须注册历史追溯页面。')
assert.match(routes, /BatchRecordHistoryPage\.vue/, '历史追溯路由必须加载正式历史页面组件。')
assert.match(history, /downloadEdhrBatchArchive/, '历史追溯页必须提供最终归档下载。')
assert.match(history, /printEdhrBatchArchive/, '历史追溯页必须提供最终归档打印。')
assert.match(history, /下载打印版 PDF/, '历史追溯页下载动作必须使用明确文案。')
assert.match(history, /打印/, '历史追溯页必须显示打印动作。')
assert.doesNotMatch(history, /生成归档/, '历史追溯页必须保持只读，不能生成新归档。')

console.log('PASS: eDHR batch history entry static contract')
