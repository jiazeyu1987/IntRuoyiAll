const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routes = read('src/router/modules/remaining.ts')
const tagsViewStore = read('src/store/modules/tagsView.ts')
const profilePage = read('src/views/Profile/Index.vue')
const messagePopover = read('src/layout/components/Message/src/Message.vue')
const formTracePage = read('src/views/mes/pro/edhr/FormTracePage.vue')
const iotProductList = read('src/views/iot/product/product/index.vue')
const iotProductDetail = read('src/views/iot/product/product/detail/index.vue')
const iotDeviceList = read('src/views/iot/device/device/index.vue')
const iotDeviceDetail = read('src/views/iot/device/device/detail/index.vue')

const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const sliceRoute = (startMarker, endMarker) => {
  const start = routes.indexOf(startMarker)
  assert(start >= 0, `route block start not found: ${startMarker}`)
  const end = routes.indexOf(endMarker, start + startMarker.length)
  assert(end > start, `route block end not found: ${endMarker}`)
  return routes.slice(start, end)
}

const assertPathIdentityRoute = (label, routeBlock) => {
  assert.match(
    routeBlock,
    /tagsViewKeyMode:\s*'path'/,
    `${label} must use path tag identity so query-only internal tabs reuse one TagsView tab`
  )
  assert.doesNotMatch(
    routeBlock,
    /tagsViewKey:\s*['"]/,
    `${label} must not use one shared tagsViewKey that would merge different route params`
  )
}

assert.match(
  tagsViewStore,
  /view\.meta\?\.tagsViewKeyMode\s*===\s*'path'[\s\S]*return normalizedPath \? `\/\$\{normalizedPath\}` : view\.path/,
  'TagsView path identity must ignore query while keeping concrete route paths isolated'
)

assert.match(profilePage, /route\.query\.tab\s*===\s*'notifyMessage'/)
assert.match(messagePopover, /name:\s*'Profile'[\s\S]*tab:\s*'notifyMessage'/)
assertPathIdentityRoute(
  'Profile notify message tab route',
  sliceRoute("path: 'profile'", "path: 'notify-message'")
)

assert.match(formTracePage, /route\.query\.tab\s*===\s*'release'/)
assert.match(formTracePage, /router\.replace\(\{[\s\S]*query:\s*\{[\s\S]*\.\.\.route\.query[\s\S]*tab/)
assertPathIdentityRoute(
  'eDHR form trace tab route',
  sliceRoute("path: 'pro/feedback/edhr-form-trace'", "path: 'pro/feedback/edhr-field-audit'")
)

assert.match(iotProductList, /name:\s*'IoTProductDetail'[\s\S]*query:\s*\{[\s\S]*tab:\s*'thingModel'/)
assert.match(iotProductDetail, /route\.query\.tab/)
assertPathIdentityRoute(
  'IoT product detail internal tab route',
  sliceRoute("path: 'product/product/detail/:id'", "path: 'device/detail/:id'")
)

assert.match(iotDeviceList, /name:\s*'IoTDeviceDetail'[\s\S]*query:\s*\{[\s\S]*tab:\s*'model'/)
assert.match(iotDeviceDetail, /route\.query\.tab/)
assertPathIdentityRoute(
  'IoT device detail internal tab route',
  sliceRoute("path: 'device/detail/:id'", "path: 'ota/operation/firmware/detail/:id'")
)

for (const routePattern of ['product/product/detail/:id', 'device/detail/:id']) {
  const pattern = new RegExp(`['"]${escapeRegExp(routePattern)}['"][\\s\\S]*tagsViewKeyMode:\\s*'path'`)
  assert.match(
    routes,
    pattern,
    `${routePattern} must use path identity on its route record, not a static whitelist wildcard`
  )
}

console.log('PASS: query-only internal tab routes reuse one TagsView tab')
