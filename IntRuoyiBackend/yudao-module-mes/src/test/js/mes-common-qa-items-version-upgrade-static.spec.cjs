const fs = require('fs')
const path = require('path')
const assert = require('assert')

const backendRoot = path.resolve(__dirname, '..', '..')
const service = fs.readFileSync(
  path.join(
    backendRoot,
    'main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
  ),
  'utf8'
)

assert(
  service.includes('requireCommonVersionSnapshot') &&
    service.includes('Set.of(STATUS_PUBLISHED, STATUS_RETIRED)') &&
    service.includes('buildCommonRegulationSetMemberResp') &&
    service.includes('requireCommonVersionSnapshot(member.getRegulationVersionId())'),
  'RED: common set member responses must read locked published or retired common regulation snapshots'
)

assert(
  service.includes('requireCommonCurrentPublishedVersion') &&
    service.includes('replaceCommonRegulationSetVersionMembers') &&
    service.includes('requireCommonCurrentPublishedVersion(memberReq.getCommonRegulationVersionId())'),
  'RED: saving a common set version must still require current published member versions'
)

assert(
  service.includes('getSourceCommonRegulationVersionId') &&
    service.includes('检验项目升版成员必须与来源套版本一致') &&
    service.includes('sourceMemberSnapshots.get(memberReq.getSourceCommonRegulationVersionId())'),
  'RED: common item upgrade must validate that members match the source common-set version'
)

console.log('GREEN: common QA item version-upgrade backend contract is present')
