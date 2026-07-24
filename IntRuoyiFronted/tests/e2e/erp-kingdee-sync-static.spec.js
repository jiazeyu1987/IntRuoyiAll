import { readFileSync } from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const root = process.cwd()
const page = readFileSync(path.join(root, 'src/views/erp/sync/index.vue'), 'utf8')
const api = readFileSync(path.join(root, 'src/api/erp/sync/index.ts'), 'utf8')

assert.match(api, /\/erp\/kingdee-sync\/run\/page/)
assert.match(api, /\/erp\/kingdee-sync\/watermark\/list/)
assert.match(api, /runIncrementalSyncJob/)
assert.match(api, /JobApi\.getJobPage/)
assert.match(api, /JobApi\.runJob/)
assert.match(page, /kingdeeProductItemSyncJob/)
assert.match(page, /kingdeeProductionOrderSyncJob/)
assert.match(page, /kingdeeProductionMaterialListSyncJob/)
assert.match(page, /PRODUCTION_MATERIAL_LIST/)
assert.match(page, /生产用料清单/)
assert.doesNotMatch(page, /runProductionMaterialListBackfill/)
assert.match(page, /kingdeeBomSyncJob/)
assert.match(page, /ErpKingdeeSyncApi\.runIncrementalSyncJob/)
assert.match(api, /未找到同步任务处理器/)
assert.match(page, /v-hasPermi="\['infra:job:trigger'\]"/)
assert.match(page, /defineOptions\(\{ name: 'ErpKingdeeSync' \}\)/)

console.log('erp kingdee sync static contract passed')
