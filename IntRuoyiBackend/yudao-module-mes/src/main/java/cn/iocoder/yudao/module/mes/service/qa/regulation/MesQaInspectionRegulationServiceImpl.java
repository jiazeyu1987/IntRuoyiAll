package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED;

@Service
@Validated
public class MesQaInspectionRegulationServiceImpl implements MesQaInspectionRegulationService {

    private final MesQaInspectionRegulationMapper regulationMapper;
    private final MesQaInspectionRegulationVersionMapper versionMapper;
    private final MesQaInspectionRegulationItemMapper itemMapper;

    public MesQaInspectionRegulationServiceImpl(MesQaInspectionRegulationMapper regulationMapper,
                                                MesQaInspectionRegulationVersionMapper versionMapper,
                                                MesQaInspectionRegulationItemMapper itemMapper) {
        this.regulationMapper = regulationMapper;
        this.versionMapper = versionMapper;
        this.itemMapper = itemMapper;
    }

    @Override
    public MesQaInspectionRegulationPublishedVersionRespVO getPublishedVersion(Long versionId) {
        MesQaInspectionRegulationVersionDO version = versionId == null
                ? versionMapper.selectLatestPublished()
                : versionMapper.selectById(versionId);
        if (version == null) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_NOT_EXISTS, versionId);
        }
        if (!Objects.equals(version.getLifecycleStatus(), "PUBLISHED")) {
            throw exception(QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED, version.getId());
        }

        MesQaInspectionRegulationDO regulation = regulationMapper.selectById(version.getRegulationId());
        if (regulation == null) {
            throw exception(QA_INSPECTION_REGULATION_NOT_EXISTS, version.getRegulationId());
        }

        JSONObject snapshot = parseSnapshot(version);
        List<MesQaInspectionRegulationItemDO> items = itemMapper.selectListByVersionId(version.getId());
        List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionRule> firstRules =
                rulesByType(items, "FIRST");
        List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionRule> patrolRules =
                rulesByType(items, "PATROL");
        List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionRule> finalRules =
                rulesByType(items, "FINAL");

        return MesQaInspectionRegulationPublishedVersionRespVO.builder()
                .regulationId(regulation.getId())
                .publishedVersionId(version.getId())
                .versionNo(version.getVersionNo())
                .publishedAt(version.getPublishedAt())
                .immutable(true)
                .regulationCode(regulation.getRegulationCode())
                .regulationName(regulation.getRegulationName())
                .productId(regulation.getProductId())
                .productName(firstText(snapshot, "productName", "productDisplayName", "productCode"))
                .routeId(regulation.getRouteId())
                .routeName(firstText(snapshot, "routeName", "routeCode"))
                .routeVersionId(regulation.getRouteVersionId())
                .routeVersionNo(firstText(snapshot, "routeVersionNo", "routeVersionName", "versionNo"))
                .routeProcessId(regulation.getRouteProcessId())
                .processId(regulation.getProcessId())
                .routeProcessName(firstText(snapshot, "routeProcessName", "processName", "routeProcessCode"))
                .batchRecordBindingSummary(resolveBatchRecordBindingSummary(snapshot))
                .firstInspectionRules(firstRules)
                .patrolInspectionRules(patrolRules)
                .finalInspectionRules(finalRules)
                .build();
    }

    private static JSONObject parseSnapshot(MesQaInspectionRegulationVersionDO version) {
        if (StrUtil.isBlank(version.getSnapshotJson())) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, version.getId());
        }
        try {
            return JSON.parseObject(version.getSnapshotJson());
        } catch (RuntimeException ex) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, version.getId());
        }
    }

    private static List<MesQaInspectionRegulationPublishedVersionRespVO.InspectionRule> rulesByType(
            List<MesQaInspectionRegulationItemDO> items, String inspectionType) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(item -> Objects.equals(item.getInspectionType(), inspectionType))
                .map(item -> MesQaInspectionRegulationPublishedVersionRespVO.InspectionRule.builder()
                        .inspectionType(item.getInspectionType())
                        .itemCode(item.getItemCode())
                        .itemName(item.getItemName())
                        .inspectionMethod(item.getInspectionMethod())
                        .standardText(item.getStandardText())
                        .resultType(item.getResultType())
                        .firstInspectionQuantity(item.getFirstInspectionQuantity())
                        .patrolInspectionRatio(item.getPatrolInspectionRatio())
                        .build())
                .toList();
    }

    private static String firstText(JSONObject snapshot, String... keys) {
        for (String key : keys) {
            String value = snapshot.getString(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static String resolveBatchRecordBindingSummary(JSONObject snapshot) {
        for (String key : List.of("batchRecordReports", "batchRecordForms", "batchRecords")) {
            JSONArray records = snapshot.getJSONArray(key);
            if (records == null || records.isEmpty()) {
                continue;
            }
            String summary = records.stream()
                    .filter(JSONObject.class::isInstance)
                    .map(JSONObject.class::cast)
                    .map(MesQaInspectionRegulationServiceImpl::batchRecordName)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .reduce((left, right) -> left + "，" + right)
                    .orElse(null);
            if (StrUtil.isNotBlank(summary)) {
                return summary;
            }
        }
        return null;
    }

    private static String batchRecordName(JSONObject record) {
        return firstText(record, "batchRecordReportName", "reportName", "batchRecordName", "formName", "name",
                "batchRecordReportId", "reportId");
    }
}
