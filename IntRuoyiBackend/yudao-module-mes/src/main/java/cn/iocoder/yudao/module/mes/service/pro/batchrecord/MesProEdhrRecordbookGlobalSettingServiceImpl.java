package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordbookGlobalSettingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordbookGlobalSettingUpdateReqVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RECORDBOOK_GLOBAL_CONFIG_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RECORDBOOK_GLOBAL_CONFIG_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RECORDBOOK_GLOBAL_DISABLED;

@Service
@Validated
public class MesProEdhrRecordbookGlobalSettingServiceImpl implements MesProEdhrRecordbookGlobalSettingService {

    private static final String RECORD_CATEGORY_INTERNAL = "INTERNAL_RECORD";

    @Resource
    private ConfigService configService;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;

    @Override
    public EdhrRecordbookGlobalSettingRespVO getGlobalSetting() {
        ConfigDO config = requireConfig();
        return toResp(config, parseStrictBoolean(config.getValue()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordbookGlobalSettingRespVO updateGlobalSetting(EdhrRecordbookGlobalSettingUpdateReqVO reqVO) {
        ConfigDO config = requireConfig();
        boolean beforeEnabled = parseStrictBoolean(config.getValue());
        boolean afterEnabled = Boolean.TRUE.equals(reqVO.getEnabled());

        ConfigSaveReqVO saveReqVO = new ConfigSaveReqVO();
        saveReqVO.setId(config.getId());
        saveReqVO.setCategory(StrUtil.blankToDefault(config.getCategory(), "mes"));
        saveReqVO.setName(StrUtil.blankToDefault(config.getName(), "eDHR 记录本全局开关"));
        saveReqVO.setKey(CONFIG_KEY);
        saveReqVO.setValue(Boolean.toString(afterEnabled));
        saveReqVO.setVisible(config.getVisible() == null ? Boolean.TRUE : config.getVisible());
        saveReqVO.setRemark(StrUtil.blankToDefault(config.getRemark(),
                "金手指专用全局开关；关闭后所有用户只能走批记录流程，记录本入口和写入被运行态门禁禁止。"));
        configService.updateConfig(saveReqVO);

        ConfigDO updated = requireConfig();
        boolean updatedEnabled = parseStrictBoolean(updated.getValue());
        recordAudit(beforeEnabled, updatedEnabled);
        return toResp(updated, updatedEnabled);
    }

    @Override
    public boolean isGlobalRecordbookEnabled() {
        return parseStrictBoolean(requireConfig().getValue());
    }

    @Override
    public Boolean resolveEffectiveRecordbookEnabled(Boolean recordbookEnabled, String recordCategory) {
        if (RECORD_CATEGORY_INTERNAL.equals(StrUtil.trim(recordCategory))) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE.equals(recordbookEnabled) && isGlobalRecordbookEnabled();
    }

    @Override
    public void requireRecordbookWriteAllowed(Boolean recordbookEnabled, String recordCategory) {
        if (!Boolean.TRUE.equals(resolveEffectiveRecordbookEnabled(recordbookEnabled, recordCategory))) {
            throw exception(PRO_EDHR_RECORDBOOK_GLOBAL_DISABLED);
        }
    }

    private ConfigDO requireConfig() {
        ConfigDO config = configService.getConfigByKey(CONFIG_KEY);
        if (config == null) {
            throw exception(PRO_EDHR_RECORDBOOK_GLOBAL_CONFIG_MISSING, CONFIG_KEY);
        }
        return config;
    }

    private boolean parseStrictBoolean(String value) {
        String normalized = StrUtil.trim(value);
        if (Objects.equals("true", normalized)) {
            return true;
        }
        if (Objects.equals("false", normalized)) {
            return false;
        }
        throw exception(PRO_EDHR_RECORDBOOK_GLOBAL_CONFIG_INVALID, CONFIG_KEY, value);
    }

    private EdhrRecordbookGlobalSettingRespVO toResp(ConfigDO config, boolean enabled) {
        return new EdhrRecordbookGlobalSettingRespVO()
                .setEnabled(enabled)
                .setConfigKey(CONFIG_KEY)
                .setUpdatedBy(config.getUpdater())
                .setUpdatedAt(config.getUpdateTime());
    }

    private void recordAudit(boolean beforeEnabled, boolean afterEnabled) {
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        if (actorUserId == null) {
            throw exception(UNAUTHORIZED);
        }
        String before = Boolean.toString(beforeEnabled);
        String after = Boolean.toString(afterEnabled);
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-AUD-" + java.util.UUID.randomUUID())
                .setObjectType("EDHR_RECORDBOOK_GLOBAL_SETTING")
                .setObjectId(CONFIG_KEY)
                .setOperationType("UPDATE_GLOBAL_SETTING")
                .setActionName("更新 eDHR 记录本全局开关")
                .setActorUserId(actorUserId)
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode(MesProEdhrGoldenFingerPermissionService.PERMISSION)
                .setPermissionDecision("ALLOW_GOLDEN_FINGER")
                .setResultStatus("SUCCESS")
                .setBeforeSummaryHash(MesProBatchRecordExecutionFieldAuditHasher.sha256(CONFIG_KEY + ":" + before))
                .setAfterSummaryHash(MesProBatchRecordExecutionFieldAuditHasher.sha256(CONFIG_KEY + ":" + after))
                .setMetadataJson(JsonUtils.toJsonString(Map.of(
                        "configKey", CONFIG_KEY,
                        "beforeEnabled", beforeEnabled,
                        "afterEnabled", afterEnabled))));
    }
}
