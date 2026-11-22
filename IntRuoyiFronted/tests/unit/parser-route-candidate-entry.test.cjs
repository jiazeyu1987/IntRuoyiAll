const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const pinia = require('pinia')
const read = file => fs.readFileSync(path.resolve(__dirname, '../..', file), 'utf8')
function load(source, imports) {
  const context = { exports: {}, require: name => {
    assert.ok(Object.hasOwn(imports, name), `Unexpected import: ${name}`)
    return imports[name]
  } }
  vm.runInNewContext(ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context.exports
}

test('流程编辑替换、恢复页签不遍历 matched 中的组件实例和计算属性', async () => {
  const lodash = await import('lodash-es')
  const helper = load(read('src/utils/routerHelper.ts').replace(
    "import.meta.glob('../views/**/*.{vue,tsx}')", '{}'
  ), {
    'vue-router': {}, 'lodash-es': lodash, qs: {}, '@/utils/is': {},
    '@/utils/edhrNavigationIcons': { edhrDirectoryIcons: {}, edhrPageIcons: {} }
  })
  const store = pinia.createPinia()
  const { useTagsViewStore } = load(read('src/store/modules/tagsView.ts'), {
    '@/router': {}, '@/utils/routerHelper': helper, pinia, '../index': { store },
    '@/utils': {}, './user': {}, 'lodash-es': lodash
  })
  const tags = useTagsViewStore(store)
  const matched = { name: 'MesProRoute', path: '/mes/pro/route', meta: {} }
  Object.defineProperty(matched, 'instances', { enumerable: true, get() {
    throw new RangeError('运行时组件不可深拷贝')
  } })
  const list = { name: 'MesProRoute', path: '/mes/pro/route', fullPath: '/mes/pro/route', meta: {}, matched: [matched] }
  tags.addView(list)
  const edit = { ...list, name: 'MesProRouteEdit', path: '/mes/pro/route/edit/922033',
    fullPath: '/mes/pro/route/edit/922033?routeVersionId=22',
    params: { id: '922033' }, query: { routeVersionId: '22' },
    meta: { activeMenu: '/mes/pro/route', tagsViewKeyMode: 'path', title: '流程编辑' } }
  const snapshot = tags.replaceActiveMenuView(edit)
  assert.equal(tags.visitedViews.length, 1)
  assert.equal(tags.visitedViews[0].query.routeVersionId, '22')
  assert.equal(tags.visitedViews[0].matched[0].instances, undefined)
  assert.equal(snapshot.matched[0].instances, undefined)
  assert.notEqual(tags.visitedViews[0].meta, edit.meta)
  tags.restoreActiveMenuView(edit, snapshot)
  assert.equal(tags.visitedViews[0].path, '/mes/pro/route')
})

test('生产编制进入已创建草稿时直接编辑该候选版本，生效版本保持浏览模式', () => {
  const source = read('src/views/form-center/parser/SimpleParserPage.vue')
  const fn = source.slice(source.indexOf('function openRoute()'), source.indexOf('function beforeUnload('))
  for (const draft of [true, false]) {
    let navigation
    const published = { value: { routeId: 922033, routeVersionId: 21, routeVersionNo: 'V1',
      ...(draft ? { routeCandidateVersionId: 22, routeCandidateVersionNo: 'V2' } : {}) } }
    vm.runInNewContext(`${fn}; openRoute()`, { published, router: { push: value => { navigation = value } } })
    assert.equal(navigation.path, '/mes/pro/route/edit/922033')
    assert.equal(navigation.query.routeVersionId, draft ? '22' : '21')
    assert.equal(navigation.query.routeEditMode, draft ? 'edit' : undefined)
    assert.equal(navigation.query.routeVersionStatus, draft ? 'DRAFT' : 'ACTIVE')
  }
})
