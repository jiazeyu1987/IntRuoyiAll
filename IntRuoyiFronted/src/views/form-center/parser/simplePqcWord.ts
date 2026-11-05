import PizZip from 'pizzip'
import type { Draft } from './simplePqc'

const payloadPath = 'customXml/simplePqc.xml'
const payloadNamespace = 'urn:intruoyi:simple-pqc:1'
const wordNamespace = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
export const pqcWordMime = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'

export interface PqcWordSnapshot {
  format: 'IntRuoyi.SimplePqc' | 'IntRuoyi.SimpleCommon'
  version: 1
  tenantId: string
  draft: Draft
  confirmedUnbound: string[]
}

const xml = (value: unknown) =>
  String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
const paragraph = (value: unknown, bold = false) =>
  `<w:p><w:r>${bold ? '<w:rPr><w:b/></w:rPr>' : ''}${String(value ?? '')
    .split(/\r\n|\r|\n/)
    .map((line, index) => `${index ? '<w:br/>' : ''}<w:t xml:space="preserve">${xml(line)}</w:t>`)
    .join('')}</w:r></w:p>`
const labels: Record<string, string> = {
  FIRST: '首检',
  PATROL: '巡检',
  FINAL: '末检',
  BOOLEAN: '合格 / 不合格',
  NUMERIC: '测量数值',
  TEXT: '文字记录'
}

/** A normal readable DOCX with a versioned custom XML part for lossless re-import. */
export function createPqcWord(
  draft: Draft,
  tenantId: string,
  confirmedUnbound: string[]
): Uint8Array {
  const common = draft.ownerModule === 'MES_QA_COMMON'
  const snapshot: PqcWordSnapshot = {
    format: common ? 'IntRuoyi.SimpleCommon' : 'IntRuoyi.SimplePqc',
    version: 1,
    tenantId,
    draft,
    confirmedUnbound
  }
  const bytes = new TextEncoder().encode(JSON.stringify(snapshot))
  const encoded = Array.from(bytes, (byte) => String.fromCharCode(byte)).join('')
  const zip = new PizZip()
  zip.file(
    '[Content_Types].xml',
    `<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>`
  )
  zip.file(
    '_rels/.rels',
    `<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>`
  )
  zip.file(
    'word/_rels/document.xml.rels',
    `<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/customXml" Target="../${payloadPath}"/></Relationships>`
  )
  zip.file(
    payloadPath,
    `<?xml version="1.0" encoding="UTF-8"?><simplePqc xmlns="${payloadNamespace}" encoding="base64">${btoa(encoded)}</simplePqc>`
  )
  const content = [
    paragraph(draft.regulationName, true),
    paragraph(`文件编号：${draft.regulationCode}　版本：${draft.versionNo}`),
    paragraph(`生效日期：${draft.effectiveDate ?? ''}`),
    paragraph(
      common
        ? '本文件包含完整编辑数据，可在简易通用解析中通过“导入 Word”还原，无需选择项目代码。'
        : '本文件包含完整编辑数据，可在简易PQC解析中选择原产品，通过“导入检验规程”还原。'
    ),
    paragraph(
      `末检适用：${draft.finalInspectionApplicable ? '是' : '否'}；不适用依据：${draft.finalInspectionNotApplicableReason ?? ''}`
    ),
    ...draft.inspectionTypeRules.map((rule) =>
      paragraph(
        `${rule.label}：${rule.required ? '适用' : '不适用'}；数量：${rule.fixedQuantity ?? ''}；轮次：${rule.roundLabel}；任务规则：${rule.taskRule}；放行规则：${rule.releaseGate}；不适用依据：${rule.notApplicableReason ?? ''}`
      )
    )
  ]
  draft.processes.forEach((process, pi) => {
    content.push(paragraph(`${pi + 1}. ${process.processName}`, true))
    process.items.forEach((item, ii) => {
      content.push(paragraph(`${pi + 1}.${ii + 1} ${item.itemName}`, true))
      const fields: Array<[string, unknown]> = [
        ['接受标准', item.standardText],
        ['检验方法', item.inspectionMethod],
        ['结果填写方式', labels[item.resultType]],
        ['适用检验', item.applicableInspectionTypes.map((type) => labels[type]).join('、')],
        ['关键检验项目', item.critical ? '是' : '否'],
        ['下限', item.standardLowerLimit],
        ['上限', item.standardUpperLimit],
        ['单位', item.standardUnit],
        ['小数位数', item.standardPrecision],
        ['抽样要求', item.samplingPlanText],
        ['首检数量（件）', item.firstInspectionQuantity],
        ['巡检检验水平', item.inspectionLevel],
        ['AQL', item.aql],
        ['检验器具及设备要求', item.inspectionTool],
        ['器具未绑定原因', item.equipmentUnboundReason],
        [
          '设备编号 / 名称',
          item.equipmentOptions
            .map((equipment) => `${equipment.equipmentNumber} / ${equipment.equipmentName}`)
            .join('、')
        ],
        ['不合格处理要求', item.failureRule]
      ]
      fields.forEach(([label, value]) => content.push(paragraph(`${label}：${value ?? ''}`)))
    })
  })
  zip.file(
    'word/document.xml',
    `<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="${wordNamespace}"><w:body>${content.join('')}<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/></w:sectPr></w:body></w:document>`
  )
  return zip.generate({ type: 'uint8array', compression: 'DEFLATE' })
}

