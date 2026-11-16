import request from '@/config/axios'

export interface GxpAuditEvent {
  id: number
  operationId: string
  subjectType: string
  subjectId: string
  reason: string
  actorId: number
  actorUsername: string
  actorDisplayName: string
  serverOccurredAt: string
  beforeStateJson: string
  afterStateJson: string
  resultStatus: string
  requestId: string
  idempotencyKey: string
  eventHash: string
  integrityStatus: string
}

export const getGxpAuditEventPage = (params: PageParam & {
  operationId: string
  subjectId?: string
}) => request.get<{ list: GxpAuditEvent[]; total: number }>({
  url: '/system/gxp-audit-event/page',
  params
})

export const getGxpAuditEvent = (id: number) => request.get<GxpAuditEvent | null>({
  url: '/system/gxp-audit-event/get',
  params: { id }
})
