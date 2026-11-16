const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),{parse}=require('vue/compiler-sfc')
const src=parse(fs.readFileSync('src/views/approval-center/index.vue','utf8')).descriptor.scriptSetup.content,ast=ts.createSourceFile('approval-center.ts',src,ts.ScriptTarget.Latest,true)
const names=['EMPTY_APPROVAL_DISPLAY','APPROVAL_STATUS_LABELS','APPROVAL_NODE_LABELS','APPROVAL_BUSINESS_TITLE_LABELS','ENGLISH_LETTER_PATTERN','CHINESE_CHARACTER_PATTERN','BUSINESS_CONTEXT_CODE_PATTERN','normalizeApprovalDisplayText','containsEnglishLetters','containsChineseCharacters','isBusinessContextCode','resolveMappedApprovalText','resolveReviewerLabel','resolveNodeSubLabel','resolveBusinessContextValueLabel']
const code=ast.statements.filter(s=>s.declarationList?.declarations.some(d=>names.includes(d.name?.text))).map(s=>s.getText(ast)).join('\n')+'\nthis.node=resolveNodeSubLabel;this.context=resolveBusinessContextValueLabel'
const h={};vm.runInNewContext(ts.transpileModule(code,{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,h)
test('native supervisor task without a reviewer label displays its real Chinese status in node and context',()=>{
 assert.equal(h.node({moduleCode:'DCC',sourceTaskType:'DCC_CONTROLLED_FILE_TASK',businessStatus:'PENDING_SUPERVISOR_REVIEW'}),'待直属主管审核')
 assert.equal(h.context('PENDING_SUPERVISOR_REVIEW','未配置中文值'),'待直属主管审核')
})

test('native project request states display consistent Chinese labels in approval nodes, context and application records',()=>{
 const exports = {}
 const model = fs.readFileSync('src/views/dcc/controlled-file/basic-data/components/project-reviewer.ts','utf8')
 new Function('exports',ts.transpileModule(model,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(exports)
 for(const [status,label] of Object.entries({PENDING_REVIEW:'待审核',PENDING_APPROVAL:'待批准',REJECTED:'已驳回',WRITING:'写入中',COMPLETED:'已完成',WRITE_FAILED:'写入失败'})) {
  assert.equal(h.node({moduleCode:'DCC',sourceTaskType:'DCC_PROJECT_PRODUCT_CREATE',businessStatus:status}),label)
  assert.equal(h.context(status,'未配置中文值'),label)
  assert.equal(exports.projectRequestStatusLabel(status),label)
 }
 assert.equal(exports.projectRequestStatusLabel('UNKNOWN_PROJECT_STATE'),'未配置中文状态')
 assert.equal(exports.projectRequestStatusLabel(null),'--')
 const template=parse(fs.readFileSync('src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue','utf8')).descriptor.template.content
 assert.match(template, /prop="status"[^>]*>[\s\S]*?projectRequestStatusLabel\(row\.status\)/)
})
test('explicit actual reviewer remains the reviewer; existing stage statuses preserve their meanings',()=>{
 assert.equal(h.node({businessStatus:'PENDING_SUPERVISOR_REVIEW',assigneeUserName:'直属审核乙'}),'审核人：直属审核乙')
 for(const [status,label] of Object.entries({PENDING_MATRIX_REVIEW:'待会签',PENDING_MATRIX_APPROVAL:'待批准',PENDING_DOC_CONTROL_REVIEW:'待文控审核',ACTIVE:'现行',REJECTED:'已驳回'}))assert.equal(h.node({businessStatus:status}),label)
})
test('unknown or absent states still explicitly remain unconfigured or empty rather than a normal status',()=>{
 assert.equal(h.node({businessStatus:'UNKNOWN_DCC_STAGE'}),'未配置中文状态')
 assert.equal(h.context('UNKNOWN_DCC_STAGE','未配置中文值'),'未配置中文值')
 assert.equal(h.node({businessStatus:null}),'--')
})
