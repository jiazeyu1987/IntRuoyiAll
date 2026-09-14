const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const backendRoot = path.resolve(repoRoot, '../IntRuoyiBackend')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
const readBackend = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const panel = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const feedbackApi = read('src/api/mes/pro/feedback/index.ts')
const backendSubmitVo = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesProFrontlineFeedbackSubmitReqVO.java'
)

const sliceBetween = (source, startNeedle, endNeedle) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${startNeedle} must exist.`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `${endNeedle} must appear after ${startNeedle}.`)
  return source.slice(start, end)
}

const onMountedBlock = sliceBetween(panel, 'onMounted(async () => {', 'onUnmounted(() => {')
const initializeProductionSelectionBlock = sliceBetween(
  panel,
  'const initializeProductionSelection = async () => {',
  'const resolveErrorMessage'
)
const pqcStartupBlock = sliceBetween(
  onMountedBlock,
  'if (isPqcMode.value) {',
  '  const [loadedCatalog] = await Promise.all(['
)
assert.match(
  pqcStartupBlock,
  /loadFrontlinePqcActiveOrders\(deviceState\)/,
  'PQC mode must keep using active orders because PQC inspection is order scoped.'
)
assert.match(
  initializeProductionSelectionBlock,
  /loadFrontlineProductionActiveOrders\(deviceState\)[\s\S]*requestedActiveOrder\s*\|\|\s*activeOrders\[0\][\s\S]*await handleSelectActiveOrder\(initialActiveOrder,\s*requestedProcessIdentity\)/,
  'Production mode must select the requested or first active order before refreshing its route processes.'
)
assert.doesNotMatch(
  initializeProductionSelectionBlock,
  /loadFrontlineDeviceProcesses\(deviceState\)/,
  'Production startup must not select from a global process read before the order is selected.'
)
assert.match(
  onMountedBlock,
  /Promise\.all\(\[[\s\S]*catalogRequest,[\s\S]*initializeProductionSelection\(\)[\s\S]*\]\)/,
  'Production startup must load the catalog and order-driven selection workflow concurrently.'
)
const productionStartupBlock = onMountedBlock.slice(onMountedBlock.indexOf('const [loadedCatalog] = await Promise.all(['))
assert.doesNotMatch(
  productionStartupBlock,
  /loadFrontlinePqcActiveOrders|selectFrontlinePqcActiveOrder/,
  'Production mode must not depend on PQC active-order loading.'
)

const submitBlockedBlock = sliceBetween(panel, 'const isSubmitBlocked = computed(() =>', 'const statusText = computed')
assert.match(
  submitBlockedBlock,
  /!deviceState\.selectedActiveOrder/,
  'Production and PQC modes must block filling until the current active order is formally selected.'
)
assert.doesNotMatch(submitBlockedBlock, /!context\.taskId/)

const preliminaryPayloadContextBlock = sliceBetween(
  panel,
  'const assertFormalPayloadContext = () => {',
  'interface FrontlineFormalSubmitContext'
)
assert.doesNotMatch(
  preliminaryPayloadContextBlock,
  /if\s*\(\s*!context\.workOrderId\s*\)/,
  'Preliminary production payload validation must rely on the selected active order state, not stale route query context.'
)

const formalSubmitContextBlock = sliceBetween(
  panel,
  'const assertFrontlineFormalSubmitContext = (formalContext: FrontlineFormalSubmitContext) => {',
  'const buildFrontlineFormalSubmitPayload = ('
)
assert.doesNotMatch(
  formalSubmitContextBlock,
  /\['workOrderId', '订单上下文'\]|\['taskId', '生产任务'\]|\['itemId', '产品物料'\]|\['recordbookId', '记录本'\]/,
  'Production formal submit must not require work order, task, item, or recordbook context.'
)
assert.match(
  formalSubmitContextBlock,
  /\['routeId', '路线'\][\s\S]*\['routeProcessId', '路线工序'\][\s\S]*\['processId', '工序'\]/,
  'Production formal submit must still fail fast on route and process identity.'
)

assert.match(
  backendSubmitVo,
  /processPoolSubmissionIdempotencyKey/,
  'Backend formal submit VO requires processPoolSubmissionIdempotencyKey.'
)
assert.match(
  feedbackApi,
  /processPoolSubmissionIdempotencyKey:\s*string/,
  'Frontend formal submit request type must expose backend-required processPoolSubmissionIdempotencyKey.'
)
assert.match(
  panel,
  /processPoolSubmissionIdempotencyKey:\s*submitIdempotencyKey/,
  'Formal submit payload must send the generated process-pool idempotency key.'
)
const idempotencyBuilderBlock = sliceBetween(
  panel,
  'const buildFrontlineProductionSubmitIdempotencyKey = (',
  'const buildFrontlineFormalSubmitPayload = ('
)
assert.match(
  idempotencyBuilderBlock,
  /productionSubmitDraftKey\.value/,
  'Production idempotency key must include the per-draft key instead of order context.'
)
assert.doesNotMatch(
  idempotencyBuilderBlock,
  /workOrderId|taskId|recordbookId|firstRouteQueryText/,
  'Production idempotency key must not be derived from work order, task, recordbook, or URL query context.'
)

console.log('PASS: AC-M10 SOP production static contract')
