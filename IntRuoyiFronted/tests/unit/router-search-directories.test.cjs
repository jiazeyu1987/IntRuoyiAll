const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { computed, ref } = require('vue')

const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/components/RouterSearch/index.vue'),
  'utf8'
)
const script = source.match(/<script lang="ts" setup>([\s\S]*?)<\/script>/)[1]
const leaf = (path, title, meta = {}) => ({ path, meta: { title, ...meta } })
const directories = [
  '/mes',
  '/mes/edhr-process-routes',
  '/mes/with-page',
  '/system',
  '/system/role'
]
const tree = [
  {
    ...leaf('/mes', 'MES'),
    children: [
      {
        ...leaf('/mes/edhr-process-routes', '工艺路线'),
        children: [leaf('/mes/pro/route', '工艺流程')]
      },
      {
        ...leaf('with-page', '自身有页面的目录', { hasParentRoute: true }),
        children: [leaf('', '目录默认页', { hidden: true }), leaf('child', '业务子页')]
      },
      leaf('/mes/dv/machinery', '设备台账'),
      leaf('/mes/dv/machinery/type', '设备类型')
    ]
  },
  {
    ...leaf('/system', '系统管理'),
    children: [{ ...leaf('role', '角色管理'), children: [leaf('list', '权限角色')] }]
  },
  { ...leaf('/standalone', '独立页面包装'), children: [leaf('', '独立页面')] }
]
// 模拟 Vue Router 拍平后的记录：目录已无 children，并有同路径的隐藏默认页。
const routes = [
  ...directories.map((p) => leaf(p, '目录', { activeMenu: '/mes/pro/route' })),
  leaf('/mes/with-page', '目录默认页', { hidden: true }),
  leaf('/mes/pro/route', '工艺流程'),
  leaf('/mes/with-page/child', '业务子页'),
  leaf('/system/role/list', '权限角色'),
  leaf('/standalone', '独立页面'),
  leaf('/mes/dv/machinery', '设备台账'),
  leaf('/mes/dv/machinery/type', '设备类型'),
  leaf('/edit/:id', '编辑', { hidden: true, activeMenu: '/mes/pro/route' }),
  leaf('/hidden', '隐藏页', { hidden: true })
]
let history = [
  ...directories.map((value) => ({ value, label: '旧目录', query: '/' })),
  { value: '/mes/pro/route', label: '工艺流程', query: '工艺' },
  { value: '/edit/123', label: '旧编辑页', query: '编辑' }
]
const pushes = []
const context = vm.createContext({
  exports: {},
  computed,
  ref,
  useRouter: () => ({ getRoutes: () => routes, push: (value) => pushes.push(value) }),
  defineProps() {},
  defineExpose() {},
  onMounted() {},
  onUnmounted() {},
  require: (name) => {
    if (name === '@/hooks/web/useCache') {
      return {
        useCache: () => ({
          wsCache: {
            get: () => history,
            set: (_key, records) => {
              history = records
            }
          }
        })
      }
    }
    if (name === '@/utils/propTypes') return { propTypes: { string: { def() {} } } }
    if (name === '@/store/modules/permission') {
      return { usePermissionStore: () => ({ routers: tree }) }
    }
    throw new Error(`Unexpected import: ${name}`)
  }
})
vm.runInContext(
  ts.transpileModule(script, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
  }).outputText,
  context
)
const run = (code) => vm.runInContext(code, context)
const plain = (value) => JSON.parse(JSON.stringify(value))

assert.deepEqual(plain(run('Array.from(directoryPaths.value)')).sort(), directories.sort())
run('remoteMethod("/")')
assert.deepEqual(
  plain(run('filteredOptions.value.map(item => item.value)')).sort(),
  [
    '/mes/pro/route',
    '/mes/with-page/child',
    '/system/role/list',
    '/standalone',
    '/mes/dv/machinery',
    '/mes/dv/machinery/type'
  ].sort()
)
assert.deepEqual(plain(history.map((item) => item.value)), ['/mes/pro/route'])
for (const directory of directories) {
  run(`handleChange(${JSON.stringify(directory)})`)
}
assert.equal(pushes.length, 0, 'Directories must not navigate, even with an activeMenu')
run('handleChange("/mes/pro/route")')
assert.deepEqual(plain(pushes), [{ path: '/mes/pro/route' }])
assert.equal(run('resolveSearchablePath("/edit/123")'), '/mes/pro/route')
assert.equal(run('resolveSearchablePath("/hidden")'), '')
assert.equal(run('resolveSearchablePath("/mes/edhr-process-routes/?old=1")'), '')
console.log('RouterSearch directory filtering, history cleanup, and page navigation passed')
