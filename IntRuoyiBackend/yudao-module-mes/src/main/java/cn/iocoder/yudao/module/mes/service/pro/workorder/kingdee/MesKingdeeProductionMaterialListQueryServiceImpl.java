package cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListGroupRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListRespVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
public class MesKingdeeProductionMaterialListQueryServiceImpl
        implements MesKingdeeProductionMaterialListQueryService {

    @Resource
    private MesKingdeeProductionMaterialListMapper productionMaterialListMapper;

    @Override
    public PageResult<MesKingdeeProductionMaterialListRespVO> getPage(
            MesKingdeeProductionMaterialListPageReqVO pageReqVO) {
        return BeanUtils.toBean(productionMaterialListMapper.selectPage(pageReqVO),
                MesKingdeeProductionMaterialListRespVO.class);
    }

    @Override
    public PageResult<MesKingdeeProductionMaterialListGroupRespVO> getGroupPage(
            MesKingdeeProductionMaterialListPageReqVO pageReqVO) {
        if (PageParam.PAGE_SIZE_NONE.equals(pageReqVO.getPageSize())) {
            List<MesKingdeeProductionMaterialListGroupRespVO> list =
                    productionMaterialListMapper.selectGroupList(pageReqVO);
            return new PageResult<>(list, (long) list.size());
        }
        IPage<MesKingdeeProductionMaterialListGroupRespVO> page = MyBatisUtils.buildPage(pageReqVO);
        IPage<MesKingdeeProductionMaterialListGroupRespVO> result =
                productionMaterialListMapper.selectGroupPage(page, pageReqVO);
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    @Override
    public List<MesKingdeeProductionMaterialListDetailRespVO> getDetailList(String sourceBillNo) {
        return BeanUtils.toBean(productionMaterialListMapper.selectListBySourceBillNo(sourceBillNo),
                MesKingdeeProductionMaterialListDetailRespVO.class);
    }

}
