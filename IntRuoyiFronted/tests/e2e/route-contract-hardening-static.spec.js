const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const backendRoot = path.resolve(frontendRoot, '..', 'ruoyi-vue-pro')

const readFrontend = (relativePath) =>
  fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
const readBackend = (relativePath) =>
  fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')
const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const staleFrontendRoutes = [
  ['src/api/bpm/processExpression/index.ts', '/bpm/process-expression/export-excel'],
  [
    'src/api/bpm/processInstance/index.ts',
    '/bpm/process-instance/get-form-fields-permission'
  ],
  ['src/api/bpm/task/index.ts', '/bpm/task/my-todo'],
  ['src/api/mes/wm/miscissue/line/index.ts', '/mes/wm/misc-issue-line/list-by-issue-id'],
  ['src/api/mes/wm/miscissue/index.ts', '/mes/wm/misc-issue/check-quantity'],
  [
    'src/api/mes/wm/productreceipt/detail/index.ts',
    '/mes/wm/product-receipt-detail/list'
  ],
  ['src/api/pay/channel/index.ts', '/pay/channel/page'],
  ['src/api/pay/channel/index.ts', '/pay/channel/export-excel'],
  ['src/api/pay/refund/index.ts', '/pay/refund/create'],
  ['src/api/pay/refund/index.ts', '/pay/refund/update'],
  ['src/api/pay/refund/index.ts', '/pay/refund/delete'],
  [
    'src/api/mall/promotion/coupon/couponTemplate.ts',
    '/promotion/coupon-template/export-excel'
  ]
]

for (const [relativePath, route] of staleFrontendRoutes) {
  assert.doesNotMatch(
    readFrontend(relativePath),
    new RegExp(`${escapeRegExp(route)}(?:\\?|['"\`])`),
    `Unused frontend API must not keep a backend-missing route: ${route}`
  )
}

const activeContracts = [
  {
    frontend: 'src/api/iot/ota/firmware/index.ts',
    frontendRoute: '/iot/ota/firmware/delete',
    backend:
      'yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/controller/admin/ota/IotOtaFirmwareController.java',
    backendMapping: '@DeleteMapping("/delete")'
  },
  {
    frontend: 'src/api/mall/product/comment.ts',
    frontendRoute: '/product/comment/get',
    backend:
      'yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/comment/ProductCommentController.java',
    backendMapping: '@GetMapping("/get")'
  },
  {
    frontend: 'src/api/system/mail/log/index.ts',
    frontendRoute: '/system/mail-log/export-excel',
    backend:
      'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/mail/MailLogController.java',
    backendMapping: '@GetMapping("/export-excel")'
  }
]

for (const contract of activeContracts) {
  assert.ok(
    readFrontend(contract.frontend).includes(contract.frontendRoute),
    `Frontend active route is missing: ${contract.frontendRoute}`
  )
  assert.ok(
    readBackend(contract.backend).includes(contract.backendMapping),
    `Backend mapping is missing for active route: ${contract.frontendRoute}`
  )
}

console.log('PASS: frontend-backend route contract hardening')
