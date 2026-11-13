package cn.iocoder.yudao.module.system.api.workbench;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text;

/** Both indexed and source queries share the same explicit business-column whitelist. */
public final class WorkbenchListFilters {
    private WorkbenchListFilters() { }
    public static QueryWrapper<Object> wrapper(String filters, boolean projection) {
        PageParam page = new PageParam(); page.setListFilters(filters);
        String alias = projection ? "t." : "r.";
        return ListFilterQueryUtils.wrapper(page, Map.of(
            "taskType", text(alias + "task_type"),
            "source", text(alias + (projection ? "source_label" : "source")),
            "detail", text(alias + "detail"),
            "statusTime", text(alias + "status_label"),
            "statusLabel", text(alias + "status_label")));
    }
}
