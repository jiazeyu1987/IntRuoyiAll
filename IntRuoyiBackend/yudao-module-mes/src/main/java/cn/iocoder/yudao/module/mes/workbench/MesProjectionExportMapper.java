package cn.iocoder.yudao.module.mes.workbench;
import cn.iocoder.yudao.module.system.api.workbench.QueryContext;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface MesProjectionExportMapper {
 List<MesEdhrWorkbenchRow> exportHandoff(@Param("q") QueryContext query);
 List<MesEdhrWorkbenchRow> exportEdhr(@Param("q") QueryContext query);
 List<MesWorkOrderWorkbenchRow> exportWorkOrder(@Param("q") QueryContext query);
}
