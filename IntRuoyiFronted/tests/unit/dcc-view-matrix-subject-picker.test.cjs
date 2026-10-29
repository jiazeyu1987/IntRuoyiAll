const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue'),{parse,compileTemplate}=require('vue/compiler-sfc')
const raw=fs.readFileSync('src/views/dcc/controlled-file/categories/components/CategoryViewMatrixDialog.vue','utf8'),sfc=parse(raw).descriptor
const cell=sfc.template.content.match(/<el-table-column label="对应(?:部门|主体)"[\s\S]*?<template #default="\{ row \}">([\s\S]*?)<\/template>\s*<\/el-table-column>/)[1]
const c={render:new Function('Vue',compileTemplate({source:cell,filename:'view-subject.vue',id:'view-subject',compilerOptions:{mode:'function'}}).code)(vue)}
function controls(type){const vnode=c.render({row:{subjectType:type},departmentLoadError:'',postOptions:[],approvalPositionOptions:[],defaultProps:{},getDepartmentSelectValue:()=>undefined,getDepartmentTreeForRule:()=>[],handleUserChange(){},handleRoleChange(){},handlePostChange(){},handleDccPositionChange(){},handleDepartmentChange(){}},[]),names=[]
 function walk(v){if(!v||typeof v!=='object')return;if(typeof v.type==='string')names.push(v.type);if(Array.isArray(v.children))v.children.forEach(walk)}walk(vnode);return names}
test('actual view-matrix slot renders user and role selectors instead of a department control',()=>{assert.ok(controls('USER').includes('UserSelectV2'));assert.ok(!controls('USER').includes('el-tree-select'));assert.ok(controls('ROLE').includes('RoleSelect'))})
test('department and organization/approval roles retain distinct real controls',()=>{assert.ok(controls('DEPT').includes('el-tree-select'));assert.ok(controls('POST').includes('el-select'));assert.ok(controls('DCC_POSITION').includes('el-select'))})
const ast=ts.createSourceFile('view.ts',sfc.scriptSetup.content,ts.ScriptTarget.Latest,true)
function declaration(name){return ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text===name))?.getText(ast)||''}
test('actual user/role handlers retain the selected kind and ID, and clear stale identity on removal',()=>{
 const context={resetPreviewState(){},postOptions:{value:[]},approvalPositionOptions:{value:[]}}
 const code=['handleUserChange','handleRoleChange','handlePostChange','handleDccPositionChange'].map(declaration).join('\n')+'\nthis.handlers={handleUserChange,handleRoleChange,handlePostChange,handleDccPositionChange}'
 vm.runInNewContext(ts.transpileModule(code,{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,context)
 for(const [kind,name,subject] of [['USER','handleUserChange',{id:910343,nickname:'正文浏览乙'}],['ROLE','handleRoleChange',{id:991234,name:'下载角色'}]]){const row={subjectType:'DEPT',subjectId:100,subjectDepartmentPath:'旧部门'};context.handlers[name](row,subject);assert.equal(row.subjectType,kind);assert.equal(row.subjectId,subject.id);assert.equal(row.subjectDepartmentPath,undefined);context.handlers[name](row,undefined);assert.equal(row.subjectId,undefined)}
})
