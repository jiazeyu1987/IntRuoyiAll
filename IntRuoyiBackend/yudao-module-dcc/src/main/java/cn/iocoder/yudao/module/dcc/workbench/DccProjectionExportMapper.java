package cn.iocoder.yudao.module.dcc.workbench;
import cn.iocoder.yudao.module.system.api.workbench.QueryContext;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface DccProjectionExportMapper {
 List<DccDistributionWorkbenchRow> exportDistribution(@Param("q") QueryContext query);
 List<DccTrainingWorkbenchRow> exportTraining(@Param("q") QueryContext query);
}
