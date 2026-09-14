const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../../..')
const backendRoot = path.resolve(moduleRoot, '..')

const readModule = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')
const readBackend = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const finalizationService = readModule(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java'
)
const contentAdapter = readModule(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledContentAdapter.java'
)
const finalizationServiceTest = readModule(
  'src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImplTest.java'
)
const lifecycleCore = readBackend(
  'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/controlledcontent/ControlledContentLifecycleCoreService.java'
)
const transitionAuditMapper = readBackend(
  'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/mysql/controlledcontent/ControlledContentTransitionAuditMapper.java'
)

assert.doesNotMatch(
  finalizationService,
  /String\s+eventKey\s*=\s*"dcc-finalization-retry:"\s*\+\s*id\s*;/,
  'retryStamp must not reuse one file-scoped key for every retry attempt'
)
assert.match(
  finalizationService,
  /platformAdapter\.nextFinalizationRetryEventKey\(file\)/,
  'retryStamp must ask the controlled-content adapter for the next attempt-scoped retry key'
)
assert.match(
  contentAdapter,
  /String\s+nextFinalizationRetryEventKey\(DccControlledFileDO file\)/,
  'adapter must expose deterministic retry attempt key generation'
)
assert.match(
  contentAdapter,
  /countVersionRefTransitions\(dccKey\(file\),\s*file\.getId\(\),\s*ControlledContentTransitionAction\.RETRY_FINALIZATION\)/,
  'retry attempt key must be based on committed RETRY_FINALIZATION audit count'
)
assert.match(
  contentAdapter,
  /"dcc-finalization-retry:"\s*\+\s*file\.getId\(\)\s*\+\s*":attempt-"\s*\+\s*\(retryAttemptCount\s*\+\s*1\)/,
  'retry attempt key must include the next attempt number'
)
assert.match(
  lifecycleCore,
  /long\s+countVersionRefTransitions\(ControlledContentKey key,\s*Long nativeVersionId,\s*ControlledContentTransitionAction action\)/,
  'lifecycle core must expose an audited transition-count read for attempt numbering'
)
assert.match(
  transitionAuditMapper,
  /countByVersionRefIdAndAction\(Long versionRefId,\s*String action\)/,
  'transition audit mapper must count committed action audits for a version ref'
)
assert.match(
  finalizationServiceTest,
  /retryStamp_failedRetryThenSecondRetryUsesNewAttemptEventKey/,
  'unit regression for failed retry followed by a second retry is missing'
)
assert.match(
  finalizationServiceTest,
  /dcc-finalization-retry:902:attempt-1[\s\S]*dcc-finalization-retry:902:attempt-2/,
  'regression must assert first and second retry attempts use distinct keys'
)

console.log('DCC-STATIC-026 finalization retry event key contract PASS')
