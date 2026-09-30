const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/dcc/controlledFile/approvalRoutes.ts')
const options = read('src/views/dcc/controlled-file/shared/options.ts')
const routeForm = read('src/views/dcc/controlled-file/routes/components/RouteForm.vue')
const routeList = read('src/views/dcc/controlled-file/routes/index.vue')

assert.match(
  api,
  /ControlledFileRouteCandidateSourceType\s*=\s*'USER'\s*\|\s*'POSITION'\s*\|\s*'DEPT'/,
  'approval route API type must include DEPT candidate source'
)
assert.match(
  api,
  /candidateSourceIds:\s*number\[\]/,
  'approval route save payload must carry candidateSourceIds'
)
assert.match(
  api,
  /ControlledFileApprovalRouteActionType\s*=\s*'LEGACY'\s*\|\s*'NEW'\s*\|\s*'REVISION'\s*\|\s*'OBSOLETE'/,
  'approval route API type must expose independent route action types'
)
assert.match(
  api,
  /ControlledFileApprovalRouteSaveReqVO[\s\S]{0,120}actionType:\s*ControlledFileApprovalRouteActionType/,
  'approval route save payload must carry actionType'
)
assert.match(
  api,
  /ControlledFileApprovalRoutePreviewReqVO[\s\S]{0,120}actionType\?:\s*ControlledFileApprovalRouteActionType/,
  'approval route preview payload must support actionType'
)
assert.match(
  options,
  /label:\s*'部门负责人',\s*value:\s*'DEPT'/,
  'candidate source options must expose department leader source'
)
assert.match(
  options,
  /ROUTE_ACTION_TYPE_OPTIONS[\s\S]{0,280}value:\s*'NEW'[\s\S]{0,120}value:\s*'REVISION'[\s\S]{0,120}value:\s*'OBSOLETE'/,
  'route options must expose upload, revision and obsolete action types'
)
assert.match(
  routeForm,
  /import type \{ DeptVO \} from '@\/api\/system\/dept'/,
  'route form must receive formal department DTOs'
)
assert.match(
  routeForm,
  /v-else[\s\S]{0,260}v-model="row\.candidateSourceIds"[\s\S]{0,220}\bmultiple\b[\s\S]{0,260}请选择会签部门/,
  'DEPT source must use a real multiple department selector bound to candidateSourceIds'
)
assert.match(
  routeForm,
  /candidateSourceType === 'DEPT'[\s\S]{0,160}item\.candidateSourceIds/,
  'route form normalization must use candidateSourceIds for DEPT nodes'
)
assert.match(
  routeForm,
  /candidateSourceId:\s*candidateSourceIds\[0\],[\s\S]{0,80}candidateSourceIds,/,
  'save payload must keep first candidateSourceId projection and full candidateSourceIds array'
)
assert.match(
  routeForm,
  /v-model="formData\.actionType"[\s\S]{0,260}ROUTE_ACTION_TYPE_OPTIONS/,
  'route form must let admins choose actionType'
)
assert.match(
  routeForm,
  /ACTION_ROUTE_APPROVAL_POLICY[\s\S]{0,360}approveMethod:\s*'ALL'[\s\S]{0,80}approveRatio:\s*100[\s\S]{0,420}EXPECTED_ACTION_ROUTE_STAGE_NOS\s*=\s*\[1,\s*2,\s*3\]/,
  'three-action routes must use the three-node signoff approval doc-control policy'
)
assert.match(
  routeForm,
  /!isLegacyRoute\.value\s*&&\s*index\s*===\s*0\s*\?\s*'DEPT'/,
  'three-action route first node must default to department leader signoff'
)
assert.match(
  routeList,
  /getSimpleDeptList/,
  'route list must load departments for route form and display'
)
assert.match(
  routeList,
  /queryParamKey:\s*'actionType'[\s\S]{0,160}ROUTE_ACTION_TYPE_OPTIONS/,
  'route list must filter routes by actionType'
)
assert.match(
  routeList,
  /previewApprovalRoute\(\{[\s\S]{0,120}categoryId:\s*queryParams\.categoryId,[\s\S]{0,120}actionType:\s*queryParams\.actionType/,
  'route preview must request the selected actionType'
)
assert.match(
  routeList,
  /candidateSourceType === 'DEPT'[\s\S]{0,120}resolveDepartmentNames/,
  'preview display must resolve DEPT candidate IDs as department names'
)
assert.match(
  routeList,
  /candidateSourceType === 'DEPT'[\s\S]{0,160}resolveRouteNodeDepartmentNames/,
  'route list node display must resolve DEPT candidate IDs as department names'
)

console.log('PASS: DCC route department candidate static contract')
