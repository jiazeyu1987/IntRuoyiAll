package cn.iocoder.yudao.module.bpm.dal.mysql.task;

import org.apache.ibatis.annotations.*;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

@Mapper
public interface BpmSupervisorHandoverMapper {
    /** Flowable 表无通用租户列，显式租户约束不可省略。 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT ID_ FROM ACT_RU_TASK WHERE ID_ = #{taskId} AND TENANT_ID_ = #{tenantId} FOR UPDATE")
    String lockTask(@Param("taskId") String taskId, @Param("tenantId") String tenantId);
}
