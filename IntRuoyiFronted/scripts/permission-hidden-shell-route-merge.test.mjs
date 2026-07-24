import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import vm from 'node:vm'
import ts from 'typescript'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const toPlainArray = (value) => Array.from(value || [])

const flattenRelativeRouteEntries = (routes, parentPath = '') => {
  const entries = []
  for (const route of routes || []) {
    const pathPart = String(route.path || '')
    const normalizedPath = pathPart
      ? `${parentPath}/${pathPart}`.replace(/\/+/g, '/').replace(/^\//, '')
      : parentPath
    entries.push({
      name: String(route.name || ''),
      path: normalizedPath
    })
    if (route.children?.length) {
      entries.push(...flattenRelativeRouteEntries(route.children, normalizedPath))
    }
  }
  return entries
}

const buildPermissionHelpers = () => {
  const source = readText('src/store/modules/permission.ts')
  const start = source.indexOf('const normalizeTopLevelRoutePath =')
  const end = source.indexOf('export interface PermissionState')

  assert.notEqual(start, -1, 'permission helper block should exist')
  assert.notEqual(end, -1, 'permission helper block end should exist')

  const snippet = `
const cloneDeep = globalThis.__cloneDeep
const isUrl = globalThis.__isUrl
${source.slice(start, end)}
module.exports = {
  mergeStaticRoutesWithDynamicRoutes
}
`

  const transpiled = ts.transpileModule(snippet, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020
    }
  })

  const module = { exports: {} }
  const context = {
    module,
    exports: module.exports,
    globalThis: {
      __cloneDeep: (value) => structuredClone(value),
      __isUrl: (value) => /^https?:\/\//.test(String(value || ''))
    }
  }

  vm.runInNewContext(transpiled.outputText, context)
  return module.exports
}

test('duplicate hidden static shells should not swallow visible top-level DCC and MES menus', () => {
  const { mergeStaticRoutesWithDynamicRoutes } = buildPermissionHelpers()

  const staticRoutes = [
    {
      path: '/dcc',
      name: 'DccCenterHidden',
      meta: { hidden: true },
      children: [
        {
          path: 'controlled-file/detail/:id(\\d+)',
          name: 'DccControlledFileDetail',
          meta: { hidden: true, canTo: true }
        }
      ]
    },
    {
      path: '/mes',
      name: 'MesWmRouter',
      meta: { hidden: true },
      children: [
        {
          path: 'wm/warehouse/location',
          name: 'MesWmLocation',
          meta: { hidden: true, canTo: true }
        }
      ]
    }
  ]

  const dynamicRoutes = [
    {
      path: '/dcc',
      name: 'DccCenter',
      meta: { hidden: false, title: '文控中心' },
      children: [
        {
          path: 'controlled-file/mine',
          name: 'DccControlledFileMine',
          meta: { hidden: false, title: '我的受控文件' }
        }
      ]
    },
    {
      path: '/mes',
      name: 'Mes',
      meta: { hidden: false, title: 'MES系统' },
      children: [
        {
          path: 'home',
          name: 'MesHome',
          meta: { hidden: false, title: 'MES首页' }
        }
      ]
    }
  ]

  const { mergedStaticRoutes, dynamicRoutesToAdd, mergedRoutesToReplace } =
    mergeStaticRoutesWithDynamicRoutes(
      staticRoutes,
      dynamicRoutes
    )

  assert.equal(dynamicRoutesToAdd.length, 0)

  const dccRoute = mergedStaticRoutes.find((route) => route.path === '/dcc')
  const mesRoute = mergedStaticRoutes.find((route) => route.path === '/mes')
  const dccRouteToReplace = mergedRoutesToReplace.find((route) => route.path === '/dcc')
  const mesRouteToReplace = mergedRoutesToReplace.find((route) => route.path === '/mes')

  assert.equal(dccRoute?.meta?.hidden, false)
  assert.deepEqual(
    toPlainArray(dccRoute?.children?.map((child) => child.name)),
    ['DccControlledFileMine', 'DccControlledFileDetail']
  )

  assert.equal(mesRoute?.meta?.hidden, false)
  assert.deepEqual(
    toPlainArray(mesRoute?.children?.map((child) => child.name)),
    ['MesHome', 'MesWmLocation']
  )

  assert.equal(dccRouteToReplace?.meta?.hidden, false)
  assert.deepEqual(
    toPlainArray(dccRouteToReplace?.children?.map((child) => child.name)),
    ['DccControlledFileMine', 'DccControlledFileDetail']
  )

  assert.equal(mesRouteToReplace?.meta?.hidden, false)
  assert.deepEqual(
    toPlainArray(mesRouteToReplace?.children?.map((child) => child.name)),
    ['MesHome', 'MesWmLocation']
  )
})

