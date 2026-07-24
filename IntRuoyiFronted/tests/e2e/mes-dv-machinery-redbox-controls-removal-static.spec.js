const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const machineryPagePath = path.join(root, 'src/views/mes/dv/machinery/index.vue')
const source = fs.readFileSync(machineryPagePath, 'utf8')

const scriptStart = source.indexOf('<script')
assert.notEqual(scriptStart, -1, '设备台账页面脚本必须存在。')

const template = source.slice(0, scriptStart)
const listStart = template.indexOf('<UnifiedListTemplate')
const listEnd = template.indexOf('</UnifiedListTemplate>', listStart)
assert.notEqual(listStart, -1, '设备台账列表必须保留标准列表模板。')
assert.notEqual(listEnd, -1, '设备台账标准列表模板必须正常闭合。')

const listTemplate = template.slice(listStart, listEnd)

assert.equal(
  listTemplate.includes('<template #extra-filters>'),
  false,
  '设备台账必须删除截图红框内的额外筛选区。'
)

const actionStart = listTemplate.indexOf('<template #actions>')
const actionEnd = listTemplate.indexOf('</template>', actionStart)
assert.notEqual(actionStart, -1, '设备台账必须保留工具栏业务操作区。')
assert.notEqual(actionEnd, -1, '设备台账工具栏业务操作区必须正常闭合。')

const actionSource = listTemplate.slice(actionStart, actionEnd)
for (const fragment of [
  '@click="handleQuery"',
  '@click="resetQuery"',
  '> 搜索',
  '> 重置'
]) {
  assert.equal(
    actionSource.includes(fragment),
    false,
    `设备台账必须删除截图红框内操作按钮：${fragment}`
  )
}

for (const fragment of [
  "v-hasPermi=\"['mes:dv-machinery:create']\"",
  '@click="openForm(\'create\')"',
  '@click="handleImport"',
  '@click="handleExport"'
]) {
  assert.equal(
    actionSource.includes(fragment),
    true,
    `设备台账必须保留红框外业务操作：${fragment}`
  )
}

for (const fragment of [
  'queryParams.code',
  'queryParams.name',
  'queryParams.workshopId',
  'const isMachineryExtraFilterEmpty',
  'const resetQuery',
  'workshopList',
  'MdWorkshopApi'
]) {
  assert.equal(
    source.includes(fragment),
    false,
    `设备台账删除红框筛选后必须清理废弃引用：${fragment}`
  )
}

for (const fragment of [
  'label="设备编码"',
  'label="设备名称"',
  'label="所属车间"',
  'label="设备状态"',
  'label="创建时间"',
  'label="操作"'
]) {
  assert.equal(
    template.includes(fragment),
    true,
    `设备台账表格列必须保留：${fragment}`
  )
}

assert.match(
  source,
  /const machineryQuickFilterDefinitions[\s\S]*key: 'status'[\s\S]*label: '设备状态'/,
  '设备台账必须继续保留设备状态快速过滤。'
)

console.log('PASS: MES DV machinery red-box controls are removed')
