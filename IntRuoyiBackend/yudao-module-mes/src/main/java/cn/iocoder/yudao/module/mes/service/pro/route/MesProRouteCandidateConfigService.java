package cn.iocoder.yudao.module.mes.service.pro.route;

/**
 * 工艺路线候选版本配置快照服务。
 */
public interface MesProRouteCandidateConfigService {

    /**
     * 保存候选版本的配置快照。
     *
     * @param candidateRouteVersionId 候选路线版本编号
     * @param configKey 配置快照键，例如 flowGraph、scheduleConfigs、batchUseConfigs、products
     * @param configSnapshot 配置快照对象
     */
    void saveConfigSnapshot(Long candidateRouteVersionId, String configKey, Object configSnapshot);
}