test('bpm hidden shell merge should not keep duplicate hidden children that conflict with dynamic descendants', () => {
  const { mergeStaticRoutesWithDynamicRoutes } = buildPermissionHelpers()

  const staticRoutes = [
    {
      path: '/bpm',
      name: 'bpm',
      meta: { hidden: true },
      children: [
        {
          path: 'process-instance/detail',
          name: 'BpmProcessInstanceDetail',
          meta: { hidden: true, canTo: true }
        },
        {
          path: 'process-instance/create',
          name: 'BpmProcessInstanceCreate',
          meta: { hidden: true, canTo: true }
        },
        {
          path: 'process-instance/my',
          name: 'BpmProcessInstanceMy',
          meta: { hidden: true, canTo: true }
        },
        {
          path: 'task/todo',
          name: 'BpmTodoTask',
          meta: { hidden: true, canTo: true }
        }
      ]
    }
  ]

  const dynamicRoutes = [
    {
      path: '/bpm',
      name: 'BpmRoot',
      meta: { hidden: false, title: '工作流程' },
      children: [
        {
          path: 'task',
          name: 'Task',
          meta: { hidden: false, title: '审批中心' },
          children: [
            {
              path: 'create',
              name: 'BpmProcessInstanceCreate',
              meta: { hidden: false, title: '发起流程' }
            },
            {
              path: 'my',
              name: 'BpmProcessInstanceMy',
              meta: { hidden: false, title: '我的流程' }
            },
            {
              path: 'todo',
              name: 'BpmTodoTask',
              meta: { hidden: false, title: '待办任务' }
            }
          ]
        }
      ]
    }
  ]

  const { mergedStaticRoutes, mergedRoutesToReplace } = mergeStaticRoutesWithDynamicRoutes(
    staticRoutes,
    dynamicRoutes
  )

  const mergedBpmRoute = mergedStaticRoutes.find((route) => route.path === '/bpm')
  const replaceBpmRoute = mergedRoutesToReplace.find((route) => route.path === '/bpm')

  const mergedEntries = flattenRelativeRouteEntries(mergedBpmRoute?.children || [])
  const replaceEntries = flattenRelativeRouteEntries(replaceBpmRoute?.children || [])

  assert.deepEqual(
    mergedEntries.filter((entry) => entry.name === 'BpmProcessInstanceCreate').map((entry) => entry.path),
    ['task/create']
  )
  assert.deepEqual(
    mergedEntries.filter((entry) => entry.name === 'BpmProcessInstanceMy').map((entry) => entry.path),
    ['task/my']
  )
  assert.deepEqual(
    mergedEntries.filter((entry) => entry.name === 'BpmTodoTask').map((entry) => entry.path),
    ['task/todo']
  )
  assert.deepEqual(
    replaceEntries.filter((entry) => entry.name === 'BpmProcessInstanceCreate').map((entry) => entry.path),
    ['task/create']
  )
  assert.deepEqual(
    replaceEntries.filter((entry) => entry.name === 'BpmProcessInstanceMy').map((entry) => entry.path),
    ['task/my']
  )
  assert.deepEqual(
    replaceEntries.filter((entry) => entry.name === 'BpmTodoTask').map((entry) => entry.path),
    ['task/todo']
  )
  assert.ok(
    mergedEntries.some((entry) => entry.name === 'BpmProcessInstanceDetail' && entry.path === 'process-instance/detail')
  )
})

test('showroom merge should not keep visible frontstage screen menu when dynamic showroom children omit it', () => {
  const { mergeStaticRoutesWithDynamicRoutes } = buildPermissionHelpers()

  const staticRoutes = [
    {
      path: '/showroom',
      name: 'Showroom',
      meta: { hidden: false, title: '展柜' },
      children: [
        {
          path: 'company',
          name: 'ShowroomAdminCompany',
          meta: { hidden: false, title: '公司信息' }
        },
        {
          path: 'display/screen/home',
          name: 'ShowroomDisplayScreenHome',
          meta: { hidden: false, title: '前台大屏' }
        },
        {
          path: 'display/screen/company',
          name: 'ShowroomDisplayScreenCompany',
          meta: { hidden: true, title: '大屏公司', activeMenu: '/showroom/display/screen/home' }
        }
      ]
    }
  ]

  const dynamicRoutes = [
    {
      path: '/showroom',
      name: 'Showroom',
      meta: { hidden: false, title: '展柜' },
      children: [
        {
          path: 'company',
          name: 'ShowroomAdminCompany',
          meta: { hidden: false, title: '公司信息' }
        }
      ]
    }
  ]

  const { mergedStaticRoutes } = mergeStaticRoutesWithDynamicRoutes(staticRoutes, dynamicRoutes)
  const showroomRoute = mergedStaticRoutes.find((route) => route.path === '/showroom')
  const visibleChildNames = toPlainArray(
    showroomRoute?.children?.filter((child) => !child.meta?.hidden).map((child) => child.name)
  )

  assert.deepEqual(visibleChildNames, ['ShowroomAdminCompany'])
})

test('showroom route module no longer registers frontstage screen device routes or legacy aliases', () => {
  const source = readText('src/router/modules/showroom.ts')

  assert.doesNotMatch(source, /path: 'display\/screen\/home'/)
  assert.doesNotMatch(source, /path: 'display\/screen\/company'/)
  assert.doesNotMatch(source, /path: 'display\/screen\/hall\/:hallId\(\\\\d\+\)'/)
  assert.doesNotMatch(source, /path: 'display\/screen\/product\/:productId\(\\\\d\+\)'/)
  assert.doesNotMatch(source, /path: 'display\/screen\/settings'/)
  assert.doesNotMatch(source, /path: 'display\/screen\/narration'/)
  assert.doesNotMatch(source, /path: 'display\/pad\/home'/)
  assert.doesNotMatch(source, /path: 'display\/mobile\/home'/)
  assert.doesNotMatch(source, /path: 'home'/)
  assert.doesNotMatch(source, /path: 'company-intro'/)
  assert.doesNotMatch(source, /path: 'display-hall\/:hallId\(\\\\d\+\)'/)
  assert.doesNotMatch(source, /path: 'display-product\/:productId\(\\\\d\+\)'/)
  assert.doesNotMatch(source, /legacyFrontstageAlias: true/)
  assert.doesNotMatch(source, /canonicalFrontstage: true/)
  assert.doesNotMatch(source, /deviceMode: 'screen'/)
  assert.doesNotMatch(source, /title: '前台大屏'/)
})
