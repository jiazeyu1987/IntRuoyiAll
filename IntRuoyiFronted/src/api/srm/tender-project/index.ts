import request from '@/config/axios'

export interface SrmTenderProjectLineVO {
  id: number
  sourcePlanLineId?: number
  lineNo?: string
  materialId: number
  materialCode: string
  materialName: string
  quantity: number
  unit: string
}

export interface SrmTenderNoticeVO {
  id?: number
  noticeTitle: string
  noticeAttachmentUrl: string
  publishedTime?: string
}

export interface SrmTenderDocumentVO {
  id?: number
  documentName: string
  documentAttachmentUrl: string
}

export interface SrmTenderSubmissionVO {
  id: number
  supplierId: number
  supplierName: string
  bidAmount: number
  submissionStatus: string
  attachmentUrl?: string
  submittedName?: string
  submittedTime?: string
}

export interface SrmTenderCommitteeMemberVO {
  id: number
  applicationId: number
  expertId: number
  expertName: string
  specialtyType: string
}

export interface SrmTenderCandidateVO {
  id: number
  submissionId: number
  supplierId: number
  supplierName: string
  bidAmount: number
  rankNo: number
  candidateStatus: string
}

export interface SrmTenderWinningResultVO {
  id?: number
  candidateId: number
  supplierId: number
  supplierName: string
  winningAmount: number
  winningRemark: string
  confirmedName?: string
  confirmedTime?: string
}

export interface SrmTenderProjectVO {
  id: number
  projectNo: string
  projectTitle: string
  projectType: string
  projectTypeLabel?: string
  projectStatus: string
  projectStatusLabel?: string
  sourcePlanId: number
  sourcePlanNo: string
  expectedAmount: number
  submissionStartTime?: string
  submissionEndTime?: string
  dealSupplierId?: number
  dealSupplierName?: string
  dealAmount?: number
  contractId?: number
  createTime?: string
  notice?: SrmTenderNoticeVO
  document?: SrmTenderDocumentVO
  lines: SrmTenderProjectLineVO[]
  submissions: SrmTenderSubmissionVO[]
  committeeMembers: SrmTenderCommitteeMemberVO[]
  candidates: SrmTenderCandidateVO[]
  winningResult?: SrmTenderWinningResultVO
}

export interface SrmTenderProjectPageReqVO extends PageParam {
  projectNo?: string
  projectTitle?: string
  projectStatus?: string
  supplierId?: number
}

export interface SrmTenderPublishReqVO {
  projectId: number
  noticeTitle: string
  noticeAttachmentUrl: string
  documentName: string
  documentAttachmentUrl: string
  submissionStartTime: number
  submissionEndTime: number
}

export interface SrmTenderSubmissionReqVO {
  projectId: number
  supplierId: number
  bidAmount: number
  attachmentUrl?: string
}

export interface SrmTenderExpertSaveReqVO {
  expertName: string
  specialtyType: string
}

export interface SrmTenderExpertAuditReqVO {
  id: number
  auditRemark?: string
}

export interface SrmTenderCommitteeReqVO {
  projectId: number
  applicationMethod: string
  requiredSpecialtyType: string
  requiredExpertCount: number
  expertIds: number[]
}

export interface SrmTenderCandidateReqVO {
  projectId: number
  submissionIds: number[]
}

export interface SrmTenderWinningReqVO {
  projectId: number
  candidateId: number
  winningRemark: string
}

export const srmTenderProjectStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '评委会已确认', value: 'COMMITTEE_CONFIRMED' },
  { label: '候选已确认', value: 'CANDIDATE_CONFIRMED' },
  { label: '中标已确认', value: 'WINNING_CONFIRMED' },
  { label: '已建合同', value: 'CONTRACT_CREATED' }
] as const

export const srmTenderApplicationMethodOptions = [
  { label: '指定', value: 'DIRECT' },
  { label: '抽取', value: 'DRAW' }
] as const

export const SrmTenderProjectApi = {
  getProjectPage: async (params: SrmTenderProjectPageReqVO) => {
    return await request.get<PageResult<SrmTenderProjectVO[]>>({
      url: '/srm/tender-project/page',
      params
    })
  },

  getProject: async (id: number) => {
    return await request.get<SrmTenderProjectVO>({
      url: '/srm/tender-project/get',
      params: { id }
    })
  },

  publishProject: async (data: SrmTenderPublishReqVO) => {
    return await request.post<SrmTenderProjectVO>({
      url: '/srm/tender-project/publish',
      data
    })
  },

  submitBid: async (data: SrmTenderSubmissionReqVO) => {
    return await request.post<SrmTenderProjectVO>({
      url: '/srm/tender-project/submit-bid',
      data
    })
  },

  createExpert: async (data: SrmTenderExpertSaveReqVO) => {
    return await request.post<number>({
      url: '/srm/tender-project/expert/create',
      data
    })
  },

  approveExpert: async (data: SrmTenderExpertAuditReqVO) => {
    return await request.put<boolean>({
      url: '/srm/tender-project/expert/approve',
      data
    })
  },

  formCommittee: async (data: SrmTenderCommitteeReqVO) => {
    return await request.post<SrmTenderProjectVO>({
      url: '/srm/tender-project/committee',
      data
    })
  },

  createCandidates: async (data: SrmTenderCandidateReqVO) => {
    return await request.post<SrmTenderProjectVO>({
      url: '/srm/tender-project/candidate',
      data
    })
  },

  confirmWinning: async (data: SrmTenderWinningReqVO) => {
    return await request.post<SrmTenderProjectVO>({
      url: '/srm/tender-project/winning',
      data
    })
  }
}
