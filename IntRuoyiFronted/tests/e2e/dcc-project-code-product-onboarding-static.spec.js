const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const apiSource = readSource('src/api/dcc/controlledFile/projectCodes.ts')
const panelSource = readSource(
  'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'
)

assert.equal(
  packageJson.scripts['e2e:dcc:project-code-product-onboarding:static'],
  'node tests/e2e/dcc-project-code-product-onboarding-static.spec.js',
  'package.json 必须暴露 DCC 产品建档闭环静态契约脚本'
)

for (const apiToken of [
  'export interface DccProductOnboardingCreateReqVO',
  'export interface DccProductOnboardingRespVO',
  'createProductOnboardingRequest',
  '/dcc/product-onboarding-requests/create',
  'approveProductOnboardingRequest',
  '/dcc/product-onboarding-requests/${id}/approve'
]) {
  assert.ok(apiSource.includes(apiToken), `产品建档 API 契约必须包含 ${apiToken}`)
}

for (const panelToken of [
  '产品建档申请',
  'data-testid="dcc-product-onboarding-open"',
  'data-testid="dcc-product-onboarding-submit"',
  'data-testid="dcc-product-onboarding-approve"',
  'getProductSimpleList',
  'createProductOnboardingRequest',
  'approveProductOnboardingRequest',
  'productOnboardingFormData',
  'productMasterId',
  'dccProductCode',
  'productNameCn',
  '审批通过后生成 DCC 项目代码并绑定 MDM 产品'
]) {
  assert.ok(panelSource.includes(panelToken), `项目代码页必须提供产品建档闭环能力：${panelToken}`)
}

assert.ok(
  !panelSource.includes('catch (error) {\n  }'),
  '产品建档链路不得吞掉后端错误'
)
