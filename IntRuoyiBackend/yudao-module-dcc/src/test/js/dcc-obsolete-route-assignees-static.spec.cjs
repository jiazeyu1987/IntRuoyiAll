const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = process.cwd().endsWith('IntRuoyiBackend')
  ? process.cwd()
  : path.join(process.cwd(), 'IntRuoyiBackend')

const obsoleteService = fs.readFileSync(
  path.join(
    root,
    'yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileObsoleteServiceImpl.java'
  ),
  'utf8'
)
const obsoleteReq = fs.readFileSync(
  path.join(
    root,
    'yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileObsoleteReqVO.java'
  ),
  'utf8'
)

const obsoleteSubmitMatch = obsoleteService.match(
  /public FormInstanceRespVO obsoleteControlledFile[\s\S]*?return formCenterRuntimeService\.submitInstance\(draft\.getId\(\), submitReqVO, userId\);/
)
assert(obsoleteSubmitMatch, 'DCC obsolete submit implementation must remain route-controlled.')
const obsoleteSubmit = obsoleteSubmitMatch[0]

assert(
  /String actionType\s*=\s*DccControlledFileProcessDefinitionKeys\.toActionType\(\s*DccControlledFileProcessDefinitionKeys\.OBSOLETE\s*\)/.test(
    obsoleteSubmit
  ),
  'DCC obsolete submit must use the OBSOLETE action type.'
)

assert(
  (obsoleteSubmit.match(/approvalRouteAssigneeResolver\.resolveRoute\s*\(/g) || []).length === 1 &&
    /ResolvedRoute\s+resolvedRoute\s*=\s*approvalRouteAssigneeResolver\.resolveRoute\(\s*file\.getCategoryId\(\)\s*,\s*userId\s*,\s*actionType\s*\)/.test(
      obsoleteSubmit
    ),
  'DCC obsolete submit must resolve the OBSOLETE route exactly once.'
)

assert(
  /buildStartUserSelectAssigneeMap\(\s*resolvedRoute\.nodes\(\)\s*\)/.test(obsoleteSubmit) &&
    /buildApproveUserSelectAssigneeMap\(\s*resolvedRoute\.nodes\(\)\s*\)/.test(obsoleteSubmit) &&
    /setStartUserSelectAssignees\(\s*startUserSelectAssignees\s*\)/.test(obsoleteSubmit) &&
    /setApproveUserSelectAssignees\(\s*approveUserSelectAssignees\s*\)/.test(obsoleteSubmit),
  'DCC obsolete signoff and approval candidates must come from the same ResolvedRoute snapshot.'
)

assert(
  !obsoleteService.includes('resolveStartUserSelectAssignees(') &&
    !obsoleteService.includes('reqVO.getStartUserSelectAssignees()'),
  'DCC obsolete submit must not trust request-provided starter-selected assignees.'
)

assert(
  obsoleteReq.includes('private Map<String, List<Long>> startUserSelectAssignees;'),
  'DCC obsolete request may keep the FormCenter field contract, but service must ignore it for route-controlled assignees.'
)

console.log('PASS dcc-obsolete-route-assignees-static')
