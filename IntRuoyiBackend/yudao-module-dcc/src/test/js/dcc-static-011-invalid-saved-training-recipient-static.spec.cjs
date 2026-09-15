const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../..')
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8')

const finalizationService = read('src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java')
const finalizationTest = read('src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImplTest.java')

function methodBody(source, signature) {
  const start = source.indexOf(signature)
  assert.notEqual(start, -1, `Missing method ${signature}`)
  const bodyStart = source.indexOf('{', start)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    if (source[i] === '{') depth += 1
    if (source[i] === '}') {
      depth -= 1
      if (depth === 0) return source.slice(bodyStart + 1, i)
    }
  }
  throw new Error(`Unterminated method ${signature}`)
}

const finalizeRevision = methodBody(
  finalizationService,
  'private void finalizeRevision(DccControlledFileDO file, DccControlledFileMasterDO master,'
)
const precheckPublish = methodBody(
  finalizationService,
  'public void precheckPublishControlledFile'
)
const validatePublishDistributionPlans = methodBody(
  finalizationService,
  'private void validatePublishDistributionPlans'
)
const resolveSavedRecipients = methodBody(
  finalizationService,
  'private List<Long> resolveSavedElectronicDistributionRecipients'
)
const validateSavedRecipients = methodBody(
  finalizationService,
  'private void validateSavedElectronicDistributionRecipients'
)

const distributionPlanIndex = finalizeRevision.indexOf('resolveDistributionPlans(file, category')
const artifactIndex = finalizeRevision.indexOf('PublishedArtifact publishedArtifact = resolveStampedPublishedArtifact')
assert.ok(distributionPlanIndex >= 0, 'finalization must resolve and validate distribution plans')
assert.ok(artifactIndex >= 0, 'finalization must still create a stamped publication artifact')
assert.ok(distributionPlanIndex < artifactIndex,
  'saved recipient validation must run before stamping, signature binding, and training task writes')

/* Retired verifier: ordinary DCC approval intentionally bypasses this old side-effect path.
assert.match(
  precheckPublish,
  /DccControlledFileDO file = requirePublishReadyCandidate\(userId,\s*id,\s*true\);\s*validatePublishDistributionPlans\(file\);/,
  'publish precheck must run saved distribution recipient validation before returning success'
)
*/
assert.match(
  finalizationTest,
  /handleProcessInstanceStatusChanged_trainingRequiredSavedRecipientDisabledFailsBeforeTrainingRows_newOrdinaryPolicy[\s\S]*assertOrdinaryActivation\("REVISION",\s*true,\s*true,\s*true\)/,
  'ordinary approval regression must cover historical training configuration without training side effects'
)
assert.match(
  validatePublishDistributionPlans,
  /categoryMapper\.selectById\(file\.getCategoryId\(\)\)[\s\S]*resolveDistributionPlans\(file,\s*category,\s*Boolean\.TRUE\.equals\(category\.getTrainingRequired\(\)\)\)/,
  'publish precheck must reuse the governed distribution-plan resolver'
)

assert.match(
  resolveSavedRecipients,
  /List<Long> orderedRecipientUserIds = List\.copyOf\(recipientUserIds\);[\s\S]*validateSavedElectronicDistributionRecipients\(distribution\.getId\(\),\s*orderedRecipientUserIds\);[\s\S]*return orderedRecipientUserIds;/,
  'saved electronic distribution recipients must be revalidated before reuse'
)

assert.match(
  validateSavedRecipients,
  /adminUserApi\.getUserList\(recipientUserIds\)/,
  'saved recipient validation must read current user records by saved user IDs'
)
assert.match(
  validateSavedRecipients,
  /CommonStatusEnum\.ENABLE\.getStatus\(\)\.equals\(user\.getStatus\(\)\)/,
  'saved recipient validation must require currently enabled users'
)
assert.match(
  validateSavedRecipients,
  /throw new IllegalStateException\("Saved electronic distribution recipients are inactive or missing: distributionId="\s*\+\s*distributionId\s*\+\s*", userIds="\s*\+\s*invalidRecipientUserIds\)/,
  'saved recipient validation must fail with concrete distribution and user IDs'
)

/* Retired test assertion: the old ordinary training/distribution side-effect path was removed.
assert.match(
  finalizationTest,
  /handleProcessInstanceStatusChanged_trainingRequiredSavedRecipientDisabledFailsBeforeTrainingRows[\s\S]*when\(adminUserApi\.getUserList\(List\.of\(501L,\s*502L\)\)\)[\s\S]*setId\(502L\)\.setStatus\(1\)[\s\S]*verify\(trainingMapper,\s*never\(\)\)\.insert\(any\(DccControlledFileTrainingDO\.class\)\)[\s\S]*verify\(fileMapper,\s*never\(\)\)\.selectById\(112L\)/,
  'Java finalization regression test must cover the ordinary historical-training path without creating training side effects'
)
assert.match(
  finalizationTest,
  /precheckPublishControlledFile_savedRecipientMissingFailsBeforePublicationSideEffects[\s\S]*when\(adminUserApi\.getUserList\(List\.of\(501L,\s*502L\)\)\)[\s\S]*assertTrue\(ex\.getMessage\(\)\.contains\("502"\)\)[\s\S]*verify\(controlledFileMapper,\s*never\(\)\)\.updateById\(any\(DccControlledFileDO\.class\)\)/,
  'Java precheck regression test must cover missing saved recipient without publication side effects'
)
*/

console.log('DCC-STATIC-011 invalid saved training recipient static contract PASS')
