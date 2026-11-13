import PizZip from 'pizzip'
import Docxtemplater from 'docxtemplater'
import { clone, parseRecognition, type EditableProcess } from './simpleRecognition'
import type { BatchRecordTotalRecognitionJson as Recognition } from './productionRecognition'

export interface SimpleWordContent {
  filename: string
  routeName: string
  recognition: Recognition
  processes: EditableProcess[]
}

const snapshotPart = 'customXml/simple-parser.xml'
const snapshotNamespace = 'urn:intruoyi:simple-parser:word:v1'
const mime = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
const digest = async (text: string) =>
  Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('')

function validate(value: SimpleWordContent): SimpleWordContent {
  if (
    !value ||
    typeof value.filename !== 'string' ||
    typeof value.routeName !== 'string' ||
    !Array.isArray(value.processes)
  )
    throw new Error('Word 中的简易解析数据无效')
  const ids = new Set<string>()
  for (const process of value.processes) {
    if (
      !process ||
      typeof process.id !== 'string' ||
      !process.id ||
      ids.has(process.id) ||
      typeof process.sourceName !== 'string' ||
      (process.processId != null &&
        (!Number.isSafeInteger(process.processId) || process.processId <= 0)) ||
      (process.processCode != null && typeof process.processCode !== 'string')
    )
      throw new Error('Word 中的工序数据无效')
    ids.add(process.id)
    if (process.craftSelections != null) {
      if (typeof process.craftSelections !== 'object' || Array.isArray(process.craftSelections))
        throw new Error('Word 中的工艺绑定无效')
      for (const [index, id] of Object.entries(process.craftSelections)) {
        if (
          !/^\d+$/.test(index) ||
          !process.value?.equipmentGroups?.[Number(index)] ||
          !process.processId ||
          !Number.isSafeInteger(id) ||
          id <= 0
        )
          throw new Error('Word 中的工艺绑定无效')
      }
    }
  }
  const recognition = parseRecognition(JSON.stringify(value.recognition))
  parseRecognition(
    JSON.stringify({ ...recognition, processes: value.processes.map((p) => p.value) })
  )
  if (JSON.stringify(recognition.processes) !== JSON.stringify(value.processes.map((p) => p.value)))
    throw new Error('Word 中的工序与识别内容不一致')
  return clone(value)
}

const display = (value: unknown): string =>
  value == null ? '' : typeof value === 'object' ? JSON.stringify(value) : String(value)

function documentLines(content: SimpleWordContent) {
  const lines: string[] = [
    `产品名称：${content.recognition.product.name || ''}`,
    `产品编号：${content.recognition.product.code || ''}`,
    `源文件：${content.filename}`,
    '本文件可通过简易解析的“导入 Word”恢复导出时的编辑内容。请保留原文件；外部修改后不能用于完整恢复。'
  ]
  content.processes.forEach((process, index) => {
    lines.push(
      '',
      `工序 ${index + 1}：${process.value.name}`,
      `关键 / 特殊工序：${process.value.criticalProcess ? '是' : '否'}`,
      process.processId ? `使用已有工序：${process.processCode || ''}` : '新建工序'
    )
    for (const [key, label] of [
      ['inputs', '输入物料'],
      ['outputs', '输出物料']
    ] as const) {
      lines.push(label)
      process.value[key]?.forEach((item) => lines.push(`  ${item.name || ''}  ${item.code || ''}`))
    }
    process.value.equipmentGroups?.forEach((group, groupIndex) => {
      lines.push(
        `设备 / 参数组 ${groupIndex + 1}（${group.selectionMode === 'MULTIPLE' ? '多选' : '单选'}）`
      )
      if (group.equipmentRequired === false) lines.push('本组已关闭：设备及专属参数不参与发布')
      group.equipmentOptions?.forEach((device) =>
        lines.push(`  设备：${device.name || ''}  ${device.code || ''}`)
      )
      group.parameters?.forEach((parameter) => {
        lines.push(
          `  参数：${parameter.name || ''}；参考值：${parameter.referenceValue || ''}；实际值：${parameter.actualValue || ''}`
        )
        const labels: Record<string, string> = {
          control: '控件类型',
          defaultValue: '默认值',
          step: '步长',
          min: '下限',
          max: '上限',
          unit: '单位',
          displayName: '显示名称',
          options: '选项'
        }
        Object.entries(parameter.ui || {}).forEach(([key, value]) =>
          lines.push(`    ${labels[key] || key}：${display(value)}`)
        )
      })
    })
  })
  return lines.map((text) => ({ text }))
}

export async function exportSimpleWord(
  template: ArrayBuffer,
  value: SimpleWordContent
): Promise<Blob> {
  const content = validate(value)
  const doc = new Docxtemplater(new PizZip(template), { paragraphLoop: true, linebreaks: true })
  doc.render({ routeName: content.routeName, lines: documentLines(content) })
  const zip = doc.getZip()
  const payload = JSON.stringify({
    version: 1,
    documentHash: await digest(zip.file('word/document.xml')!.asText()),
    content
  })
  const encoded = btoa(
    Array.from(new TextEncoder().encode(payload), (byte) => String.fromCharCode(byte)).join('')
  )
  zip.file(
    snapshotPart,
    `<?xml version="1.0" encoding="UTF-8"?><snapshot xmlns="${snapshotNamespace}">${encoded}</snapshot>`
  )
  // Register the snapshot as a standard custom XML part in the DOCX package.
  const rels = zip.file('word/_rels/document.xml.rels')!.asText()
  zip.file(
    'word/_rels/document.xml.rels',
    rels.replace(
      '</Relationships>',
      '<Relationship Id="simpleParserSnapshot" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/customXml" Target="../customXml/simple-parser.xml"/></Relationships>'
    )
  )
  const types = zip.file('[Content_Types].xml')!.asText()
  zip.file(
    '[Content_Types].xml',
    types.replace(
      '</Types>',
      `<Override PartName="/${snapshotPart}" ContentType="application/xml"/></Types>`
    )
  )
  return zip.generate({ type: 'blob', mimeType: mime, compression: 'DEFLATE' })
}

// Absence means an ordinary Word document. A damaged snapshot is an error, never AI re-recognition.
export async function readSimpleWord(bytes: ArrayBuffer): Promise<SimpleWordContent | undefined> {
  const zip = new PizZip(bytes)
  const part = zip.file(snapshotPart)
  if (!part) return undefined
  const match = part
    .asText()
    .match(
      new RegExp(
        `^<\\?xml version="1.0" encoding="UTF-8"\\?><snapshot xmlns="${snapshotNamespace}">([A-Za-z0-9+/=]+)</snapshot>$`
      )
    )
  if (!match) throw new Error('Word 中的简易解析数据损坏，请重新导出')
  const payload = JSON.parse(
    new TextDecoder('utf-8', { fatal: true }).decode(
      Uint8Array.from(atob(match[1]), (char) => char.charCodeAt(0))
    )
  )
  if (payload.version !== 1) throw new Error('不支持此简易解析 Word 版本')
  const document = zip.file('word/document.xml')
  if (!document || payload.documentHash !== (await digest(document.asText())))
    throw new Error('Word 正文已修改或损坏，请导入未经外部修改的导出文件')
  return validate(payload.content)
}
