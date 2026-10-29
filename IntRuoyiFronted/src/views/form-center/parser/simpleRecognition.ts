import PizZip from 'pizzip'
import type {
  BatchRecordTotalRecognitionJson as Recognition,
  BatchRecordTotalRecognitionProcess as Process,
  BatchRecordTotalRecognitionParameter as Parameter
} from './productionRecognition'

export interface SourceCell {
  text: string
  colspan: number
  rowspan: number
}
export interface SourceBlock {
  rows: SourceCell[][]
}
export interface EditableProcess {
  id: string
  sourceName: string
  value: Process
  processId?: number
  processCode?: string
  craftSelections?: Record<number, number>
}
export interface SimpleDraft {
  version: 1
  filename: string
  recognition: Recognition
  processes: EditableProcess[]
  source: SourceBlock[]
  reviewed: string[]
  savedAt: string
  requestId?: string
  routeName?: string
  published?: unknown
}
export interface ReviewIssue {
  process: number
  message: string
}

export const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value))
export const signature = (process: EditableProcess) =>
  JSON.stringify([process.value, process.processId, process.craftSelections])
export const newProcess = (): EditableProcess => ({
  id: crypto.randomUUID(),
  sourceName: '',
  value: { name: '', criticalProcess: false, inputs: [], outputs: [], equipmentGroups: [] }
})

export function parseRecognition(text: string): Recognition {
  const value = JSON.parse(text)
  if (
    !value ||
    typeof value !== 'object' ||
    !value.product ||
    typeof value.product !== 'object' ||
    Array.isArray(value.product) ||
    ![2, 3].includes(value.schemaVersion) ||
    !Array.isArray(value.processes)
  ) {
    throw new Error('识别结果缺少产品或工序结构，请检查文档后重新识别')
  }
  for (const process of value.processes) {
    if (!process || typeof process !== 'object' || typeof process.name !== 'string')
      throw new Error('识别结果的工序结构无效')
    for (const key of ['inputs', 'outputs', 'equipmentGroups']) {
      if (process[key] != null && !Array.isArray(process[key]))
        throw new Error(`工序「${process.name}」的${key}结构无效`)
      process[key]?.forEach((item: unknown) => {
        if (!item || typeof item !== 'object' || Array.isArray(item))
          throw new Error(`工序「${process.name}」包含无效条目`)
      })
    }
    process.equipmentGroups?.forEach((group: Record<string, any>) => {
      for (const key of ['equipmentOptions', 'parameters']) {
        if (group[key] != null && !Array.isArray(group[key]))
          throw new Error(`工序「${process.name}」的设备组结构无效`)
        group[key]?.forEach((item: unknown) => {
          if (!item || typeof item !== 'object' || Array.isArray(item))
            throw new Error('设备或参数结构无效')
        })
      }
      group.parameters?.forEach((parameter: Parameter) => {
        if (
          parameter.ui != null &&
          (typeof parameter.ui !== 'object' || Array.isArray(parameter.ui))
        )
          throw new Error('参数设置结构无效')
        if (parameter.ui?.options != null && !Array.isArray(parameter.ui.options))
          throw new Error('参数选项结构无效')
      })
    })
  }
  return value
}

// Read text and cell merges from the uploaded Word itself; never render document HTML/scripts.
export function readWordSource(bytes: ArrayBuffer): SourceBlock[] {
  const zip = new PizZip(bytes)
  const part = zip.file('word/document.xml')
  if (!part) throw new Error('文件缺少 Word 正文，不能打开原文对照')
  const doc = new DOMParser().parseFromString(part.asText(), 'application/xml')
  if (doc.querySelector('parsererror')) throw new Error('Word 正文格式损坏')
  const ns = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
  const body = doc.getElementsByTagNameNS(ns, 'body')[0]
  if (!body) throw new Error('Word 正文为空')
  const children = (node: Element, name: string) =>
    Array.from(node.children).filter(
      (child) => child.namespaceURI === ns && child.localName === name
    )
  const text = (node: Element) =>
    Array.from(node.getElementsByTagNameNS(ns, 'p'))
      .map((p) =>
        Array.from(p.getElementsByTagNameNS(ns, 't'))
          .map((t) => t.textContent)
          .join('')
      )
      .join('\n')
  const result: SourceBlock[] = []
  for (const node of Array.from(body.children)) {
    if (node.localName === 'p') {
      const value = Array.from(node.getElementsByTagNameNS(ns, 't'))
        .map((t) => t.textContent)
        .join('')
      if (value.trim()) result.push({ rows: [[{ text: value, colspan: 1, rowspan: 1 }]] })
    } else if (node.localName === 'tbl') {
      const active = new Map<number, SourceCell>()
      const rows = children(node, 'tr').map((row) => {
        let column = 0
        const cells: SourceCell[] = []
        for (const cell of children(row, 'tc')) {
          const spanNode = cell.getElementsByTagNameNS(ns, 'gridSpan')[0]
          const colspan = Number(spanNode?.getAttributeNS(ns, 'val') ?? 1)
          const merge = cell.getElementsByTagNameNS(ns, 'vMerge')[0]
          if (merge && merge.getAttributeNS(ns, 'val') !== 'restart') {
            const start = active.get(column)
            if (!start) throw new Error('Word 表格存在无起始单元格的纵向合并')
            start.rowspan++
          } else {
            const value = { text: text(cell), colspan, rowspan: 1 }
            cells.push(value)
            for (let i = column; i < column + colspan; i++) active.delete(i)
            if (merge) active.set(column, value)
          }
          column += colspan
        }
        return cells
      })
      result.push({ rows })
    }
  }
  return result
}

