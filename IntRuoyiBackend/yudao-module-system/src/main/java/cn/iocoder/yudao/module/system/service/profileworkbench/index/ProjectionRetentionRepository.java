package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRetention.*;

/** All methods execute inside the service's global -> target tenant fenced master transaction. */
public interface ProjectionRetentionRepository {
    Retirement lockRetirement(long tenant,String generation,boolean exclusive);
    Metadata metadata(long tenant,String generation);
    void insert(Retirement retirement);
    Selection nextBatch(long tenant,String generation,int batchSize,boolean exclusive);
    void deleteRows(long tenant,String generation,Selection batch);
    void finish(Retirement retirement);
}
