const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const backendRoot = path.resolve(repoRoot, '..', 'ruoyi-vue-pro')

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function readBackend(relativePath) {
  return fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')
}

const api = read('src/api/srm/procurement-contract/index.ts')
const page = read('src/views/srm/procurement-contract/index.vue')
const sql = readBackend('sql/mysql/20260620_srm_d9_1_contract.sql')
const service = readBackend('yudao-module-srm/src/main/java/cn/iocoder/yudao/module/srm/service/contract/SrmProcurementContractServiceImpl.java')
const mapper = readBackend('yudao-module-srm/src/main/java/cn/iocoder/yudao/module/srm/dal/mysql/procurement/SrmSourcingProjectMapper.java')

for (const snippet of [
  '/srm/procurement-contract/page',
  '/srm/procurement-contract/get',
  '/srm/procurement-contract/create',
  '/srm/procurement-contract/cancel',
  '/srm/procurement-contract/delete',
  'SrmProcurementContractPaymentVO',
  'SrmProcurementContractSigningVO',
  'SrmProcurementContractAttachmentVO'
]) {
  assert(api.includes(snippet), `missing API endpoint or type: ${snippet}`)
}

for (const snippet of [
  '创建采购合同',
  '付款约定',
  '签署信息',
  '合同附件',
  '作废采购合同',
  '采购合同已创建并回写来源项目',
  '来源项目已恢复可建合同状态',
  "v-hasPermi=\"['srm:procurement-contract:create']\"",
  "v-hasPermi=\"['srm:procurement-contract:cancel']\"",
  "v-hasPermi=\"['srm:procurement-contract:delete']\"",
  "throw new Error('合同到期日期不能早于生效日期。')"
]) {
  assert(page.includes(snippet), `missing page contract: ${snippet}`)
}

for (const snippet of [
  "component` = 'srm/procurement-contract/index'",
  "'srm:procurement-contract:query'",
  "'srm:procurement-contract:create'",
  "'srm:procurement-contract:cancel'",
  "'srm:procurement-contract:delete'",
  'srm_procurement_contract',
  'srm_procurement_contract_payment',
  'srm_procurement_contract_signing',
  'srm_procurement_contract_attachment',
  "'PROCUREMENT_CONTRACT'"
]) {
  assert(sql.includes(snippet), `missing SQL contract: ${snippet}`)
}

for (const snippet of [
  'PROCUREMENT_CONTRACT_PAYMENT_REQUIRED',
  'PROCUREMENT_CONTRACT_SIGNING_REQUIRED',
  'PROCUREMENT_CONTRACT_ATTACHMENT_REQUIRED',
  'SrmSourcingProjectStatusEnum.CONTRACT_CREATED',
  'SrmSourcingProjectStatusEnum.DEAL_CONFIRMED',
  'SrmSourcingProjectStatusEnum.WINNING_CONFIRMED',
  'restoreSource(contract)',
  'clearContractAndRestoreStatus'
]) {
  assert(service.includes(snippet), `missing service guard: ${snippet}`)
}

assert(mapper.includes('.set(SrmSourcingProjectDO::getContractId, null)'), 'source writeback must explicitly clear contract_id')
assert(!/catch\s*\{\s*\}/.test(page), 'page must not contain empty catch blocks')
assert(!/mock|fallback|默认成功|空列表/.test(`${api}\n${page}`), 'frontend must not introduce mock/fallback/default success')
assert(!/INSERT INTO `ERP_|UPDATE `ERP_|DELETE FROM `ERP_|INSERT INTO `K3_|UPDATE `K3_|DELETE FROM `K3_|FINANCE_/i.test(sql), 'SQL must not write ERP/K3/finance')

console.log('PASS: SRM D9-1 procurement contract static contract')
