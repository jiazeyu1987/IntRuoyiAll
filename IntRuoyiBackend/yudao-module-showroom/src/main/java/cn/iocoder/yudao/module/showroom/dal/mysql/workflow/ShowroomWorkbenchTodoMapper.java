package cn.iocoder.yudao.module.showroom.dal.mysql.workflow;
import cn.iocoder.yudao.module.system.api.profileworkbench.*;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface ShowroomWorkbenchTodoMapper {
    @Options(timeout = 10)
    Long countAssignment(@Param("q") ProfileWorkbenchTodoQueryDTO query);
    @Options(timeout = 10)
    List<ProfileWorkbenchTodoRowDTO> chunkAssignment(@Param("q") ProfileWorkbenchTodoQueryDTO query, @Param("after") ProfileWorkbenchTodoRowDTO after, @Param("fetchLimit") int fetchLimit);
    @Options(timeout = 10)
    Long countInvalidNotifications(@Param("q") ProfileWorkbenchTodoQueryDTO query);
}
