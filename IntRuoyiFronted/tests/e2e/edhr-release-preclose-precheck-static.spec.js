import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const frontendRoot = process.cwd()
const backendRoot = path.resolve(frontendRoot, '..', 'ruoyi-vue-pro')
const servicePath = path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java'
)
const source = fs.readFileSync(servicePath, 'utf8')

const dhrCompletenessMatch = source.match(
  /private MesProEdhrReleaseCheckItemDO buildDhrCompletenessItem\([\s\S]*?private boolean ordinaryProcessFillEvidenceComplete/
)

assert.ok(dhrCompletenessMatch, '放行预检必须保留 DHR 完整性检查。')

const dhrCompletenessBlock = dhrCompletenessMatch[0]

assert.match(
  dhrCompletenessBlock,
  /ordinaryProcessFillEvidenceComplete\(batch\.getId\(\)\)/,
  'DHR 完整性检查必须基于普通工序填写与提交签名证据。'
)

assert.doesNotMatch(
  dhrCompletenessBlock,
  /BATCH_STATUS_CLOSED|BATCH_STATUS_ARCHIVED|getAggregateHash\(\)/,
  '放行预检发生在关闭前，预检不得依赖已关闭、已归档或关闭后聚合摘要。'
)
