import request from '@/config/axios'

export interface ReleaseTaskNotificationReceipt {
  id: string
  workTaskId: string
  userId: string
  status: 'PENDING' | 'FAILED' | 'SENT'
  rowVersion: number
  attemptCount: number
  lastAttemptAt: string | null
  sentAt: string | null
  systemMessageId: string | null
  lastErrorSummary: string | null
}

export const listReleaseTaskNotifications = (workTaskId: string) =>
  request.get<ReleaseTaskNotificationReceipt[]>({
    url: '/mes/pro/production-release-task-notification/list',
    params: { workTaskId }
  })

export const retryReleaseTaskNotification = (data: {
  deliveryId: string
  expectedVersion: number
  reason: string
}) => request.post<boolean>({
  url: '/mes/pro/production-release-task-notification/retry',
  data
})
