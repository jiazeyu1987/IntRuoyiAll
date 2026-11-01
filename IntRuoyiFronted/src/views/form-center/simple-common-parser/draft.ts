import type { SimpleCommonDraft } from '@/api/mes/qc/template/simpleCommonParser'

export interface DraftIssue {
  message: string
  field: 'metadata' | 'final' | 'process' | 'item' | 'equipment'
  processIndex?: number
  itemIndex?: number
}

export function validateCommonDraft(draft: SimpleCommonDraft): DraftIssue[] {
  const errors: DraftIssue[] = []
  if (!draft.dccProjectCodeId) errors.push({ message: '请选择规程所属项目', field: 'metadata' })
  if (!draft.regulationName.trim() || !draft.regulationCode.trim())
    errors.push({ message: '请填写规程名称和文件编号', field: 'metadata' })
  if (!draft.finalInspectionApplicable && !draft.finalInspectionNotApplicableReason?.trim())
    errors.push({ message: '请确认是否无需末检', field: 'final' })
  if (!draft.processes.length) errors.push({ message: '请添加工序和检验项目', field: 'process' })
  draft.processes.forEach((p, pi) => {
    if (!p.processName.trim() || !p.items.length)
      errors.push({
        message: `第${pi + 1}道工序缺少名称或检验项目`,
        field: 'process',
        processIndex: pi
      })
    p.items.forEach((i, ii) => {
      const label = `${p.processName} / ${i.itemName || '未命名项目'}`
      const add = (message: string, field: DraftIssue['field'] = 'item') =>
        errors.push({ message: `${label}：${message}`, field, processIndex: pi, itemIndex: ii })
      if (
        !i.itemName.trim() ||
        !i.standardText.trim() ||
        !i.inspectionMethod.trim() ||
        !i.inspectionTool.trim() ||
        !i.samplingPlanText.trim()
      )
        add('请补全项目、标准、方法、器具和抽样要求')
      if (!i.applicableInspectionTypes.length) add('请选择检验类型')
      if (i.applicableInspectionTypes.includes('FIRST') && !(Number(i.firstInspectionQuantity) > 0))
        add('首检数量必须大于0')
      if (i.applicableInspectionTypes.includes('PATROL') && !(Number(i.patrolInspectionRatio) > 0))
        add('请确认系统巡检抽样比例')
      if (
        i.resultType === 'NUMERIC' &&
        (i.standardLowerLimit == null ||
          i.standardUpperLimit == null ||
          i.standardLowerLimit > i.standardUpperLimit)
      )
        add('请填写正确的上下限')
      if (i.equipmentOptions.some((e) => !e.equipmentId || !e.equipmentNumber.trim()))
        add('设备编号不完整', 'equipment')
    })
  })
  return errors
}
