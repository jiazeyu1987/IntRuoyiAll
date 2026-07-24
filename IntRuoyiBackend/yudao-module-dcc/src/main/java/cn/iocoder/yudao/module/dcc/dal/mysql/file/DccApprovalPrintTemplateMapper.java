package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccApprovalPrintTemplateDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * DCC approval print template mapper.
 */
@Mapper
public interface DccApprovalPrintTemplateMapper extends BaseMapperX<DccApprovalPrintTemplateDO> {

    default DccApprovalPrintTemplateDO selectActive() {
        return selectOne(new LambdaQueryWrapperX<DccApprovalPrintTemplateDO>()
                .eq(DccApprovalPrintTemplateDO::getActive, Boolean.TRUE)
                .orderByDesc(DccApprovalPrintTemplateDO::getUpdateTime)
                .orderByDesc(DccApprovalPrintTemplateDO::getId)
                .last("LIMIT 1"));
    }
}
