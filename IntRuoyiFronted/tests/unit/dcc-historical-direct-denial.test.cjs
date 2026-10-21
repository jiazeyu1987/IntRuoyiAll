const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),{parse}=require('vue/compiler-sfc')
const routes=ts.createSourceFile('remaining.ts',fs.readFileSync('src/router/modules/remaining.ts','utf8'),ts.ScriptTarget.Latest,true)
let guard
function visit(n){if(ts.isObjectLiteralExpression(n)&&n.properties.some(p=>p.name?.getText(routes)==='name'&&p.initializer?.text==='DccControlledFileDetail'))guard=n.properties.find(p=>p.name?.getText(routes)==='beforeEnter')?.initializer;ts.forEachChild(n,visit)}
visit(routes);assert.ok(guard)
const context={String,Boolean,exports:{}}
vm.runInNewContext(ts.transpileModule('exports.guard='+guard.getText(routes),{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,context)
test('the native historical readonly address can reload without being silently sent to browser',()=>{
  const query={traceability:'1',traceScope:'trace',from:'version-history',returnTo:'%2Fdcc%2Fcontrolled-file%2Fdetail%2F21',bodyReadContext:'exact-history-context'}
  assert.equal(context.exports.guard({query}),true)
})
test('unscoped detail routes still return to browser and normal viewers remain admitted',()=>{
  assert.equal(context.exports.guard({query:{}}).name,'DccControlledFileBrowser')
  assert.equal(context.exports.guard({query:{viewer:'1',from:'version-history'}}),true)
})
test('a failed selected detail renders persistent explicit error without requiring loaded file metadata',()=>{
  const template=parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')).descriptor.template.content
  assert.match(template,/<ContentWrap\s+v-else-if="accessExplanationError && !fileDetail"[\s\S]*?data-testid="dcc-detail-read-error"[\s\S]*?:title="accessExplanationError"/)
})
