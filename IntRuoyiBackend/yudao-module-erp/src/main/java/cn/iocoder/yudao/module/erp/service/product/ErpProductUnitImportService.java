package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitImportVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUnitMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Slf4j
public class ErpProductUnitImportService {

    @Resource
    private ErpProductUnitMapper productUnitMapper;

    /** 仅新增，按名字跳过文件内及系统已有的重复单位；其他校验或写入失败时整体回滚。 */
    @Transactional(rollbackFor = Exception.class)
    public int importUnits(List<ErpProductUnitImportVO> rows) {
        if (rows == null || rows.isEmpty()) {
            throw invalidParamException("导入文件不能为空");
        }
        Set<String> names = new HashSet<>();
        List<ErpProductUnitDO> units = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            ErpProductUnitImportVO row = rows.get(i);
            String prefix = "第 " + (i + 2) + " 行：";
            if (row == null || row.getName() == null || row.getName().isBlank()) {
                throw invalidParamException(prefix + "单位名字不能为空");
            }
            String name = row.getName().trim();
            // 明确跳过重复单位，保留已有记录；文件内同名单位以首条为准。
            if (!names.add(name) || productUnitMapper.selectByName(name) != null) {
                continue;
            }
            String status = row.getStatus() == null ? "" : row.getStatus().trim();
            int statusValue;
            switch (status) {
                case "0", "开启" -> statusValue = 0;
                case "1", "关闭" -> statusValue = 1;
                default -> throw invalidParamException(prefix + "单位状态必须为 0（开启）或 1（关闭）");
            }
            units.add(ErpProductUnitDO.builder().name(name).status(statusValue).build());
        }
        units.forEach(productUnitMapper::insert);
        log.info("产品单位导入：新增 {} 条，跳过重复 {} 条", units.size(), rows.size() - units.size());
        return units.size();
    }

}
