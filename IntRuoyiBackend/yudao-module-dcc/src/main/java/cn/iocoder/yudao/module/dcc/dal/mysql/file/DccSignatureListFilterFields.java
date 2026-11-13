package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

/** Static signature display columns. Current-copy verification fields are evaluated by the service. */
public final class DccSignatureListFilterFields {
    private DccSignatureListFilterFields() { }
    private static final Map<String,String> MEANINGS = Map.ofEntries(
            Map.entry("SUPERVISOR_REVIEW_APPROVE","直属主管审核通过"),
            Map.entry("SUPERVISOR_REVIEW_REJECT","直属主管审核驳回"),
            Map.entry("MATRIX_REVIEW_ASSIGN","会签指派"),
            Map.entry("DOC_CONTROL_REVIEW_APPROVE","文控审核通过"),
            Map.entry("DOC_CONTROL_REVIEW_REJECT","文控审核驳回"),
            Map.entry("DOC_CONTROL_REVIEW_RETURN","文控审核回退"),
            Map.entry("DOC_CONTROL_REVIEW_TRANSFER","文控审核转办"),
            Map.entry("DOC_CONTROL_REVIEW_ADD_SIGN","文控审核加签"),
            Map.entry("MATRIX_REVIEW_APPROVE","会签审核通过"),
            Map.entry("MATRIX_REVIEW_REJECT","会签审核驳回"),
            Map.entry("MATRIX_REVIEW_RETURN","会签审核回退"),
            Map.entry("MATRIX_REVIEW_TRANSFER","会签审核转办"),
            Map.entry("MATRIX_REVIEW_ADD_SIGN","会签审核加签"),
            Map.entry("MATRIX_APPROVAL_APPROVE","会签批准通过"),
            Map.entry("MATRIX_APPROVAL_REJECT","会签批准驳回"),
            Map.entry("MATRIX_APPROVAL_RETURN","会签批准回退"),
            Map.entry("MATRIX_APPROVAL_TRANSFER","会签批准转办"),
            Map.entry("MATRIX_APPROVAL_ADD_SIGN","会签批准加签"),
            Map.entry("DOC_CONTROL_APPROVAL_APPROVE","文控批准通过"),
            Map.entry("DOC_CONTROL_APPROVAL_REJECT","文控批准驳回"),
            Map.entry("DOC_CONTROL_APPROVAL_RETURN","文控批准回退"),
            Map.entry("DOC_CONTROL_APPROVAL_TRANSFER","文控批准转办"),
            Map.entry("DOC_CONTROL_APPROVAL_ADD_SIGN","文控批准加签"),
            Map.entry("DISTRIBUTION_ACK","发放签收"),
            Map.entry("DISTRIBUTION_SIGN","发放签发"),
            Map.entry("ARCHIVE_SEAL","归档盖章"),
            Map.entry("OBSOLETE_CONFIRM","作废确认"));
    private static final Map<String,String> ACTIONS = Map.ofEntries(
            Map.entry("ASSIGN","会签指派"),
            Map.entry("ASSIGNED","会签指派"),
            Map.entry("APPROVE","审批通过"),
            Map.entry("REJECT","审批驳回"),
            Map.entry("APPROVED","审批通过"),
            Map.entry("REJECTED","审批驳回"),
            Map.entry("RETURNED","流程回退"),
            Map.entry("TRANSFERRED","任务转办"),
            Map.entry("SIGN_ADDED","任务加签"),
            Map.entry("DISTRIBUTION_ACK","发放签收"),
            Map.entry("DISTRIBUTION_SIGN","发放签发"));
    public static Map<String, ListFilterQueryUtils.Field> fields() {
        String fileStatus="CASE "+file("status")
            +" WHEN 'DRAFT' THEN '草稿' WHEN 'PENDING_DOC_CONTROL_REVIEW' THEN '待文控审核' WHEN 'PENDING_MATRIX_REVIEW' THEN '待会签审核'"
            +" WHEN 'PENDING_MATRIX_APPROVAL' THEN '待会签批准' WHEN 'PENDING_DOC_CONTROL_APPROVAL' THEN '待文控批准' WHEN 'FINALIZING' THEN '受控处理中'"
            +" WHEN 'TRAINING_IN_PROGRESS' THEN '培训中' WHEN 'PENDING_MANUAL_DISTRIBUTION' THEN '待文控下发' WHEN 'ACTIVE' THEN '受控（已生效）'"
            +" WHEN 'REJECTED' THEN '已驳回' WHEN 'WITHDRAWN' THEN '已撤回' WHEN 'OBSOLETE' THEN '已作废' WHEN 'SUPERSEDED' THEN '已替代' WHEN 'FINALIZATION_FAILED' THEN '受控处理失败' ELSE '-' END";
        String action="CASE action_type WHEN 'ASSIGN' THEN 'ASSIGNED' WHEN 'APPROVE' THEN 'APPROVED' WHEN 'REJECT' THEN 'REJECTED' WHEN 'RETURN' THEN 'RETURNED' WHEN 'TRANSFER' THEN 'TRANSFERRED' WHEN 'ADD_SIGN' THEN 'SIGN_ADDED' ELSE action_type END";
        String actionLabel=label(action,ACTIONS,"CONCAT('未识别签名动作（',"+action+",'）')");
        String summary="CONCAT("+label("meaning_code",MEANINGS,"meaning_code")+",' ',"+actionLabel+",' ',"
            +snapshot("COALESCE(NULLIF(signature_purpose,''),meaning_code)")+",' ',COALESCE(DATE_FORMAT(signed_at,'%Y-%m-%d %H:%i:%s'),'-'))";
        return Map.ofEntries(
            Map.entry("fileName",text(file("file_name"))), Map.entry("fileNumber",text(file("file_number"))),
            Map.entry("revisionId",text("revision_id")),Map.entry("versionNo",text("version_no")),
            Map.entry("controlledFileStatus",text(fileStatus)),Map.entry("signerUserId",number("actor_id")),
            Map.entry("actorDeptPost",text("CONCAT("+snapshot("actor_dept_name_snapshot")+",' ',"+snapshot("actor_post_names_snapshot")+")")),
            Map.entry("actorRoleNamesSnapshot",text(snapshot("actor_role_names_snapshot"))),Map.entry("signatureSummary",text(summary)),
            Map.entry("sourceFileHash",text("source_file_hash")),Map.entry("evidenceHash",text("evidence_hash")),
            Map.entry("evidenceHashShort",text("LOWER(LEFT(evidence_hash,12))")),
            Map.entry("taskActionResult",text(action)),Map.entry("meaningCode",text("meaning_code")));
    }
    private static String file(String column) {
        return "(SELECT f."+column+" FROM dcc_controlled_file f WHERE f.id=dcc_controlled_file_signature.controlled_file_id AND f.deleted=0)";
    }
    private static String snapshot(String expression) {
        return "COALESCE(NULLIF(TRIM("+expression+"),''),'旧版证据未记录')";
    }
    private static String label(String expression,Map<String,String> values,String unknown) {
        StringBuilder sql=new StringBuilder("CASE ").append(expression);
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> sql.append(" WHEN '").append(entry.getKey()).append("' THEN '").append(entry.getValue()).append("'"));
        return sql+" ELSE CASE WHEN COALESCE("+expression+",'')='' THEN '-' ELSE "+unknown+" END END";
    }
}
