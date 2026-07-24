package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportCatalogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportCatalogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportDefinitionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportDefinitionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportExportAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportExportAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportExportAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportQueryRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDatasetDefinitionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrExportAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReportCatalogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReportDefinitionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDatasetDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrExportAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReportCatalogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReportDefinitionMapper;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReportErrorCodeConstants.PRO_EDHR_REPORT_CALIBER_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReportErrorCodeConstants.PRO_EDHR_REPORT_CATALOG_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReportErrorCodeConstants.PRO_EDHR_REPORT_DATA_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReportErrorCodeConstants.PRO_EDHR_REPORT_DEFINITION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReportErrorCodeConstants.PRO_EDHR_REPORT_EXPORT_AUDIT_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReportErrorCodeConstants.PRO_EDHR_REPORT_NOT_PUBLISHED;

@Service
public class MesProEdhrReportServiceImpl implements MesProEdhrReportService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String DATA_SOURCE_READY = "READY";
    private static final String OPERATION_EXPORT_AUDIT = "EXPORT_AUDIT";
    private static final String RESULT_RECORDED = "RECORDED";

    private static final Map<String, String> REPORT_SOURCE_TABLES = Map.of(
            "PRODUCTION_TRACE", "mes_pro_batch_record_execution",
            "DHR_TRACE", "mes_pro_edhr_batch_execution"
    );

    @Resource
    private MesProEdhrReportCatalogMapper catalogMapper;
    @Resource
    private MesProEdhrReportDefinitionMapper definitionMapper;
    @Resource
    private MesProEdhrDatasetDefinitionMapper datasetDefinitionMapper;
    @Resource
    private MesProEdhrExportAuditMapper exportAuditMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<MesProEdhrReportCatalogRespVO> getCatalogPage(MesProEdhrReportCatalogPageReqVO reqVO) {
        return BeanUtils.toBean(catalogMapper.selectPage(reqVO), MesProEdhrReportCatalogRespVO.class);
    }

    @Override
    public MesProEdhrReportCatalogRespVO getCatalogDetail(Long id) {
        MesProEdhrReportCatalogDO catalog = requireCatalog(id);
        return BeanUtils.toBean(catalog, MesProEdhrReportCatalogRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrReportDefinitionRespVO> getDefinitionPage(MesProEdhrReportDefinitionPageReqVO reqVO) {
        return BeanUtils.toBean(definitionMapper.selectPage(reqVO), MesProEdhrReportDefinitionRespVO.class);
    }

    @Override
    public MesProEdhrReportDefinitionRespVO getDefinitionDetail(Long id) {
        MesProEdhrReportDefinitionDO definition = requireDefinitionById(id);
        return BeanUtils.toBean(definition, MesProEdhrReportDefinitionRespVO.class);
    }

    @Override
    public MesProEdhrReportQueryRespVO runReportQuery(MesProEdhrReportQueryReqVO reqVO) {
        MesProEdhrReportDefinitionDO definition = requireRunnableDefinition(reqVO.getReportDefinitionId(),
                reqVO.getReportCode());
        MesProEdhrDatasetDefinitionDO dataset = requireReadyDataset(definition);
        String sourceTable = requireWhitelistedSource(definition.getReportCode(), dataset.getSourceObject());
        assertSourceTableExists(sourceTable);
        Long recordCount = queryRecordCount(sourceTable);
        LocalDateTime dataUpdatedAt = queryLatestUpdateTime(sourceTable);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("reportCode", definition.getReportCode());
        row.put("reportName", definition.getReportName());
        row.put("sourceObject", sourceTable);
        row.put("recordCount", recordCount);
        row.put("latestCreateTime", dataUpdatedAt);
        row.put("caliberVersion", definition.getCaliberVersion());
        return new MesProEdhrReportQueryRespVO()
                .setReportDefinitionId(definition.getId())
                .setReportCode(definition.getReportCode())
                .setReportName(definition.getReportName())
                .setCaliberVersion(definition.getCaliberVersion())
                .setDataUpdatedAt(dataUpdatedAt == null ? now() : dataUpdatedAt)
                .setFilterSnapshotJson(StrUtil.blankToDefault(reqVO.getFilterSnapshotJson(), "{}"))
                .setPermissionSummaryJson(definition.getPermissionSummaryJson())
                .setDataSourceSummary(dataset.getSourceObject())
                .setRows(List.of(row));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrReportExportAuditRespVO recordExportAudit(MesProEdhrReportExportAuditReqVO reqVO) {
        MesProEdhrReportDefinitionDO definition = requireRunnableDefinition(reqVO.getReportDefinitionId(),
                reqVO.getReportCode());
        requireReadyDataset(definition);
        MesProEdhrExportAuditDO audit = new MesProEdhrExportAuditDO()
                .setReportDefinitionId(definition.getId())
                .setReportCode(definition.getReportCode())
                .setReportName(definition.getReportName())
                .setCaliberVersion(definition.getCaliberVersion())
                .setOperationType(OPERATION_EXPORT_AUDIT)
                .setFilterSnapshotJson(reqVO.getFilterSnapshotJson())
                .setPermissionSummaryJson(reqVO.getPermissionSummaryJson())
                .setDataRangeSummary(reqVO.getDataRangeSummary())
                .setResultStatus(RESULT_RECORDED)
                .setOperatorUserId(SecurityFrameworkUtils.getLoginUserId())
                .setOperatorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setOccurredAt(now());
        if (exportAuditMapper.insert(audit) != 1 || audit.getId() == null) {
            throw exception(PRO_EDHR_REPORT_EXPORT_AUDIT_FAILED);
        }
        return BeanUtils.toBean(audit, MesProEdhrReportExportAuditRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrReportExportAuditRespVO> getExportAuditPage(MesProEdhrReportExportAuditPageReqVO reqVO) {
        return BeanUtils.toBean(exportAuditMapper.selectPage(reqVO), MesProEdhrReportExportAuditRespVO.class);
    }

    private MesProEdhrReportCatalogDO requireCatalog(Long id) {
        MesProEdhrReportCatalogDO catalog = id == null ? null : catalogMapper.selectById(id);
        if (catalog == null) {
            throw exception(PRO_EDHR_REPORT_CATALOG_NOT_EXISTS);
        }
        return catalog;
    }

    private MesProEdhrReportDefinitionDO requireDefinitionById(Long id) {
        MesProEdhrReportDefinitionDO definition = id == null ? null : definitionMapper.selectById(id);
        if (definition == null) {
            throw exception(PRO_EDHR_REPORT_DEFINITION_NOT_EXISTS);
        }
        return definition;
    }

    private MesProEdhrReportDefinitionDO requireRunnableDefinition(Long definitionId, String reportCode) {
        MesProEdhrReportDefinitionDO definition = definitionId == null
                ? definitionMapper.selectPublishedByReportCode(reportCode)
                : requireDefinitionById(definitionId);
        if (definition == null) {
            throw exception(PRO_EDHR_REPORT_DEFINITION_NOT_EXISTS);
        }
        if (!STATUS_PUBLISHED.equals(definition.getStatus())) {
            throw exception(PRO_EDHR_REPORT_NOT_PUBLISHED);
        }
        if (StrUtil.isBlank(definition.getCaliberVersion())) {
            throw exception(PRO_EDHR_REPORT_CALIBER_MISSING);
        }
        if (!DATA_SOURCE_READY.equals(definition.getDataSourceStatus())) {
            throw exception(PRO_EDHR_REPORT_DATA_SOURCE_INVALID, definition.getDataSourceStatus());
        }
        return definition;
    }

    private MesProEdhrDatasetDefinitionDO requireReadyDataset(MesProEdhrReportDefinitionDO definition) {
        MesProEdhrDatasetDefinitionDO dataset = datasetDefinitionMapper.selectById(definition.getDatasetId());
        if (dataset == null) {
            throw exception(PRO_EDHR_REPORT_DATA_SOURCE_INVALID, definition.getDatasetCode());
        }
        if (!STATUS_PUBLISHED.equals(dataset.getStatus()) || StrUtil.isBlank(dataset.getCaliberVersion())) {
            throw exception(PRO_EDHR_REPORT_CALIBER_MISSING);
        }
        if (!DATA_SOURCE_READY.equals(dataset.getDataSourceStatus())) {
            throw exception(PRO_EDHR_REPORT_DATA_SOURCE_INVALID, dataset.getFailureReason());
        }
        return dataset;
    }

    private String requireWhitelistedSource(String reportCode, String sourceObject) {
        String expectedSourceTable = REPORT_SOURCE_TABLES.get(reportCode);
        if (!StrUtil.equals(expectedSourceTable, sourceObject)) {
            throw exception(PRO_EDHR_REPORT_DATA_SOURCE_INVALID, sourceObject);
        }
        return expectedSourceTable;
    }

    private void assertSourceTableExists(String sourceTable) {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """, Integer.class, sourceTable);
        if (tableCount == null || tableCount != 1) {
            throw exception(PRO_EDHR_REPORT_DATA_SOURCE_INVALID, sourceTable);
        }
    }

    private Long queryRecordCount(String sourceTable) {
        return jdbcTemplate.queryForObject("SELECT COUNT(1) FROM `" + sourceTable + "` WHERE `deleted` = b'0'",
                Long.class);
    }

    private LocalDateTime queryLatestUpdateTime(String sourceTable) {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(`create_time`) FROM `" + sourceTable + "` WHERE `deleted` = b'0'",
                LocalDateTime.class);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
