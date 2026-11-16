import Docxtemplater from 'docxtemplater'
import PizZip from 'pizzip'
import type { BatchRecordTotalRecognitionJson } from './productionRecognition'

const text = (value: unknown): string => {
  if (value === undefined || value === null) return ''
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}

const filenamePart = (value: string) =>
  value.replace(/[\\/:*?"<>|\u0000-\u001f]/g, '_').replace(/[. ]+$/, '').slice(0, 80)

/** Each document starts from the same immutable DOCX bytes; layout lives in the template. */
export const createProductionWordCollection = (
  mapping: BatchRecordTotalRecognitionJson,
  template: ArrayBuffer | Uint8Array,
  sourceFileName: string
): Uint8Array => {
  if (!mapping.processes.length) throw new Error('没有可导出的工序')
  const archive = new PizZip()
  mapping.processes.forEach((process, index) => {
    const materials = [
      ...(process.inputs ?? []).map((item) => ({ ...item, direction: '输入' })),
      ...(process.outputs ?? []).map((item) => ({ ...item, direction: '输出' }))
    ].map((item) => ({
      direction: item.direction, code: text(item.code), name: text(item.name),
      sourceCodeLabel: text(item.sourceCodeLabel)
    }))
    const groups = process.equipmentGroups ?? []
    const equipment = groups.flatMap((group, groupIndex) =>
      (group.equipmentOptions ?? []).map((item) => ({
        groupNumber: groupIndex + 1, selectionMode: text(group.selectionMode),
        code: text(item.code), name: text(item.name), sourceCodeLabel: text(item.sourceCodeLabel)
      }))
    )
    const parameters = groups.flatMap((group, groupIndex) =>
      (group.parameters ?? []).map((parameter) => {
        const ui = parameter.ui
        const fields: [string, unknown][] = [
          ['显示名称', ui?.displayName], ['控件', ui?.control], ['默认值', ui?.defaultValue],
          ['步长', ui?.step], ['最小值', ui?.min], ['最大值', ui?.max],
          ['单位', ui?.unit], ['选项', ui?.options]
        ]
        return {
          groupNumber: groupIndex + 1, name: text(parameter.name),
          referenceValue: text(parameter.referenceValue), actualValue: text(parameter.actualValue),
          uiDetails: fields.filter(([, value]) => value !== undefined && value !== null)
            .map(([label, value]) => `${label}：${text(value)}`).join('\n')
        }
      })
    )
    const document = new Docxtemplater(new PizZip(template), {
      paragraphLoop: true,
      linebreaks: true,
      nullGetter: (part) => { throw new Error(`Word 模板字段未提供：${part.value}`) }
    })
    document.render({
      sourceFileName, productName: text(mapping.product?.name), productCode: text(mapping.product?.code),
      schemaVersion: mapping.schemaVersion, processNumber: index + 1, processCount: mapping.processes.length,
      processName: process.name,
      criticalProcess: process.criticalProcess === undefined ? '' : process.criticalProcess ? '是' : '否',
      materialCount: materials.length, equipmentCount: equipment.length, groupCount: groups.length,
      parameterCount: parameters.length, materials, equipment, parameters
    })
    const number = String(index + 1).padStart(3, '0')
    archive.file(`${number}-${filenamePart(process.name)}.docx`, document.getZip().generate({ type: 'uint8array' }))
  })
  archive.file('模板/生产工序模板.docx', template)
  return archive.generate({ type: 'uint8array', compression: 'DEFLATE' })
}
