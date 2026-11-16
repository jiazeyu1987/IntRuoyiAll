package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummaryEnricher.FilterContext;
import java.util.Map;
import static cn.iocoder.yudao.module.dcc.approval.DccApprovalListFilterSql.*;

/** Exact file-version overrides applied by the two native summary enrichers. */
final class DccNativeApprovalFilterSql {
    private DccNativeApprovalFilterSql() { }
    static void enrich(Map<String,String> facts, FilterContext context, boolean obsolete) {
        if (!obsolete && !"BPM_PROCESS_INSTANCE".equals(context.sourceType())) return;
        String keys=obsolete ? "'dcc-controlled-file-obsolete','dcc-controlled-file-obsolete-approval'"
                : "'dcc-controlled-file-approval','dcc-controlled-file-upload','dcc-controlled-file-revision'";
        String applies="("+context.applicable()+") AND "+context.definitionKey()+" IN ("+keys+")";
        String fileId=obsolete ? "(SELECT nf.object_id FROM bpm_form_action_instance nf WHERE nf.tenant_id=CAST("+context.tenantId()+" AS UNSIGNED) AND CAST(nf.bpm_process_instance_id AS BINARY)=CAST("+context.processId()+" AS BINARY) AND nf.deleted=0 AND nf.system_code='DCC' AND nf.object_type='CONTROLLED_FILE' AND nf.action_code='OBSOLETE')" : context.businessKey();
        String from=" FROM dcc_controlled_file df LEFT JOIN dcc_file_category dc ON dc.id=df.category_id AND dc.tenant_id=df.tenant_id AND dc.deleted=0 WHERE ("+applies+") AND df.tenant_id=CAST("+context.tenantId()+" AS UNSIGNED) AND CAST(CAST(df.id AS CHAR) AS BINARY)=CAST("+fileId+" AS BINARY)"+(obsolete?" AND df.deleted=0":"");
        String base="ndcc_"+(obsolete?"obsolete":"native");
        String category="COALESCE(NULLIF(TRIM(dc.name),''),CASE WHEN df.category_id IS NULL THEN '-' ELSE CONCAT('缺失类别#',df.category_id) END)";
        context.projections().put(base+"_file","(SELECT JSON_OBJECT('title',df.title,'code',df.file_number,'version',COALESCE(NULLIF(TRIM(df.version_no),''),'-'),'category',"+category+",'stamp',CASE WHEN df.stamped_file_id IS NOT NULL THEN '已生成' ELSE '需要' END,'distribution',CASE WHEN dc.distribution_required=1 THEN '需要' ELSE '不需要' END)"+from+")");
        context.projections().put(base+"_node",context.currentNodeName());
        java.util.function.Function<String,String> file=key->"JSON_UNQUOTE(JSON_EXTRACT("+base+"_file.value,'$."+key+"'))";
        replace(facts,context,obsolete,"title",applies,titleLabel(file.apply("title")));
        replace(facts,context,obsolete,"identifier",applies,"CONCAT('业务编号：',TRIM("+file.apply("code")+"))");
        String array="JSON_ARRAY(JSON_OBJECT('label','文件编号','value',"+file.apply("code")+"),JSON_OBJECT('label','版本','value',"+file.apply("version")+"),JSON_OBJECT('label','分类','value',"+file.apply("category")+"),JSON_OBJECT('label','当前节点','value',"+base+"_node.value),JSON_OBJECT('label','盖章','value',"+file.apply("stamp")+"),JSON_OBJECT('label','分发','value',"+file.apply("distribution")+"))";
        replace(facts,context,obsolete,"extraSummary",applies,visibleTags(array));
    }
    private static String visibleTags(String array) {
        String positions="(SELECT 0 seq UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5)";
        String value="JSON_UNQUOTE(JSON_EXTRACT(da.tags,CONCAT('$[',di.seq,'].value')))";
        String label="JSON_UNQUOTE(JSON_EXTRACT(da.tags,CONCAT('$[',di.seq,'].label')))";
        String visible="NULLIF(TRIM(dv.tag_value),'') IS NOT NULL AND LOWER(TRIM(dv.tag_value)) NOT IN ('--','null','undefined') AND dv.tag_value NOT LIKE '未配置%' AND dv.tag_value NOT LIKE '%已配置'";
        String aggregate="(SELECT JSON_ARRAYAGG(CONCAT(dv.tag_label,'：',dv.tag_value)) OVER (ORDER BY dv.seq ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM (SELECT di.seq,"+label+" tag_label,"+cn.iocoder.yudao.module.bpm.approval.service.NativeApprovalListFilterSql.contextValue(value)+" tag_value FROM (SELECT "+array+" tags) da CROSS JOIN "+positions+" di) dv WHERE "+visible+" LIMIT 1)";
        return "COALESCE((SELECT JSON_UNQUOTE(CONCAT(CHAR(34),REPLACE(SUBSTRING(CAST(dj.tags AS CHAR),3,CHAR_LENGTH(CAST(dj.tags AS CHAR))-4),CONCAT(CHAR(34),', ',CHAR(34)),' '),CHAR(34))) FROM (SELECT "+aggregate+" tags) dj),'')";
    }
    private static void replace(Map<String,String> facts,FilterContext context,boolean obsolete,String key,String applies,String value) {
        String alias="ndcc_"+(obsolete?"obsolete_":"native_")+key;
        context.projections().put(alias,value);
        facts.put(key,"CASE WHEN "+applies+" THEN "+alias+".value ELSE "+facts.getOrDefault(key,"NULL")+" END");
    }
}
