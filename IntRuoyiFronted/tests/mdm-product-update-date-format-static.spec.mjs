import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync('src/views/mdm/product/index.vue', 'utf8')

assert(
  source.includes("import { dateFormatter2 } from '@/utils/formatTime'"),
  '产品主数据页面必须复用项目已有 dateFormatter2'
)
assert(
  /<el-table-column[\s\S]*?label="更新时间"[\s\S]*?prop="updateTime"[\s\S]*?:formatter="dateFormatter2"[\s\S]*?\/>/.test(source),
  '产品主数据更新时间列必须使用 dateFormatter2 展示 YYYY-MM-DD'
)
