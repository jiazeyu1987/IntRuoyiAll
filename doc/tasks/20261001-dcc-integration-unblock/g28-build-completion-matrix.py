"""Inspect formal requirements and concrete source anchors; do not infer UI completion."""
from pathlib import Path
from html.parser import HTMLParser
import hashlib
import json
import re

ROOT=Path(__file__).resolve().parent
REPO=Path('C:/IntRuoyi/20261001-dcc-integration')
FE='IntRuoyiFronted/src/views/dcc/controlled-file/'
BE='IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/'

ANCHORS={
'AC-01':[(FE+'basic-data/components/ProjectReviewerConfiguration.vue',''),(BE+'service/projectcode/productcreate/DccProjectReviewerConfigurationService.java','requireConfigured'),(BE+'service/projectcode/productcreate/DccProjectProductCreateServiceImpl.java','')],
'AC-02':[(FE+'upload/index.vue',''),(BE+'service/projectcode/attributes/DccProjectApplicationSnapshotService.java','')],
'AC-03':[(FE+'detail/index.vue',''),(BE+'service/projectcode/attributes/DccProjectApplicationSnapshotService.java','')],
'AC-04':[(FE+'project-attributes/ProjectApplicationAttributes.vue',''),(BE+'service/projectcode/attributes/DccProjectApplicationSnapshotService.java','')],
'AC-05':[(FE+'project-attributes/ProjectAttributesFields.vue','')],
'AC-06':[(FE+'upload/index.vue',''),(FE+'upload/submitter.ts','')],
'AC-07':[(BE+'service/file/DccControlledFileWorkflowServiceImpl.java',''),(FE+'detail/index.vue','')],
'AC-08':[(BE+'service/file/DccControlledFileRevisionServiceImpl.java','requireFailedProcess'),(FE+'detail/index.vue','')],
'AC-09':[(BE+'service/file/DccControlledFileWorkflowServiceImpl.java',''),(FE+'detail/index.vue','')],
'AC-10':[(BE+'service/file/DccControlledFileQueryServiceImpl.java','checkin'),(FE+'browser/index.vue','')],
'AC-11':[(BE+'service/file/DccControlledFileVersionPolicy.java','formalTarget'),(BE+'service/file/DccControlledFileRevisionServiceImpl.java','createRevision')],
'AC-12':[(BE+'service/file/DccControlledFileLifecycleService.java',''),(BE+'service/file/DccWorkflowFileStateAudit.java','recordAutomaticObsolete')],
'AC-13':[(BE+'service/file/relations/DccRelationRemediationService.java','')],
'AC-14':[(FE+'detail/DetailRelationsPanel.vue',''),(BE+'service/file/DccControlledFileRelatedFileServiceImpl.java','')],
'AC-15':[(FE+'browser/ProjectBrowserPanel.vue',''),(FE+'detail/index.vue','')],
'AC-16':[(FE+'relations/DccProjectReferences.vue',''),(BE+'service/file/relations/DccProjectReferenceService.java','')],
'AC-17':[(FE+'relations/DccProjectReferences.vue',''),(BE+'service/file/relations/DccProjectReferenceService.java','')],
'AC-18':[(FE+'basic-data/components/FolderTemplateLibraryEditor.vue',''),(BE+'service/projectcode/folder/DccProjectFolderMaintenanceService.java','')],
'AC-19':[(FE+'workbench/index.vue',''),(BE+'service/file/DccControlledFileWorkflowServiceImpl.java','')],
'AC-20':[(BE+'service/file/DccControlledFileObsoleteService.java',''),(FE+'detail/index.vue','')],
'AC-21':[(BE+'service/file/DccControlledFileNameClaimService.java','preflightNewSourceName'),(BE+'dal/mysql/file/DccControlledFileNameClaimMapper.java','')],
'AC-22':[(FE+'shared/lifecycle.ts',''),(BE+'service/file/DccControlledFileRevisionServiceImpl.java','')],
'AC-23':[(FE+'shared/lifecycle.ts',''),(FE+'browser/presentation.ts','')],
'AC-24':[(BE+'service/file/DccControlledFileNameClaimService.java','retainObsoleteIdentity'),(BE+'service/file/DccWorkflowFileStateAudit.java','')],
'AC-25':[(BE+'service/file/DccControlledFileLifecycleService.java',''),(BE+'service/file/DccWorkflowDatePolicy.java','')],
'AC-26':[(FE+'workbench/index.vue',''),(BE+'service/file/listener/DccControlledFileActivationJob.java','')],
'AC-27':[(BE+'service/file/DccRevisionReworkPolicy.java','requireAttempt'),(BE+'service/file/DccControlledFileRevisionServiceImpl.java','createRevision')],
}

