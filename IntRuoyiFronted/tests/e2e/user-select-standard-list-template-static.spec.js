const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/system/user/components/UserSelectDialogV2.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

assert.match(
  source,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/,
  '人员选择弹窗必须导入标准列表模板。'
)
assert.match(
  source,
  /import \{ useUserTableColumns, type UserTableColumnDefinition \} from '@\/hooks\/web\/useUserTableColumns'/,
  '人员选择列表必须接入显示字段和列宽持久化 hook。'
)

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="system\.user\.selectDialog"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, '人员选择列表必须用稳定 tableKey system.user.selectDialog 接入 UnifiedListTemplate。')
const template = templateMatch[0]

assert.match(template, /:query-model="queryParams"/, '标准模板必须绑定原查询参数。')
assert.match(template, /:show-quick-filter="false"/, '人员选择弹窗保留原筛选项，不应额外显示快速过滤。')
assert.match(template, /:filter-definitions="userSelectQuickFilterDefinitions"/, '标准模板必须显式绑定空快速过滤定义。')
assert.match(template, /:columns="userSelectColumns"/, '标准模板必须绑定显示字段配置。')
assert.match(template, /:column-saving="userSelectColumnSaving"/, '标准模板必须绑定显示字段保存状态。')
assert.match(template, /@column-change="saveUserSelectColumnConfig"/, '显示字段变化必须自动保存。')
assert.match(template, /@column-reset="resetUserSelectColumnConfig"/, '显示字段重置必须接入标准列配置重置。')
assert.match(template, /v-model:page="queryParams\.pageNo"/, '标准模板分页必须绑定 pageNo。')
assert.match(template, /v-model:limit="queryParams\.pageSize"/, '标准模板分页必须绑定 pageSize。')
assert.match(template, /@pagination="getList"/, '标准模板分页必须复用原列表查询。')

const filtersMatch = template.match(/<template #extra-filters>([\s\S]*?)<\/template>/)
assert.ok(filtersMatch, '人员选择列表必须把原筛选项放入标准模板 extra-filters 插槽。')
const filters = filtersMatch[1]
for (const label of ['用户名称', '用户昵称', '手机号码', '状态']) {
  assert.match(filters, new RegExp(`label="${label}"`), `筛选项必须保留：${label}`)
}

const actionsMatch = template.match(/<template #actions>([\s\S]*?)<\/template>/)
assert.ok(actionsMatch, '人员选择列表必须把搜索和重置放入标准模板 actions 插槽。')
const actions = actionsMatch[1]
assert.match(actions, /@click="handleQuery"/, '标准模板工具栏必须保留搜索操作。')
assert.match(actions, /@click="resetQuery"/, '标准模板工具栏必须保留重置操作。')

assert.match(
  template,
  /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*data-user-table-column-explicit[\s\S]*data-user-table-key="system\.user\.selectDialog"[\s\S]*@header-dragend="handleUserSelectHeaderDragend"/,
  '人员选择表格必须位于标准模板 table 插槽，并接入列宽拖拽持久化。'
)
assert.match(template, /type="selection"[\s\S]*:reserve-selection="true"/, '多选 checkbox 跨页保留逻辑必须保留。')
assert.match(template, /<el-radio[\s\S]*v-model="selectedRadioId"/, '单选 radio 逻辑必须保留。')

for (const field of ['id', 'username', 'nickname', 'deptName', 'mobile', 'status', 'createTime']) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `${field} 必须注册到人员选择列配置。`)
  assert.match(
    template,
    new RegExp(`isUserSelectColumnVisible\\('${field}'\\)`),
    `${field} 列必须受显示字段配置控制。`
  )
}

assert.match(
  source,
  /useUserTableColumns\(USER_SELECT_TABLE_KEY, userSelectDefaultColumns\)/,
  '人员选择列表必须用稳定 tableKey 保存显示字段。'
)
assert.doesNotMatch(
  source,
  /<ContentWrap[\s\S]*数据表格：单选 radio \/ 多选 checkbox[\s\S]*<Pagination/,
  '旧的裸 ContentWrap + el-table + Pagination 列表壳必须移除。'
)

console.log('PASS: user select standard list template static contract')

