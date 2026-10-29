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
    finalInspectionNotApplicableReason: '',
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
        key: 'PATROL_AM',
        inspectionType: 'PATROL',
        label: '上午巡检',
        required: true,
        roundLabel: '',
        taskRule: '',
        releaseGate: ''
      },
      {
        key: 'PATROL_PM',
        inspectionType: 'PATROL',
        label: '下午巡检',
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
    'patrolInspectionRatio',
    'standardLowerLimit',
    'standardUpperLimit',
    'standardUnit',
    'standardPrecision',
    'critical',
    'failureRule',
    'equipmentOptions'
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

export function validateDraft(draft: Draft, confirmedUnbound: string[] = []): string[] {
  const errors: string[] = []
  if (!draft.dccProjectCodeId) errors.push('请选择所属产品')
  if (!draft.regulationName.trim() || !draft.regulationCode.trim())
    errors.push('请填写规程名称和文件编号')
  if (!draft.finalInspectionApplicable && !draft.finalInspectionNotApplicableReason?.trim())
    errors.push('请确认末检不适用依据')
  if (!draft.processes.length) errors.push('请添加工序和检验项目')
  draft.processes.forEach((p, pi) => {
    if (!p.processName.trim() || !p.items.length) errors.push(`第${pi + 1}道工序缺少名称或检验项目`)
    p.items.forEach((i) => {
      const label = `${p.processName} / ${i.itemName || '未命名项目'}`
      if (
        !i.itemName.trim() ||
        !i.standardText.trim() ||
        !i.inspectionMethod.trim() ||
        !i.inspectionTool.trim() ||
        !i.samplingPlanText.trim()
      )
        errors.push(`${label}：请补全项目、标准、方法、器具和抽样要求`)
      if (!i.applicableInspectionTypes.length) errors.push(`${label}：请选择检验类型`)
      if (i.applicableInspectionTypes.includes('FIRST') && !(Number(i.firstInspectionQuantity) > 0))
        errors.push(`${label}：首检数量必须大于0`)
      if (i.applicableInspectionTypes.includes('PATROL') && !(Number(i.patrolInspectionRatio) > 0))
        errors.push(`${label}：请确认系统巡检抽样比例`)
      if (
        i.resultType === 'NUMERIC' &&
        (i.standardLowerLimit == null ||
          i.standardUpperLimit == null ||
          i.standardLowerLimit > i.standardUpperLimit)
      )
        errors.push(`${label}：请填写正确的上下限`)
      if (
        !['目测', '手感', '目测、手感', '/', '无'].includes(i.inspectionTool.trim()) &&
        !i.equipmentOptions.length &&
        !confirmedUnbound.includes(i.itemCode)
      )
        errors.push(`${label}：请选择设备编号`)
      if (i.equipmentOptions.some((e) => !e.equipmentId || !e.equipmentNumber.trim()))
        errors.push(`${label}：设备编号不完整`)
    })
  })
  return errors
}
