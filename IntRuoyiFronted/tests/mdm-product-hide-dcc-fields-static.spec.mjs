import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync('src/views/mdm/product/index.vue', 'utf8')

const quickFilterSection = source.slice(
  source.indexOf('const productQuickFilterDefinitions'),
  source.indexOf('const productDefaultColumns')
)
const listSection = source.slice(
  source.search(/<template\s+#table\b[^>]*>/),
  source.indexOf('</UnifiedListTemplate>')
)
const queryParamsSection = source.slice(
  source.indexOf('const queryParams = reactive'),
  source.indexOf('const formVisible = ref')
)

assert(quickFilterSection.includes("label: '产品编码'"), '产品主数据快速过滤必须保留产品编码过滤项')
assert(quickFilterSection.includes("label: '状态'"), '产品主数据快速过滤必须保留状态过滤项')
assert(!quickFilterSection.includes("label: 'DCC编号'"), '产品主数据快速过滤不得显示 DCC 编号过滤项')
assert(!quickFilterSection.includes('dccProductCode'), '产品主数据快速过滤不得绑定 dccProductCode')

assert(listSection.includes('label="产品编码"'), '产品主数据列表必须保留产品编码列')
assert(listSection.includes('label="中文名称"'), '产品主数据列表必须保留中文名称列')
assert(listSection.includes('label="更新时间"'), '产品主数据列表必须保留更新时间列')
assert(!listSection.includes('label="DCC产品编号"'), '产品主数据列表不得显示 DCC 产品编号列')
assert(!listSection.includes('prop="dccProductCode"'), '产品主数据列表不得绑定 dccProductCode 列')

assert(!queryParamsSection.includes('dccProductCode'), '产品主数据分页查询参数不得提交 dccProductCode')

console.log('PASS: 产品主数据页面已隐藏 DCC 筛选项和列表列')
