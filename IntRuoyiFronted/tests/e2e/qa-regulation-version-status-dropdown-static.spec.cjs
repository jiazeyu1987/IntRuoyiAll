const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const root = path.resolve(__dirname, '..', '..')
const pagePath = path.join(root, 'src/views/mes/pro/processpool/QaRegulationPage.vue')
const apiPath = path.join(root, 'src/api/mes/qc/template/index.ts')
const controllerPath = path.join(
  root,
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/MesQaInspectionRegulationController.java'
)
const servicePath = path.join(
  root,
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationService.java'
)
const serviceImplPath = path.join(
  root,
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
)

const page = fs.readFileSync(pagePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')
const controller = fs.readFileSync(controllerPath, 'utf8')
const service = fs.readFileSync(servicePath, 'utf8')
const serviceImpl = fs.readFileSync(serviceImplPath, 'utf8')

assert.match(
  api,
  /export interface QaInspectionRegulationVersionOptionVO[\s\S]*versionId:\s*number[\s\S]*versionNo:\s*string[\s\S]*lifecycleStatus:\s*string[\s\S]*currentPublished:\s*boolean/,
  'API wrapper must expose a formal QA regulation version option contract.'
)
assert.match(
  api,
  /listQaRegulationVersions:\s*async\s*\(\s*dccProjectCodeId:\s*number\s*\)[\s\S]*\/mes\/qa\/inspection-regulation\/versions[\s\S]*params:\s*\{\s*dccProjectCodeId\s*\}/,
  'API wrapper must call the formal version list endpoint.'
)

const headerBlock = page.match(
  /<div class="qa-regulation-page__version-publish"[\s\S]*?<el-button[\s\S]*?data-qa-regulation-header-save/
)
assert.ok(headerBlock, 'QA regulation header block must be locatable.')
const header = headerBlock[0]
assert.match(
  header,
  /<el-select[\s\S]*v-model="selectedQaRegulationVersionId"[\s\S]*data-qa-regulation-version-dropdown[\s\S]*@change="handleQaRegulationVersionChange"/,
  'Header version control must be a dropdown bound to the selected formal version id.'
)
assert.doesNotMatch(
  header,
  /<el-input[\s\S]*v-model="qaRegulationDraft\.versionNo"/,
  'Header version control must not remain a free text input.'
)
assert.match(
  header,
  /v-for="version in qaRegulationVersionOptions"[\s\S]*:label="formatQaRegulationVersionOption\(version\)"[\s\S]*:value="version\.versionId"/,
  'Version dropdown must render every formal version option.'
)
assert.match(
  header,
  /data-qa-regulation-selected-version-status[\s\S]*resolveQaRegulationLifecycleStatusTagType\(selectedQaRegulationVersionStatus\)[\s\S]*qaSelectedVersionStatusText/,
  'Status tag must render the selected version lifecycle status text.'
)

assert.match(
  page,
  /const\s+QA_REGULATION_LIFECYCLE_STATUS_LABELS[\s\S]*PUBLISHED:\s*'已发布'[\s\S]*RETIRED:\s*'已作废'[\s\S]*DRAFT:\s*'草稿'/,
  'Frontend must map QA regulation lifecycle statuses to the required Chinese labels.'
)
assert.match(
  page,
  /const\s+loadQaRegulationVersionOptions\s*=\s*async\s*\(dccProjectCodeId:\s*number\)[\s\S]*QcTemplateApi\.listQaRegulationVersions\(dccProjectCodeId\)/,
  'Frontend must load all formal versions from the dedicated endpoint.'
)
assert.match(
  page,
  /const\s+handleQaRegulationVersionChange\s*=\s*async\s*\(versionId\?:\s*number\)[\s\S]*QcTemplateApi\.getPublishedQaRegulationVersion\(dccProjectCodeId,\s*versionId\)/,
  'Selecting a version must read the version detail by formal version id.'
)
assert.match(
  page,
  /const\s+shouldLoadQaEquipmentBindingsForSelectedVersion\s*=\s*\(\s*configuration:\s*QaInspectionRegulationPublishedVersionVO[\s\S]*selectedOption\?\.currentPublished\s*===\s*true[\s\S]*configuration\.lifecycleStatus\s*===\s*'DRAFT'/,
  'Equipment bindings must only load for current published or draft versions, never for historical read-only versions.'
)
assert.match(
  page,
  /handleQaRegulationVersionChange[\s\S]*if\s*\(\s*shouldLoadQaEquipmentBindingsForSelectedVersion\(configuration\)\s*\)\s*\{[\s\S]*await loadQaEquipmentBindings\(dccProjectCodeId\)/,
  'Switching a historical version must not call the current-version equipment binding contract.'
)
assert.match(
  page,
  /qaRegulationVersionOptionsLoadError\.value\s*=\s*'QA 规程版本列表加载失败：'/,
  'Version-list failures must be visible and must not silently fall back.'
)

assert.match(
  controller,
  /@GetMapping\("\/versions"\)[\s\S]*CommonResult<List<MesQaInspectionRegulationVersionOptionRespVO>>\s+listVersions/,
  'Backend controller must expose the QA regulation version list endpoint.'
)
assert.match(
  service,
  /List<MesQaInspectionRegulationVersionOptionRespVO>\s+listVersions\(Long dccProjectCodeId\)/,
  'Backend service interface must expose listVersions.'
)
assert.match(
  serviceImpl,
  /public\s+List<MesQaInspectionRegulationVersionOptionRespVO>\s+listVersions\(Long dccProjectCodeId\)[\s\S]*versionMapper\.selectListByRegulationId\(regulation\.getId\(\)\)/,
  'Backend service implementation must list all versions for the selected DCC project.'
)

console.log('qa regulation version status dropdown static contract passed')
