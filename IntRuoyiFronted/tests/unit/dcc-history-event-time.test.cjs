const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),{parse}=require('vue/compiler-sfc')
const ast=ts.createSourceFile('detail.ts',parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')).descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const decl=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text==='getVersionOperatorText'))
const c={userNameMap:{value:new Map([[7,'编制人甲']])},formatControlledFileDateTime:v=>v==null?'-':v}
vm.runInNewContext(ts.transpileModule(decl.getText(ast)+'\nexports=getVersionOperatorText',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
test('unsent working history shows its real creation event, including after it is superseded',()=>{assert.equal(c.exports({status:'SUPERSEDED',versionNo:'Z/1-1',submitterId:7,createTime:'2026-10-14 03:18:43'}),'编制人甲；创建：2026-10-14 03:18:43')})
test('formal submitted history keeps submission time distinct from row creation',()=>{assert.equal(c.exports({submitterId:7,createTime:'2026-10-14 01:00:00',submittedTime:'2026-10-14 02:00:00'}),'编制人甲；提交：2026-10-14 02:00:00')})
test('no recorded event time is reported explicitly without a browser clock default',()=>{assert.equal(c.exports({submitterId:7}),'编制人甲；时间未记录')})
