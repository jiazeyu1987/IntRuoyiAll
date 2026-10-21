package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import java.util.List;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionHistory.*;

/** All methods run in the caller's fenced master transaction. */
public interface ProjectionHistoryRepository {
    void initializeFloor(Scope scope,long tenant);
    long floor(Scope scope,long tenant,boolean exclusive);
    void appendIdentity(Batch batch);
    void verifyBatch(Batch batch);
    boolean rememberTask(String generation,Task task);
    boolean rememberAuthorization(String generation,Authorization authorization);
    Snapshot snapshot(Request request,String operation,Global global,List<Tenant> tenants,boolean exclusive);
    List<Long> controlTenants(int maximum);
    void observe(Request request,Snapshot snapshot,Observation observation);
    void register(Request request,Snapshot snapshot);
    void purge(Request request,Snapshot snapshot);
}
