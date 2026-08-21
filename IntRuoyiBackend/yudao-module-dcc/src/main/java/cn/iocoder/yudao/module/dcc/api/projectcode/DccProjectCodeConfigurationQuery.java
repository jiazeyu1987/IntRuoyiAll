package cn.iocoder.yudao.module.dcc.api.projectcode;

/**
 * DCC 项目代码配置状态查询上下文。
 */
public record DccProjectCodeConfigurationQuery(Long projectCodeId, String projectName,
                                               boolean routeStatusRequired,
                                               boolean mainBatchRecordStatusRequired,
                                               boolean qaRegulationStatusRequired) {

    public DccProjectCodeConfigurationQuery(Long projectCodeId, String projectName) {
        this(projectCodeId, projectName, true, true, true);
    }
}
