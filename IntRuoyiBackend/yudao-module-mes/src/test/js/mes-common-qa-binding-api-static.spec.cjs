const assert = require('assert')
const fs = require('fs')
const path = require('path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const repoRoot = path.resolve(moduleRoot, '..', '..')
const read = (...segments) => fs.readFileSync(path.join(moduleRoot, ...segments), 'utf8')
const readRepo = (...segments) => fs.readFileSync(path.join(repoRoot, ...segments), 'utf8')

const controller = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'controller', 'admin', 'qa', 'regulation', 'MesQaInspectionRegulationController.java')
const service = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'service', 'qa', 'regulation', 'MesQaInspectionRegulationServiceImpl.java')
const wordImportService = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'service', 'qa', 'regulation', 'MesQaInspectionRegulationWordImportService.java')
const serviceInterface = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'service', 'qa', 'regulation', 'MesQaInspectionRegulationService.java')
const bindingMapper = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'dal', 'mysql', 'qa', 'regulation', 'MesQaCommonRegulationProductBindingMapper.java')
const serviceTest = read('src', 'test', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'service', 'qa', 'regulation', 'MesQaInspectionRegulationServiceTest.java')
const regulationMapper = read('src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes',
  'dal', 'mysql', 'qa', 'regulation', 'MesQaInspectionRegulationMapper.java')
const voDir = path.join(moduleRoot, 'src', 'main', 'java', 'cn', 'iocoder', 'yudao',
  'module', 'mes', 'controller', 'admin', 'qa', 'regulation', 'vo')
const frontendApi = readRepo('IntRuoyiFronted', 'src', 'api', 'mes', 'qc', 'template', 'index.ts')
const qaPage = readRepo('IntRuoyiFronted', 'src', 'views', 'mes', 'pro', 'processPool', 'QaRegulationPage.vue')

assert(controller.includes('/common-binding/current')
  && controller.includes('/common-binding/published-versions')
  && controller.includes('/common-binding/bind')
  && controller.includes('/common-binding/unbind'),
  'QA regulation controller must expose current/list/bind/unbind common regulation binding endpoints.')
assert(controller.match(/common-binding\/bind[\s\S]*mes:qc-template:update/)
  && controller.match(/common-binding\/unbind[\s\S]*mes:qc-template:update/),
  'Common regulation bind/unbind endpoints must require QA update permission.')

assert(serviceInterface.includes('getCurrentCommonRegulationBinding')
  && serviceInterface.includes('listCommonRegulationPublishedVersions')
  && serviceInterface.includes('bindCommonRegulationVersion')
  && serviceInterface.includes('unbindCommonRegulation'),
  'QA regulation service contract must expose binding read, version options, bind and unbind methods.')
assert(service.includes('requireProductIdFromDccProjectCode')
  && service.includes('projectCode.getProductMasterId()'),
  'Binding service must resolve the formal product id from selected DCC project code productMasterId.')
assert(service.includes('OWNER_MODULE_MES_QA_COMMON')
  && service.includes('STATUS_PUBLISHED')
  && service.includes('requireCommonPublishedVersion'),
  'Binding service must only allow published MES_QA_COMMON regulation versions.')
assert(service.includes('resolveOwnerModule(reqVO.getOwnerModule())')
  && service.includes('regulationMapper.selectByDccProjectCodeId(dccProjectCode.getId(), ownerModule)')
  && service.includes('.ownerModule(ownerModule)'),
  'QA regulation save/publish must persist explicit ownerModule so common regulations do not become product QA.')
assert(/importWordDraft\(\s*MultipartFile file,\s*Long dccProjectCodeId,\s*String ownerModule,\s*boolean publishAfterImport\)/.test(wordImportService)
  && wordImportService.includes('OWNER_MODULE_MES_QA_COMMON')
  && wordImportService.includes('publishAfterImport')
  && wordImportService.includes('publishedVersion.getPublishedVersionId()'),
  'Word import must support explicit MES_QA_COMMON import-and-publish for common regulation documents.')
assert(service.includes('disableEnabledCommonRegulationBinding')
  && service.includes('STATUS_DISABLED')
  && service.includes('STATUS_ENABLED'),
  'Binding service must disable an existing enabled product binding before inserting the new binding.')
assert(serviceTest.includes('MesQaCommonRegulationProductBindingMapper')
  && /new MesQaInspectionRegulationServiceImpl\([\s\S]*commonRegulationProductBindingMapper\)/.test(serviceTest),
  'Existing QA regulation service tests must inject the common regulation binding mapper mock.')

assert(bindingMapper.includes('selectEnabledByProductId')
  && bindingMapper.includes('updateEnabledStatusByProductId')
  && bindingMapper.includes('selectByProductIdOrderByCreateTimeDesc'),
  'Binding mapper must support current lookup, disabling previous binding, and history lookup.')
assert(regulationMapper.includes('selectCommonList')
  && regulationMapper.includes('OWNER_MODULE_MES_QA_COMMON'),
  'Regulation mapper must provide an owner-scoped common regulation list without changing product QA lookups.')

const requiredVos = [
  'MesQaCommonRegulationBindingRespVO.java',
  'MesQaCommonRegulationBindReqVO.java',
  'MesQaCommonRegulationVersionOptionRespVO.java'
]
for (const file of requiredVos) {
  assert(fs.existsSync(path.join(voDir, file)), `${file} must exist for the binding API contract.`)
}

assert(frontendApi.includes('QaCommonRegulationBindingVO')
  && frontendApi.includes('getCurrentCommonRegulationBinding')
  && frontendApi.includes('listCommonRegulationPublishedVersions')
  && frontendApi.includes('bindCommonRegulationVersion')
  && frontendApi.includes('unbindCommonRegulation'),
  'Frontend API wrapper must expose common regulation binding endpoints.')
assert(frontendApi.includes('ownerModule?: QaInspectionRegulationOwnerModule')
  && frontendApi.includes('publishAfterImport?: boolean'),
  'Frontend API contract must allow explicit common regulation Word import and publish.')
assert(!qaPage.includes('通用检验规程绑定接口尚未接入')
  && !qaPage.includes('notifyCommonRegulationBindingApiPending'),
  'QA page must remove the pending placeholder and use the real binding API.')
assert(/selectedCommonRegulationVersionId[\s\S]*commonRegulationVersionOptions/.test(qaPage)
  && /handleBindCommonRegulationVersion/.test(qaPage)
  && /handleUnbindCommonRegulation/.test(qaPage),
  'QA page must load common versions, allow selecting a published version, and save/unbind it.')
assert(qaPage.includes('data-qa-regulation-common-word-import')
  && qaPage.includes('openCommonQaWordImportDialog')
  && qaPage.includes("ownerModule: importOwnerModule")
  && qaPage.includes("publishAfterImport: importCommonRegulation")
  && qaPage.includes("qaWordImportOwnerModule.value = 'MES_QA_COMMON'"),
  'QA common tab must provide a formal page entry to import and publish common regulation Word documents.')
