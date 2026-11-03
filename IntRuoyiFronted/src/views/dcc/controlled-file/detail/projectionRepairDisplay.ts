import type { LifecycleProjectionRepairPreview } from '@/api/dcc/controlledFile/lifecycleProjectionRepair'
import { DCC_CONTROLLED_FILE_STATUS_DEFINITIONS } from '../shared/lifecycle'
export const projectionStatusLabel = (value?: string | null) =>
  value
    ? DCC_CONTROLLED_FILE_STATUS_DEFINITIONS.find((item) => item.value === value)?.label ||
      `未识别状态（${value}）`
    : '未记录'
export const projectionActionLabel = (value: string) => {
  const labels: Record<string, string> = {
    COMPLETE_CONTROL: '完成受控',
    ACTIVATE_CONTROLLED: '启用已受控版本'
  }
  return labels[value] || `未识别动作（${value}）`
}
export const projectionTechnicalFields = (preview: LifecycleProjectionRepairPreview) =>
  [
    ['文件编号', preview.fileId],
    ['文件主记录编号', preview.masterId],
    ['共享版本投影编号', preview.versionRefId],
    ['租户编号', preview.tenantId],
    ['审批流程实例编号', preview.processInstanceId],
    ['电子签名记录编号', preview.signatureIds.join('、')],
    ['发布文件编号', preview.publishedFileId],
    ['盖章文件编号', preview.stampedFileId],
    ['最新受控文件编号', preview.latestControlledFileId],
    ['当前执行文件编号', preview.currentActiveControlledFileId],
    ['文件原始状态码', preview.dccStatus],
    ['共享生命周期原始状态码', preview.canonicalStatus],
    ['共享业务原始状态码', preview.domainStatus],
    ['目标状态码', preview.expectedTargetStatus],
    ['正式动作码', preview.expectedActions.join('、')],
    ['原事实校验码', preview.sourceFactsHash],
    ['原投影预像校验码', preview.preimageHash]
  ].map(([label, value]) => ({ label: label!, value: value || '未记录' }))
