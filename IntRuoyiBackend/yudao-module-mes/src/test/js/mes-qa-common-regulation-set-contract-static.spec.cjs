const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = process.cwd()
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8')

const migration = read('IntRuoyiBackend/sql/mysql/20260909_mes_qa_common_regulation_product_binding.sql')
assert(
  migration.includes('mes_qa_common_regulation_set') &&
    migration.includes('mes_qa_common_regulation_set_version') &&
    migration.includes('mes_qa_common_regulation_set_version_member'),
  'RED: migration must create common regulation set, set version, and set version member tables'
)
assert(
  migration.includes('common_regulation_set_version_id') &&
    migration.includes('idx_mes_qa_common_binding_set_version_status'),
  'RED: product binding must persist the bound common regulation set version'
)

const api = read('IntRuoyiFronted/src/api/mes/qc/template/index.ts')
assert(
  api.includes('listCommonRegulationSets') &&
    api.includes('saveCommonRegulationSet') &&
    api.includes('deleteCommonRegulationSet') &&
    api.includes('saveCommonRegulationSetVersion') &&
    api.includes('deleteCommonRegulationSetVersion') &&
    api.includes('listCommonRegulationPublishedSetVersions'),
  'RED: frontend API must expose CRUD for common regulation sets and set versions'
)

const controller = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/MesQaInspectionRegulationController.java'
)
assert(
  controller.includes('/common-sets') &&
    controller.includes('/common-set-versions/save') &&
    controller.includes('/common-binding/published-set-versions'),
  'RED: backend controller must expose common regulation set management endpoints'
)

const service = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
)
assert(
  service.includes('requireCommonPublishedSetVersion') &&
    service.includes('replaceCommonRegulationSetVersionMembers') &&
    service.includes('commonRegulationSetVersionId'),
  'RED: backend service must bind products to a common regulation set version and validate members'
)
assert(
  service.includes('requireCommonPublishedSetVersion(reqVO.getCommonRegulationSetVersionId())') &&
    !service.includes('requireCommonPublishedVersion(reqVO.getCommonRegulationVersionId())'),
  'RED: product binding must require a set version and must not fall back to a single regulation version'
)
assert(
  service.includes('Objects.equals(set.getCurrentVersionId(), version.getId())') &&
    service.includes('当前发布套版本不能删除'),
  'RED: deleting the current published set version must fail fast'
)

const frontline = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
)
assert(
  frontline.includes('resolveCommonQaVersionSources') &&
    frontline.includes('commonRegulationSetVersionMemberMapper.selectListBySetVersionId'),
  'RED: active-order task generation must expand all common regulation set members'
)

const frontlineDisplay = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)
assert(
  frontlineDisplay.includes('getLockedCommonVersionForOrder') &&
    !frontlineDisplay.includes('Objects.equals(activeOrder.getDccProjectCodeId(), regulation.getDccProjectCodeId())'),
  'RED: frontline PQC display must not require common regulation DCC to equal product DCC'
)

console.log('GREEN: common regulation set management contract is present')
