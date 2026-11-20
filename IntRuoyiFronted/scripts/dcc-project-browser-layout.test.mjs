import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import ts from 'typescript'
import * as Vue from 'vue'
import { parse, compileScript } from 'vue/compiler-sfc'

const scrollExports = {}
new Function('exports', ts.transpileModule(readFileSync(new URL('../src/views/dcc/controlled-file/browser/project-list-scroll.ts', import.meta.url),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText)(scrollExports)
globalThis.ResizeObserver = class { observe() {} disconnect() {} }

const source = readFileSync(new URL('../src/views/dcc/controlled-file/browser/ProjectBrowserPanel.vue', import.meta.url), 'utf8')
const project = { id: '5', projectName: '测试12345', projectCode: 'P5' }
const folder = { key: 'folder:51', folderId: '51', projectId: '5', name: 'G44文档' }
const node = type => ({ type, children: [], props: {}, text: '', scrollTop:0, querySelectorAll:()=>[] })
const renderer = Vue.createRenderer({
  createElement: node, createText: text => ({ ...node('#text'), text }), createComment: node,
  setText: (n, text) => { n.text = text }, setElementText: (n, text) => { n.text = text; n.children = [] },
  patchProp: (n, key, old, value) => { n.props[key] = value },
  insert(n, parent, anchor) { if(n.parent) { const at=n.parent.children.indexOf(n); if(at>=0)n.parent.children.splice(at,1) }; const at=anchor?parent.children.indexOf(anchor):-1; if(at<0)parent.children.push(n);else parent.children.splice(at,0,n); n.parent = parent },
  remove(n) { if(n.parent) { const at=n.parent.children.indexOf(n); if(at>=0)n.parent.children.splice(at,1) };n.parent=null },
  parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n)+1], setScopeId() {}
})
const all = n => [n, ...n.children.flatMap(all)]
const text = n => n.text + n.children.map(text).join('')
const settle = async () => { for(let i=0;i<12;i++) { await Promise.resolve(); await Vue.nextTick() } }

// State/API rules are covered by dcc-current-project-browser; this checks the parent UI wiring.
function mount() {
  let state
  class State {
    constructor() { state = this; Object.assign(this, { projects:[project], projectsTotal:1, projectPage:1, projectPageSize:20,
      projectQuery:{}, scope:'global', rows:[], references:[], directories:[], pageNo:1, pageSize:20, total:0, usage:{},
      keyword:'', projectKeyword:'', versionView:'LATEST_CONTROLLED', contextKey:'5', canReference:false, canMaintainFolders:true }) }
    async loadProjects() {} async loadActor() {} async loadFiles() {}
    async selectProject() { this.project = project; this.directories = [folder]; this.folder = undefined; this.scope = 'directory' }
    async selectFolder(value) { this.folder = value; this.scope = 'directory' }
    async search(scope) { this.scope = scope }
  }
  const stub = { setup: (_, ctx) => () => Vue.h('child') }
  const list = { props:['total'], setup:(props, ctx) => () => Vue.h('list', ctx.attrs, [
    ctx.slots.filters?.(), ctx.slots.actions?.(), ctx.slots.table?.({resolveRows: rows => rows})
  ]) }
  const compiled = compileScript(parse(source).descriptor, { id:'panel-layout-test', inlineTemplate:true }).content
  const exports = {}
  new Function('exports','require',ts.transpileModule(compiled,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(exports, name => {
    if(name==='vue')return Vue
    if(name==='vue-router')return { useRoute:()=>({fullPath:'/browser'}),useRouter:()=>({push(){}}) }
    if(name==='@/utils/auth')return {getTenantId:()=>1,getVisitTenantId:()=>undefined}
    if(name==='@/store/modules/user')return {useUserStore:()=>({getUser:{id:1}})}
    if(name==='@/utils/permission')return {checkPermi:()=>false}
    if(name==='./project-list-scroll')return scrollExports
    if(name==='./project-browser')return {ProjectBrowserState:State,PROJECT_BROWSER_VERSION_VIEWS:[]}
    if(name.endsWith('/project-reference-contract'))return {referenceIdentity:String}
    if(name.includes('UnifiedListTemplate'))return {default:list}
    return {default:stub}
  })
  const app=renderer.createApp(exports.default)
  for(const name of ['el-button','el-input','el-tree','el-tabs','el-tab-pane','el-alert','el-empty','el-tooltip','el-select','el-option','el-option-group'])
    app.component(name,{setup:(_,ctx)=>()=>Vue.h(name,ctx.attrs,ctx.slots.default?.())})
  app.directive('loading',{});app.directive('hasPermi',{})
  app.config.errorHandler=(error)=>{console.error('PANEL ERROR',error);throw error};const root=node('root');app.mount(root)
  return {app,root,get state(){return state}}
}

test('project selection, all-folders entry, folder and tabs keep the same project and synchronized path',async()=>{
  const m=mount()
  try {
    await settle()
    const projectButton=()=>all(m.root).find(n=>n.type==='button'&&text(n).includes('测试12345'))
    await projectButton().props.onClick();await settle()
    assert.equal(m.state.scope,'project');assert.equal(projectButton().props['aria-pressed'],true)
    assert.ok(text(m.root).includes('测试12345 / 全部文件夹'))
    const tree=()=>all(m.root).find(n=>n.type==='el-tree')
    assert.equal(tree().props['current-node-key'],'project:5:all')
    assert.equal(tree().props.data[0].children[0].folderId,'51')
    await tree().props.onNodeClick(folder);await settle()
    assert.equal(m.state.scope,'directory');assert.equal(tree().props['current-node-key'],folder.key)
    assert.ok(text(m.root).includes('测试12345 / G44文档'))
    assert.equal(projectButton().props['aria-pressed'],true)
    await tree().props.onNodeClick(tree().props.data[0]);await settle()
    assert.equal(m.state.scope,'project');assert.equal(tree().props['current-node-key'],'project:5:all')
    const maintenance=all(m.root).filter(n=>n.type==='el-button'&&['编辑文件夹','删除文件夹'].includes(text(n)))
    assert.equal(maintenance.length,2);assert.ok(maintenance.every(n=>n.props.disabled))
    assert.equal(m.state.folder.folderId,'51','remember folder for returning to the directory tab')
    const lists=all(m.root).filter(n=>n.type==='list')
    m.state.keyword='old';m.state.versionView='ALL';lists[1].props['server-filter-reset']()
    assert.equal(m.state.keyword,'');assert.equal(m.state.versionView,'LATEST_CONTROLLED')
  } finally {await settle();m.app.unmount()}
})


test('variable-height rows settle with complete first and last records at every scroll position',()=>{
  for (const heights of [[60,60,60,60,60,60],[61,89,44,110,75,68],[350,42,77],[32],[61.5,88.25,41.75,130.5]]) {
    let offset=0
    const rows=heights.map(height=>{const row={top:offset,height};offset+=height;return row})
    for(let top=0;top<=offset;top+=7) {
      const result=scrollExports.completeProjectWindow(rows,top)
      const first=rows.find(row=>row.top===result.top)
      const last=rows.find(row=>Math.abs(row.top+row.height-result.top-result.height)<0.001)
      assert.ok(first&&last,'viewport edges must be exact record boundaries')
      assert.ok(result.height<=280 || result.height===first.height,'a tall record must remain complete')
      assert.ok(result.top<=Math.max(0,offset-result.height),'scroll target must not be clamped by the browser')
    }
  }
})
