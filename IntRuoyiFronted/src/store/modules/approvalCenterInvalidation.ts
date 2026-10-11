import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { getAccessToken, getTenantId, getVisitTenantId } from '@/utils/auth'
import { useUserStore } from './user'
import { createApprovalInvalidationState, type ApprovalSession, type ApprovalView, type EdhrSuccessIdentity, type EdhrSuccessEvent } from './approvalCenterInvalidationState'

export const useApprovalCenterInvalidationStore = defineStore('approvalCenterInvalidation', () => {
  const user = useUserStore()
  const state = createApprovalInvalidationState()
  const contextKey = ref('')
  const change = ref(0)
  let epoch = 0
  let lastIdentity = ''
  const syncSession = () => {
    const loginUserId = getAccessToken() && user.getUser.id ? String(user.getUser.id) : ''
    const tenantId = String(getVisitTenantId() || getTenantId() || '')
    const identity = JSON.stringify([tenantId, loginUserId])
    if (identity !== lastIdentity) {
      lastIdentity = identity
      contextKey.value = `${++epoch}:${identity}`
      state.setSession(contextKey.value)
      change.value++
    }
    return Object.freeze({ key: contextKey.value, tenantId, loginUserId })
  }
  watch(() => user.getUser.id, syncSession, { flush: 'sync' })
  syncSession()
  const captureEdhrSuccess = (identity: EdhrSuccessIdentity) => state.capture(syncSession(), identity)
  const captureHandoffSourceSuccess = (sourceType: string, sourceId: string | number) =>
    captureEdhrSuccess({ sourceTaskType: 'ACTIVE_ORDER_HANDOFF', businessSourceType: sourceType, businessSourceId: String(sourceId) })
  const captureHandoffReceiptSuccess = (session: ApprovalSession, sourceType: string, sourceId: string | number) =>
    state.capture(session, { sourceTaskType: 'ACTIVE_ORDER_HANDOFF', businessSourceType: sourceType, businessSourceId: String(sourceId) })
  const isCurrent = (sessionKey: string) => syncSession().key === sessionKey
  const publishEdhrSuccess = (event: EdhrSuccessEvent) => {
    syncSession()
    if (state.publish(event)) change.value++
  }
  const revision = (moduleCode: string | undefined, view: ApprovalView) => {
    syncSession()
    void change.value
    return state.revision(moduleCode, view)
  }
  const todoRevision = computed(() => revision(undefined, 'TODO'))
  return { contextKey, todoRevision, syncSession, isCurrent, captureEdhrSuccess, captureHandoffSourceSuccess, captureHandoffReceiptSuccess, publishEdhrSuccess, revision }
})
