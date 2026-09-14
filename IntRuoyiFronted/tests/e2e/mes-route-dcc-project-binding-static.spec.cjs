const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/mes/pro/route/index.ts')
const routeForm = read('src/views/mes/pro/route/RouteFormContent.vue')
const controller = read('../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/MesRouteDccProjectBindingController.java')

assert.match(api, /getRouteDccProjectBinding\s*:\s*async/, 'route API must expose a route-DCC binding reader')
assert.match(api, /saveRouteDccProjectBinding\s*:\s*async/, 'route API must expose a route-DCC binding saver')
assert.match(api, /deleteRouteDccProjectBinding\s*:\s*async/, 'route API must expose a route-DCC binding remover')
assert.match(api, /expectedVersion\??:\s*number/, 'route-DCC save/delete API must carry expectedVersion')

assert.match(routeForm, /dccProjectBinding/i, 'route edit form must keep DCC binding as its own state')
assert.match(routeForm, /saveRouteDccProjectBinding\(/, 'DCC binding save must be a separate request from route save')
assert.match(routeForm, /deleteRouteDccProjectBinding\(/, 'DCC binding unbind must call the dedicated route-DCC API')
assert.doesNotMatch(routeForm, /submitForm[\s\S]{0,400}saveRouteDccProjectBinding/, 'route save must not masquerade as DCC binding save success')

assert.match(controller, /@GetMapping\("\/dcc-project-binding"\)/, 'backend must expose GET route-DCC binding')
assert.match(controller, /@PutMapping\("\/dcc-project-binding"\)/, 'backend must expose PUT route-DCC binding')
assert.match(controller, /@DeleteMapping\("\/dcc-project-binding"\)/, 'backend must expose DELETE route-DCC binding')
assert.match(controller, /@PreAuthorize\("@ss\.hasPermission\('mes:pro-route:update'\)"\)[\s\S]*deleteBinding/, 'unbind must only require route update permission')
assert.doesNotMatch(controller, /mes:dcc|dcc:|qa:/i, 'route-DCC binding controller must not require DCC or QA permissions')

console.log('mes-route-dcc-project-binding-static contract PASS')
