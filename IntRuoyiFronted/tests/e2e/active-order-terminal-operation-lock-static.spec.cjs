const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const detailPanel = read(
  'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const backend = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java'
)

const requireMatch = (source, pattern, message) => {
  assert.ok(pattern.test(source), message)
}

requireMatch(
  detailPanel,
  /const activeOrderDossierMutationLockReason = computed\(\(\) => \{[\s\S]*?status === 'RELEASED'[\s\S]*?status === 'VOIDED'/,
  'Released and voided orders must have a read-only dossier lock reason'
)
requireMatch(
  detailPanel,
  /<el-upload[\s\S]*?activeOrderDossierMutationLocked[\s\S]*?data-active-order-dossier-file-upload/,
  'The upload control must be disabled for terminal orders'
)
requireMatch(
  detailPanel,
  /data-active-order-dossier-file-delete[\s\S]*?:disabled="activeOrderDossierMutationLocked"/,
  'The delete control must be disabled for terminal orders'
)
requireMatch(
  detailPanel,
  /const uploadDossierFile = async[\s\S]*?if \(activeOrderDossierMutationLocked\.value\)/,
  'Upload handler must enforce the terminal-order lock'
)
requireMatch(
  detailPanel,
  /const deleteDossierFile = async[\s\S]*?if \(activeOrderDossierMutationLocked\.value\)/,
  'Delete handler must enforce the terminal-order lock'
)
requireMatch(
  backend,
  /nonconformanceReviewMapper\.selectLatestBySource\("PQC_RELEASE",\s*latestApplication\.getId\(\)\)/,
  'Backend must resolve the latest PQC release review'
)
requireMatch(
  backend,
  /MesReleaseFlowStatus\.RELEASED\.equals\(applicationStatus\)/,
  'Released application must be immutable'
)
requireMatch(backend, /"closed"\.equals\(review\.getReviewStatus\(\)\)/, 'Voided review must be closed')
requireMatch(backend, /"void"\.equals\(review\.getDisposition\(\)\)/, 'Voided disposition must be immutable')

console.log('PASS active-order-terminal-operation-lock-static')
