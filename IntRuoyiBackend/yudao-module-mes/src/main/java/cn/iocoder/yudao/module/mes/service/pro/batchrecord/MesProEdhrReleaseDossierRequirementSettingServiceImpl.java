package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrReleaseDossierRequirementSettingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrReleaseDossierRequirementSettingUpdateReqVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_STALE;

@Service
@Validated
public class MesProEdhrReleaseDossierRequirementSettingServiceImpl
        implements MesProEdhrReleaseDossierRequirementSettingService {

    private static final String FIELD_INCOMING = "incomingInspectionReportRequired";
    private static final String FIELD_STERILIZATION = "sterilizationReportRequired";
    private static final String FIELD_FINISHED_REPORT = "finishedProductInspectionReportRequired";
    private static final String FIELD_FINISHED_RECORD = "finishedProductInspectionRecordRequired";

    @Resource
    private ConfigService configService;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;

    @Override
    public EdhrReleaseDossierRequirementSettingRespVO getRequirementSetting() {
        ConfigDO config = requireConfig();
        MesProEdhrReleaseDossierRequirementState state = parseStrictState(config.getValue());
        return toResp(config, state);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrReleaseDossierRequirementSettingRespVO updateRequirementSetting(
            EdhrReleaseDossierRequirementSettingUpdateReqVO reqVO) {
        ConfigDO config = requireConfig();
        MesProEdhrReleaseDossierRequirementState beforeState = parseStrictState(config.getValue());
        String beforeJson = canonicalJson(beforeState);
        String afterJson = canonicalJson(reqVO);

        ConfigSaveReqVO saveReqVO = new ConfigSaveReqVO();
        saveReqVO.setId(config.getId());
        saveReqVO.setCategory(StrUtil.blankToDefault(config.getCategory(), "mes"));
        saveReqVO.setName(StrUtil.blankToDefault(config.getName(), "eDHR 放行资料限制开关"));
        saveReqVO.setKey(CONFIG_KEY);
        saveReqVO.setValue(afterJson);
        saveReqVO.setVisible(config.getVisible() == null ? Boolean.TRUE : config.getVisible());
        saveReqVO.setRemark(StrUtil.blankToDefault(config.getRemark(),
                "金手指专用放行资料限制开关；开启后对应特殊节点必须完成且存在已保存 ADD 附件。"));
        configService.updateConfig(saveReqVO);

        ConfigDO updated = requireConfig();
        MesProEdhrReleaseDossierRequirementState updatedState = parseStrictState(updated.getValue());
        recordAudit(beforeJson, canonicalJson(updatedState));
        return toResp(updated, updatedState);
    }

    @Override
    public MesProEdhrReleaseDossierRequirementState getRequirementState() {
        return parseStrictState(requireConfig().getValue());
    }

    @Override
    public void requireCurrentConfigHash(String precheckConfigHash) {
        MesProEdhrReleaseDossierRequirementState current = getRequirementState();
        if (StrUtil.isBlank(precheckConfigHash) || !Objects.equals(current.configHash(), precheckConfigHash)) {
            throw exception(PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_STALE);
        }
    }

    private ConfigDO requireConfig() {
        ConfigDO config = configService.getConfigByKey(CONFIG_KEY);
        if (config == null) {
            throw exception(PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_MISSING, CONFIG_KEY);
        }
        return config;
    }

    private MesProEdhrReleaseDossierRequirementState parseStrictState(String value) {
        JSONObject jsonObject;
        try {
            jsonObject = JSON.parseObject(StrUtil.trim(value));
        } catch (RuntimeException ex) {
            throw exception(PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_INVALID, CONFIG_KEY, value);
        }
        if (jsonObject == null || jsonObject.size() != 4) {
            throw exception(PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_INVALID, CONFIG_KEY, value);
        }
        boolean incomingRequired = strictBoolean(jsonObject, FIELD_INCOMING, value);
        boolean sterilizationRequired = strictBoolean(jsonObject, FIELD_STERILIZATION, value);
        boolean finishedReportRequired = strictBoolean(jsonObject, FIELD_FINISHED_REPORT, value);
        boolean finishedRecordRequired = strictBoolean(jsonObject, FIELD_FINISHED_RECORD, value);
        String canonicalJson = canonicalJson(incomingRequired, sterilizationRequired,
                finishedReportRequired, finishedRecordRequired);
        return new MesProEdhrReleaseDossierRequirementState(incomingRequired, sterilizationRequired,
                finishedReportRequired, finishedRecordRequired,
                MesProBatchRecordExecutionFieldAuditHasher.sha256(CONFIG_KEY + ":" + canonicalJson));
    }

    private boolean strictBoolean(JSONObject jsonObject, String fieldName, String rawValue) {
        Object value = jsonObject.get(fieldName);
        if (!(value instanceof Boolean booleanValue)) {
            throw exception(PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_INVALID, CONFIG_KEY, rawValue);
        }
        return booleanValue;
    }

    private String canonicalJson(EdhrReleaseDossierRequirementSettingUpdateReqVO reqVO) {
        return canonicalJson(Boolean.TRUE.equals(reqVO.getIncomingInspectionReportRequired()),
                Boolean.TRUE.equals(reqVO.getSterilizationReportRequired()),
                Boolean.TRUE.equals(reqVO.getFinishedProductInspectionReportRequired()),
                Boolean.TRUE.equals(reqVO.getFinishedProductInspectionRecordRequired()));
    }

    private String canonicalJson(MesProEdhrReleaseDossierRequirementState state) {
        return canonicalJson(state.incomingInspectionReportRequired(), state.sterilizationReportRequired(),
                state.finishedProductInspectionReportRequired(), state.finishedProductInspectionRecordRequired());
    }

    private String canonicalJson(boolean incomingRequired,
                                 boolean sterilizationRequired,
                                 boolean finishedReportRequired,
                                 boolean finishedRecordRequired) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(FIELD_INCOMING, incomingRequired);
        payload.put(FIELD_STERILIZATION, sterilizationRequired);
        payload.put(FIELD_FINISHED_REPORT, finishedReportRequired);
        payload.put(FIELD_FINISHED_RECORD, finishedRecordRequired);
        return JSON.toJSONString(payload);
    }

    private EdhrReleaseDossierRequirementSettingRespVO toResp(
            ConfigDO config, MesProEdhrReleaseDossierRequirementState state) {
        return new EdhrReleaseDossierRequirementSettingRespVO()
                .setIncomingInspectionReportRequired(state.incomingInspectionReportRequired())
                .setSterilizationReportRequired(state.sterilizationReportRequired())
                .setFinishedProductInspectionReportRequired(state.finishedProductInspectionReportRequired())
                .setFinishedProductInspectionRecordRequired(state.finishedProductInspectionRecordRequired())
                .setConfigKey(CONFIG_KEY)
                .setConfigHash(state.configHash())
                .setUpdatedBy(config.getUpdater())
                .setUpdatedAt(config.getUpdateTime());
    }

    private void recordAudit(String beforeJson, String afterJson) {
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        if (actorUserId == null) {
            throw exception(UNAUTHORIZED);
        }
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-AUD-" + java.util.UUID.randomUUID())
                .setObjectType("EDHR_RELEASE_DOSSIER_REQUIREMENT_SETTING")
                .setObjectId(CONFIG_KEY)
                .setOperationType("UPDATE_DOSSIER_REQUIREMENT_SETTING")
                .setActionName("更新 eDHR 放行资料限制开关")
                .setActorUserId(actorUserId)
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode(MesProEdhrGoldenFingerPermissionService.PERMISSION)
                .setPermissionDecision("ALLOW_GOLDEN_FINGER")
                .setResultStatus("SUCCESS")
                .setBeforeSummaryHash(MesProBatchRecordExecutionFieldAuditHasher.sha256(CONFIG_KEY + ":" + beforeJson))
                .setAfterSummaryHash(MesProBatchRecordExecutionFieldAuditHasher.sha256(CONFIG_KEY + ":" + afterJson))
                .setMetadataJson(JsonUtils.toJsonString(Map.of(
                        "configKey", CONFIG_KEY,
                        "beforeSetting", beforeJson,
                        "afterSetting", afterJson))));
    }
}
