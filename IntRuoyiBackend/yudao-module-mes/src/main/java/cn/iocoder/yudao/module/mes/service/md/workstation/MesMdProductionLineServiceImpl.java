package cn.iocoder.yudao.module.mes.service.md.workstation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdProductionLineMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_PRODUCTION_LINE_IS_DISABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_PRODUCTION_LINE_NOT_EXISTS;

@Service
@Validated
public class MesMdProductionLineServiceImpl implements MesMdProductionLineService {

    @Resource
    private MesMdProductionLineMapper productionLineMapper;

    @Override
    public MesMdProductionLineDO validateProductionLineExists(Long id) {
        MesMdProductionLineDO line = productionLineMapper.selectById(id);
        if (line == null) {
            throw exception(MD_PRODUCTION_LINE_NOT_EXISTS);
        }
        return line;
    }

    @Override
    public MesMdProductionLineDO validateProductionLineExistsAndEnable(Long id) {
        MesMdProductionLineDO line = validateProductionLineExists(id);
        if (ObjUtil.notEqual(CommonStatusEnum.ENABLE.getStatus(), line.getStatus())) {
            throw exception(MD_PRODUCTION_LINE_IS_DISABLE);
        }
        return line;
    }

    @Override
    public MesMdProductionLineDO getProductionLine(Long id) {
        return productionLineMapper.selectById(id);
    }

    @Override
    public List<MesMdProductionLineDO> getProductionLineList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return productionLineMapper.selectListByIds(ids);
    }

}
