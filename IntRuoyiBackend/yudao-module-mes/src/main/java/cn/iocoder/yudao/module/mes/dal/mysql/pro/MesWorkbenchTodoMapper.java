package cn.iocoder.yudao.module.mes.dal.mysql.pro;
import cn.iocoder.yudao.module.system.api.profileworkbench.*;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface MesWorkbenchTodoMapper {
    @Options(timeout = 10)
    Long countEdhr(@Param("q") ProfileWorkbenchTodoQueryDTO query);
    @Options(timeout = 10)
    List<ProfileWorkbenchTodoRowDTO> chunkEdhr(@Param("q") ProfileWorkbenchTodoQueryDTO query, @Param("after") ProfileWorkbenchTodoRowDTO after, @Param("fetchLimit") int fetchLimit);
    @Options(timeout = 10)
    Long countWorkOrder(@Param("q") ProfileWorkbenchTodoQueryDTO query);
    @Options(timeout = 10)
    List<ProfileWorkbenchTodoRowDTO> chunkWorkOrder(@Param("q") ProfileWorkbenchTodoQueryDTO query, @Param("after") ProfileWorkbenchTodoRowDTO after, @Param("fetchLimit") int fetchLimit);
}
