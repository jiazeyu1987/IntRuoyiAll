package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

/** Expressions follow the visible text, including labels and reference counts. */
public final class DccBrowserListFilterSql {
    private DccBrowserListFilterSql() {}
    public static final String PROJECT_COUNT="(SELECT COUNT(DISTINCT usage_ref.project_id) FROM dcc_project_file_reference usage_ref WHERE usage_ref.tenant_id=f.tenant_id AND usage_ref.master_id=f.master_id AND usage_ref.selected_controlled_file_id=f.id)";
    public static String status(String expression) {
        return "CASE "+expression+" WHEN 'DRAFT' THEN '草稿' WHEN 'WORKING' THEN '工作版本' WHEN 'PENDING_SUPERVISOR_REVIEW' THEN '待直属主管审核' WHEN 'PENDING_DOC_CONTROL_REVIEW' THEN '待文控审核' WHEN 'PENDING_MATRIX_REVIEW' THEN '待会签审核' WHEN 'PENDING_MATRIX_APPROVAL' THEN '待会签批准' WHEN 'PENDING_DOC_CONTROL_APPROVAL' THEN '待文控批准' WHEN 'PENDING_APPLICANT_REWORK' THEN '待申请人处理回退' WHEN 'PENDING_APPLICANT_TRAINING_RECORD' THEN '待文件编制人上传培训记录' WHEN 'PENDING_DELAYED_EFFECTIVE_CONFIRM' THEN '待延迟生效确认' WHEN 'READY_TO_PUBLISH' THEN '待受控' WHEN 'FINALIZING' THEN '受控处理中' WHEN 'TRAINING_IN_PROGRESS' THEN '培训中' WHEN 'PENDING_MANUAL_DISTRIBUTION' THEN '待文控下发' WHEN 'CONTROLLED_PENDING_EFFECTIVE' THEN '受控（待生效）' WHEN 'ACTIVE' THEN '受控（已生效）' WHEN 'REJECTED' THEN '已驳回' WHEN 'WITHDRAWN' THEN '已撤回' WHEN 'OBSOLETE' THEN '已作废' WHEN 'SUPERSEDED' THEN '已替代' WHEN 'FINALIZATION_FAILED' THEN '受控处理失败' ELSE "+expression+" END";
    }
    public static Map<String, ListFilterQueryUtils.Field> projectFields() {
        return Map.of("file.fileName",text("f.file_name"), "file.fileNumber",text("f.file_number"),
                "file.projectName",text("p.project_name"), "file.folderName",text("COALESCE(folder.name,'未记录')"),
                "file.versionNo",text("f.version_no"), "file.status",text(status("f.status")),
                "projectCount",text("CONCAT("+PROJECT_COUNT+",' 个项目')"));
    }
    public static Map<String, ListFilterQueryUtils.Field> folderUnionFields() {
        return Map.of("file.fileName",text("f.file_name"), "file.fileNumber",text("f.file_number"),
                "file.projectName",text("f.project_name"), "file.folderName",text("COALESCE(f.project_folder_name,'未记录')"),
                "file.versionNo",text("f.version_no"), "file.status",text(status("f.status")),
                "projectCount",text("CONCAT(CASE WHEN f.reference_id IS NULL THEN '' ELSE '引用 ' END,"+PROJECT_COUNT+",' 个项目')"));
    }
    public static Map<String, ListFilterQueryUtils.Field> storageFields() {
        return Map.of("fileName",text("f.filter_file_name"),"fileNumber",text("f.filter_file_number"),
                "directory",text("f.filter_directory"),"category",text("f.filter_category"),"remark",text("f.filter_remark"));
    }
    public static String storageDisplayProjection() {
        String kind="CASE sf.status WHEN 'WORKING' THEN '工作小版本' WHEN 'CONTROLLED_PENDING_EFFECTIVE' THEN '受控（待生效）' "
                +"WHEN 'ACTIVE' THEN CASE WHEN NULLIF(TRIM(current_active.version_no),'') IS NULL THEN '受控版本（执行身份未记录）' "
                +"WHEN TRIM(sf.version_no)=TRIM(current_active.version_no) THEN '当前执行受控版本' ELSE '受控版本（非当前执行）' END "
                +"WHEN 'OBSOLETE' THEN '历史已作废' WHEN 'SUPERSEDED' THEN '历史已替换' WHEN 'REJECTED' THEN '历史申请' WHEN 'WITHDRAWN' THEN '历史申请' WHEN 'DRAFT' THEN '草稿版本' "
                +"WHEN 'PENDING_DOC_CONTROL_REVIEW' THEN '在途版本' WHEN 'PENDING_MATRIX_REVIEW' THEN '在途版本' WHEN 'PENDING_MATRIX_APPROVAL' THEN '在途版本' WHEN 'PENDING_DOC_CONTROL_APPROVAL' THEN '在途版本' WHEN 'PENDING_APPLICANT_REWORK' THEN '在途版本' WHEN 'PENDING_APPLICANT_TRAINING_RECORD' THEN '在途版本' WHEN 'READY_TO_PUBLISH' THEN '在途版本' WHEN 'FINALIZING' THEN '在途版本' WHEN 'TRAINING_IN_PROGRESS' THEN '在途版本' WHEN 'PENDING_MANUAL_DISTRIBUTION' THEN '在途版本' WHEN 'FINALIZATION_FAILED' THEN '在途版本' ELSE '版本属性未记录' END";
        String version="CASE WHEN sf.version_no IS NULL OR TRIM(sf.version_no)='' THEN '未记录' WHEN TRIM(sf.version_no) REGEXP '^[A-Za-z]+(/[1-9][0-9]*)+-[1-9][0-9]*$' THEN '草稿' ELSE sf.version_no END";
        String name="COALESCE(NULLIF(sf.title,''),NULLIF(f.file_name,''),'-')";
        String metadata=kind+",' ',"+version+",' ',COALESCE(path.path,'-'),' ',"
                +"CASE WHEN EXISTS(SELECT 1 FROM infra_file artifact WHERE artifact.id=sf.published_file_id AND artifact.deleted=0) THEN '已生成' ELSE '未生成' END,' ',"
                +"CASE WHEN EXISTS(SELECT 1 FROM infra_file artifact WHERE artifact.id=sf.stamped_file_id AND artifact.deleted=0) THEN '已生成' ELSE '未生成' END,' ',"
                +"CASE WHEN NULLIF(TRIM(current_active.version_no),'') IS NULL THEN '当前执行受控版本：未记录' ELSE CONCAT('当前执行受控版本：master 当前执行版本 ',TRIM(current_active.version_no)) END";
        return name+" AS filter_name,CONCAT("+metadata+") AS filter_metadata,COALESCE(sf.file_number,'') AS filter_number,"
                +"COALESCE(path.path,'-') AS filter_directory,COALESCE(category.name,'-') AS filter_category,"
                +"COALESCE(NULLIF(sf.remark,''),'-') AS filter_remark";
    }
}
