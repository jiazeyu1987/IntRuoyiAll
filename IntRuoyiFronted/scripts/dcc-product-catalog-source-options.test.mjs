import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'

const root = process.cwd()
const componentPath =
  'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'
const source = fs.readFileSync(path.resolve(root, componentPath), 'utf8')

test('DCC product catalog source options only expose Yingtai products', () => {
  assert.doesNotMatch(source, /子公司产品/, '产品目录前端不得再出现子公司产品来源')
  assert.match(
    source,
    /const dataSourceOptions = \[\s*\{\s*label:\s*'瑛泰产品',\s*value:\s*'瑛泰产品'\s*\}\s*\]/,
    '数据来源下拉只允许瑛泰产品'
  )
  assert.match(
    source,
    /const formData = ref<DccProductCatalogUpdateReqVO>\(\{\s*dataSource:\s*'瑛泰产品'/,
    '新增产品目录表单默认数据来源必须是瑛泰产品'
  )
  assert.match(
    source,
    /const resetFormData = \(\) => \{\s*formData\.value = \{\s*dataSource:\s*'瑛泰产品'/,
    '重置产品目录表单必须恢复为瑛泰产品'
  )
  assert.match(
    source,
    /key:\s*'dataSource'[\s\S]*label:\s*'数据来源'[\s\S]*type:\s*'select'[\s\S]*options:\s*dataSourceOptions/,
    '数据来源快速过滤必须复用受控来源选项'
  )
})