class Acceptance(HTMLParser):
    def __init__(self):super().__init__();self.in_row=False;self.in_cell=False;self.cells=[];self.buffer='';self.items={}
    def handle_starttag(self,tag,attrs):
        if tag=='tr':self.in_row=True;self.cells=[]
        if tag=='td' and self.in_row:self.in_cell=True;self.buffer=''
    def handle_endtag(self,tag):
        if tag=='td' and self.in_cell:self.cells.append(' '.join(self.buffer.split()));self.in_cell=False
        if tag=='tr':
            if len(self.cells)==3 and re.fullmatch('AC-[0-9]{2}',self.cells[0]):self.items[self.cells[0]]=self.cells[1:]
            self.in_row=False
    def handle_data(self,data):
        if self.in_cell:self.buffer+=data

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    requirement=REPO/'docs/product/dcc-final-requirements.html';parse=Acceptance();parse.feed(requirement.read_text(encoding='utf-8'))
    assert set(parse.items)==set(ANCHORS)
    rows=[];unresolved=[]
    for key,description in parse.items.items():
        sources=[]
        for path,method in ANCHORS[key]:
            p=REPO/path
            exists=p.is_file();method_found=exists and (not method or method in p.read_text(encoding='utf-8'))
            if not method_found:unresolved.append({'id':key,'path':path,'anchor':method})
            sources.append({'path':path,'anchor':method,'exists':exists,'anchorFound':method_found,'sha256':sha(p) if exists else None})
        rows.append({'id':key,'scenario':description[0],'expected':description[1],'sourceAnchors':sources,
                     'sourceTraceStatus':'SOURCE_LOCATED_NOT_BUSINESS_VERIFIED' if all(s['anchorFound'] for s in sources) else 'SOURCE_ANCHOR_NEEDS_REVIEW',
                     'realFrontendResult':'NOT_EXECUTED','completion':'NOT_PROVEN','nextGate':'Approved runtime prerequisites, exact task-owned data and actual Playwright page action/result proof'})
    result={'status':'COMPLETION_UNPROVEN_NO_SCOPE_REDUCTION','requirementsSha256':sha(requirement),'acceptanceCount':len(rows),'sourceAnchorFailures':unresolved,'requirements':rows,
            'knownRuntimePrerequisites':['Three unique missing original objects not approved for restoration; source proof35/39.','One new sidecar migration and exact verified history registration not executed.','26 audit operation configurations and genuine quality signature not approved/executed.','Natural minute activation and Shanghai7-day workbench reminders not runtime verified.'],
            'otherCompletionGates':{'finalUnitAndCombination':'Core579/17 andlatest324/5 overlap PASS','frontEndTypesAndBuild':'Prior verified unchanged16 frontend assets; G20types/build PASS','mainPackage':'G27 RootverifiedJar81b56aa3 PASS','realUiE2E':'NOT_EXECUTED','localIntQmsIntegration':'NOT_EXECUTED','postMergeVerification':'NOT_EXECUTED'},
            'authorization':'Code/helper/read-only review allowed; denied or missing runtime approvals remain enforced.','explicitDeferredScope':'20-year expiry disposal/release permissions and public reuse of retained Master identity are not approved for automatic action.'}
    (ROOT/'g28-completion-matrix.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    table=['# G28 完整需求完成审查','','所有27项前端验收均尚未实际执行，源码/隔离测试/打包不能替代真实页面结果。此表保留原目标，不缩为已有通过项。','','| 编号 | 前端验收场景 | 当前证明边界 |','| --- | --- | --- |']
    table.extend(f'| {r["id"]} | {r["scenario"].replace("|","／")} | 源码定位已记录；真实页面未验证 |' for r in rows)
    table+=['','Core579项与选择器324项有重叠；主应用已打包通过。恢复、三表登记和26项审计启用仍受实际批准约束，尚未合入本地int_qms。','',f'机器清单：g28-completion-matrix.json；源码定位缺项数量：{len(unresolved)}。']
    (ROOT/'g28-completion-matrix.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    print(json.dumps({'acceptanceCount':len(rows),'realUiPass':0,'sourceAnchorFailures':unresolved},ensure_ascii=False))

if __name__=='__main__':main()
