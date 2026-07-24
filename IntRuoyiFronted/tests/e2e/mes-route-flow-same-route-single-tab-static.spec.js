import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(process.cwd())
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const routes = read('src/router/modules/remaining.ts')
const tagsViewStore = read('src/store/modules/tagsView.ts')
const routerTypes = read('types/router.d.ts')
const routeGraph = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

const routeEditStart = routes.indexOf("path: 'pro/route/edit/:id'")
const routeEditEnd = routes.indexOf("path: 'pro/feedback/edhr-work-task'", routeEditStart)
assert(routeEditStart >= 0 && routeEditEnd > routeEditStart, 'route edit config must exist')
const routeEditConfig = routes.slice(routeEditStart, routeEditEnd)

assert.match(
  routeGraph,
  /router\.replace\(\{\s*query:\s*nextQuery\s*\}\)/,
  'selected process state must continue using query replacement for return restoration'
)
assert.match(
  routeEditConfig,
  /tagsViewKeyMode:\s*'path'/,
  'route edit page must identify its tag by resolved path so query-only changes reuse the same tab'
)
assert.match(
  tagsViewStore,
  /view\.meta\?\.tagsViewKeyMode\s*===\s*'path'/,
  'tags view store must support path-based tag identity'
)
assert.match(
  tagsViewStore,
  /return normalizedPath \? `\/\$\{normalizedPath\}` : view\.path/,
  'path-based tag identity must keep different route ids isolated with a normalized path while ignoring query changes'
)
assert.match(
  routerTypes,
  /tagsViewKeyMode\?:\s*'fullPath'\s*\|\s*'path'/,
  'route metadata must type the supported tag identity modes'
)

process.stdout.write('mes route flow same-route single-tab static contract passed\n')
