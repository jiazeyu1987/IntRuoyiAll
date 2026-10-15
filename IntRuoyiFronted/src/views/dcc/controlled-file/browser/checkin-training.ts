interface CheckinTrainingSource {
  changeType?: string | null
  status: string
  versionNo?: string | null
  controlledTime?: string | number | null
  needTraining?: boolean | null
}

export const requireCheckinTrainingDefault = (source: CheckinTrainingSource): boolean => {
  if (source.changeType !== 'NEW' && source.changeType !== 'REVISION') {
    throw new Error('所选版本未记录正式申请类型，无法准备检入培训选择')
  }
  if (source.status === 'ACTIVE' || source.status === 'CONTROLLED_PENDING_EFFECTIVE') {
    if (!source.controlledTime || !source.versionNo || source.versionNo.includes('-')) {
      throw new Error('所选受控版本的控制或正式版本事实不完整')
    }
    if (source.changeType === 'NEW') return false
  } else if (!['WORKING', 'REJECTED', 'WITHDRAWN', 'PENDING_APPLICANT_REWORK'].includes(source.status)) {
    throw new Error('所选版本状态不允许准备检入')
  }
  if (typeof source.needTraining !== 'boolean') {
    throw new Error('所选申请未记录培训选择，请核对原工作稿事实')
  }
  return source.needTraining
}
