import request from '@/config/axios'

export interface SrmCodeRuleVO {
  id?: number
  ruleCode: string
  ruleName?: string
  targetForm: string
  prefix: string
  datePattern: string
  dateSegmentEnabled: boolean
  serialWidth: number
  step: number
  minSerial: number
  maxSerial: number
  separator?: string
  enabled: boolean
  remark?: string
  createTime?: string
}

export interface SrmCodeRulePageReqVO extends PageParam {
  ruleCode?: string
  targetForm?: string
  enabled?: boolean
}

export interface SrmCodeRuleEnableReqVO {
  id: number
  enabled: boolean
}

export const srmCodeRuleTargetFormOptions = [
  { label: '采购计划', value: 'PROCUREMENT_PLAN' },
  { label: '采购计划行', value: 'PROCUREMENT_PLAN_LINE' },
  { label: '采购订单协同单', value: 'PURCHASE_ORDER' },
  { label: '采购订单协同行', value: 'PURCHASE_ORDER_LINE' },
  { label: '采购订单变更单', value: 'PURCHASE_ORDER_CHANGE' },
  { label: '框架采购计划', value: 'FRAMEWORK_PLAN' },
  { label: '框架协议', value: 'FRAMEWORK_AGREEMENT' },
  { label: '招标项目', value: 'TENDER_PROJECT' },
  { label: '非招标项目', value: 'NON_TENDER_PROJECT' },
  { label: '采购合同', value: 'PROCUREMENT_CONTRACT' },
  { label: '专家抽取申请', value: 'EXPERT_DRAW_APPLICATION' }
] as const

export const SrmCodeRuleApi = {
  getCodeRulePage: async (params: SrmCodeRulePageReqVO) => {
    return await request.get<PageResult<SrmCodeRuleVO[]>>({
      url: '/srm/code-rule/page',
      params
    })
  },

  getCodeRule: async (id: number) => {
    return await request.get<SrmCodeRuleVO>({
      url: `/srm/code-rule/get?id=${id}`
    })
  },

  createCodeRule: async (data: SrmCodeRuleVO) => {
    return await request.post({
      url: '/srm/code-rule/create',
      data
    })
  },

  updateCodeRule: async (data: SrmCodeRuleVO) => {
    return await request.put({
      url: '/srm/code-rule/update',
      data
    })
  },

  enableCodeRule: async (data: SrmCodeRuleEnableReqVO) => {
    return await request.put({
      url: '/srm/code-rule/enable',
      data
    })
  }
}
