package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProProductionReportRevisionLogPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MesProProcessPoolEventRevisionMapper extends BaseMapperX<MesProProcessPoolEventRevisionDO> {

    default MesProProcessPoolEventRevisionDO selectBySignatureId(Long revisionSignatureId) {
        return selectOne(new LambdaQueryWrapperX<MesProProcessPoolEventRevisionDO>()
                .eq(MesProProcessPoolEventRevisionDO::getRevisionSignatureId, revisionSignatureId));
    }

    default List<MesProProcessPoolEventRevisionDO> selectListByEventId(Long eventId) {
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolEventRevisionDO>()
                .eq(MesProProcessPoolEventRevisionDO::getEventId, eventId)
                .eq(MesProProcessPoolEventRevisionDO::getRevisionStatus,
                        MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE)
                .orderByDesc(MesProProcessPoolEventRevisionDO::getServerRevisionTime)
                .orderByDesc(MesProProcessPoolEventRevisionDO::getId));
    }

    Long selectProductionReportRevisionLogCount(
            @Param("reqVO") MesProProductionReportRevisionLogPageReqVO reqVO,
            @Param("employeeUserIds") List<Long> employeeUserIds);

    List<MesProProcessPoolEventRevisionDO> selectProductionReportRevisionLogPage(
            @Param("reqVO") MesProProductionReportRevisionLogPageReqVO reqVO,
            @Param("employeeUserIds") List<Long> employeeUserIds,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);
}
