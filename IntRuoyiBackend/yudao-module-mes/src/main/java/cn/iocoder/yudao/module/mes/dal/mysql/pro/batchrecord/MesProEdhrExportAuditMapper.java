package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReportExportAuditPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrExportAuditDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrExportAuditMapper extends BaseMapperX<MesProEdhrExportAuditDO> {

    default PageResult<MesProEdhrExportAuditDO> selectPage(MesProEdhrReportExportAuditPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrExportAuditDO>()
                .eqIfPresent(MesProEdhrExportAuditDO::getReportCode, reqVO.getReportCode())
                .eqIfPresent(MesProEdhrExportAuditDO::getOperationType, reqVO.getOperationType())
                .eqIfPresent(MesProEdhrExportAuditDO::getResultStatus, reqVO.getResultStatus())
                .orderByDesc(MesProEdhrExportAuditDO::getOccurredAt)
                .orderByDesc(MesProEdhrExportAuditDO::getId));
    }
}
