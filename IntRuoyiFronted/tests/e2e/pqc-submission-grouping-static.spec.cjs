const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const read = (...segments) => fs.readFileSync(path.join(repoRoot, ...segments), 'utf8')

const page = read(
  'IntRuoyiFronted',
  'src',
  'views',
  'mes',
  'pro',
  'processpool',
  'TeamLeaderWorkbenchPage.vue'
)
const api = read('IntRuoyiFronted', 'src', 'api', 'mes', 'pro', 'processpool', 'index.ts')

assert(api.includes('groupedEventIds'), 'PQC 分组列表必须返回全部来源事件编号')
assert(api.includes('groupedOriginalPayloadJsons'), 'PQC 分组列表必须返回全部来源 payload')
assert(page.includes('groupedOriginalPayloadJsons'), 'PQC 列表详情必须读取全部来源 payload')
assert(page.includes('resolvePqcItemSnapshotDetails'), 'PQC 列表必须保留检验项目明细')

console.log('pqc-submission-grouping-static: PASS')
