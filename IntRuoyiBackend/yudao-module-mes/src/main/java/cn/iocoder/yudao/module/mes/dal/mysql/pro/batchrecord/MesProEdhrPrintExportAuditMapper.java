package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPrintExportAuditDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrPrintExportAuditMapper extends BaseMapperX<MesProEdhrPrintExportAuditDO> {

    default MesProEdhrPrintExportAuditDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(MesProEdhrPrintExportAuditDO::getIdempotencyKey, idempotencyKey);
    }

    default PageResult<MesProEdhrPrintExportAuditDO> selectPage(PageParam pageParam) {
        return selectPage(pageParam, new LambdaQueryWrapperX<MesProEdhrPrintExportAuditDO>()
                .orderByDesc(MesProEdhrPrintExportAuditDO::getId));
    }
}
