const fs = require('fs')
const path = require('path')

const filePath = path.resolve(__dirname, '../../src/views/mes/pro/route/index.vue')
const source = fs.readFileSync(filePath, 'utf8')

if (source.includes('label="路线说明"')) {
  throw new Error(`found obsolete description column in ${filePath}`)
}

if (source.includes('label="备注"')) {
  throw new Error(`found obsolete remark column in ${filePath}`)
}

if (!source.includes('label="负责人"') || !source.includes('prop="ownerName"')) {
  throw new Error(`missing owner column binding in ${filePath}`)
}

if (source.includes('label="末道工序"')) {
  throw new Error(`found obsolete last process column in ${filePath}`)
}

if (!source.includes('label="关键工序"') || !source.includes('prop="keyProcessName"')) {
  throw new Error(`missing key process column binding in ${filePath}`)
}

if (!source.includes('label="关联产品"') || !source.includes('prop="productCodes"')) {
  throw new Error(`missing product codes column binding in ${filePath}`)
}

const formPath = path.resolve(__dirname, '../../src/views/mes/pro/route/RouteForm.vue')
const formSource = fs.readFileSync(formPath, 'utf8')
if (!formSource.includes('v-model="formData.ownerName"')) {
  throw new Error(`missing owner edit field in ${formPath}`)
}

console.log('PASS: route list columns and route form owner field match the latest requirements')
