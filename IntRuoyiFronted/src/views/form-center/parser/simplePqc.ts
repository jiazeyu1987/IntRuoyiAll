import type {
  QaInspectionRegulationParseVO,
  QaInspectionRegulationItemVO,
  QaInspectionRegulationPublishedVersionVO,
  QaInspectionRegulationSaveReqVO
} from '@/api/mes/qc/template'

export type Draft = Omit<QaInspectionRegulationSaveReqVO, 'processes'> & {
  processes: Array<{
    processCode: string
    processName: string
    sort: number
    items: QaInspectionRegulationItemVO[]
  }>
}

type PatrolSampling = Pick<
  QaInspectionRegulationItemVO,
  'applicableInspectionTypes' | 'inspectionLevel' | 'samplingPlanText'
>

export const MATRIX_SAMPLING_RULE =
  '巡检按批量与检验水平查询GB/T 2828.1正常一次名义样本量；达到批量时全检。AQL单独记录。'

export const EQUIPMENT_UNBOUND_NOTICE = '保留原文器具要求，发布后执行前需补齐绑定'

export function isNonSensoryTool(inspectionTool: string): boolean {
  return !['目测', '手感', '目测、手感', '/', '无'].includes(inspectionTool.trim())
}

export function inspectionLevelIssue(item: PatrolSampling): string | undefined {
  if (
    item.applicableInspectionTypes.includes('PATROL') &&
    !['I', 'II', 'III', 'S1', 'S2', 'S3', 'S4'].includes(item.inspectionLevel ?? '')
  )
    return '请选择有效的巡检检验水平'
}

export function formatInspectionLevel(item: PatrolSampling): string {
  if (!item.applicableInspectionTypes.includes('PATROL')) return '不适用'
  return inspectionLevelIssue(item) ? '待确认检验水平' : item.inspectionLevel!.replace(/^S/, 'S-')
}

export function makeDraft(parsed: QaInspectionRegulationParseVO, projectId: number): Draft {
  return {
    dccProjectCodeId: projectId,
    expectedDraftRevision: 0,
    ownerModule: 'MES_QA',
    regulationCode: parsed.regulationCode,
    regulationName: parsed.regulationName,
    versionNo: parsed.versionNo,
    effectiveDate: parsed.effectiveDate,
    finalInspectionApplicable: false,
    finalInspectionNotApplicableReason: '导入后按检验工序设置末检，当前均未开启',
    inspectionTypeRules: [
      {
        key: 'FIRST',
        inspectionType: 'FIRST',
        label: '首检',
        required: true,
        roundLabel: '',
        taskRule: '',
        releaseGate: ''
      },
      {
        key: 'PATROL',
        inspectionType: 'PATROL',
        label: '巡检',
        required: true,
        roundLabel: '',
        taskRule: '',
        releaseGate: ''
      },
      {
        key: 'FINAL',
        inspectionType: 'FINAL',
        label: '末检',
        required: false,
        roundLabel: '',
        taskRule: '',
        releaseGate: ''
      }
    ],
    processes: parsed.processes.map((p) => ({
      ...p,
      items: p.items.map((i) => ({
        ...i,
        critical: false,
        failureRule: parsed.ruleSourceText,
        equipmentOptions: [],
        sourceOriginalExcerpt: i.standardText,
        sourceOriginalMethod: i.inspectionMethod,
        sourceOriginalItem: `${p.processName} / ${i.itemName}`
      }))
    }))
  }
}

export function nextVersion(source: string, versions: string[]): string {
  if (!versions.includes(source)) return source
  const match = source.match(/^(.*?)(\d+)$/)
  const prefix = match ? match[1] : `${source}.`
  let n = match ? Number(match[2]) + 1 : 1
  while (versions.includes(`${prefix}${n}`)) n++
  return `${prefix}${n}`
}

