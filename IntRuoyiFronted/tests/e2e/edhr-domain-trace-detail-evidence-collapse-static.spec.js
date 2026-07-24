const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const detailPath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr/DomainTraceDetailPage.vue'
)
const source = fs.readFileSync(detailPath, 'utf8')

assert(
  source.includes('const domainTraceDetailTechnicalEvidenceNames = ref<string[]>([])'),
  'Domain trace detail technical evidence collapse must be closed by default.'
)

assert(
  source.includes('class="edhr-domain-trace-detail__technical-evidence"') &&
    source.includes('<el-collapse-item title="技术证据" name="technical-evidence">'),
  'Domain trace detail must move trace hash and snapshot ID into a technical evidence collapse.'
)

const technicalEvidenceIndex = source.indexOf('edhr-domain-trace-detail__technical-evidence')
const traceHashIndex = source.indexOf('detail.domainTraceHash')
const snapshotIdIndex = source.indexOf('detail.domainTraceSnapshotId')

assert(
  technicalEvidenceIndex > -1 &&
    traceHashIndex > technicalEvidenceIndex &&
    snapshotIdIndex > technicalEvidenceIndex,
  'Domain trace hash and snapshot ID must be rendered inside the technical evidence section.'
)

assert(
  !/<div class="edhr-domain-trace-detail__section-title">追溯证据<\/div>[\s\S]*?detail\.domainTraceHash/.test(
    source
  ),
  'Trace evidence must not remain as a default-visible top-level section.'
)

assert(
  source.includes('追溯摘要') &&
    source.includes('阻塞原因') &&
    source.includes('追溯明细') &&
    source.includes('type="expand"') &&
    source.includes('empty-text="暂无追溯项"'),
  'Domain trace detail must preserve the business summary, blockers, trace item table, and expandable row evidence.'
)

assert(
  !/mock|fallback|降级|静默跳过/.test(source),
  'Domain trace detail optimization must not introduce mock data, fallback, downgrade, or silent skip wording.'
)

console.log('PASS: EDHR domain trace detail evidence collapse static contract')
