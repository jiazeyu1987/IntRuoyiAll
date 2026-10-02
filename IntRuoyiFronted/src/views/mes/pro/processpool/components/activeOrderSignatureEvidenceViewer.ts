import { reactive } from 'vue'
import type { ActiveOrderAuditIdentity, ActiveOrderAuditScope } from '@/api/mes/pro/edhr/activeOrderAudit'
import type { ActiveOrderSignatureEvidence } from '@/api/mes/pro/edhr/activeOrderSignature'

export interface SignatureBusinessContext {
  scope: ActiveOrderAuditScope
  identity: ActiveOrderAuditIdentity
  activeOrderId: number | string
}

export const createActiveOrderSignatureEvidenceViewer = (
  context: () => SignatureBusinessContext | undefined,
  load: (scope: ActiveOrderAuditScope, identity: ActiveOrderAuditIdentity, signatureId: number | string)
    => Promise<ActiveOrderSignatureEvidence>
) => {
  const state = reactive({ visible: false, loading: false, error: '',
    selected: undefined as ActiveOrderSignatureEvidence | undefined })
  let requestId = 0
  const key = (value: SignatureBusinessContext | undefined) => JSON.stringify(value)
  const close = () => {
    requestId += 1
    state.visible = false
    state.loading = false
    state.error = ''
    state.selected = undefined
  }
  const open = async (signatureId: number | string) => {
    const id = ++requestId
    const source = context()
    const sourceKey = key(source)
    const current = () => id === requestId && sourceKey === key(context())
    state.visible = true
    state.loading = true
    state.error = ''
    state.selected = undefined
    try {
      if (!source) throw new Error('当前详情缺少正式签名查询身份')
      const result = await load(source.scope, source.identity, signatureId)
      if (!current()) return
      if (!result || String(result.activeOrderId) !== String(source.activeOrderId) ||
          String(result.evidence?.id) !== String(signatureId) ||
          String(result.verification?.signatureId) !== String(signatureId)) {
        throw new Error('签名证据响应与当前业务记录不一致')
      }
      state.selected = result
    } catch (error) {
      if (!current()) return
      state.error = error instanceof Error ? error.message : String(error)
    } finally {
      if (current()) state.loading = false
    }
  }
  return { state, open, close }
}
