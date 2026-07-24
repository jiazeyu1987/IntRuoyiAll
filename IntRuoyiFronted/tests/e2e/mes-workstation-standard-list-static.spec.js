const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const component = read('src/views/mes/md/workstation/index.vue')
const workstationApi = read('src/api/mes/md/workstation/index.ts')

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} must include: ${expected}`)
  }
}

const assertNotIncludes = (content, expected, label) => {
  if (content.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

const assertMatches = (content, pattern, label) => {
  if (!pattern.test(content)) {
    throw new Error(`${label} must match: ${pattern}`)
  }
}

assertIncludes(
  component,
  "import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'",
  'Workstation list standard template import'
)
assertIncludes(component, '<UnifiedListTemplate', 'Workstation list standard template shell')
assertIncludes(component, 'table-key="mes.md.workstation.main"', 'Workstation list stable table key')
assertIncludes(
  component,
  "useUserTableColumns('mes.md.workstation.main', workstationDefaultColumns)",
  'Workstation list display fields and width persistence hook'
)
assertIncludes(
  component,
  'useTableQuickFilter(',
  'Workstation list quick filter hook'
)
assertIncludes(component, "'mes.md.workstation.main',", 'Workstation list quick filter table key')
assertIncludes(
  component,
  '@header-dragend="handleWorkstationHeaderDragend"',
  'Workstation list column width drag persistence'
)
assertIncludes(component, 'data-user-table-column-explicit', 'Workstation list explicit column marker')
assertIncludes(
  component,
  'data-user-table-key="mes.md.workstation.main"',
  'Workstation list table column key marker'
)
assertIncludes(
  component,
  '@column-change="saveWorkstationColumnConfig"',
  'Workstation list display field persistence action'
)
assertNotIncludes(component, '<Pagination', 'Workstation list standalone pagination after template migration')

for (const [key, label] of [
  ['code', '工作站编码'],
  ['name', '工作站名称'],
  ['address', '工作站地点'],
  ['workshopName', '所在车间'],
  ['processName', '所属工序'],
  ['machineryCount', '绑定设备'],
  ['configuredWorkerCount', '理论配置人数'],
  ['currentWorkerCount', '当前在岗人数'],
  ['singleStandardHourlyCapacity', '人工标准小时产能'],
  ['machineryStandardHourlyCapacity', '设备标准小时产能'],
  ['shiftHours', '班次小时'],
  ['todayCapacity', '班次产能'],
  ['status', '状态'],
  ['createTime', '创建时间'],
  ['operation', '操作']
]) {
  assertIncludes(component, `key: '${key}', label: '${label}'`, `Workstation default column ${label}`)
  assertIncludes(
    component,
    `v-if="isWorkstationColumnVisible('${key}')"`,
    `Workstation visible column guard ${label}`
  )
}

assertIncludes(
  component,
  'prop="machineryCount"',
  'Workstation bound equipment count column data prop'
)
assertIncludes(
  component,
  "{{ formatWorkstationIntegerCapacity(scope.row.singleStandardHourlyCapacity, '-') }}",
  'Workstation manual standard hourly capacity must display as integer'
)
assertIncludes(
  component,
  'openWorkstationMachineDialog(scope.row)',
  'Workstation bound equipment count must open readonly machine dialog'
)
assertIncludes(
  component,
  'scope.row.machineryCount > 0',
  'Workstation bound equipment count must only be clickable when positive'
)
assertNotIncludes(
  component,
  "{{ scope.row.machinerySummary || '未绑定' }}",
  'Workstation bound equipment column must not show summary text in main table'
)
assertMatches(
  component,
  /<Dialog[\s\S]*?:title="workstationMachineDialogTitle"[\s\S]*?v-model="workstationMachineDialogVisible"[\s\S]*?width="760px"[\s\S]*?@closed="handleWorkstationMachineDialogClosed"[\s\S]*?>/,
  'Workstation bound equipment readonly dialog'
)
assertIncludes(
  component,
  ':data="workstationMachineList"',
  'Workstation bound equipment dialog table data'
)
assertIncludes(
  component,
  'MdWorkstationMachineApi.getWorkstationMachineList(row.id)',
  'Workstation bound equipment dialog loads existing workstation machine API'
)
assertIncludes(
  component,
  'const pendingMachineryLedgerId = ref<number | undefined>()',
  'Workstation bound equipment dialog stores pending machinery ledger navigation until dialog closed'
)
assertIncludes(
  component,
  'pendingMachineryLedgerId.value = normalizedMachineryId',
  'Workstation bound equipment device click records pending machinery ledger id'
)
assertIncludes(
  component,
  'const handleWorkstationMachineDialogClosed = async () => {',
  'Workstation bound equipment dialog closed handler owns machinery ledger navigation'
)
assertIncludes(
  component,
  "await router.push({ path: '/mes/dv/machinery', query: { openId: String(machineryId) } })",
  'Workstation bound equipment dialog closed handler links to machinery ledger openId'
)
const openMachineryLedgerBlock = component.slice(
  component.indexOf('const openMachineryLedger = async'),
  component.indexOf('const handleWorkstationMachineDialogClosed = async')
)
assertMatches(
  openMachineryLedgerBlock,
  /const openMachineryLedger = async \(machineryId\?: number\) => \{[\s\S]*?pendingMachineryLedgerId\.value = normalizedMachineryId[\s\S]*?workstationMachineDialogVisible\.value = false[\s\S]*?\n\}/,
  'Workstation bound equipment click must close dialog before machinery ledger navigation'
)
assertNotIncludes(
  openMachineryLedgerBlock,
  'router.push(',
  'Workstation bound equipment click must not navigate before Element Plus dialog closed event'
)
assertIncludes(
  workstationApi,
  'machinerySummary?: string // 绑定设备摘要',
  'Workstation API type must expose bound equipment summary'
)
assertIncludes(
  workstationApi,
  'machineryCount?: number // 绑定设备个数',
  'Workstation API type must expose bound equipment count'
)

assertNotIncludes(component, '<template #extra-filters>', 'Workstation red-box extra filter slot removed')
assertNotIncludes(component, '<MdWorkshopSelect', 'Workstation workshop red-box control removed')
assertNotIncludes(component, '<ProProcessSelect', 'Workstation process red-box control removed')
assertNotIncludes(component, '<el-button @click="handleQuery"', 'Workstation red-box search button removed')
assertNotIncludes(component, '<el-button @click="resetQuery"', 'Workstation red-box reset button removed')
assertNotIncludes(component, "import MdWorkshopSelect from './components/MdWorkshopSelect.vue'", 'Workstation workshop select import removed')
assertNotIncludes(
  component,
  "import ProProcessSelect from '@/views/mes/pro/process/components/ProProcessSelect.vue'",
  'Workstation process select import removed'
)
assertIncludes(component, '<template #actions>', 'Workstation yellow-box actions remain in template actions slot')
assertIncludes(component, "openForm('create')", 'Workstation create action remains in moved actions slot')
assertIncludes(component, 'handleExport', 'Workstation export action remains in moved actions slot')
assertNotIncludes(component, ':show-column-settings="false"', 'Workstation display fields remain in template toolbar')
assertIncludes(component, 'MdWorkstationApi.getWorkstationPage(buildWorkstationPageParams())', 'Workstation page API contract')
assertIncludes(
  component,
  'normalizePositiveProcessId(processIdText)',
  'Workstation route processId query must ignore zero or invalid sentinel values'
)
assertIncludes(component, "v-hasPermi=\"['mes:md-workstation:create']\"", 'Workstation create permission')
assertIncludes(component, "v-hasPermi=\"['mes:md-workstation:export']\"", 'Workstation export permission')
assertIncludes(component, "v-hasPermi=\"['mes:md-workstation:update']\"", 'Workstation update permission')
assertIncludes(component, "v-hasPermi=\"['mes:md-workstation:delete']\"", 'Workstation delete permission')
assertIncludes(component, "v-hasPermi=\"['mes:md-workstation:query']\"", 'Workstation barcode permission')
assertIncludes(component, "download.excel(data, '工作站.xls')", 'Workstation export file name')
assertIncludes(component, 'openByBusiness', 'Workstation barcode business action')

console.log('mes-workstation-standard-list-static PASS')
