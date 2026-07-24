const fs = require('fs')
const path = require('path')

const contractsPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/product/contracts.ts'
)
const contractsSource = fs.readFileSync(contractsPath, 'utf8')

if (contractsSource.includes("throw new Error('当前登录用户缺少归属部门，无法解析主管审批人')")) {
  throw new Error(`found obsolete no-dept approval route error in ${contractsPath}`)
}

if (!contractsSource.includes('submitterDeptId: null')) {
  throw new Error(`missing no-dept submitter fallback in ${contractsPath}`)
}

if (!contractsSource.includes('supervisorUserId: null')) {
  throw new Error(`missing no-dept supervisor skip in ${contractsPath}`)
}

if (!contractsSource.includes("supervisorName: '已跳过，直接进入企宣审批'")) {
  throw new Error(`missing no-dept supervisor label in ${contractsPath}`)
}

if (!contractsSource.includes('skipSupervisorReview: true')) {
  throw new Error(`missing no-dept skip flag in ${contractsPath}`)
}

const indexPath = path.resolve(__dirname, '../../src/views/showroom-admin/index.vue')
const indexSource = fs.readFileSync(indexPath, 'utf8')

if (!indexSource.includes('productCompanyOptions.value.length > 0 && productApprovalRoutePreview.value')) {
  throw new Error(`missing approval preview readiness guard in ${indexPath}`)
}

console.log('PASS: showroom product approval route allows no-dept users to skip supervisor review')