/** null means an ordinary source Word; damaged exports must never go through recognition. */
export function readPqcWord(
  bytes: ArrayBuffer | Uint8Array,
  tenantId: string,
  projectId: number | undefined,
  ownerModule: 'MES_QA' | 'MES_QA_COMMON' = 'MES_QA'
): PqcWordSnapshot | null {
  const zip = new PizZip(bytes)
  if (!zip.file('word/document.xml')) throw new Error('文件缺少 Word 正文')
  const part = zip.file(payloadPath)
  if (!part) {
    if (zip.file('word/_rels/document.xml.rels')?.asText().includes(payloadPath.split('/')[1]))
      throw new Error('Word 导出数据缺失，请重新导出')
    return null
  }
  const match = part
    .asText()
    .match(
      new RegExp(
        `<simplePqc xmlns="${payloadNamespace}" encoding="base64">([A-Za-z0-9+/=]+)</simplePqc>`
      )
    )
  if (!match) throw new Error('Word 导出数据格式无效')
  let snapshot: PqcWordSnapshot
  try {
    const binary = atob(match[1])
    snapshot = JSON.parse(
      new TextDecoder('utf-8', { fatal: true }).decode(
        Uint8Array.from(binary, (char) => char.charCodeAt(0))
      )
    )
  } catch {
    throw new Error('Word 导出数据损坏，不能还原')
  }
  if (
    !['IntRuoyi.SimplePqc', 'IntRuoyi.SimpleCommon'].includes(snapshot?.format) ||
    snapshot.version !== 1
  )
    throw new Error('不支持此 Word 导出数据版本')
  const expectedFormat =
    ownerModule === 'MES_QA_COMMON' ? 'IntRuoyi.SimpleCommon' : 'IntRuoyi.SimplePqc'
  if (
    snapshot.format !== expectedFormat ||
    (snapshot.draft?.ownerModule && snapshot.draft.ownerModule !== ownerModule)
  )
    throw new Error('规程类型不一致，请在对应的简易PQC解析或简易通用解析页面导入')
  if (snapshot.tenantId !== tenantId)
    throw new Error('此 Word 属于其他租户，不能还原设备及产品绑定')
  if (ownerModule === 'MES_QA' && snapshot.draft?.dccProjectCodeId !== projectId)
    throw new Error('请选择导出时的所属产品后再导入')
  const draft = snapshot.draft
  if (
    !Array.isArray(draft.processes) ||
    !Array.isArray(draft.inspectionTypeRules) ||
    !Array.isArray(snapshot.confirmedUnbound) ||
    !snapshot.confirmedUnbound.every((code) => typeof code === 'string') ||
    !draft.processes.every(
      (process) =>
        typeof process.processName === 'string' &&
        Array.isArray(process.items) &&
        process.items.every(
          (item) =>
            typeof item.itemName === 'string' &&
            (item.equipmentUnboundReason == null ||
              typeof item.equipmentUnboundReason === 'string') &&
            Array.isArray(item.equipmentOptions) &&
            Array.isArray(item.applicableInspectionTypes)
        )
    )
  )
    throw new Error('Word 导出数据结构无效')
  if (ownerModule === 'MES_QA_COMMON') delete draft.dccProjectCodeId
  return snapshot
}

export function pqcWordFilename(draft: Draft): string {
  const name = `${draft.regulationCode}_${draft.regulationName}`
    .replace(/[\\/:*?"<>|\u0000-\u001f]/g, '_')
    .slice(0, 120)
  return `${name}.docx`
}
