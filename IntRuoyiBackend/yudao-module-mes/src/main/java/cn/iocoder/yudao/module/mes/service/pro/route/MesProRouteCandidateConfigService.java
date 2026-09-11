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

    /**
     * 原子保存候选版本的多个配置快照。
     *
     * @param candidateRouteVersionId 候选路线版本编号
     * @param configSnapshots 配置快照键值
     */
    void saveConfigSnapshots(Long candidateRouteVersionId, java.util.Map<String, Object> configSnapshots);

    /**
     * 原子保存候选版本配置，并拒绝覆盖已变化的候选快照。
     */
    void saveConfigSnapshots(Long candidateRouteVersionId, String expectedRouteSnapshotSha256,
                             java.util.Map<String, Object> configSnapshots);
}
