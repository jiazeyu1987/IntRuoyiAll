const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.resolve(moduleRoot, relativePath), 'utf8')

const reqVoSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProEdhrWorkTaskReleaseApprovalRuleReqVO.java'
)
const serviceSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java'
)
const testSource = read(
  'src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImplTest.java'
)

assert(
  reqVoSource.includes('private String candidateSourceType;') &&
    reqVoSource.includes('private Long candidateSourceId;'),
  'Release approval rule request must save candidateSourceType and candidateSourceId.'
)
assert(
  !reqVoSource.includes('最终放行审批责任人不能为空') &&
    !reqVoSource.includes('处理时限不能为空') &&
    !reqVoSource.includes('private Integer dueMinutes;'),
  'Release approval rule request must not require a single assignee or dueMinutes.'
)
assert(
  serviceSource.includes('saveReleaseApprovalRuleCandidate') &&
    serviceSource.includes('reqVO.getCandidateSourceType()') &&
    serviceSource.includes('reqVO.getCandidateSourceId()'),
  'Release approval save service must persist the selected user or role candidate source.'
)
assert(
  serviceSource.includes('CANDIDATE_SOURCE_TYPE_ROLE_GROUP') &&
    serviceSource.includes('roleApi.validRoleList(Set.of(candidateSourceId))') &&
    serviceSource.includes('candidateResolver.resolveAssignmentRule(rule)'),
  'Role candidate saves must validate the role and resolve a non-empty authorized user pool.'
)
assert(
  /TASK_TYPE_RELEASE_APPROVE\.equals\(task\.getTaskType\(\)\)[\s\S]*return null/.test(serviceSource),
  'Release approval work tasks must not receive a dueTime from the route rule.'
)
assert(
  testSource.includes('saveReleaseApprovalRule_acceptsRoleCandidateWithoutDueMinutes') &&
    testSource.includes('createReleaseApprovalTaskAfterSubmit_allowsRoleCandidateUsersToRelease'),
  'Backend unit tests must cover role candidate save and release approval task candidate pool creation.'
)

console.log('PASS: backend eDHR release approval role candidate static contract')
