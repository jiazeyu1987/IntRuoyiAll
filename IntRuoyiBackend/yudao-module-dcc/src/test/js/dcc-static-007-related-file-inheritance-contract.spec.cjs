const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../..')
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8')

const queryService = read('src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java')
const relatedService = read('src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRelatedFileServiceImpl.java')
const followupService = read('src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupServiceImpl.java')
const queryTest = read('src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java')
const relatedTest = read('src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRelatedFileServiceTest.java')

assert.match(
  queryService,
  /sourceOwnershipService\.claimSubmissionSource\(next\.getId\(\), preparedSource, userId, "CHECKIN"\);\s*relatedFileService\.inheritRelatedFiles\(file\.getId\(\), next\.getId\(\)\);/s,
  'checkin must inherit related-file rows immediately after the new iteration owns its source file'
)

assert.match(
  relatedService,
  /void\s+inheritRelatedFiles\(Long\s+sourceControlledFileId,\s*Long\s+targetControlledFileId\)[\s\S]*selectListByControlledFileId\(sourceControlledFileId\)[\s\S]*controlledFileId\(targetControlledFileId\)[\s\S]*relationSource\(RELATION_SOURCE_CHECKIN_INHERITED\)/,
  'related-file service must copy source relations onto the checkin iteration with an inherited relation source'
)

assert.match(
  followupService,
  /relatedFileService\.listForwardRelations\(publishedFile\.getId\(\)\)/,
  'publication follow-up snapshots must read explicit relations from the published iteration'
)

assert.match(
  queryTest,
  /checkinRealSourceCreatesWorkingA2AndLeavesA1FormalPointerUnchanged[\s\S]*verify\(relatedFileService\)\.inheritRelatedFiles\(900L,\s*901L\)/,
  'checkin regression test must prove A/2 inherits A/1 related-file rows'
)

assert.match(
  relatedTest,
  /inheritRelatedFiles_copiesSourceRelationsToCheckinIterationWithoutChangingSource[\s\S]*assertEquals\("CHECKIN_INHERITED", inherited\.getRelationSource\(\)\)[\s\S]*assertEquals\(100L, sourceRelation\.getControlledFileId\(\)\)/,
  'related-file service regression test must prove copied rows are inherited and source rows remain historical'
)

console.log('DCC-STATIC-007 related-file inheritance static contract passed')
