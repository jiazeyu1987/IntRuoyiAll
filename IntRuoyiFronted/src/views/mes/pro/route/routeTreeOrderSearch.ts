import { ProWorkOrderApi } from '@/api/mes/pro/workorder'

/** 查询所有匹配订单的产品，避免只取第一页而遗漏流程。 */
export const findOrderProductCodes = async (code: string, isCurrent: () => boolean) => {
  const productCodes = new Set<string>()
  let orderCount = 0
  for (let pageNo = 1; ; pageNo++) {
    const page = await ProWorkOrderApi.getWorkOrderPage({ code: code.trim(), pageNo, pageSize: 100 })
    if (!isCurrent()) return undefined
    if (!page.list.length && orderCount < page.total) throw new Error('生产订单查询结果已变化，请重新搜索')
    for (const order of page.list) {
      if (!order.productCode?.trim()) throw new Error(`生产订单 ${order.code} 缺少产品编码`)
      productCodes.add(order.productCode.trim().toLowerCase())
    }
    orderCount += page.list.length
    if (orderCount >= page.total) return { productCodes: [...productCodes], orderCount }
  }
}
