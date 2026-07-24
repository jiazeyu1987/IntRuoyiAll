const assert = require('node:assert/strict')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')

const { auditRouteContracts } = require('../support/route-contract-audit')

const fixtureRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'route-contract-audit-'))

try {
  const fixtureApiRoot = path.join(fixtureRoot, 'frontend', 'src', 'api')
  const fixtureBackendRoot = path.join(fixtureRoot, 'backend')
  fs.mkdirSync(fixtureApiRoot, { recursive: true })
  fs.mkdirSync(fixtureBackendRoot, { recursive: true })

  fs.writeFileSync(
    path.join(fixtureApiRoot, 'demo.ts'),
    `
const CREATE_URL = '/demo/create'
export const getPresent = () => request.get({ url: '/demo/present' })
export const getMissing = () => request.get({ url: '/demo/missing' })
export const createDemo = () => request.post({ url: CREATE_URL })
export const getDynamic = (url) => request.get({ url: buildDisplayUrl(url) })
export const getNestedGeneric = () =>
  request.get<PageResult<DemoVO[]>>({ url: '/demo/present' })
export const getConcatenatedPath = (id) =>
  request.get({ url: '/demo/item/' + id })
`,
    'utf8'
  )
  fs.writeFileSync(
    path.join(fixtureBackendRoot, 'DemoController.java'),
    `
@RequestMapping("/demo")
public class DemoController {
  @GetMapping("/present")
  public void getPresent() {}

  @PostMapping("/create")
  public void createDemo() {}

  @GetMapping("/item/{id}")
  public void getItem() {}
}
`,
    'utf8'
  )

  const fixtureAudit = auditRouteContracts({
    frontendApiRoot: fixtureApiRoot,
    backendRoot: fixtureBackendRoot
  })

  assert.deepEqual(
    fixtureAudit.missingRoutes.map(({ method, route }) => ({ method, route })),
    [{ method: 'GET', route: '/demo/missing' }],
    'The audit must report a real method-and-path contract gap.'
  )
  assert.deepEqual(
    fixtureAudit.unresolvedRequests,
    [
      {
        method: 'GET',
        relativeFile: 'demo.ts',
        urlExpression: 'buildDisplayUrl(url)'
      }
    ],
    'The audit must expose dynamic request URLs instead of silently skipping them.'
  )
  assert.equal(
    fixtureAudit.frontendRequestCount,
    6,
    'The audit must count nested generic calls and concatenated URL expressions.'
  )
} finally {
  fs.rmSync(fixtureRoot, { recursive: true, force: true })
}

const frontendRoot = path.resolve(__dirname, '../..')
const backendRoot = path.resolve(frontendRoot, '..', 'ruoyi-vue-pro')
const dynamicRequestClassifications = [
  {
    method: 'GET',
    relativeFile: 'showroom-admin/version-center.ts',
    urlExpression: 'option.url',
    reason: 'Typed version-center wrapper receives literal URLs from local public API functions.'
  },
  {
    method: 'POST',
    relativeFile: 'showroom-admin/version-center.ts',
    urlExpression: 'option.url',
    reason: 'Typed version-center wrapper receives literal URLs from local public API functions.'
  },
  {
    method: 'GET',
    relativeFile: 'showroom-frontstage/index.ts',
    urlExpression: 'buildDisplayUrl(websiteConfigPath)',
    reason: 'Display API builds an absolute URL from the local website-config path.'
  },
  {
    method: 'GET',
    relativeFile: 'showroom-frontstage/index.ts',
    urlExpression: "buildDisplayUrl('/showroom/display/narration')",
    reason: 'Display API builds an absolute URL from the literal narration path.'
  }
]

assert.ok(
  fs.existsSync(backendRoot),
  `Backend source tree is required beside the frontend worktree: ${backendRoot}`
)

const workspaceAudit = auditRouteContracts({
  frontendApiRoot: path.join(frontendRoot, 'src', 'api'),
  backendRoot,
  dynamicRequestClassifications
})

assert.ok(
  workspaceAudit.frontendRequestCount >= 2640,
  `Frontend request coverage unexpectedly dropped: ${workspaceAudit.frontendRequestCount}`
)
assert.ok(
  workspaceAudit.frontendRouteCount >= 2500,
  `Frontend route coverage unexpectedly dropped: ${workspaceAudit.frontendRouteCount}`
)
assert.ok(
  workspaceAudit.backendRouteCount >= 2900,
  `Backend route coverage unexpectedly dropped: ${workspaceAudit.backendRouteCount}`
)
assert.deepEqual(
  workspaceAudit.missingRoutes,
  [],
  `Frontend routes without backend contracts:\n${workspaceAudit.missingRoutes
    .map(({ method, route, relativeFile }) => `${method} ${route} (${relativeFile})`)
    .join('\n')}`
)
assert.deepEqual(
  workspaceAudit.unclassifiedRequests,
  [],
  `Unclassified request URLs:\n${workspaceAudit.unresolvedRequests
    .map(
      ({ method, relativeFile, urlExpression }) =>
        `${method} ${relativeFile}: ${urlExpression}`
    )
    .join('\n')}`
)
assert.deepEqual(
  workspaceAudit.unusedDynamicRequestClassifications,
  [],
  'Every dynamic request classification must match a current unresolved request.'
)
assert.deepEqual(
  workspaceAudit.classifiedDynamicRequests,
  dynamicRequestClassifications,
  'Known dynamic requests and their reasons must remain explicit and exact.'
)

console.log(
  `PASS: route contract coverage requests=${workspaceAudit.frontendRequestCount}; routes=${workspaceAudit.frontendRouteCount}/${workspaceAudit.backendRouteCount}; dynamic=${workspaceAudit.classifiedDynamicRequests.length}`
)
