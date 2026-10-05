import { reactive } from 'vue'
import type { SignatureBusinessContext } from './activeOrderSignatureEvidenceViewer'
import type { ActiveOrderCorrectionTimeline } from '@/api/mes/pro/edhr/activeOrderCorrection'

const formalId = (id: unknown) =>
  (typeof id === 'string' || (typeof id === 'number' && Number.isSafeInteger(id))) &&
  /^[1-9]\d*$/.test(String(id)) && BigInt(String(id)) <= 9223372036854775807n

export const createActiveOrderCorrectionHistory = (
  context: () => SignatureBusinessContext | undefined,
  load: (source: SignatureBusinessContext) => Promise<ActiveOrderCorrectionTimeline>
) => {
  const state = reactive({ visible: false, loading: false, error: '', timeline: undefined as ActiveOrderCorrectionTimeline | undefined })
  let generation = 0
  const key = () => JSON.stringify(context())
  const close = () => { generation += 1; state.visible = false; state.loading = false; state.error = ''; state.timeline = undefined }
  const open = async () => {
    const currentGeneration = ++generation, source = context(), sourceKey = key()
    const current = () => generation === currentGeneration && sourceKey === key()
    state.visible = true; state.loading = true; state.error = ''; state.timeline = undefined
    try {
      if (!source || !formalId(source.activeOrderId)) throw new Error('当前详情缺少正式补正历史查询身份')
      const timeline = await load(source)
      if (!current()) return
      if (!timeline || !formalId(timeline.activeOrderId) || String(timeline.activeOrderId) !== String(source.activeOrderId) || !Array.isArray(timeline.corrections)) {
        throw new Error('补正历史响应与当前业务周期不一致')
      }
      const revisions = new Set<string>(), signatures = new Set<string>()
      for (const row of timeline.corrections) {
        if (!row || ![row.revisionId, row.eventId, row.signatureId].every(formalId) ||
            !['PRODUCTION_SUBMIT', 'PQC_INSPECTION'].includes(row.eventType) || row.verificationStatus !== 'VALID' ||
            !row.signerName?.trim() || !row.reason?.trim() || !row.processName?.trim() || !row.revisedAt || !row.signedAt ||
            !Array.isArray(row.changes) || !row.changes.length || row.changes.some(change =>
              !change || !change.fieldCode?.trim() || !change.fieldName?.trim() || change.beforeValue === change.afterValue) ||
            revisions.has(String(row.revisionId)) || signatures.has(String(row.signatureId))) {
          throw new Error('补正历史缺少唯一正式修订、真实差异或有效签名证据')
        }
        revisions.add(String(row.revisionId)); signatures.add(String(row.signatureId))
      }
      state.timeline = timeline
    } catch (error) { if (current()) state.error = error instanceof Error ? error.message : String(error) }
    finally { if (current()) state.loading = false }
  }
  return { state, open, close }
}
