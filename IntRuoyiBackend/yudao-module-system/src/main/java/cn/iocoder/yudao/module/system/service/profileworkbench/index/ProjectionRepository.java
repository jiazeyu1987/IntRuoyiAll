package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import cn.iocoder.yudao.module.system.api.workbench.QueryContext;
import cn.iocoder.yudao.module.system.api.workbench.TaskSummary;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;

/** Neutral storage port; no business or original permission queries are allowed here. */
public interface ProjectionRepository {
    void initializeControls(long tenant,String registryRevision);
    Global lockGlobal(boolean exclusive);
    Tenant lockTenant(long tenant, boolean exclusive);
    void advanceGlobal(Global expected, Global next);
    void advanceTenant(Tenant expected, Tenant next);
    void append(Batch batch);
    List<Event> events(Scope scope, long tenant, long sequence);
    /** Permanently retired identities cannot be queried, replayed, rebuilt or published. */
    void requireRetained(String generation,long tenant);
    Checkpoint checkpoint(String generation, long tenant);
    void createGeneration(String generation, long tenant, Watermark initial, String registryRevision, Set<String> sources);
    void advanceCheckpoint(Checkpoint expected, Checkpoint next);
    void applyTask(String generation, Task task);
    void applyAuthorization(String generation, Authorization authorization);
    /** Returns false only for an identical already-applied event; conflicting identity/content fails. */
    boolean recordEvent(String generation, long tenant, Event event);
    void setComplete(Checkpoint expected);
    void publish(Tenant expected, String generation);
    Set<String> generationSources(String generation, long tenant, String registryRevision);
    List<Task> allTasks(String generation, long tenant);
    List<Authorization> allAuthorizations(String generation, long tenant);
    List<String> authorizedSources(String generation, long tenant, long user, Instant at);
    Instant nextAuthorizationBoundary(String generation, long tenant, long user, List<String> sources, Instant at);
    record Query(String generation, long tenant, long user, List<String> sources, Instant authorizationAt, QueryContext spec) { }
    record Totals(long business, long hidden, long filtered) { }
    Totals totals(Query query);
    List<TaskSummary> page(Query query, long offset, int limit);
    TaskSummary eligibleTask(Query query, String key);
}
