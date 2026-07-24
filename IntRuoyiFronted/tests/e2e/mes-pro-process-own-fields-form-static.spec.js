const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const formSource = readText('src/views/mes/pro/process/ProProcessForm.vue')
const apiSource = readText('src/api/mes/pro/process/index.ts')

assert(
  apiSource.includes('manualShiftCapacity?: number'),
  '工序设置 API 类型必须声明人工班产能字段 manualShiftCapacity。'
)

assert(
  formSource.includes('label="人工班产能"') &&
    formSource.includes('prop="manualShiftCapacity"'),
  '工序新增/编辑/详情表单必须展示人工班产能字段。'
)

assert.match(
  formSource,
  /v-model(?:\.number)?="formData\.manualShiftCapacity"/,
  '人工班产能字段必须绑定 formData.manualShiftCapacity，确保新增和编辑提交该字段。'
)

assert(
  formSource.includes('<el-input-number') || formSource.includes('type="number"'),
  '人工班产能必须使用数字输入控件，避免纯文本录入。'
)

assert.match(
  formSource,
  /manualShiftCapacity:\s*undefined/,
  'resetForm 和初始 formData 必须将 manualShiftCapacity 初始化为 undefined，避免提交错误默认值。'
)

assert(
  !formSource.includes('prop="routeList"') &&
    !formSource.includes('prop="batchRecordForms"') &&
    !formSource.includes('prop="productionFillers"') &&
    !formSource.includes('prop="machineryQuantityTotal"'),
  '工序弹窗不得维护所属工艺路线、批记录表单、填写人或设备等关联/派生列。'
)

console.log('PASS: MES pro process own fields form contract is satisfied')
