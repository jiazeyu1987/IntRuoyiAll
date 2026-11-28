import { parseExactIntegerJson } from '@/utils/exactIntegerJson'

export interface PropertyValueDefinition {
  dataType: string
  dataSpecs?: { childDataType?: string; unitName?: string; dataSpecsList?: PropertyValueField[] }
  dataSpecsList?: PropertyValueField[]
}
interface PropertyValueField extends PropertyValueDefinition {
  identifier?: string
  name?: string
  childDataType?: string
  value?: unknown
}
export interface PropertyValueRow { label: string; value: string }

/** Read device values against the product's actual thing-model definition. */
export const presentPropertyValue = (raw: unknown, definition: PropertyValueDefinition) => {
  const rows: PropertyValueRow[] = []
  try {
    if (raw === null || raw === undefined || raw === '') return { rows: [{ label: '属性值', value: '未记录' }], error: '' }
    const complex = ['struct', 'array'].includes(definition.dataType)
    const value = complex && typeof raw === 'string' ? parseExactIntegerJson(raw) : raw
    const visit = (value: unknown, schema: PropertyValueDefinition, path: string[]) => {
      const label = path.join(' / ') || '属性值'
      if (value === null || value === undefined) {
        rows.push({ label, value: '未记录' })
        return
      }
      if (schema.dataType === 'struct') {
        if (typeof value !== 'object' || Array.isArray(value)) throw new Error('设备上报值不是物模型要求的结构资料')
        const fields = schema.dataSpecsList
        if (!fields?.length || fields.some(field => !field.identifier || !field.name || !field.childDataType)) {
          throw new Error('物模型缺少完整的子属性名称或类型，无法展示设备读数')
        }
        const object = value as Record<string, unknown>
        if (Object.keys(object).some(key => !fields.some(field => field.identifier === key))) {
          throw new Error('设备上报值包含物模型未定义的子属性，请联系管理员核查')
        }
        fields.forEach(field => visit(object[field.identifier!], { ...field, dataType: field.childDataType! }, [...path, field.name!]))
        return
      }
      if (schema.dataType === 'array') {
        if (!Array.isArray(value)) throw new Error('设备上报值不是物模型要求的条目列表')
        const specs = schema.dataSpecs
        if (!specs?.childDataType) throw new Error('物模型缺少列表元素类型，无法展示设备读数')
        if (!value.length) rows.push({ label, value: '无条目' })
        value.forEach((item, index) => visit(item, { dataType: specs.childDataType!, dataSpecsList: specs.dataSpecsList }, [...path, `第 ${index + 1} 项`]))
        return
      }
      if (typeof value === 'object') throw new Error('设备上报值与物模型定义的属性类型不一致')
      let text: string
      if (schema.dataType === 'enum' || schema.dataType === 'bool') {
        const enumValue = typeof value === 'boolean' ? Number(value) : value
        const option = schema.dataSpecsList?.find(item => String(item.value) === String(enumValue))
        if (option?.name) text = option.name
        else if (schema.dataType === 'bool' && !schema.dataSpecsList?.length && ['0', '1'].includes(String(enumValue))) text = String(enumValue) === '1' ? '是' : '否'
        else throw new Error('设备上报值没有对应的物模型枚举说明，请联系管理员核查')
      } else {
        if (!['int', 'float', 'double', 'text', 'date'].includes(schema.dataType)) throw new Error('物模型属性类型未定义，无法展示设备读数')
        text = String(value)
      }
      rows.push({ label, value: schema.dataSpecs?.unitName ? `${text} ${schema.dataSpecs.unitName}` : text })
    }
    visit(value, definition, [])
    return { rows, error: '' }
  } catch (error) {
    return { rows: [], error: error instanceof SyntaxError ? '设备上报资料格式错误，无法读取属性值，请联系管理员核查' : (error as Error).message }
  }
}
