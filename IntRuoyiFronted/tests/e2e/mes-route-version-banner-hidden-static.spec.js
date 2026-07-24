const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const routeFormContentPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'route',
  'RouteFormContent.vue'
)
const routeFormContent = fs.readFileSync(routeFormContentPath, 'utf8')

test('MES route edit page does not render the version workflow banner', () => {
  assert.doesNotMatch(
    routeFormContent,
    /<ControlledContentStateStrip/,
    'RouteFormContent must not render the top candidate-version workflow banner.'
  )
  assert.doesNotMatch(
    routeFormContent,
    /route-version-workflow-status|route-form-content__version-context/,
    'RouteFormContent must not keep the hidden banner test id or styling hook.'
  )
  assert.doesNotMatch(
    routeFormContent,
    /data-route-version-action="(?:edit-production-config|submit-route-candidate)"/,
    'RouteFormContent must not render banner actions inside the removed red-box area.'
  )
})

test('MES route edit page keeps production-config tabs and readonly gate', () => {
  assert.match(
    routeFormContent,
    /<el-tab-pane label="流转关系图" name="flow" lazy>[\s\S]*<RouteFlowGraphDesigner/,
    'Hiding the banner must not remove the flow graph tab.'
  )
  assert.match(
    routeFormContent,
    /const hasRouteVersionPageContext = computed\(\(\) => mode\.value === 'page' && Boolean\(formData\.value\.id\)\)/,
    'RouteFormContent must keep an explicit page-context gate for production config readonly behavior.'
  )
  assert.match(
    routeFormContent,
    /const productionConfigFormType = computed\(\(\) =>[\s\S]*hasRouteVersionPageContext\.value && !isDraftCandidateVersion\.value \? 'detail' : formType\.value/,
    'Non-draft route-version pages must remain read-only after removing the banner.'
  )
})

