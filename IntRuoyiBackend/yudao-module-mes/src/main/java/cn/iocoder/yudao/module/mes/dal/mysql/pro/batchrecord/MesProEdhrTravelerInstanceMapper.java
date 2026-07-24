package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrTravelerInstanceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MesProEdhrTravelerInstanceMapper extends BaseMapperX<MesProEdhrTravelerInstanceDO> {

    default PageResult<MesProEdhrTravelerInstanceDO> selectPage(MesProEdhrTravelerPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrTravelerInstanceDO>()
                .likeIfPresent(MesProEdhrTravelerInstanceDO::getTravelerCode, reqVO.getTravelerCode())
                .eqIfPresent(MesProEdhrTravelerInstanceDO::getBatchExecutionId, reqVO.getBatchExecutionId())
                .likeIfPresent(MesProEdhrTravelerInstanceDO::getBatchExecutionCode, reqVO.getBatchExecutionCode())
                .likeIfPresent(MesProEdhrTravelerInstanceDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .likeIfPresent(MesProEdhrTravelerInstanceDO::getBatchCode, reqVO.getBatchCode())
                .likeIfPresent(MesProEdhrTravelerInstanceDO::getSerialNo, reqVO.getSerialNo())
                .eqIfPresent(MesProEdhrTravelerInstanceDO::getRouteProcessId, reqVO.getRouteProcessId())
                .likeIfPresent(MesProEdhrTravelerInstanceDO::getProcessCode, reqVO.getProcessCode())
                .likeIfPresent(MesProEdhrTravelerInstanceDO::getProcessName, reqVO.getProcessName())
                .eqIfPresent(MesProEdhrTravelerInstanceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesProEdhrTravelerInstanceDO::getPrintStatus, reqVO.getPrintStatus())
                .betweenIfPresent(MesProEdhrTravelerInstanceDO::getGeneratedAt, reqVO.getGeneratedAt())
                .orderByDesc(MesProEdhrTravelerInstanceDO::getId));
    }

    default MesProEdhrTravelerInstanceDO selectByBusinessKeyHash(String businessKeyHash) {
        return selectOne(MesProEdhrTravelerInstanceDO::getBusinessKeyHash, businessKeyHash);
    }

    @Select("SELECT * FROM mes_pro_edhr_traveler_instance WHERE id = #{id} FOR UPDATE")
    MesProEdhrTravelerInstanceDO selectByIdForUpdate(Long id);
}
