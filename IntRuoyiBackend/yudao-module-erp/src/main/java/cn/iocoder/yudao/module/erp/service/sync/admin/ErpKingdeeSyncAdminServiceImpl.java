package cn.iocoder.yudao.module.erp.service.sync.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncWatermarkRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncWatermarkMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class ErpKingdeeSyncAdminServiceImpl implements ErpKingdeeSyncAdminService {

    @Resource
    private ErpKingdeeSyncRunMapper runMapper;
    @Resource
    private ErpKingdeeSyncWatermarkMapper watermarkMapper;

    @Override
    public PageResult<ErpKingdeeSyncRunRespVO> getRunPage(ErpKingdeeSyncRunPageReqVO pageReqVO) {
        PageResult<ErpKingdeeSyncRunDO> pageResult = runMapper.selectPage(pageReqVO);
        return new PageResult<>(BeanUtils.toBean(pageResult.getList(), ErpKingdeeSyncRunRespVO.class),
                pageResult.getTotal());
    }

    @Override
    public List<ErpKingdeeSyncWatermarkRespVO> getWatermarks() {
        return BeanUtils.toBean(watermarkMapper.selectListOrderBySyncType(), ErpKingdeeSyncWatermarkRespVO.class);
    }
}
