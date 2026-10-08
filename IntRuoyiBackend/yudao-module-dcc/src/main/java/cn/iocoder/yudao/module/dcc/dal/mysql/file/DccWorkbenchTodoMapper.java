package cn.iocoder.yudao.module.dcc.dal.mysql.file;
import cn.iocoder.yudao.module.system.api.profileworkbench.*;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface DccWorkbenchTodoMapper {
    @Options(timeout = 10)
    Long countDistribution(@Param("q") ProfileWorkbenchTodoQueryDTO query);
    @Options(timeout = 10)
    List<ProfileWorkbenchTodoRowDTO> chunkDistribution(@Param("q") ProfileWorkbenchTodoQueryDTO query, @Param("after") ProfileWorkbenchTodoRowDTO after, @Param("fetchLimit") int fetchLimit);
    @Options(timeout = 10)
    Long countTraining(@Param("q") ProfileWorkbenchTodoQueryDTO query);
    @Options(timeout = 10)
    List<ProfileWorkbenchTodoRowDTO> chunkTraining(@Param("q") ProfileWorkbenchTodoQueryDTO query, @Param("after") ProfileWorkbenchTodoRowDTO after, @Param("fetchLimit") int fetchLimit);
}
