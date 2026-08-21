package cn.iocoder.yudao.module.dcc.api.projectcode;

import java.util.Collection;
import java.util.Map;

/**
 * DCC 项目代码配置状态查询 API。
 *
 * <p>具体配置来源由对应业务模块提供，DCC 列表只消费独立状态结果。</p>
 */
public interface DccProjectCodeConfigurationStatusApi {

    Map<Long, DccProjectCodeConfigurationStatus> getStatus(
            Collection<DccProjectCodeConfigurationQuery> projects);
}
