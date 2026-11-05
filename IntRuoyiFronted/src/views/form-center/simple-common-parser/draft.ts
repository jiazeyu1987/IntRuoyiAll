import type { SimpleCommonDraft } from '@/api/mes/qc/template/simpleCommonParser'
import { inspectionLevelIssue } from '../parser/simplePqc'

export interface DraftIssue {
  message: string
  field: 'metadata' | 'final' | 'process' | 'item' | 'equipment'
  processIndex?: number
  itemIndex?: number
  target?: string
}

export function validateCommonDraft(draft: SimpleCommonDraft): DraftIssue[] {
  const errors: DraftIssue[] = []
  for (const [key, label] of [
    ['regulationName', '规程名称'],
    ['regulationCode', '文件编号']
  ] as const)
    if (!draft[key].trim())
      errors.push({ message: `请填写${label}`, field: 'metadata', target: key })
  if (!draft.finalInspectionApplicable && !draft.finalInspectionNotApplicableReason?.trim())
    errors.push({ message: '请确认是否无需末检', field: 'final' })
  if (!draft.processes.length)
    errors.push({ message: '请添加工序和检验项目', field: 'process', target: 'addProcess' })
  draft.processes.forEach((p, pi) => {
    if (!p.processName.trim())
      errors.push({
        message: `第${pi + 1}道工序缺少名称`,
        field: 'item',
        processIndex: pi,
        target: 'processName'
      })
    if (!p.items.length)
      errors.push({
        message: `第${pi + 1}道工序缺少检验项目`,
        field: 'item',
        processIndex: pi,
        target: 'addItem'
      })
    p.items.forEach((i, ii) => {
      const label = `${p.processName} / ${i.itemName || '未命名项目'}`
      const add = (message: string, field: DraftIssue['field'] = 'item', target?: string) =>
        errors.push({
          message: `${label}：${message}`,
          field,
          processIndex: pi,
          itemIndex: ii,
          target
        })
      for (const [key, name] of [
        ['itemName', '检验项目'],
        ['standardText', '接受标准'],
        ['inspectionMethod', '检验方法'],
        ['inspectionTool', '器具要求'],
        ['samplingPlanText', '抽样要求']
      ] as const)
        if (!i[key].trim()) add(`请填写${name}`, 'item', key)
      if (!i.applicableInspectionTypes.length)
        add('请选择检验类型', 'item', 'applicableInspectionTypes')
      if (i.applicableInspectionTypes.includes('FIRST') && !(Number(i.firstInspectionQuantity) > 0))
        add('首检数量必须大于0', 'item', 'firstInspectionQuantity')
      const samplingIssue = inspectionLevelIssue(i)
      if (samplingIssue) add(samplingIssue, 'item', 'inspectionLevel')
      if (
        i.resultType === 'NUMERIC' &&
        (i.standardLowerLimit == null ||
          i.standardUpperLimit == null ||
          i.standardLowerLimit > i.standardUpperLimit)
      )
        add(
          '请填写正确的上下限',
          'item',
          i.standardLowerLimit == null ? 'standardLowerLimit' : 'standardUpperLimit'
        )
      if (i.equipmentOptions.some((e) => !e.equipmentId || !e.equipmentNumber.trim()))
        add('设备编号不完整', 'equipment')
    })
  })
  return errors
}
