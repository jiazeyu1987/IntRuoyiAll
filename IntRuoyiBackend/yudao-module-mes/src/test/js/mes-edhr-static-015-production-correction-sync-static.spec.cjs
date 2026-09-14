const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const backend = path.resolve(__dirname, '../../../../')
const mes = path.join(backend, 'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes')

const read = (base, file) => fs.readFileSync(path.join(base, file), 'utf8')
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

const service = read(mes, 'service/pro/processpool/MesProcessPoolProductionReportCorrectionService.java')
const correct = sliceMethod(service, 'public Long correct(MesProcessPoolProductionReportCorrectionCommand command)')

assert(service.includes('private static final String FEEDBACK_SOURCE_TYPE = "MES_PRO_FEEDBACK"'),
  'EDHR-STATIC-015: production correction must only synchronize formal MES_PRO_FEEDBACK sources')
assert(service.includes('MesProFeedbackMapper') && service.includes('MesProFeedbackMaterialMapper'),
  'EDHR-STATIC-015: production correction service must own formal feedback and material fact mappers')

const revisionAt = correct.indexOf('revisionService.updateProductionReportRecord')
const syncAt = correct.indexOf('syncFormalFeedbackSource(event, afterPayload)')
assert(revisionAt !== -1 && syncAt > revisionAt,
  'EDHR-STATIC-015: formal feedback sync must run after the signed effective revision is accepted')

const syncFeedback = sliceMethod(service, 'private void syncFormalFeedbackSource(')
assert(syncFeedback.includes('FEEDBACK_SOURCE_TYPE.equals(event.getFeedbackSourceType())')
  && syncFeedback.includes('event.getFeedbackSourceId()'),
  'EDHR-STATIC-015: correction must fail fast when the event lacks a formal feedback source')
assert(syncFeedback.includes('feedbackMapper.selectListByIdsForUpdate(List.of(event.getFeedbackSourceId()))')
  && syncFeedback.includes('formalFeedback.size() != 1'),
  'EDHR-STATIC-015: correction must lock and uniquely resolve the formal feedback row')
assert(syncFeedback.includes('BigDecimal feedbackQuantity = outputQuantity.add(lossQuantity)')
  && syncFeedback.includes('BigDecimal qualifiedQuantity = outputQuantity')
  && syncFeedback.includes('feedbackMapper.updateCorrectedProductionReport('),
  'EDHR-STATIC-015: formal feedback totals must use the corrected output and loss quantities')
assert(syncFeedback.includes('firstLossReason(afterPayload)')
  && syncFeedback.includes('reason == null ? null : reason.reasonId()')
  && syncFeedback.includes('reason == null ? null : reason.reasonCode()')
  && syncFeedback.includes('reason == null ? null : reason.reasonName()'),
  'EDHR-STATIC-015: formal feedback reason snapshot must be rebuilt from corrected structured loss details')
assert(syncFeedback.includes('feedbackMapper.updateCorrectedProductionReport(')
  && syncFeedback.includes('formalFeedback.update'),
  'EDHR-STATIC-015: correction must fail fast when the formal feedback update is not persisted')

const syncMaterials = sliceMethod(service, 'private void syncFormalMaterialFacts(')
assert(syncMaterials.includes('feedbackMaterialMapper.selectListByFeedbackIdForUpdate(feedbackId)'),
  'EDHR-STATIC-015: material facts must be locked by formal feedback id before correction')
assert(syncMaterials.includes('materialById.remove(materialId)')
  && syncMaterials.includes('missingFormalMaterialIds')
  && syncMaterials.includes('feedbackMaterialMapper.updateCorrectedMaterialFact('),
  'EDHR-STATIC-015: every corrected material fact must update its formal row or fail fast')
assert(syncMaterials.includes('materialLossDetailsJson(material, lossQuantity)')
  && syncMaterials.includes('jsonStringOrNull(material.get("selectedDevice"))')
  && syncMaterials.includes('jsonStringOrEmptyArray(material.get("deviceParameterReadings"))'),
  'EDHR-STATIC-015: formal material rows must carry corrected loss/device JSON snapshots')

const feedbackMapper = read(mes, 'dal/mysql/pro/feedback/MesProFeedbackMapper.java')
assert(feedbackMapper.includes('updateCorrectedProductionReport')
  && feedbackMapper.includes('new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MesProFeedbackDO>()')
  && feedbackMapper.includes('MesProFeedbackDO::getFeedbackQuantity')
  && feedbackMapper.includes('MesProFeedbackDO::getQualifiedQuantity')
  && feedbackMapper.includes('MesProFeedbackDO::getUnqualifiedQuantity')
  && feedbackMapper.includes('MesProFeedbackDO::getLossReasonId')
  && feedbackMapper.includes('MesProFeedbackDO::getLossReasonCodeSnapshot')
  && feedbackMapper.includes('MesProFeedbackDO::getLossReasonNameSnapshot'),
  'EDHR-STATIC-015: feedback mapper must explicitly set corrected totals and nullable reason snapshots')

const materialMapper = read(mes, 'dal/mysql/pro/feedback/MesProFeedbackMaterialMapper.java')
assert(materialMapper.includes('selectListByFeedbackIdForUpdate')
  && materialMapper.includes('MesProFeedbackMaterialDO::getFeedbackId')
  && materialMapper.includes('FOR UPDATE'),
  'EDHR-STATIC-015: feedback material mapper must expose a FOR UPDATE read by feedback id')
assert(materialMapper.includes('updateCorrectedMaterialFact')
  && materialMapper.includes('new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MesProFeedbackMaterialDO>()')
  && materialMapper.includes('MesProFeedbackMaterialDO::getLossDetailsJson')
  && materialMapper.includes('MesProFeedbackMaterialDO::getSelectedDeviceJson')
  && materialMapper.includes('MesProFeedbackMaterialDO::getDeviceParameterReadingsJson'),
  'EDHR-STATIC-015: feedback material mapper must explicitly set corrected material JSON snapshots')

console.log('PASS: EDHR-STATIC-015 production correction formal feedback sync contract')
