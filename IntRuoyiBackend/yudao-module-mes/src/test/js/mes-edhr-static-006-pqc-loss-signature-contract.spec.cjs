const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const backend = path.resolve(__dirname, '../../../../')
const repo = path.resolve(backend, '..')
const mes = path.join(backend, 'yudao-module-mes/src/main')
const front = path.join(repo, 'IntRuoyiFronted/src')

const read = (base, file) => fs.readFileSync(path.join(base, file), 'utf8')

const sliceBetween = (source, start, end) => {
  const startAt = source.indexOf(start)
  assert.notEqual(startAt, -1, `missing start anchor: ${start}`)
  const endAt = source.indexOf(end, startAt + start.length)
  assert.notEqual(endAt, -1, `missing end anchor: ${end}`)
  return source.slice(startAt, endAt)
}

const sliceMethod = (source, signature) => {
  const startAt = source.indexOf(signature)
  assert.notEqual(startAt, -1, `missing method anchor: ${signature}`)
  const bodyStart = source.indexOf('{', startAt)
  assert.notEqual(bodyStart, -1, `missing method body: ${signature}`)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    const char = source[i]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(startAt, i + 1)
      }
    }
  }
  assert.fail(`unterminated method body: ${signature}`)
}

const detailModel = read(mes, 'java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java')
assert(detailModel.includes('private List<SignatureDetail> productionSubmitterSignatures = List.of();'),
  'EDHR-STATIC-006: backend detail model must carry row-owned production submitter signatures')

const detailVo = read(mes, 'java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java')
assert(detailVo.includes('private List<SignatureDetail> productionSubmitterSignatures;'),
  'EDHR-STATIC-006: response VO must expose row-owned production submitter signatures')

const mapperXml = read(mes, 'resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml')
assert(mapperXml.includes("$.productionSubmitEventId") && mapperXml.includes('END AS productionEventId'),
  'EDHR-STATIC-006: detail SQL must read each PQC event source production event id')

const detailService = read(mes, 'java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java')
const attachPqcSubmissions = sliceMethod(detailService, 'private void attachPqcSubmissions(')
assert(attachPqcSubmissions.includes('entry.getValue().toDetail(')
  && attachPqcSubmissions.includes('accumulator.productionSubmitterSignaturesByEventId()'),
  'EDHR-STATIC-006: PQC detail rows must be enriched from the matched process production event signatures')
const pqcToDetail = sliceMethod(detailService, 'private MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail toDetail(')
assert(pqcToDetail.includes('productionEventIds.stream()')
  && pqcToDetail.includes('productionSignaturesByEventId::get')
  && pqcToDetail.includes('setProductionSubmitterSignatures(productionSignatures)'),
  'EDHR-STATIC-006: source signatures must be resolved by production event id, not by latest process signature')
const productionSignatureIndex = sliceMethod(detailService, 'private Map<Long, MesTeamLeaderActiveOrderDetail.SignatureDetail> productionSubmitterSignaturesByEventId()')
assert(productionSignatureIndex.includes('submission.getEventId()')
  && productionSignatureIndex.includes('submission.getSubmitterSignature()'),
  'EDHR-STATIC-006: production signature index must use actual production submission ids and submitter signatures')

const controller = read(mes, 'java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java')
assert(controller.includes('.setProductionSubmitterSignatures(submission.getProductionSubmitterSignatures().stream()'),
  'EDHR-STATIC-006: controller must map row-owned source production signatures to the frontend VO')

const api = read(front, 'api/mes/pro/processpool/teamLeader.ts')
assert(api.includes('productionSubmitterSignatures?: TeamLeaderActiveOrderSignatureDetailRespVO[]'),
  'EDHR-STATIC-006: frontend API type must expose row-owned source production signatures')

const panel = read(front, 'views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue')
assert(panel.includes('const resolveLossProductionSignatures = ('),
  'EDHR-STATIC-006: loss report must resolve production signatures as a row-scoped collection')
const lossRows = sliceBetween(
  panel,
  'const pqcLossReportRows = computed<ActiveOrderPqcLossReportRow[]>(() => {',
  'const pqcLossReportApprovalText = computed(() => {'
)
assert(lossRows.includes('resolveLossProductionSignatures(submission)')
  && lossRows.includes('formatLossReportSignaturesDateText(productionSignatures)'),
  'EDHR-STATIC-006: each loss row must format signatures from its own PQC submission source')
assert(!lossRows.includes('resolveLatestProductionSubmitterSignature'),
  'EDHR-STATIC-006: loss rows must not reuse the latest production signature for the whole process')

console.log('PASS: EDHR-STATIC-006 row-scoped PQC loss production signature contract')
