import { readFileSync } from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const root = process.cwd()
const page = readFileSync(path.join(root, 'src/views/erp/sync/index.vue'), 'utf8')
const api = readFileSync(path.join(root, 'src/api/erp/sync/index.ts'), 'utf8')
const service = readFileSync(
  path.resolve(
    root,
    '../IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sync/admin/ErpKingdeeSyncAdminServiceImpl.java'
  ),
  'utf8'
)
const errorCodes = readFileSync(
  path.resolve(
    root,
    '../IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/enums/ErrorCodeConstants.java'
  ),
  'utf8'
)

assert.match(api, /\/erp\/kingdee-sync\/run\/page/)
assert.match(api, /\/erp\/kingdee-sync\/watermark\/list/)
assert.match(api, /\/erp\/kingdee-sync\/incremental-sync/)
assert.match(api, /runIncrementalSync/)
assert.doesNotMatch(api, /JobApi\.(getJobPage|runJob)|runIncrementalSyncJob/)
assert.match(page, /kingdeeProductItemSyncJob/)
assert.match(page, /kingdeeProductionOrderSyncJob/)
assert.match(page, /kingdeeProductionMaterialListSyncJob/)
assert.match(page, /PRODUCTION_MATERIAL_LIST/)
assert.match(page, /生产用料清单/)
assert.doesNotMatch(page, /runProductionMaterialListBackfill/)
assert.match(page, /kingdeeBomSyncJob/)
assert.match(page, /ErpKingdeeSyncApi\.runIncrementalSync/)
assert.match(service, /KINGDEE_TABLE_AUTO_SYNC_TYPE_UNSUPPORTED/)
assert.match(errorCodes, /ERP 表格自动同步不支持同步类型/)
assert.match(page, /v-hasPermi="\['erp:kingdee-sync:query'\]"/)
assert.match(page, /defineOptions\(\{ name: 'ErpKingdeeSync' \}\)/)

console.log('erp kingdee sync static contract passed')
