const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const indexOfRequired = (source, token, label = token) => {
  const index = source.indexOf(token)
  assert.ok(index >= 0, `missing required token: ${label}`)
  return index
}

const controller = readSource(
  'src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/DccProjectCodeController.java'
)
const service = readSource(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessServiceImpl.java'
)
const serviceInterface = readSource(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessService.java'
)
const mapper = readSource(
  'src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/projectcode/DccProjectAccessRuleMapper.java'
)
const saveVo = readSource(
  'src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/vo/access/DccProjectAccessRuleSaveReqVO.java'
)
const workflow = readSource(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java'
)

for (const token of [
  '@GetMapping("/{id:\\\\d+}/access-rules")',
  '@PutMapping("/{id:\\\\d+}/access-rules")',
  'projectAccessService.getProjectAccessRules(id)',
  'projectAccessService.replaceProjectAccessRules(id, reqVO.getRules())',
  "@PreAuthorize(\"@ss.hasPermission('dcc:project-code:update')\")"
]) {
  assert.ok(controller.includes(token), `controller must expose formal access-rule maintenance: ${token}`)
}

for (const token of [
  'List<DccProjectAccessRuleDO> getProjectAccessRules(Long projectCodeId);',
  'List<DccProjectAccessRuleDO> replaceProjectAccessRules(Long projectCodeId,',
  'void assertProjectOwner(Long userId, Long projectCodeId);',
  'void assertProjectEditorOrOwner(Long userId, Long projectCodeId);'
]) {
  assert.ok(serviceInterface.includes(token), `service interface must include formal access API: ${token}`)
}

for (const token of [
  'default List<DccProjectAccessRuleDO> selectListByProjectCodeId(Long projectCodeId)',
  'default void deleteByProjectCodeId(Long projectCodeId)'
]) {
  assert.ok(mapper.includes(token), `mapper must support formal access-rule maintenance: ${token}`)
}

assert.ok(saveVo.includes('@NotBlank(message = "变更原因不能为空")'), 'save VO must reject blank change reason')

const validateIndex = indexOfRequired(service, 'validateProjectCodeExists(projectCodeId);')
const normalizeIndex = indexOfRequired(service, 'List<DccProjectAccessRuleDO> normalizedRules = normalizeRules(projectCodeId, rules);')
const ownerCheckIndex = indexOfRequired(service, 'normalizedRules.stream().noneMatch(this::isCurrentlyActiveOwner)')
const deleteIndex = indexOfRequired(service, 'accessRuleMapper.deleteByProjectCodeId(projectCodeId);')
assert.ok(validateIndex < normalizeIndex, 'project must be validated before normalizing rules')
assert.ok(normalizeIndex < ownerCheckIndex, 'rules must be normalized before active OWNER validation')
assert.ok(ownerCheckIndex < deleteIndex, 'existing rules must not be deleted before active OWNER validation')

const changeReasonIndex = indexOfRequired(
  service,
  'String changeReason = rule.getChangeReason() == null ? null : rule.getChangeReason().trim();'
)
const changeReasonRejectIndex = indexOfRequired(service, 'if (changeReason == null || changeReason.isEmpty())')
const setReasonIndex = indexOfRequired(service, 'accessRule.setChangeReason(changeReason);')
assert.ok(changeReasonIndex < changeReasonRejectIndex, 'change reason must be normalized before blank validation')
assert.ok(changeReasonRejectIndex < setReasonIndex, 'blank change reason must be rejected before persistence')

const mutationGuard = indexOfRequired(workflow, 'projectAccessService.assertProjectEditorOrOwner(userId, projectCode.getId());')
const ownerGuard = indexOfRequired(workflow, 'projectAccessService.assertProjectOwner(userId, projectCode.getId());')
assert.ok(ownerGuard < mutationGuard, 'revision OWNER guard and new upload EDIT/OWNER guard must both exist')

assert.doesNotMatch(
  service,
  /projectLeader|requesterId|hasAnyPermissions|fallback|mock|placeholder|默认负责人|自动负责人/,
  'formal project access service must not use text owners, requester, menu permission, fallback, mock, or placeholders'
)

console.log('PASS: DCC-STATIC-001 backend project access closure contract')
