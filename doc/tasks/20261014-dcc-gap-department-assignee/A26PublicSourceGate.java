import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.service.file.DccApprovalMatrixCanonicalizer;
import cn.iocoder.yudao.module.dcc.service.file.DccApprovalMatrixSourceService;
import java.util.*;

/** Real current production public canonicalization and public-baseline restore guards, no replacement implementation. */
public class A26PublicSourceGate {
    static int passed;
    static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static Map<String,Object> baseline(Object version) {
        var raw=new TreeMap<String,Object>();raw.put("formatVersion",DccApprovalMatrixCanonicalizer.FORMAT);
        raw.put("tenantId",1L);raw.put("categoryId",2L);raw.put("fileTypeTaxonomyId",3L);raw.put("routeId",4L);raw.put("routeVersionNo",version);raw.put("actionType","LEGACY");raw.put("effectiveTime",null);raw.put("routeRemark",null);
        var node=new TreeMap<String,Object>();
        for(String field:List.of("id","stageNo","stageCode","stageName","stageOrder","sort","candidateSourceType","candidateSourceId","candidateSourceIds","approveMethod","approveRatio","requireAllApprovals","required","stageType","subjectLabel","marker","subjectType","subjectId","subjectName","subjectDepartmentPath","ruleRemark"))node.put(field,null);
        node.put("id",10L);node.put("stageNo",2);node.put("sort",1);node.put("stageName","真实公共会签");node.put("candidateSourceType","DEPT");node.put("approveMethod","ALL");node.put("requireAllApprovals",true);node.put("required",true);
        raw.put("nodes",List.of(node));return raw;
    }
    static void rejected(Runnable action,String name) {
        try{action.run();throw new AssertionError("PUBLIC accepted invalid version: "+name);}
        catch(IllegalArgumentException expected){require(expected.getMessage().contains("公共矩阵原配置合同无效"),"must reject with actual source-contract guard");passed++;System.out.println("PASS PUBLIC "+name);}
    }
    public static void main(String[] args) {
        String canonical=DccApprovalMatrixCanonicalizer.canonicalize(baseline(7));
        for(Object invalid:Arrays.asList(null,0,-1,"7")) {
            var raw=baseline(invalid);
            rejected(()->DccApprovalMatrixCanonicalizer.canonicalize(raw),"canonical-version="+invalid);
            // Keep every optional/raw key from a real valid canonical. Only the version changes.
            String changedValue=invalid==null?"null":invalid instanceof String?"\"7\"":invalid.toString();
            String malformed=canonical.replace("\"routeVersionNo\":7", "\"routeVersionNo\":"+changedValue);
            rejected(()->DccApprovalMatrixSourceService.readCanonical(malformed),"restore-version="+invalid);
        }
        var missing=baseline(7);missing.remove("routeVersionNo");
        rejected(()->DccApprovalMatrixCanonicalizer.canonicalize(missing),"canonical-version-missing");
        rejected(()->DccApprovalMatrixSourceService.readCanonical(canonical.replace("\"routeVersionNo\":7,", "")),"restore-version-missing");
        require(canonical.contains("\"routeVersionNo\":7"),"real version7 must be preserved");
        var restored=DccApprovalMatrixSourceService.readCanonical(canonical);
        require(Integer.valueOf(7).equals(restored.getRouteVersionNo())&&Long.valueOf(4).equals(restored.getRouteId()),"public restore must preserve exact original identity");passed++;System.out.println("PASS PUBLIC real7 canonical and restore exact");
        System.out.println("A26 PUBLIC SOURCE SUMMARY pass="+passed+"; actual2 fresh production sources; no full snapshot save/restore or reactor acceptance");
    }
}
