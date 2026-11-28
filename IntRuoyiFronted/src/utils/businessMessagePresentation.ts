export interface BusinessMessagePresentation {
  title: string
  body: string
  fields: { label: string; value: string }[]
}

export const HANDOFF_TASK_LABELS: Record<string, string> = {
  PRODUCTION_HANDOFF: '生产接手',
  PRODUCTION_REVIEW: '生产组长复核',
  PQC_HANDOFF: 'PQC检验接手',
  PQC_REVIEW: 'PQC组长复核',
  PRODUCTION_RETURN: '生产记录退回更正',
  PQC_RETURN: 'PQC记录退回更正',
  QA_REVIEW: 'QA不合格评审',
  NCR_FREEZE_NOTICE: '工单/批次冻结通知',
  NCR_DISPOSITION_NOTICE: 'QA处置结果通知',
  QA_DECISION_HANDOFF: 'QA处置结果交接'
}

// 仅拆分后端正式交接描述的固定尾段，不改写自由填写的业务意见。
export const splitHandoffReason = (reason: string): Pick<BusinessMessagePresentation, 'body' | 'fields'> => {
  const match = reason.match(/^([\s\S]*?)\s*【工单：([^；\r\n]+)；工序：([^；\r\n]+)；发起人：([^；\r\n]+)（SYSTEM_USER）；状态：([^】]+)】\s*$/)
  if (!match) return { body: reason, fields: [] }
  return {
    body: match[1].trim(),
    fields: [
      { label: '工单', value: match[2] },
      { label: '工序', value: match[3] },
      { label: '发起人', value: match[4] },
      { label: '交接时状态', value: match[5] }
    ]
  }
}

export const describeHandoffMessage = (taskType: string, reason: string): BusinessMessagePresentation => {
  const { body, fields } = splitHandoffReason(reason)
  const freeze = taskType === 'NCR_FREEZE_NOTICE'
    ? body.match(/^NCR (\S+) 已冻结工单\/批次，暂停受影响生产、检验和放行，等待QA正式处置：([\s\S]*)$/)
    : null
  return {
    title: HANDOFF_TASK_LABELS[taskType] ?? `未识别的交接类型（${taskType}）`,
    body: freeze
      ? `已暂停相关生产、检验和放行，等待QA正式处置。\n冻结原因：${freeze[2]}`
      : (taskType === 'PQC_REVIEW' && body === '本人PQC检验提交已完成，请组长签名复核'
        ? 'PQC检验已提交，请组长签名复核。' : body),
    fields: freeze ? [{ label: '不合格记录', value: freeze[1] }, ...fields] : fields
  }
}

const workTaskTitles: Record<string, string> = {
  MES_EDHR_FILL_TASK_ASSIGNED: '请填写批记录',
  MES_EDHR_REVIEW_TASK_ASSIGNED: '请复核批记录',
  MES_EDHR_APPROVE_TASK_ASSIGNED: '请审批批记录',
  MES_EDHR_REWORK_TASK_ASSIGNED: '请更正退回的批记录',
  MES_EDHR_ARCHIVE_TASK_ASSIGNED: '请归档批记录',
  MES_EDHR_RELEASE_APPROVE_TASK_ASSIGNED: '请完成上市放行审批',
  MES_EDHR_PQC_PRODUCTION_RELEASE_TASK_ASSIGNED: '请完成PQC生产放行',
  MES_EDHR_DEVIATION_RECEIVE_ASSIGNED: '请接收偏差处理任务',
  MES_EDHR_DEVIATION_CORRECT_ASSIGNED: '请完成偏差整改',
  MES_EDHR_DEVIATION_REVIEW_ASSIGNED: '请办理偏差责任任务',
  MES_EDHR_DEVIATION_WORK_TASK_OVERDUE: '偏差任务已逾期，请及时处理',
  MES_EDHR_RECORD_CHANGE_REWORK: '请完成记录变更整改',
  MES_EDHR_FILL_TASK_REASSIGNED: '批记录填写任务已转交给您',
  MES_EDHR_WORK_TASK_OVERDUE: '批记录任务已超期，请及时处理'
}

// 来源：注册证正式提醒阈值（月）与两种站内信模板；不替换用户自由文本。
const registrationThresholdLabels: Record<string, string> = {
  T_30: '到期前30个月', T_8: '到期前8个月', T_2: '到期前2个月', T_1: '到期前1个月',
  CE_T_18: 'CE到期前18个月', CE_T_12: 'CE到期前12个月', CE_T_3: 'CE到期前3个月'
}
export const registrationThresholdLabel = (value: unknown): string =>
  registrationThresholdLabels[String(value)] || '提醒级别未识别，请联系管理员'

export const registrationDeadlineLabel = (value: unknown): string => {
  // LocalDateTime没有时区；只格式化固定日期结构，不做浏览器时区换算。
  const text = String(value ?? '')
  const match = text.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2})(?:\.\d{1,9})?$/)
  return match ? `${match[1]} ${match[2]}` : text || '未提供办理期限'
}

