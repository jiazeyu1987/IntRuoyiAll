// 与后端 ErpDataPermission.forSyncType 保持相同的页签边界。
const permissions: Record<string, string> = {
  PRODUCT: 'erp:data-product:operate', STOCK: 'erp:data-stock:operate',
  INVENTORY_DETAIL: 'erp:data-inventory:operate', BOM: 'erp:data-bom:operate', PRODUCTION_MATERIAL_LIST: 'erp:data-material:operate',
  PRODUCTION_PICK_LIST: 'erp:data-pick:operate',
  PRODUCTION_REPLENISHMENT_LIST: 'erp:data-replenishment:operate',
  PRODUCTION_ORDER: 'erp:data-order:operate', PURCHASE_ORDER: 'erp:data-config:operate',
  SALE_ORDER: 'erp:data-config:operate', STOCK_MOVE: 'erp:data-config:operate'
}
export const erpSyncPermission = (syncType: string): string => {
  const permission = permissions[syncType]
  if (!permission) throw new Error(`不支持的ERP同步类型：${syncType}`)
  return permission
}
export const requiresExplicitErpGrant = (permission: string): boolean =>
  ['erp:data-', 'erp:product:', 'erp:stock:', 'erp:bom-list:',
    'erp:production-material-list:', 'erp:production-pick-list:',
    'erp:production-replenishment-list:', 'erp:inventory-list:',
    'mes:pro-work-order:', 'erp:kingdee-sync:', 'erp:kingdee-config:']
    .some((prefix) => permission.startsWith(prefix))
