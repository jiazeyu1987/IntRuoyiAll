const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const processList = read('src/views/mes/pro/process/index.vue')
const workstationList = read('src/views/mes/md/workstation/index.vue')
const workstationApi = read('src/api/mes/md/workstation/index.ts')
const workstationForm = read('src/views/mes/md/workstation/WorkstationForm.vue')

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} must include: ${expected}`)
  }
}

assertIncludes(
  processList,
  "{ key: 'workstationNames', label: '工作站', width: 240, hideable: false }",
  'Process list workstation relation default column'
)
assertIncludes(
  processList,
  `v-if="isProcessColumnVisible('workstationNames')"`,
  'Process list workstation relation visible guard'
)
assertIncludes(processList, 'formatProcessWorkstation', 'Process list workstation display formatter')
assertIncludes(
  workstationList,
  "key: 'processName', label: '所属工序'",
  'Workstation list process relation default column'
)
assertIncludes(
  workstationList,
  `v-if="isWorkstationColumnVisible('processName')"`,
  'Workstation list process relation visible guard'
)
assertIncludes(workstationApi, 'processId: number', 'Workstation API process id relation field')
assertIncludes(workstationApi, 'processName: string', 'Workstation API process name display field')
assertIncludes(workstationForm, 'label="所属工序"', 'Workstation form process relation field')
assertIncludes(workstationForm, 'processId: [{ required: true', 'Workstation form process relation required rule')

console.log('mes-process-workstation-correspondence-static PASS')