const describeRegistrationMessage = (message: {
  templateCode?: string; templateContent?: string; templateParams?: Record<string, unknown> | null
}): BusinessMessagePresentation | undefined => {
  const params = message.templateParams
  if (message.templateCode === 'DCC_REGISTRATION_CERTIFICATE_THRESHOLD_REMINDER') {
    if (!params || ['certificateId', 'thresholdLevel', 'dueDate', 'businessDate'].some(key => params[key] == null)) {
      return { title: '注册证到期提醒信息不完整，请联系管理员', body: '', fields: [] }
    }
    const original = `注册证${params.certificateId}已进入${params.thresholdLevel}提醒窗口，到期日：${params.dueDate}，业务日期：${params.businessDate}。请及时处理。`
    if (message.templateContent !== original) return undefined
    return { title: '注册证到期提醒', body: '请及时处理相关注册证。', fields: [
      { label: '注册证记录', value: String(params.certificateId) },
      { label: '提醒窗口', value: registrationThresholdLabel(params.thresholdLevel) },
      { label: '到期日', value: String(params.dueDate) }, { label: '业务日期', value: String(params.businessDate) }
    ] }
  }
  if (message.templateCode === 'DCC_REGISTRATION_HANDLING_TASK') {
    if (!params || typeof params.title !== 'string' || params.dueAt == null) {
      return { title: '注册证办理任务信息不完整，请联系管理员', body: '', fields: [] }
    }
    const original = `请办理《${params.title}》，办理期限：${params.dueAt}。请在消息详情查看任务并上传证件或授权办理。`
    if (message.templateContent !== original) return undefined
    return { title: `请办理《${params.title}》`, body: '请在消息详情查看任务并上传证件或授权办理。', fields: [
      { label: '办理期限', value: registrationDeadlineLabel(params.dueAt) }
    ] }
  }
}

export const describeNotifyMessage = (message: {
  templateCode?: string
  templateContent?: string
  templateParams?: Record<string, unknown> | null
}): BusinessMessagePresentation => {
  const registrationMessage = describeRegistrationMessage(message)
  if (registrationMessage) return registrationMessage
  const workTaskTitle = message.templateCode && workTaskTitles[message.templateCode]
  if (workTaskTitle) {
    const params = message.templateParams
    const isDeviationTask = message.templateCode === 'MES_EDHR_DEVIATION_RECEIVE_ASSIGNED'
      || message.templateCode === 'MES_EDHR_DEVIATION_CORRECT_ASSIGNED'
      || message.templateCode === 'MES_EDHR_DEVIATION_REVIEW_ASSIGNED'
      || message.templateCode === 'MES_EDHR_DEVIATION_WORK_TASK_OVERDUE'
    if (isDeviationTask) {
      if (typeof params?.presentationError === 'string' && params.presentationError.trim()) {
        return { title: params.presentationError, body: '', fields: [] }
      }
      if (!params || typeof params.taskTitle !== 'string' || !params.taskTitle.trim()
        || typeof params.workOrderCode !== 'string' || !params.workOrderCode.trim()) {
        return { title: '偏差任务通知信息不完整，请联系管理员', body: '', fields: [] }
      }
      const taskTitle = params.taskTitle.trim()
      const workOrderCode = params.workOrderCode.trim()
      const batchCode = typeof params.batchCode === 'string' && params.batchCode.trim()
        ? params.batchCode.trim() : '未关联批次（按生产订单办理）'
      const fields = [{ label: '生产订单', value: workOrderCode }, { label: '批次', value: batchCode }]
      if (typeof params.processName === 'string' && params.processName.trim()) fields.push({ label: '工序', value: params.processName.trim() })
      if (typeof params.dueTime === 'string' && params.dueTime.trim()) fields.push({ label: '截止时间', value: params.dueTime.trim() })
      const isDeviationAssignment = message.templateCode === 'MES_EDHR_DEVIATION_RECEIVE_ASSIGNED'
        || message.templateCode === 'MES_EDHR_DEVIATION_CORRECT_ASSIGNED'
      return {
        title: message.templateCode === 'MES_EDHR_DEVIATION_WORK_TASK_OVERDUE'
          ? `偏差任务已逾期，请及时${taskTitle}` : `请${taskTitle}`,
        body: isDeviationAssignment && typeof params.reason === 'string' && params.reason !== '正式放行任务候选交接'
          ? params.reason : '',
        fields
      }
    }
    if (!params || typeof params.workOrderCode !== 'string' || !params.workOrderCode
      || typeof params.batchCode !== 'string' || !params.batchCode) {
      return { title: '任务通知缺少工单或批次信息，请联系管理员', body: message.templateContent ?? '', fields: [] }
    }
    const fields = [{ label: '工单', value: params.workOrderCode }, { label: '批次', value: params.batchCode }]
    if (typeof params.processName === 'string' && params.processName) fields.push({ label: '工序', value: params.processName })
    if (typeof params.dueTime === 'string' && params.dueTime) fields.push({ label: '截止时间', value: params.dueTime })
    return {
      title: workTaskTitle,
      body: typeof params.reason === 'string' && params.reason !== '正式放行任务候选交接' ? params.reason : '',
      fields
    }
  }
  if (message.templateCode !== 'MES_ACTIVE_ORDER_HANDOFF') {
    return { title: '', body: message.templateContent ?? '', fields: [] }
  }
  const params = message.templateParams
  if (!params || typeof params.handoffType !== 'string' || !params.handoffType
    || typeof params.reason !== 'string' || !params.reason) {
    return { title: '交接通知信息不完整，请联系管理员', body: message.templateContent ?? '', fields: [] }
  }
  return describeHandoffMessage(params.handoffType, params.reason)
}

export const notifyMessageTooltip = (message: Parameters<typeof describeNotifyMessage>[0]): string => {
  if (!message.templateCode?.startsWith('MES_EDHR_DEVIATION_') && !['DCC_REGISTRATION_CERTIFICATE_THRESHOLD_REMINDER', 'DCC_REGISTRATION_HANDLING_TASK'].includes(message.templateCode || '')) return message.templateContent || '-'
  const description = describeNotifyMessage(message)
  return [description.title, description.body, ...description.fields.map(field => `${field.label}：${field.value}`)].filter(Boolean).join('\n')
}
