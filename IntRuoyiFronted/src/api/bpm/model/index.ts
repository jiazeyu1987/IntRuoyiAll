import request from '@/config/axios'

export type ProcessDefinitionVO = {
  id: string
  version: number
  deploymentTime?: string
  deploymentTIme?: string
  suspensionState: number
  formType?: number
  formCustomCreatePath?: string
}

export type ModelVO = {
  id: number
  type?: number
  formName: string
  key: string
  name: string
  description: string
  category: string
  formType: number
  formId: number
  formCustomCreatePath: string
  formCustomViewPath: string
  processDefinition: ProcessDefinitionVO
  status: number
  remark: string
  createTime: string
  bpmnXml: string
  simpleModel?: unknown
}

export type ModelUpdateReqVO = Partial<ModelVO> & {
  id: number
}

export type ModelCreateReqVO = Partial<ModelVO> & {
  key: string
  name: string
  type: number
  formType: number
  visible: boolean
  startUserIds?: number[]
  startDeptIds?: number[]
  managerUserIds: number[]
  allowCancelRunningProcess?: boolean
  allowWithdrawTask?: boolean
  processIdRule?: {
    enable: boolean
    prefix?: string
    infix?: string
    postfix?: string
    length: number
  }
  autoApprovalType?: number
  titleSetting?: {
    enable: boolean
    title?: string
  }
  summarySetting?: {
    enable: boolean
    summary?: string[]
  }
  printTemplateSetting?: {
    enable: boolean
    template?: string
  }
}

export const getModelList = async (name: string | undefined) => {
  return await request.get({ url: '/bpm/model/list', params: { name } })
}

export const getModel = async (id: string) => {
  return await request.get({ url: '/bpm/model/get?id=' + id })
}

export const updateModel = async (data: ModelUpdateReqVO) => {
  return await request.put({ url: '/bpm/model/update', data: data })
}

// 批量修改流程分类的排序
export const updateModelSortBatch = async (ids: number[]) => {
  return await request.put({
    url: `/bpm/model/update-sort-batch`,
    params: {
      ids: ids.join(',')
    }
  })
}

export const updateModelBpmn = async (data: ModelVO) => {
  return await request.put({ url: '/bpm/model/update-bpmn', data: data })
}

// 任务状态修改
export const updateModelState = async (id: number, state: number) => {
  const data = {
    id: id,
    state: state
  }
  return await request.put({ url: '/bpm/model/update-state', data: data })
}

export const createModel = async (data: ModelCreateReqVO) => {
  return await request.post({ url: '/bpm/model/create', data: data })
}

export const deleteModel = async (id: number) => {
  return await request.delete({ url: '/bpm/model/delete?id=' + id })
}

export const deployModel = async (id: number) => {
  return await request.post({ url: '/bpm/model/deploy?id=' + id })
}

export const cleanModel = async (id: number) => {
  return await request.delete({ url: '/bpm/model/clean?id=' + id })
}
