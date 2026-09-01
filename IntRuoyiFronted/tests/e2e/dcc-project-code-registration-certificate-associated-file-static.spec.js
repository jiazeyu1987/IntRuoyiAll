const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../../')
const panelPath = path.join(
  repoRoot,
  'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'
)
const workflowApiPath = path.join(repoRoot, 'src/api/dcc/controlledFile/workflow.ts')

const panel = fs.readFileSync(panelPath, 'utf8')
const workflowApi = fs.readFileSync(workflowApiPath, 'utf8')

assert.match(
  workflowApi,
  /businessSourceType\?:\s*'DCC_CONTROLLED_FILE'\s*\|\s*'DCC_REGISTRATION_CERTIFICATE'\s*\|\s*null/,
  'ControlledFileVO must expose the formal business source type for associated documents'
)
assert.match(
  workflowApi,
  /registrationCertificateBusinessFileId\?:\s*number\s*\|\s*string\s*\|\s*null/,
  'ControlledFileVO must expose the registration certificate business file id'
)

assert.match(
  panel,
  /const\s+DCC_REGISTRATION_CERTIFICATE_SOURCE_TYPE\s*=\s*'DCC_REGISTRATION_CERTIFICATE'/,
  'Project code associated file panel must define the registration certificate source type'
)
assert.match(
  panel,
  /const\s+isRegistrationCertificateAssociatedFile\s*=\s*\(row:\s*ControlledFileVO\)/,
  'Project code associated file panel must detect registration certificate source rows'
)
assert.match(
  panel,
  /path:\s*'\/mdm\/registration-certificate\/detail\/'\s*\+\s*String\(row\.registrationCertificateId\)/,
  'Registration certificate source rows must open the registration certificate detail route'
)
assert.match(
  panel,
  /<el-table-column\s+type="selection"\s+width="48"\s+:selectable="isAssociatedControlledFileSelectable"\s*\/>/,
  'Registration certificate source rows must not be selectable for controlled-file correction assignments'
)
