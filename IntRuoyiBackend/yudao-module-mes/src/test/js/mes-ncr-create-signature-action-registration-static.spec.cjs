const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..', '..', '..', '..')
const adapterPath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesBatchRecordSignatureSubjectAdapter.java'
)
const adapter = fs.readFileSync(adapterPath, 'utf8')

assert.match(
  adapter,
  /action\(MesProBatchRecordExecutionSignatureService\.ACTION_NONCONFORMANCE_REVIEW_CREATE,\s*"eDHR不合格评审创建"\)/,
  'MES 签名适配器必须登记不合格评审创建动作'
)

console.log('PASS: MES NCR create signature action registration static contract')
