import request from '@/config/axios'

export interface ApprovalPrintTemplateSaveReqVO {
  templateFileId?: number
  templateFileUrl?: string
  remark?: string
}

export interface ApprovalPrintTemplateVO {
  id: number
  templateFileId: number
  templateFileName: string
  templateFileContentType?: string
  active: boolean
  remark?: string
  requiredPlaceholders: string[]
  supportedPlaceholders: string[]
  updateTime?: number
}

export interface ControlledFileApprovalPrintHtmlVO {
  templateId: number
  templateFileName: string
  html: string
  requiredPlaceholders: string[]
}

export const getActiveApprovalPrintTemplate =
  async (): Promise<ApprovalPrintTemplateVO | null> => {
    return await request.get({ url: '/dcc/approval-print-template/active' })
  }

export const saveActiveApprovalPrintTemplate = async (
  data: ApprovalPrintTemplateSaveReqVO
): Promise<ApprovalPrintTemplateVO> => {
  return await request.post({ url: '/dcc/approval-print-template/save', data })
}

export const getControlledFileApprovalPrintHtml = async (
  id: number | string
): Promise<ControlledFileApprovalPrintHtmlVO> => {
  return await request.get({ url: `/dcc/controlled-files/${id}/approval-print/print-html` })
}

export const exportControlledFileApprovalWord = async (
  id: number | string
): Promise<Blob> => {
  return await request.downloadOriginal({
    url: `/dcc/controlled-files/${id}/approval-print/export-word`,
    responseType: 'blob'
  })
}
