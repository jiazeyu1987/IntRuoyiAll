import request from '@/config/axios'

export interface ErpKingdeeConfigVO {
  baseUrl: string
  acctId: string
  username: string
  password: string
  lcid: number
  product: {
    queryLimit: number
  }
  productionOrder: {
    queryLimit: number
    templateBillNo: string
  }
  purchaseOrder: {
    purchaseOrgNumber: string
    queryDays: number
    queryLimit: number
  }
  saleOrder: {
    queryDays: number
    queryLimit: number
  }
}

export interface ErpKingdeeExternalWritePermissionVO {
  enabled: boolean
}

export type ErpKingdeeConnectionType = 'TEST' | 'PRODUCTION'

export interface ErpKingdeeConnectionOptionVO {
  connectionType: ErpKingdeeConnectionType
  connectionName: string
}

export interface ErpKingdeeActiveConnectionVO {
  activeConnectionType: ErpKingdeeConnectionType
  activeConnectionName: string
  options: ErpKingdeeConnectionOptionVO[]
}

export interface ErpKingdeeActiveConnectionSaveReqVO {
  connectionType: ErpKingdeeConnectionType
}

export const ErpKingdeeConfigApi = {
  getConfig: async () => {
    return await request.get({ url: `/erp/kingdee-config/get` })
  },

  saveConfig: async (data: ErpKingdeeConfigVO) => {
    return await request.put({ url: `/erp/kingdee-config/save`, data })
  },

  getActiveConnection: async (): Promise<ErpKingdeeActiveConnectionVO> => {
    return await request.get({ url: `/erp/kingdee-config/active-connection` })
  },

  updateActiveConnection: async (
    data: ErpKingdeeActiveConnectionSaveReqVO
  ): Promise<ErpKingdeeActiveConnectionVO> => {
    return await request.put({ url: `/erp/kingdee-config/active-connection`, data })
  },

  getExternalWritePermission: async (): Promise<ErpKingdeeExternalWritePermissionVO> => {
    return await request.get({ url: `/infra/external-write-permission/erp` })
  },

  updateExternalWritePermission: async (data: ErpKingdeeExternalWritePermissionVO) => {
    return await request.put({ url: `/infra/external-write-permission/erp`, data })
  }
}
