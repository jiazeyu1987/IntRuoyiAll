export interface ImportModel {
  id: number | string
  key: string
  type?: number | string
  bpmnXml?: string
  [key: string]: unknown
}

export const buildBpmnImport = <T extends ImportModel>(before: T, current: T, xml: string, processIds: string[]): T => {
  if (Number(before.type) !== 10 || Number(current.type) !== 10) throw new Error('仅 BPMN 模型支持导入流程图')
  if (!xml.trim() || processIds.length !== 1 || processIds[0] !== before.key) throw new Error('流程图必须仅包含与当前模型标识相同的一个流程')
  if (JSON.stringify(before) !== JSON.stringify(current)) throw new Error('模型或管理配置已变化，请重新打开后导入')
  return { ...current, bpmnXml: xml }
}
