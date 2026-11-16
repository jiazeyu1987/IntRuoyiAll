import request from '@/config/axios'

export type GxpAuditMode = 'FORMAL' | 'LOCAL_POLICY' | 'DEVELOPMENT_DISABLED'

export interface GxpAuditRuntimeMode {
  auditEnabled: boolean
  mode: GxpAuditMode
  serverStartedAtUtc: string
}

export const getGxpAuditRuntimeMode = (): Promise<GxpAuditRuntimeMode> =>
  request.get({ url: '/system/gxp-audit-event/runtime-mode' })
