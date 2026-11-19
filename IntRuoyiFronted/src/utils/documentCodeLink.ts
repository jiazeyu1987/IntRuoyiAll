// Explicit entity mapping: never infer a document type from its number.
export const documentTargets = {
  'mes-sales-notice': { path: '/mes/wm/sales-notice', permission: 'mes:wm-sales-notice:query' },
  'srm-purchase-order': { path: '/srm/purchase-order', permission: 'srm:purchase-order:query' },
  'erp-purchase-order': { path: '/erp/purchase/order', permission: 'erp:purchase-order:query' },
  'erp-purchase-in': { path: '/erp/purchase/in', permission: 'erp:purchase-in:query' },
  'erp-purchase-return': { path: '/erp/purchase/return', permission: 'erp:purchase-return:query' },
  'erp-sale-order': { path: '/erp/sale/order', permission: 'erp:sale-out:query' },
  'erp-sale-out': { path: '/erp/sale/out', permission: 'erp:sale-out:query' },
  'erp-sale-return': { path: '/erp/sale/return', permission: 'erp:sale-return:query' }
} as const
export type DocumentType = keyof typeof documentTargets
