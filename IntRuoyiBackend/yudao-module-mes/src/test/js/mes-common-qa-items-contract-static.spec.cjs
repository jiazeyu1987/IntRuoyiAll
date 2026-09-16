const fs = require('fs')
const path = require('path')
const assert = require('assert')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const read = (relative) => fs.readFileSync(path.join(moduleRoot, relative), 'utf8')

const commonResponse = read(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaCommonRegulationSetRespVO.java'
)
assert(
  commonResponse.includes('private Boolean finalInspectionApplicable;') &&
    commonResponse.includes('private String finalInspectionNotApplicableReason;') &&
    commonResponse.includes('private List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionTypeRule> inspectionTypeRules;'),
  'RED: common-member response must expose the formal final-inspection fields'
)

const service = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
)
const memberBuilderStart = service.indexOf('private MesQaCommonRegulationSetRespVO.Member buildCommonRegulationSetMemberResp')
const memberBuilderEnd = service.indexOf('private CommonPublishedVersion requireCommonCurrentPublishedVersion', memberBuilderStart)
assert(
  memberBuilderStart >= 0 && memberBuilderEnd > memberBuilderStart,
  'RED: common-member response mapping method is missing or cannot be isolated'
)
const memberBuilder = service.slice(memberBuilderStart, memberBuilderEnd)
assert(
  memberBuilder.includes('.finalInspectionApplicable(published.getFinalInspectionApplicable())') &&
    memberBuilder.includes(
      '.finalInspectionNotApplicableReason(published.getFinalInspectionNotApplicableReason())'
    ) &&
    memberBuilder.includes('.inspectionTypeRules(published.getInspectionTypeRules())'),
  'RED: common-member response must map final-inspection fields from the published snapshot'
)

const api = fs.readFileSync(
  path.resolve(moduleRoot, '..', '..', 'IntRuoyiFronted/src/api/mes/qc/template/index.ts'),
  'utf8'
)
assert(
  api.includes('finalInspectionApplicable: boolean') &&
    api.includes('finalInspectionNotApplicableReason?: string') &&
    api.includes('inspectionTypeRules: QaInspectionRegulationInspectionTypeRuleVO[]'),
  'RED: the frontend API contract must carry common-member final-inspection fields'
)

console.log('GREEN: common QA items backend contract is present')
