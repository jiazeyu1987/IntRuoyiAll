import request from '@/config/axios'
import type { EdhrRouteId } from './batchExecution'

export interface EdhrAttachmentPrepareUploadReqVO {
  executionId: EdhrRouteId
  workTaskId: EdhrRouteId
  file: File | Blob
}

export interface EdhrAttachmentPrepareUploadRespVO {
  uploadToken: string
  fileId: number
  fileUrl: string
  storageConfigId: number
  storagePath: string
  fileName: string
  contentType: string
  fileSize: number
  sha256: string
  storageRetentionJson: string
  storageRetentionHash: string
}

interface EdhrAttachmentPrepareUploadApiResp {
  data: EdhrAttachmentPrepareUploadRespVO
}

export const prepareEdhrAttachmentUpload = async (
  data: EdhrAttachmentPrepareUploadReqVO,
  onUploadProgress?: Function
) => {
  const formData = new FormData()
  formData.append('executionId', String(data.executionId))
  formData.append('workTaskId', String(data.workTaskId))
  formData.append('file', data.file)
  const response = await request.upload<EdhrAttachmentPrepareUploadApiResp>({
    url: '/mes/pro/batch-record-execution/attachment/prepare-upload',
    data: formData,
    onUploadProgress
  })
  if (!response.data) {
    throw new Error('eDHR 附件预登记响应缺少 data，不能进入附件审计链。')
  }
  return response.data
}
