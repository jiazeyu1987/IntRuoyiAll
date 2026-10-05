package cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MesActiveOrderHandoffTaskMapper extends BaseMapperX<MesActiveOrderHandoffTaskDO> {
    default MesActiveOrderHandoffTaskDO lock(Long id) {
        return selectOne(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                .eq(MesActiveOrderHandoffTaskDO::getId,id).last("FOR UPDATE"));
    }
    default MesActiveOrderHandoffTaskDO byIdentity(Long activeId,String type,String source,Long sourceId,Long roundId) {
        return selectOne(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                .eq(MesActiveOrderHandoffTaskDO::getActiveOrderId,activeId)
                .eq(MesActiveOrderHandoffTaskDO::getTaskType,type).eq(MesActiveOrderHandoffTaskDO::getSourceType,source)
                .eq(MesActiveOrderHandoffTaskDO::getSourceId,sourceId).eq(MesActiveOrderHandoffTaskDO::getRoundId,roundId));
    }
    @Select("SELECT * FROM mes_active_order_handoff_task WHERE tenant_id=#{tenant} AND deleted=FALSE AND FIND_IN_SET(#{actor},candidate_user_snapshot)>0 ORDER BY create_time DESC,id DESC")
    List<MesActiveOrderHandoffTaskDO> own(@Param("tenant") Long tenant,@Param("actor") Long actor);
    @Update("UPDATE mes_active_order_handoff_task SET status=#{status},completed_by=#{actor},completion_source_id=#{source},completed_at=#{at},row_version=row_version+1,update_time=#{at} WHERE tenant_id=#{tenant} AND id=#{id} AND row_version=#{version} AND status='TODO' AND deleted=FALSE")
    int close(@Param("tenant") Long tenant,@Param("id") Long id,@Param("version") Integer version,
              @Param("status") String status,@Param("actor") Long actor,@Param("source") Long source,@Param("at") LocalDateTime at);
}
