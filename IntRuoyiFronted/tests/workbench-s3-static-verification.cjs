const assert=require('node:assert/strict')
const fs=require('node:fs')
const path=require('node:path')
const {ESLint}=require('eslint')
const {createRequire}=require('node:module')
const compiler=createRequire(require.resolve('@vitejs/plugin-vue'))('@vue/compiler-sfc')
const roots=['src/views/mes/pro/edhr-config','src/views/erp/sync/config','src/views/dcc/registration-certificate/config','src/views/infra/config/basic']
const collect=dir=>fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?collect(path.join(dir,e.name)): /\.(vue|ts|tsx)$/.test(e.name)?[path.join(dir,e.name).replaceAll('\\','/')]:[])
const files=[...roots.flatMap(collect),
'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue','src/views/erp/sync/index.vue','src/views/dcc/registration-certificate/index/index.vue','src/views/infra/config/index.vue',
'src/views/Profile/Index.vue','src/views/Profile/components/index.ts','src/views/system/config-migration/BusinessConfigMigrationNotice.vue',
'src/router/modules/businessConfig.ts','src/router/modules/remaining.ts','src/store/modules/permission.ts','src/utils/routerHelper.ts','src/utils/businessConfigAccess.ts',
'src/workbench/contracts.ts','src/workbench/registryContract.ts','src/workbench/registry.ts','src/workbench/queryContext.ts',
'src/workbench-sources/dcc/registration.ts','src/workbench-sources/mes/registration.ts','src/workbench-sources/showroom/registration.ts',
'src/api/system/profileWorkbenchTodo/index.ts','src/store/modules/profileWorkbenchTodoBadge.ts','src/views/Profile/components/ProfileWorkbench.vue','src/utils/edhrWorkTaskNavigation.ts',
'src/utils/auth.ts','src/config/axios/service.ts','src/utils/exactJavaLong.ts','src/api/mes/pro/edhr/batchExecution.ts','src/layout/components/Menu/src/Menu.vue','src/layout/components/TabMenu/src/TabMenu.vue','src/layout/components/TagsView/src/TagsView.vue']
;(async()=>{
 for(const file of files)assert.ok(fs.existsSync(file),file)
 const eslint=new ESLint({ignore:false,overrideConfig:{parserOptions:{parser:require.resolve('@typescript-eslint/parser')}}})
 const results=await eslint.lintFiles(files)
 console.log(await (await eslint.loadFormatter('stylish')).format(results))
 assert.equal(results.reduce((n,r)=>n+r.errorCount,0),0,'project ESLint errors')
 let count=0
 for(const file of files.filter(f=>f.endsWith('.vue'))){
  const parsed=compiler.parse(fs.readFileSync(file,'utf8'),{filename:file});assert.deepEqual(parsed.errors,[],file)
  const script=compiler.compileScript(parsed.descriptor,{id:'s3-'+count})
  if(parsed.descriptor.template){const template=compiler.compileTemplate({source:parsed.descriptor.template.content,filename:file,id:'s3-'+count,compilerOptions:{bindingMetadata:script.bindings}})
  assert.deepEqual(template.errors,[],file)}
  for(const style of parsed.descriptor.styles){
   const source=style.lang==='scss'?fs.readFileSync('src/styles/variables.scss','utf8')+'\n'+style.content:style.content
   assert.deepEqual(compiler.compileStyle({source,filename:file,id:'s3-'+count,scoped:style.scoped,preprocessLang:style.lang,preprocessCustomRequire:id=>require(id)}).errors,[],file)
  }
  count++
 }
 console.log(JSON.stringify({lintFiles:files.length,lintErrors:0,sfcFiles:count,files},null,2))
})().catch(error=>{console.error(error);process.exitCode=1})
