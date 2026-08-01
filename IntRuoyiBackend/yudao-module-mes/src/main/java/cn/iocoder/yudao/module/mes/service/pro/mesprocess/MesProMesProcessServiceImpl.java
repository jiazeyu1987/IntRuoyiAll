package cn.iocoder.yudao.module.mes.service.pro.mesprocess;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo.MesProMesProcessMachineryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo.MesProMesProcessPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo.MesProMesProcessRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess.MesProMesProcessCatalogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess.MesProMesProcessCatalogMachineryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.mesprocess.MesProMesProcessCatalogMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.mesprocess.MesProMesProcessCatalogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Validated
public class MesProMesProcessServiceImpl implements MesProMesProcessService {

    @Resource
    private MesProMesProcessCatalogMapper catalogMapper;
    @Resource
    private MesProMesProcessCatalogMachineryMapper catalogMachineryMapper;

    @Override
    public PageResult<MesProMesProcessRespVO> getMesProcessPage(MesProMesProcessPageReqVO pageReqVO) {
        PageResult<MesProMesProcessCatalogDO> page = catalogMapper.selectPage(pageReqVO);
        List<MesProMesProcessRespVO> rows = BeanUtils.toBean(page.getList(), MesProMesProcessRespVO.class);
        if (CollUtil.isEmpty(rows)) {
            return new PageResult<>(rows, page.getTotal());
        }

        Map<Long, List<MesProMesProcessMachineryRespVO>> machineryMap = buildMachineryMap(rows);
        for (MesProMesProcessRespVO row : rows) {
            row.setRowKey(row.getSourceSheetName() + ":" + row.getSourceRowNo());
            row.setMachineryList(machineryMap.getOrDefault(row.getId(), Collections.emptyList()));
        }
        return new PageResult<>(rows, page.getTotal());
    }

    private Map<Long, List<MesProMesProcessMachineryRespVO>> buildMachineryMap(
            List<MesProMesProcessRespVO> rows) {
        List<Long> catalogIds = rows.stream()
                .map(MesProMesProcessRespVO::getId)
                .toList();
        List<MesProMesProcessCatalogMachineryDO> machineryRows =
                catalogMachineryMapper.selectListByCatalogIds(catalogIds);
        if (CollUtil.isEmpty(machineryRows)) {
            return Collections.emptyMap();
        }
        return machineryRows.stream()
                .collect(Collectors.groupingBy(MesProMesProcessCatalogMachineryDO::getCatalogId,
                        LinkedHashMap::new,
                        Collectors.mapping(item -> BeanUtils.toBean(item, MesProMesProcessMachineryRespVO.class),
                                Collectors.toList())));
    }
}
