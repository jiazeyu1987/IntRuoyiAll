import request from '@/config/axios'

// MES 工艺路线产品 VO
export interface ProRouteProductVO {
  id?: number // 编号
  routeId: number // 工艺路线编号
  routeVersionId?: number // 工艺路线版本编号
  itemId: number // 产品物料编号
  itemCode?: string // 产品编码
  itemName?: string // 产品名称
  specification?: string // 规格型号
  unitName?: string // 单位名称
  quantity?: number // 生产数量
  productionTime?: number // 生产用时
  timeUnitType?: number // 时间单位
  remark?: string // 备注
  createTime?: Date // 创建时间
}

export interface ProRouteProductCopyReqVO {
  routeVersionId?: number
  sourceRouteProductId: number
  targetItemId: number
  quantity?: number
  productionTime?: number
  timeUnitType?: number | string
  remark?: string
}

export interface ProRouteProductBindFromWorkOrdersReqVO {
  routeId: number
  routeVersionId: number
}

export interface ProRouteProductBindFromWorkOrdersRespVO {
  routeId: number
  routeName: string
  matchedCount: number
  existingCount: number
  createdCount: number
  conflictCount: number
  itemCodes: string[]
  creatableItemCodes: string[]
  existingItemCodes: string[]
  conflictItemCodes: string[]
}

export interface ProRouteProductByItemSaveReqVO {
  itemId: number
  routeId?: number | null
}

// MES 工艺路线产品 API
export const ProRouteProductApi = {
  // 按工艺路线查询产品列表
  getRouteProductListByRoute: async (routeId: number) => {
    return await request.get({ url: `/mes/pro/route-product/list-by-route?routeId=` + routeId })
  },

  // 查询工艺路线产品详情
  getRouteProduct: async (id: number) => {
    return await request.get({ url: `/mes/pro/route-product/get?id=` + id })
  },

  // 按产品物料查询当前工艺路线绑定
  getRouteProductByItem: async (itemId: number) => {
    return await request.get({ url: `/mes/pro/route-product/get-by-item`, params: { itemId } })
  },

  // 按产品物料保存或解除当前工艺路线绑定
  saveRouteProductByItem: async (data: ProRouteProductByItemSaveReqVO) => {
    return await request.post({ url: `/mes/pro/route-product/save-by-item`, data })
  },

  // 新增工艺路线产品
  createRouteProduct: async (data: ProRouteProductVO) => {
    return await request.post({ url: `/mes/pro/route-product/create`, data })
  },

  // 复制工艺路线产品
  copyRouteProduct: async (data: ProRouteProductCopyReqVO) => {
    return await request.post({ url: `/mes/pro/route-product/copy`, data })
  },

  // 预览从生产订单补齐工艺路线产品
  previewBindFromWorkOrders: async (data: ProRouteProductBindFromWorkOrdersReqVO) => {
    return await request.post({ url: `/mes/pro/route-product/preview-bind-from-work-orders`, data })
  },

  // 从生产订单补齐工艺路线产品
  bindFromWorkOrders: async (data: ProRouteProductBindFromWorkOrdersReqVO) => {
    return await request.post({ url: `/mes/pro/route-product/bind-from-work-orders`, data })
  },

  // 修改工艺路线产品
  updateRouteProduct: async (data: ProRouteProductVO) => {
    return await request.put({ url: `/mes/pro/route-product/update`, data })
  },

  // 删除工艺路线产品
  deleteRouteProduct: async (id: number, routeVersionId: number) => {
    return await request.delete({ url: `/mes/pro/route-product/delete`, params: { id, routeVersionId } })
  }
}
