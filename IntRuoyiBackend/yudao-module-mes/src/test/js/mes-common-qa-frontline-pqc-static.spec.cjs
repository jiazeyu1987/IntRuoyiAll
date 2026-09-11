const assert = require('assert');
const fs = require('fs');
const path = require('path');

const moduleRoot = path.resolve(__dirname, '..', '..', '..');
const read = (...segments) => fs.readFileSync(path.join(moduleRoot, ...segments), 'utf8');

const activeOrderService = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'service', 'pro', 'processpool', 'team', 'MesTeamLeaderActiveOrderServiceImpl.java');
const frontlinePqcService = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'service', 'pro', 'frontline', 'MesFrontlinePqcContextServiceImpl.java');
const regulationDO = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'dal', 'dataobject', 'qa', 'regulation', 'MesQaInspectionRegulationDO.java');
const bindingDO = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'dal', 'dataobject', 'qa', 'regulation', 'MesQaCommonRegulationProductBindingDO.java');
const bindingMapper = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'dal', 'mysql', 'qa', 'regulation', 'MesQaCommonRegulationProductBindingMapper.java');
const regulationMapper = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'dal', 'mysql', 'qa', 'regulation', 'MesQaInspectionRegulationMapper.java');
const frontlinePqcRespVO = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'controller', 'admin', 'pro', 'feedback', 'vo', 'frontline', 'MesFrontlinePqcProcessRespVO.java');
const migration = fs.readFileSync(path.resolve(moduleRoot, '..', 'sql', 'mysql',
  '20260909_mes_qa_common_regulation_product_binding.sql'), 'utf8');
const preflight = fs.readFileSync(path.resolve(moduleRoot, '..', 'sql', 'mysql',
  '20260814_mes_c015_route_dcc_qa_reconciliation_preflight.sql'), 'utf8');
const postflight = fs.readFileSync(path.resolve(moduleRoot, '..', 'sql', 'mysql',
  '20260814_mes_c015_route_dcc_qa_reconciliation_postflight.sql'), 'utf8');
const frontendPqcPanel = fs.readFileSync(path.resolve(moduleRoot, '..', '..', 'IntRuoyiFronted',
  'src', 'views', 'mes', 'pro', 'feedback', 'FrontlineFixedTemplatePanel.vue'), 'utf8');
const frontendFeedbackApi = fs.readFileSync(path.resolve(moduleRoot, '..', '..', 'IntRuoyiFronted',
  'src', 'api', 'mes', 'pro', 'feedback', 'index.ts'), 'utf8');

assert(regulationDO.includes('OWNER_MODULE_MES_QA_COMMON'),
  'QA regulation DO must expose the MES_QA_COMMON owner module for common packaging regulations.');
assert(/selectByDccProjectCodeId[\s\S]*OWNER_MODULE_MES_QA/.test(regulationMapper),
  'Product QA DCC lookup must filter owner_module = MES_QA so common regulations do not pollute product QA flows.');
assert(/selectListByDccProjectCodeIds[\s\S]*OWNER_MODULE_MES_QA/.test(regulationMapper),
  'Product QA DCC list lookup must filter owner_module = MES_QA so common regulations do not pollute product QA statuses.');

assert(bindingDO.includes('mes_qa_common_regulation_product_binding')
  && bindingDO.includes('SCOPE_COMMON_PACKAGING')
  && bindingDO.includes('STATUS_ENABLED'),
  'Common QA product binding DO must model the formal product-current-version binding.');
assert(bindingMapper.includes('selectEnabledByProductId')
  && bindingMapper.includes('selectEnabledListByProductIds'),
  'Common QA product binding mapper must provide enabled binding lookups.');
assert(migration.includes('mes_qa_common_regulation_product_binding')
  && migration.includes('uk_mes_qa_common_binding_active_product'),
  'Common QA product binding migration must enforce one enabled binding per product.');

assert(activeOrderService.includes('MesQaCommonRegulationProductBindingMapper'),
  'Active order service must inject the common QA product binding mapper.');
assert(activeOrderService.includes('resolveCommonQaVersionSource'),
  'Active order service must resolve the product-bound common QA version.');
assert(activeOrderService.includes('ActiveOrderQaVersionSource'),
  'Active order service must preserve per-source QA version provenance.');
assert(activeOrderService.includes('for (ActiveOrderQaVersionSource source : qaSource.sources())'),
  'PQC task planning must iterate product QA and common QA sources.');
assert(activeOrderService.includes('buildPqcTask(activeOrder, plan.qaProcess(), plan.version()'),
  'Generated PQC tasks must store each task source version, not only the product QA version.');

assert(frontlinePqcService.includes('resolveLockedQaSources'),
  'Frontline PQC process list must resolve the combined locked QA sources.');
assert(frontlinePqcService.includes('resolveLockedQaSources(activeOrder, workOrder, activeOrderTasks)'),
  'Frontline PQC process list must replay common QA sources from active-order task versions, not current product binding.');
assert(frontlinePqcService.includes('resolveLockedCommonQaSourcesFromTasks'),
  'Frontline PQC process list must derive common QA locked sources from persisted PQC task versions.');
assert(!frontlinePqcService.includes('selectEnabledByProductId'),
  'Frontline PQC process list must not resolve common QA from the product current binding.');
assert(frontlinePqcService.includes('LOCKED_QA_VERSION_STATUSES')
  && frontlinePqcService.includes('"PUBLISHED", "RETIRED"')
  && !/LOCKED_QA_VERSION_STATUSES[\s\S]*"DRAFT"/.test(frontlinePqcService),
  'Frontline PQC common-source replay must accept PUBLISHED/RETIRED locked versions and reject DRAFT.');
assert(frontlinePqcService.includes('PqcSourceProcessIdentity'),
  'Frontline PQC grouping must key tasks by version plus QA process.');
assert(/new\s+PqcSourceProcessIdentity\(\s*qaSource\.getPublishedVersionId\(\),\s*qaProcess\.getQaProcessId\(\)\s*\)/s
    .test(frontlinePqcService),
  'Frontline PQC display must bind each process to the matching source version.');
assert(frontlinePqcService.includes('respVO.setRegulationId(qaSource.getRegulationId())'),
  'Frontline PQC response must expose common regulation id for common-source processes.');
assert(frontlinePqcRespVO.includes('regulationName')
  && frontlinePqcRespVO.includes('regulationSourceType'),
  'Frontline PQC response must expose regulation name and source type for visible common-packaging distinction.');
assert(frontlinePqcService.includes('respVO.setRegulationName(qaSource.getRegulationName())')
  && frontlinePqcService.includes('respVO.setRegulationSourceType('),
  'Frontline PQC service must populate source name/type for product and common QA processes.');
assert(frontendFeedbackApi.includes('regulationName?: string')
  && frontendFeedbackApi.includes('regulationSourceType?:'),
  'Frontline PQC API type must include regulation source display fields.');
assert(/formatProcessLabel[\s\S]*regulationSourceType[\s\S]*通用包装/.test(frontendPqcPanel),
  'Frontline PQC process label must visibly distinguish common packaging regulations.');
assert(preflight.includes("owner_module")
  && preflight.toLowerCase().includes("mes_qa"),
  'C015 preflight generated-column check must recognize owner-scoped product QA identity.');
assert(postflight.includes("owner_module")
  && postflight.toLowerCase().includes("mes_qa"),
  'C015 postflight generated-column check must recognize owner-scoped product QA identity.');
