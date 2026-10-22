import request from '@/config/axios'

export interface BatchRecordVersionMigrationItemVO {
  itemId: number | string
  diffGroup?: string
  diffType?: string
  riskLevel: string
  sourceLogicalKey?: string
  targetLogicalKey?: string
  matchConfidence?: number | string
  matchEvidenceJson?: string
  ruleType?: string
  businessOwnerType?: string
  confirmed: boolean
  confirmedBy?: number | string
  confirmedAt?: string
  confirmComment?: string
  message?: string
}

export interface BatchRecordVersionMigrationDiffVO {
  versionId: number | string
  items: BatchRecordVersionMigrationItemVO[]
  blockerCount: number
  confirmRequiredCount: number
  confirmedCount: number
  approvalReady: boolean
}

export interface BatchRecordVersionMigrationConfirmReqVO {
  itemIds: (number | string)[]
  comment: string
  idempotencyKey: string
}

export interface BatchRecordVersionMigrationConfirmRespVO {
  versionId: number | string
  confirmedItemIds: (number | string)[]
  confirmedBy: number | string
  confirmedAt: string
  confirmComment: string
  idempotencyKey: string
}

export const BatchRecordVersionGovernanceApi = {
  getMigrationDiff: async (versionId: number | string) =>
    await request.get<BatchRecordVersionMigrationDiffVO>({
      url: '/mes/pro/batch-record-version/governance/migration-diff',
      params: { versionId },
      ignoreErrorMessage: true
    }),
  confirmMigration: async (
    versionId: number | string,
    data: BatchRecordVersionMigrationConfirmReqVO
  ) =>
    await request.post<BatchRecordVersionMigrationConfirmRespVO>({
      url: '/mes/pro/batch-record-version/governance/migration-confirm',
      params: { versionId },
      data,
      ignoreErrorMessage: true
    })
}
