package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProRouteVersionMapper extends BaseMapperX<MesProRouteVersionDO> {

    String STATUS_ACTIVE = "ACTIVE";
    String STATUS_DRAFT = "DRAFT";
    String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    String STATUS_READY_TO_PUBLISH = "READY_TO_PUBLISH";

    default MesProRouteVersionDO selectActiveByRouteId(Long routeId) {
        return selectOne(new LambdaQueryWrapperX<MesProRouteVersionDO>()
                .eq(MesProRouteVersionDO::getRouteId, routeId)
                .eq(MesProRouteVersionDO::getActive, Boolean.TRUE)
                .eq(MesProRouteVersionDO::getLifecycleStatus, STATUS_ACTIVE));
    }

    @Select("SELECT * FROM mes_pro_route_version "
            + "WHERE deleted = b'0' AND route_id = #{routeId} "
            + "AND active = b'1' AND lifecycle_status = 'ACTIVE' "
            + "LIMIT 1 FOR UPDATE")
    MesProRouteVersionDO selectActiveByRouteIdForUpdate(Long routeId);

    default MesProRouteVersionDO selectByRouteIdAndVersionNo(Long routeId, String versionNo) {
        return selectOne(new LambdaQueryWrapperX<MesProRouteVersionDO>()
                .eq(MesProRouteVersionDO::getRouteId, routeId)
                .eq(MesProRouteVersionDO::getVersionNo, versionNo));
    }

    default List<MesProRouteVersionDO> selectListByRouteId(Long routeId) {
        return selectList(new LambdaQueryWrapperX<MesProRouteVersionDO>()
                .eq(MesProRouteVersionDO::getRouteId, routeId)
                .orderByDesc(MesProRouteVersionDO::getId));
    }

    default List<MesProRouteVersionDO> selectListByRouteIds(Collection<Long> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProRouteVersionDO>()
                .in(MesProRouteVersionDO::getRouteId, routeIds)
                .orderByDesc(MesProRouteVersionDO::getId));
    }

    default MesProRouteVersionDO selectOpenCandidateByRouteId(Long routeId) {
        return selectOne(new LambdaQueryWrapperX<MesProRouteVersionDO>()
                .eq(MesProRouteVersionDO::getRouteId, routeId)
                .eq(MesProRouteVersionDO::getActive, Boolean.FALSE)
                .in(MesProRouteVersionDO::getLifecycleStatus,
                        List.of(STATUS_DRAFT, STATUS_PENDING_APPROVAL, STATUS_READY_TO_PUBLISH))
                .orderByDesc(MesProRouteVersionDO::getId));
    }

    default Long countOpenCandidatesByRouteId(Long routeId) {
        return selectCount(new LambdaQueryWrapperX<MesProRouteVersionDO>()
                .eq(MesProRouteVersionDO::getRouteId, routeId)
                .eq(MesProRouteVersionDO::getActive, Boolean.FALSE)
                .in(MesProRouteVersionDO::getLifecycleStatus,
                        List.of(STATUS_DRAFT, STATUS_PENDING_APPROVAL, STATUS_READY_TO_PUBLISH)));
    }

    default MesProRouteVersionDO selectByApprovalProcessInstanceId(String approvalProcessInstanceId) {
        return selectOne(new LambdaQueryWrapperX<MesProRouteVersionDO>()
                .eq(MesProRouteVersionDO::getApprovalProcessInstanceId, approvalProcessInstanceId));
    }

    default int updateApprovalFieldsToDraft(Long id) {
        return update(null, new LambdaUpdateWrapper<MesProRouteVersionDO>()
                .eq(MesProRouteVersionDO::getId, id)
                .set(MesProRouteVersionDO::getLifecycleStatus, STATUS_DRAFT)
                .set(MesProRouteVersionDO::getSubmittedBy, null)
                .set(MesProRouteVersionDO::getSubmittedTime, null)
                .set(MesProRouteVersionDO::getApprovalProcessInstanceId, null));
    }

    default int deactivateById(Long id) {
        return update(null, new LambdaUpdateWrapper<MesProRouteVersionDO>()
                .eq(MesProRouteVersionDO::getId, id)
                .set(MesProRouteVersionDO::getActive, Boolean.FALSE));
    }

    @Select("SELECT version_no FROM mes_pro_route_version "
            + "WHERE deleted = b'0' AND route_id = #{routeId} "
            + "ORDER BY CAST(SUBSTRING_INDEX(version_no, 'V', -1) AS UNSIGNED) DESC, id DESC "
            + "LIMIT 1")
    String selectMaxVersionNoByRouteId(Long routeId);

}
