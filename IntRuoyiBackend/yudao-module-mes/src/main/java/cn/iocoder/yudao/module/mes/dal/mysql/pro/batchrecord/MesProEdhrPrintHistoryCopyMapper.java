package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPrintHistoryCopyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrPrintHistoryCopyMapper extends BaseMapperX<MesProEdhrPrintHistoryCopyDO> {

    default MesProEdhrPrintHistoryCopyDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(MesProEdhrPrintHistoryCopyDO::getIdempotencyKey, idempotencyKey);
    }

    default PageResult<MesProEdhrPrintHistoryCopyDO> selectPage(PageParam pageParam) {
        return selectPage(pageParam, new LambdaQueryWrapperX<MesProEdhrPrintHistoryCopyDO>()
                .orderByDesc(MesProEdhrPrintHistoryCopyDO::getId));
    }
}
