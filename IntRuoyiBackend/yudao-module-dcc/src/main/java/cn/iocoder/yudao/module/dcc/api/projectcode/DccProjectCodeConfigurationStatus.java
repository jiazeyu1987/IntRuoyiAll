package cn.iocoder.yudao.module.dcc.api.projectcode;

/**
 * DCC 项目代码三类独立配置状态。
 */
public record DccProjectCodeConfigurationStatus(Long projectCodeId,
                                                boolean routeConfigured,
                                                boolean mainBatchRecordConfigured,
                                                boolean qaRegulationConfigured) {
}
