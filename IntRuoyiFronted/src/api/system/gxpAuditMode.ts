import request from '@/config/axios'

export interface GxpAuditRuntimeMode {
  auditEnabled: boolean
  mode: 'FORMAL' | 'DEVELOPMENT_DISABLED'
  serverStartedAtUtc: string
}

export const getGxpAuditRuntimeMode = (): Promise<GxpAuditRuntimeMode> =>
  request.get({ url: '/system/gxp-audit-event/runtime-mode' })
