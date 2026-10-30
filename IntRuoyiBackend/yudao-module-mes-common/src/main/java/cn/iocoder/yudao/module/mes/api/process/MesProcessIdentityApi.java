package cn.iocoder.yudao.module.mes.api.process;

import java.util.Collection;
import java.util.Map;

/** Production process identity lookup shared with equipment capacity calculations. */
public interface MesProcessIdentityApi {
    Map<Long, Long> getProcessIdentityMap(Collection<Long> processIds);
}
