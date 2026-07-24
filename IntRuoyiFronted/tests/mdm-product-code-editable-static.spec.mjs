import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync('src/views/mdm/product/index.vue', 'utf8')

assert(
  /<el-form-item\s+label="产品编码"\s+prop="productCode">\s*<el-input\s+v-model="formData\.productCode"\s+maxlength="64"\s*\/>/s.test(source),
  '产品主数据编辑弹窗的产品编码必须保持可编辑输入框'
)

assert(
  !/<el-input\s+v-model="formData\.productCode"[^>]*(disabled|:disabled)/s.test(source),
  '产品主数据编辑弹窗的产品编码不得被禁用'
)

console.log('PASS mdm product code editable contract')
