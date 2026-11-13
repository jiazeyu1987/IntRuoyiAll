package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportPageReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordFormSlotType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordRecognitionRouteKeys;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportView;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

/** Generated catalog rows, including version/route-product expansion, are filtered and paged in SQL. */
@Mapper
public interface MesProGeneratedReportQueryMapper {
    default QueryWrapper<Object> baseFilters(BatchRecordReportPageReqVO req) {
        QueryWrapper<Object> query = new QueryWrapper<>();
        query.setParamAlias("base");
        query.eq(StrUtil.isNotBlank(req.getReportId()), "r.report_id", StrUtil.trim(req.getReportId()))
                .eq(StrUtil.isNotBlank(req.getRouteKey()), "r.route_key", MesProBatchRecordRecognitionRouteKeys.normalize(req.getRouteKey()))
                .eq(StrUtil.isNotBlank(req.getBatchRecordName()), "r.batch_record_name", StrUtil.trim(req.getBatchRecordName()));
        if (StrUtil.isNotBlank(req.getFormSlotType())) {
            String slot = MesProBatchRecordFormSlotType.normalize(req.getFormSlotType());
            if (slot == null) query.apply("1 = 0");
            else query.eq("r.form_slot_type", slot);
        }
        if (StrUtil.isNotBlank(req.getName())) {
            String name = req.getName().trim().toLowerCase(java.util.Locale.ROOT);
            query.and(q -> q.like("LOWER(r.batch_record_name)", name).or().like("LOWER(r.report_name)", name).or().like("LOWER(r.report_code)", name));
        }
        return query;
    }

    default QueryWrapper<Object> productFilters(BatchRecordReportPageReqVO req) {
        QueryWrapper<Object> query = new QueryWrapper<>();
        query.setParamAlias("product");
        query.like(StrUtil.isNotBlank(req.getProductName()), "LOWER(r.product_name)", StrUtil.trimToEmpty(req.getProductName()).toLowerCase(java.util.Locale.ROOT))
                .eq(StrUtil.isNotBlank(req.getVersionNo()), "r.version_no", StrUtil.trim(req.getVersionNo()));
        return query;
    }

    default QueryWrapper<Object> columnFilters(BatchRecordReportPageReqVO req) {
        return ListFilterQueryUtils.wrapper(req, Map.ofEntries(
                Map.entry("productName", text("r.product_name")),
                Map.entry("projectCode", text("r.project_code")),
                Map.entry("reportName", text("r.report_name")),
                Map.entry("versionNo", text("r.version_no")),
                Map.entry("formSlotType", text("CASE r.form_slot_type WHEN 'MAIN' THEN '批记录' WHEN 'FORM' THEN '表单' WHEN 'LOSS_REPORT' THEN '损耗单' WHEN 'PROCESS_INSPECTION' THEN '过程检验单' WHEN 'PARAMETER_RECORD' THEN '参数记录表' ELSE r.form_slot_type END")),
                Map.entry("versionStatus", text("CASE r.version_status WHEN 'DRAFT' THEN '审批中' WHEN 'PRECHECK_PASSED' THEN '审批中' WHEN 'PENDING_APPROVAL' THEN '审批中' WHEN 'APPROVED' THEN '已发布' WHEN 'REJECTED' THEN '已驳回' WHEN 'VOIDED' THEN '已作废' WHEN 'OBSOLETE' THEN '已作废' ELSE COALESCE(NULLIF(r.version_status,''),'-') END")),
                Map.entry("updateTime", dateTime("r.update_time"))));
    }

    long countRows(@Param("tenantId") Long tenantId, @Param("latest") boolean latest,
                   @Param("base") QueryWrapper<Object> base, @Param("product") QueryWrapper<Object> product,
                   @Param("ew") QueryWrapper<Object> columns);
    List<Row> selectPageRows(@Param("tenantId") Long tenantId, @Param("latest") boolean latest,
                   @Param("base") QueryWrapper<Object> base, @Param("product") QueryWrapper<Object> product,
                   @Param("ew") QueryWrapper<Object> columns, @Param("offset") long offset, @Param("limit") int limit);
    List<String> selectProductNames(@Param("tenantId") Long tenantId, @Param("latest") boolean latest,
                   @Param("base") QueryWrapper<Object> base, @Param("product") QueryWrapper<Object> product,
                   @Param("ew") QueryWrapper<Object> columns);
    Row selectInvalidVersion(@Param("tenantId") Long tenantId, @Param("base") QueryWrapper<Object> base);

    @Data
    class Row {
        private String batchRecordName;
        private Long batchRecordDefinitionId;
        private Long batchRecordVersionId;
        private String productName;
        private String projectCode;
        private Long dccProjectCodeId;
        private String versionNo;
        private String versionStatus;
        private String formSlotType;
        private String routeKey;
        private Integer sourceTableIndex;
        private String tableTitle;
        private String reportId;
        private String reportCode;
        private String reportName;
        private String sourceFileName;
        private LocalDateTime lastImportTime;
        private LocalDateTime updateTime;
        public MesProBatchRecordReportView toView() {
            return new MesProBatchRecordReportView(batchRecordName, batchRecordDefinitionId, batchRecordVersionId,
                    productName, projectCode, dccProjectCodeId, versionNo, versionStatus, formSlotType, routeKey,
                    sourceTableIndex, tableTitle, reportId, reportCode, reportName, sourceFileName, lastImportTime, updateTime);
        }
    }
}
