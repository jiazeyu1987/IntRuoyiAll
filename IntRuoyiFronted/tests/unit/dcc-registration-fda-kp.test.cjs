const {test}=require('node:test');const assert=require('node:assert/strict');const fs=require('node:fs');const vm=require('node:vm');const ts=require('typescript');
const ctx={exports:{},Error};vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/registration-certificate/shared/facts.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,ctx);
test('Traditional 510k requires nonblank K/P for upload and replacement, EXEMPT remains optional',()=>{
 const facts={certificateType:'FDA',classification:'II',submissionType:'TRADITIONAL_510K',customerHeld:false,approvalLetterStatus:'VALID',factoryRegistrationStatus:'ACTIVE'};
 for(const kpNumber of [undefined,null,'','   ']) for(const validate of [ctx.exports.validateFacts,ctx.exports.validateCreationFacts]) assert.throws(()=>validate({...facts,kpNumber}),/请填写K\/P号/);
 assert.doesNotThrow(()=>ctx.exports.validateCreationFacts({...facts,kpNumber:'K001'}));
 assert.doesNotThrow(()=>ctx.exports.validateCreationFacts({...facts,submissionType:'EXEMPT'}));
 assert.equal(ctx.exports.requiresKpNumber({...facts,certificateType:'CE'}),false);
});
