const fs = require('fs')
const path = require('path')
const assert = require('assert')

const frontendRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const routePath = path.join(frontendRoot, 'src/router/modules/remaining.ts')
const detailPagePath = path.join(frontendRoot, 'src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue')
const detailPanelPath = path.join(frontendRoot, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue')

const page = fs.readFileSync(pagePath, 'utf8')
const routes = fs.readFileSync(routePath, 'utf8')

assert.ok(fs.existsSync(detailPagePath), 'must provide standalone active order submission detail page')
assert.ok(fs.existsSync(detailPanelPath), 'must extract reusable active order submission detail panel')
assert.ok(routes.includes('MesProcessPoolActiveOrderSubmissionDetail'), 'router must register standalone active order submission detail route')
assert.ok(routes.includes('ActiveOrderSubmissionDetailPage.vue'), 'router must load standalone detail page component')
assert.ok(page.includes('router.push'), 'detail entry must navigate through router.push')
assert.ok(page.includes('data-team-leader-active-order-detail'), 'row detail entry marker must stay for E2E')
assert.ok(!page.includes('data-team-leader-active-order-detail-dialog'), 'workbench must not render active-order detail dialog')
assert.ok(!page.includes('activeOrderDetailVisible.value = true'), 'workbench must not open active-order detail as dialog')

console.log('PASS: active-order detail standalone page static contract')