export function checkParameter(parameter: Parameter): string[] {
  const errors: string[] = []
  if (!parameter.name?.trim()) errors.push('请填写参数名称')
  const ui = parameter.ui
  if (ui?.defaultValue != null && !['number', 'string'].includes(typeof ui.defaultValue))
    errors.push('默认值必须是文字或数字，请核对识别结果')
  if (!parameter.referenceValue?.trim()) errors.push('请填写参考值')
  if (!ui || !['number', 'select', 'text'].includes(ui.control ?? '')) {
    errors.push('请选择参数填写类型')
    return errors
  }
  if (ui.control === 'number') {
    if (ui.defaultValue == null || ui.defaultValue === '') errors.push('数字参数需要默认值')
    if (ui.step == null || ui.step === '') errors.push('数字参数需要填写步长')
    const values = [ui.min, ui.max, ui.step, ui.defaultValue].filter((v) => v != null && v !== '')
    if (values.some((v) => !['string', 'number'].includes(typeof v) || !Number.isFinite(Number(v))))
      errors.push('数值设置必须是有效数字')
    const has = (v: unknown) => v != null && v !== ''
    if (has(ui.min) && has(ui.max) && Number(ui.min) > Number(ui.max))
      errors.push('下限不能大于上限')
    if (has(ui.step) && Number(ui.step) <= 0) errors.push('步长必须大于零')
    if (
      has(ui.defaultValue) &&
      ((has(ui.min) && Number(ui.defaultValue) < Number(ui.min)) ||
        (has(ui.max) && Number(ui.defaultValue) > Number(ui.max)))
    )
      errors.push('默认值超出上下限')
  }
  if (ui.control === 'select') {
    if (ui.defaultValue == null || ui.defaultValue === '') errors.push('请选择下拉默认值')
    if (!ui.options?.length) errors.push('下拉选项不能为空')
    if (ui.options?.some((v) => typeof v !== 'string' && typeof v !== 'number'))
      errors.push('选项必须是文字或数字，请校对原始选项')
    if (ui.defaultValue != null && ui.defaultValue !== '' && !ui.options?.includes(ui.defaultValue))
      errors.push('默认值不在下拉选项中')
  }
  return errors
}

export function reviewIssues(processes: EditableProcess[]): ReviewIssue[] {
  const issues: ReviewIssue[] = []
  const add = (process: number, message: string) => issues.push({ process, message })
  processes.forEach(({ value: p }, index) => {
    if (!p.name.trim()) add(index, '请填写工序名称')
    for (const direction of ['inputs', 'outputs'] as const) {
      p[direction]?.forEach((material) => {
        if (!material.name?.trim())
          add(index, `${direction === 'inputs' ? '输入' : '输出'}物料缺少名称`)
      })
    }
    p.equipmentGroups?.forEach((group, groupIndex) => {
      const label = `设备组${groupIndex + 1}`
      if (!group.equipmentOptions?.length && !group.parameters?.length)
        add(index, `${label}为空，请补充内容或删除空组`)
      if (!group.equipmentOptions?.length && group.parameters?.length)
        add(index, `${label}的参数尚未关联设备，现有发布流程要求先关联设备`)
      if (!['SINGLE', 'MULTIPLE'].includes(group.selectionMode ?? ''))
        add(index, `${label}请选择设备选择方式`)
      group.equipmentOptions?.forEach((equipment) => {
        if (!equipment.code?.trim()) add(index, `${label}缺少设备编号，请从设备列表选择`)
        else if (!equipment.name?.trim()) add(index, `${label}设备名称缺失，请重新选择设备编号`)
      })
      group.parameters?.forEach((parameter) =>
        checkParameter(parameter).forEach((error) =>
          add(index, `${parameter.name || '未命名参数'}：${error}`)
        )
      )
    })
  })
  return issues
}
