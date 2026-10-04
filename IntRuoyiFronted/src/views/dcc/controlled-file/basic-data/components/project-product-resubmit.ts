import type { DccProjectProductCreateReqVO, DccProjectProductCreateRespVO } from '@/api/dcc/controlledFile/projectProductRequests'
import { validateAttributes } from '../../project-attributes/state'

/** 显示门禁使用正式申请人ID，服务端再次检查，无角色/姓名/admin旁路。 */
export const canResubmitProjectProductRequest = (
  row: DccProjectProductCreateRespVO, applicantUserId: number | string | undefined
): boolean => Boolean(applicantUserId && row.applicantUserId
  && String(applicantUserId) === String(row.applicantUserId)
  && row.status === 'REJECTED' && !row.resubmittedRequestId)

export const restoreRejectedProjectProductForm = (
  row: DccProjectProductCreateRespVO, applicantUserId: number | string | undefined
): DccProjectProductCreateReqVO => {
  if (!canResubmitProjectProductRequest(row, applicantUserId)) throw new Error('仅原申请人可从未重提的驳回申请修改后重新提交')
  if (!row.defaultAttributesJson) throw new Error('原申请属性历史未记录，请明确补齐后重新申请')
  const defaultAttributes = validateAttributes(JSON.parse(row.defaultAttributesJson))
  return JSON.parse(JSON.stringify({
    projectName: row.projectName, projectCode: row.projectCode,
    projectLeaderUserId: row.projectLeaderUserId, folderTemplateId: row.folderTemplateId,
    productCode: row.productCode, productName: row.productName, classification: row.classification,
    remark: row.remark, defaultAttributes
  }))
}
