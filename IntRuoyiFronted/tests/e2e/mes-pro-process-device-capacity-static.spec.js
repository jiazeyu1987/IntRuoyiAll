const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const processPageSource = readText('src/views/mes/pro/process/index.vue')
const processApiSource = readText('src/api/mes/pro/process/index.ts')
const processFormSource = readText('src/views/mes/pro/process/ProProcessForm.vue')

assert(
  !processPageSource.includes('label="产品名称"') &&
    !processPageSource.includes('prop="productName"') &&
    !processPageSource.includes('queryParams.productName'),
  '工序设置列表不展示产品名称搜索项和产品名称列'
)

assert(
  !/label="设备"[\s\S]*?prop="machineryQuantityTotal"/.test(processPageSource) &&
    !/isProcessColumnVisible\('machineryQuantityTotal'\)/.test(processPageSource),
  '工序设置主列表不得继续渲染设备列，设备资源统一在工作站维护'
)

assert(
  !/label="班次产能"[\s\S]*?prop="availableShiftCapacityTotal"/.test(processPageSource) &&
    !/isProcessColumnVisible\('availableShiftCapacityTotal'\)/.test(processPageSource),
  '工序设置主列表不得继续渲染班次产能列，班次产能统一在工作站维护'
)

assert(
  !/label="产能"[\s\S]*?prop="availableShiftCapacityTotal"/.test(processPageSource),
  '工序设置列表不得继续把 availableShiftCapacityTotal 显示为产能'
)

assert(
  processPageSource.includes("key: 'workstationNames'") &&
    processPageSource.includes('label="工作站"') &&
    processPageSource.includes('formatProcessWorkstation'),
  '工序设置主列表必须保留工作站列作为产能来源入口'
)

assert(
  processApiSource.includes('manualShiftCapacity') &&
    processApiSource.includes('machineryQuantityTotal') &&
    processApiSource.includes('availableShiftCapacityTotal') &&
    processApiSource.includes('getProcessMachineryList'),
  '工序设置 API 类型可保留设备、产能和设备明细字段供详情或兼容读取'
)

assert(
  processFormSource.includes('prop="productName"') && processFormSource.includes('产品名称'),
  '工序新增/编辑表单必须支持产品名称'
)

console.log('PASS: pro process device capacity frontend contract is satisfied')
