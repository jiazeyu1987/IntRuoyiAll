package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.api.workbench.QueryContext;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.INVALID_QUERY;

public record WorkbenchQuerySpec(String visibility, String taskType, QuickFilter quickFilter, Sort sort) {
    public record QuickFilter(String fieldKey, String operator, String value) { }
    public record Sort(String key, String order) { }
    public WorkbenchQuerySpec normalized() {
        if (!"visible".equals(visibility) && !"hidden".equals(visibility)) throw INVALID_QUERY.exception();
        if (taskType != null && (taskType.isBlank() || taskType.length() > 100)) throw INVALID_QUERY.exception();
        QuickFilter filter = quickFilter;
        if (filter != null) {
            if (filter.value() == null || filter.value().length() > 1000
                    || !("source".equals(filter.fieldKey()) || "detail".equals(filter.fieldKey()) || "statusLabel".equals(filter.fieldKey()))
                    || !("contains".equals(filter.operator()) || "eq".equals(filter.operator()))) throw INVALID_QUERY.exception();
            filter = new QuickFilter(filter.fieldKey(), filter.operator(), filter.value().replaceAll("^[\\s\\p{Z}\\uFEFF]+|[\\s\\p{Z}\\uFEFF]+$", ""));
        }
        if (sort != null && (!("taskType".equals(sort.key()) || "source".equals(sort.key()) || "detail".equals(sort.key()) || "statusLabel".equals(sort.key()))
                || !("asc".equals(sort.order()) || "desc".equals(sort.order())))) throw INVALID_QUERY.exception();
        return new WorkbenchQuerySpec(visibility, taskType, filter, sort);
    }
    public QueryContext context(long tenant, long user, long version) {
        WorkbenchQuerySpec normalized = normalized();
        if (!equals(normalized)) throw INVALID_QUERY.exception();
        QueryContext q = new QueryContext(); q.setTenantId(tenant); q.setUserId(user);
        q.setVisibility(visibility); q.setTaskType(taskType);
        if (quickFilter != null) { q.setFilterField(quickFilter.fieldKey()); q.setFilterOperator(quickFilter.operator()); q.setFilterValue(quickFilter.value()); }
        if (sort != null) { q.setSortKey(sort.key()); q.setSortOrder(sort.order()); }
        q.freezeVisibility(version); q.validate(); return q;
    }
}
