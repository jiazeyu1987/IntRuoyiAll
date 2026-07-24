import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom route is not part of the initial remaining router', () => {
  const remainingSource = readText('src/router/modules/remaining.ts')
  const routerSource = readText('src/router/index.ts')

  assert.match(routerSource, /routes:\s*remainingRouter as RouteRecordRaw\[\]/)
  assert.doesNotMatch(remainingSource, /import showroomRoutes from '\.\/showroom'/)
  assert.doesNotMatch(remainingSource, /\.\.\.showroomRoutes/)
})

test('showroom static shell is merged only after the backend permission menu authorizes it', () => {
  const permissionSource = readText('src/store/modules/permission.ts')

  assert.match(permissionSource, /import showroomRoutes from '@\/router\/modules\/showroom'/)
  assert.match(permissionSource, /const permissionControlledStaticRoutes = showroomRoutes/)
  assert.match(permissionSource, /authorizedStaticRoutes/)
  assert.match(
    permissionSource,
    /mergeStaticRoutesWithDynamicRoutes\(\s*remainingRouter,\s*routerMap,\s*permissionControlledStaticRoutes\s*\)/
  )
  assert.match(
    permissionSource,
    /this\.routers\s*=\s*authorizedStaticRoutes\.concat\(mergedStaticRoutes,\s*dynamicRoutesToAdd\)/
  )
})

test('authorized showroom route still keeps the static component shell for permitted users', () => {
  const showroomSource = readText('src/router/modules/showroom.ts')

  assert.match(showroomSource, /name: 'Showroom'/)
  assert.match(showroomSource, /component: Layout/)
  assert.match(showroomSource, /alwaysShow: true/)
  assert.match(showroomSource, /name: 'ShowroomAdminCompany'/)
  assert.match(showroomSource, /name: 'ShowroomAdminProduct'/)
  assert.match(showroomSource, /name: 'ShowroomAdminNarration'/)
})
