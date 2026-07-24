package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPrintTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MesProEdhrPrintTaskMapper extends BaseMapperX<MesProEdhrPrintTaskDO> {

    default PageResult<MesProEdhrPrintTaskDO> selectPage(MesProEdhrPrintTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrPrintTaskDO>()
                .likeIfPresent(MesProEdhrPrintTaskDO::getTaskCode, reqVO.getTaskCode())
                .eqIfPresent(MesProEdhrPrintTaskDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(MesProEdhrPrintTaskDO::getSourceObjectId, reqVO.getSourceObjectId())
                .likeIfPresent(MesProEdhrPrintTaskDO::getSourceObjectCode, reqVO.getSourceObjectCode())
                .eqIfPresent(MesProEdhrPrintTaskDO::getTemplateType, reqVO.getTemplateType())
                .eqIfPresent(MesProEdhrPrintTaskDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesProEdhrPrintTaskDO::getPrintConfirmStatus, reqVO.getPrintConfirmStatus())
                .eqIfPresent(MesProEdhrPrintTaskDO::getIsReprint, reqVO.getIsReprint())
                .betweenIfPresent(MesProEdhrPrintTaskDO::getRequestedAt, reqVO.getRequestedAt())
                .orderByDesc(MesProEdhrPrintTaskDO::getId));
    }

    default MesProEdhrPrintTaskDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(MesProEdhrPrintTaskDO::getIdempotencyKey, idempotencyKey);
    }

    @Select("SELECT * FROM mes_pro_edhr_print_task WHERE id = #{id} FOR UPDATE")
    MesProEdhrPrintTaskDO selectByIdForUpdate(Long id);
}
