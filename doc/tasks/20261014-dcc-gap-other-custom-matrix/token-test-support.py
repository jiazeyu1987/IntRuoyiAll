"""Explicit unused old recorded-route DTO ports only. Signer, serializer, canonicalizers, key and new proof code use actual current source."""
from pathlib import Path
import sys
out=Path(sys.argv[1]).resolve()
task=Path(__file__).resolve().parent
if task not in out.parents: raise ValueError('Private token support must stay in task')
def java(pkg,name,body):
    file=out/Path(pkg.replace('.','/'))/(name+'.java');file.parent.mkdir(parents=True,exist_ok=True);file.write_text('package '+pkg+';\n'+body+'\n',encoding='utf-8')
base='cn.iocoder.yudao.module.dcc.'
methods=['TenantId','SubmitterId','DccProjectCodeId','CategoryId','FileTypeTaxonomyId','Id','SelectedIterationControlledFileId']
texts=['ChangeType','PreparedProcessDefinitionId','PreparedDefinitionDigest']
java(base+'dal.dataobject.file','DccControlledFileDO','public class DccControlledFileDO {'+''.join('public Long get'+n+'(){throw new UnsupportedOperationException("Old recorded DTO not part of isolated test");}' for n in methods)+''.join('public String get'+n+'(){throw new UnsupportedOperationException("Old recorded DTO not part of isolated test");}' for n in texts)+'}')
java(base+'controller.admin.file.vo','DccApprovalMatrixBaselineVO','public class DccApprovalMatrixBaselineVO {public String getMatrixDigest(){throw new UnsupportedOperationException("Old recorded baseline DTO not part of isolated test");}}')
java(base+'service.file','DccControlledFileApprovalRouteAssigneeResolver','public class DccControlledFileApprovalRouteAssigneeResolver {public record ResolvedRouteNode(String stageCode,java.util.List<Long> resolvedUserIds,java.util.List<Long> candidateSourceIds){}}')
print('Isolated unused legacy DTO ports only; actual signing/mac/properties/JsonUtils compiled.')