// ponytail: compare business fields directly; persistence IDs and provenance are not inspection behavior.
export function differences(
  draft: Draft,
  current: QaInspectionRegulationPublishedVersionVO | null
): string[] {
  if (!current) return ['新建检验规程']
  const result: string[] = []
  if (draft.regulationName !== current.regulationName) result.push('规程名称已修改')
  if (draft.regulationCode !== current.regulationCode) result.push('文件编号已修改')
  if (
    JSON.stringify(draft.processes.map((p) => p.processName)) !==
    JSON.stringify(current.processes.map((p) => p.processName))
  )
    result.push('工序列表或顺序已修改')
  const fields = [
    'itemName',
    'inspectionMethod',
    'inspectionTool',
    'samplingPlanText',
    'standardText',
    'resultType',
    'applicableInspectionTypes',
    'firstInspectionQuantity',
    'inspectionLevel',
    'aql',
    'standardLowerLimit',
    'standardUpperLimit',
    'standardUnit',
    'standardPrecision',
    'critical',
    'failureRule',
    'equipmentOptions',
    'equipmentUnboundReason'
  ] as const
  draft.processes.forEach((p) => {
    const matches = current.processes.filter((old) => old.processName === p.processName)
    if (matches.length !== 1) {
      result.push(`新增或待对应工序：${p.processName}`)
      return
    }
    const old = matches[0]
    if (
      JSON.stringify(p.items.map((i) => i.itemName)) !==
      JSON.stringify(old.items.map((i) => i.itemName))
    )
      result.push(`${p.processName}：项目列表或顺序已修改`)
    p.items.forEach((item) => {
      const candidates = old.items.filter((i) => i.itemName === item.itemName)
      if (candidates.length !== 1) {
        result.push(`${p.processName} / ${item.itemName}：新增或存在同名项目`)
        return
      }
      for (const field of fields) {
        if (JSON.stringify(item[field] ?? null) !== JSON.stringify(candidates[0][field] ?? null))
          result.push(`${p.processName} / ${item.itemName}：${field}`)
      }
    })
    old.items
      .filter((i) => !p.items.some((n) => n.itemName === i.itemName))
      .forEach((i) => result.push(`删除项目：${p.processName} / ${i.itemName}`))
  })
  current.processes
    .filter((p) => !draft.processes.some((n) => n.processName === p.processName))
    .forEach((p) => result.push(`删除工序：${p.processName}`))
  for (const field of [
    'effectiveDate',
    'finalInspectionApplicable',
    'finalInspectionNotApplicableReason',
    'inspectionTypeRules'
  ] as const) {
    const normalize = (value: unknown) =>
      field === 'effectiveDate' && Array.isArray(value)
        ? value
            .map((part, index) => (index === 0 ? String(part) : String(part).padStart(2, '0')))
            .join('-')
        : value
    if (
      JSON.stringify(normalize(draft[field]) ?? null) !==
      JSON.stringify(normalize(current[field]) ?? null)
    )
      result.push(`规程：${field}`)
  }
  return result
}

export interface DraftIssue {
  message: string
  field: 'metadata' | 'final' | 'process' | 'item' | 'equipment'
  processIndex?: number
  itemIndex?: number
  target?: string
}

export function validateDraft(draft: Draft, confirmedUnbound: string[] = []): DraftIssue[] {
  const errors: DraftIssue[] = []
  if (!draft.dccProjectCodeId) errors.push({ message: '请选择所属产品', field: 'metadata' })
  for (const [key, label] of [
    ['regulationName', '规程名称'],
    ['regulationCode', '文件编号']
  ] as const)
    if (!draft[key].trim())
      errors.push({ message: `请填写${label}`, field: 'metadata', target: key })
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
      if (
        isNonSensoryTool(i.inspectionTool) &&
        !i.equipmentOptions.length &&
        !i.equipmentUnboundReason?.trim() &&
        !confirmedUnbound.includes(i.itemCode)
      )
        add('请选择设备编号，或填写器具暂未绑定原因', 'equipment')
      if (i.equipmentOptions.length && i.equipmentUnboundReason?.trim())
        add('已选择设备，请清除器具未绑定原因', 'equipment')
      if (i.equipmentOptions.some((e) => !e.equipmentId || !e.equipmentNumber.trim()))
        add('设备编号不完整', 'equipment')
    })
  })
  return errors
}
