const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const ts=require('typescript')
const load=(path,requireModule)=>{const exports={};const js=ts.transpileModule(fs.readFileSync(path,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;new Function('exports','require',js)(exports,requireModule);return exports}
const facts=load('src/views/dcc/registration-certificate/shared/facts.ts')
const display=load('src/views/dcc/registration-certificate/shared/changeDisplay.ts',id=>{assert.equal(id,'./facts');return facts})
const format=models=>display.formatRegistrationCertificateChangeValue('CERTIFICATE_INFORMATION',JSON.stringify({value:JSON.stringify({certificateType:'CE',ownProduct:true,modelSpecifications:models})}))
test('CE approval history presents original, added and deleted model arrays',()=>{
 const added={before_value_json:JSON.stringify({value:JSON.stringify({certificateType:'CE',ownProduct:true,modelSpecifications:['A']})}),after_value_json:JSON.stringify({value:JSON.stringify({certificateType:'CE',ownProduct:true,modelSpecifications:['A','B']})})}
 assert.match(display.formatRegistrationCertificateChangeValue('CERTIFICATE_INFORMATION',added.before_value_json),/型号规格：A(?:\n|$)/)
 assert.match(display.formatRegistrationCertificateChangeValue('CERTIFICATE_INFORMATION',added.after_value_json),/型号规格：A；B/)
 assert.match(format(['B']),/型号规格：B(?:\n|$)/)
 assert.doesNotMatch(format(['B']),/型号规格：A/)
})
test('CE history distinguishes unmaintained models from an explicit empty list',()=>{
 assert.match(format(undefined),/型号规格：历史数据未维护/)
 assert.match(format([]),/型号规格：无（空列表）/)
 assert.match(format('A'),/记录解析失败：型号规格记录格式异常/)
})
