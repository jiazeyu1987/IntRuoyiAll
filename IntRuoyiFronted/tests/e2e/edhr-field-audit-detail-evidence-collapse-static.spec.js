const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const detailPath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr/FieldAuditDetailPage.vue'
)

const source = fs.readFileSync(detailPath, 'utf8')

assert(
  source.includes('fieldAuditDetailTechnicalEvidenceNames') &&
    source.includes('edhr-field-audit-detail__technical-evidence') &&
    source.includes('<el-collapse-item title="技术证据" name="technical-evidence">'),
  '字段审计详情必须提供默认收起的技术证据折叠区。'
)

assert(
  source.includes('const fieldAuditDetailTechnicalEvidenceNames = ref<string[]>([])'),
  '技术证据折叠区默认必须收起。'
)

const visibleSectionTitlePattern =
  /<div class="edhr-field-audit-detail__section-title">签名与链路校验<\/div>[\s\S]*?<el-descriptions/
assert(
  !visibleSectionTitlePattern.test(source),
  '签名与链路校验不应再作为默认铺开的业务区块。'
)

const chainEvidenceTitleIndex = source.indexOf('链路证据')
const technicalCollapseIndex = source.indexOf('edhr-field-audit-detail__technical-evidence')
assert(
  chainEvidenceTitleIndex > technicalCollapseIndex,
  '链路 Hash 证据必须移入技术证据折叠区。'
)

for (const token of [
  '签名编号',
  '动作码',
  '签名方式',
  '校验批次数',
  '校验明细数',
  '基础字段审计头哈希',
  '签名挑战哈希',
  '计算头哈希',
  '存储头哈希'
]) {
  const tokenIndex = source.indexOf(token)
  assert(tokenIndex > technicalCollapseIndex, `技术字段必须保留在折叠证据区：${token}`)
}

assert(
  source.includes('v-if="detail.hashVerification?.failedReason"') &&
    source.includes('异常原因') &&
    source.includes('detail.hashVerification?.failedReason'),
  '字段审计详情首屏必须在存在失败原因时给出业务可见的异常原因。'
)

for (const term of ['mock', 'fallback', '降级', '静默跳过']) {
  assert(!source.toLowerCase().includes(term.toLowerCase()), `字段审计详情优化不得引入 ${term}`)
}

console.log('PASS: EDHR field audit detail evidence collapse static contract')
