const fs = require('fs')
const path = require('path')
const assert = require('assert')

const projectRoot = path.resolve(__dirname, '../..')

const readSource = (relativePath) =>
  fs.readFileSync(path.join(projectRoot, relativePath), 'utf8')

const assertIncludes = (source, expected, message) => {
  assert(
    source.includes(expected),
    `${message}\nExpected source to include: ${expected}`
  )
}

const assertMatches = (source, pattern, message) => {
  assert(pattern.test(source), `${message}\nExpected source to match: ${pattern}`)
}

const assertNotIncludes = (source, unexpected, message) => {
  assert(
    !source.includes(unexpected),
    `${message}\nExpected source not to include: ${unexpected}`
  )
}

const notifySource = readSource('src/views/system/notify/my/components/MyNotifyMessageList.vue')
const machinerySource = readSource('src/views/mes/dv/machinery/index.vue')

assertIncludes(
  notifySource,
  "import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'",
  '我的站内信列表必须导入标准列表模板'
)
assertIncludes(
  notifySource,
  "import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'",
  '我的站内信列表必须接入显示字段和列宽持久化'
)
assertIncludes(
  notifySource,
  "import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'",
  '我的站内信列表必须接入快速过滤'
)
assertIncludes(
  notifySource,
  'table-key="system.notify.my-message"',
  '我的站内信列表必须使用稳定的标准列表 table-key'
)
assertIncludes(
  notifySource,
  '@header-dragend="handleNotifyHeaderDragend"',
  '我的站内信列表必须持久化列宽拖拽'
)
assertIncludes(
  notifySource,
  'v-if="isNotifyColumnVisible(\'templateNickname\')"',
  '我的站内信列表必须通过标准列配置控制发送人列显示'
)
assertMatches(
  notifySource,
  /<UnifiedListTemplate[\s\S]*v-model:page="queryParams\.pageNo"[\s\S]*v-model:limit="queryParams\.pageSize"[\s\S]*@pagination="getList"/,
  '我的站内信列表分页必须由标准列表模板承载'
)
assertMatches(
  notifySource,
  /const notifyQuickFilterDefinitions[\s\S]*key: 'readStatus'[\s\S]*label: '是否已读'/,
  '我的站内信列表必须保留是否已读过滤能力并接入快速过滤'
)
assertNotIncludes(
  notifySource,
  '<template #extra-filters>',
  '我的站内信列表不应再渲染截图红框内的额外筛选区'
)
assertNotIncludes(
  notifySource,
  '<el-form-item label="发送时间"',
  '我的站内信列表不应再渲染截图红框内的发送时间筛选'
)
assertNotIncludes(
  notifySource,
  '@click="resetQuery"',
  '我的站内信列表不应再渲染截图红框内的额外重置按钮'
)
assertNotIncludes(
  notifySource,
  'const isNotifyExtraFilterEmpty',
  '我的站内信列表移除额外筛选区后不应保留对应空值判断'
)

assertIncludes(
  machinerySource,
  "import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'",
  '设备台账列表必须导入标准列表模板'
)
assertIncludes(
  machinerySource,
  "import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'",
  '设备台账列表必须接入显示字段和列宽持久化'
)
assertIncludes(
  machinerySource,
  "import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'",
  '设备台账列表必须接入快速过滤'
)
assertIncludes(
  machinerySource,
  'table-key="mes.dv.machinery.main"',
  '设备台账列表必须使用稳定的标准列表 table-key'
)
assertIncludes(
  machinerySource,
  '@header-dragend="handleMachineryHeaderDragend"',
  '设备台账列表必须持久化列宽拖拽'
)
assertIncludes(
  machinerySource,
  'v-if="isMachineryColumnVisible(\'code\')"',
  '设备台账列表必须通过标准列配置控制设备编码列显示'
)
assertMatches(
  machinerySource,
  /<UnifiedListTemplate[\s\S]*v-model:page="queryParams\.pageNo"[\s\S]*v-model:limit="queryParams\.pageSize"[\s\S]*@pagination="getList"/,
  '设备台账列表分页必须由标准列表模板承载'
)
assertMatches(
  machinerySource,
  /const machineryQuickFilterDefinitions[\s\S]*key: 'status'[\s\S]*label: '设备状态'/,
  '设备台账列表必须保留设备状态过滤能力并接入快速过滤'
)

console.log('notify-machinery-standard-list-template-static: PASS')
