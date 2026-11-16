package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.*;
import cn.iocoder.yudao.module.erp.dal.mysql.product.*;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ErpProductImportService {
    @Resource private ErpProductMapper productMapper;
    @Resource private ErpProductCategoryMapper categoryMapper;
    @Resource private ErpProductUnitMapper unitMapper;

    @Transactional(rollbackFor = Exception.class)
    public int importProducts(List<ErpProductRespVO> rows) {
        if (rows.isEmpty()) throw new IllegalArgumentException("导入文件不能为空");
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            ErpProductRespVO row = rows.get(i);
            String prefix = "第 " + (i + 2) + " 行：";
            if (row.getBarCode() == null || row.getBarCode().isBlank()
                    || row.getName() == null || row.getName().isBlank())
                throw new IllegalArgumentException(prefix + "产品条码、名称不能为空");
            if (!codes.add(row.getBarCode()) || productMapper.selectByBarCode(row.getBarCode()) != null)
                throw new IllegalArgumentException(prefix + "产品条码重复，导入仅支持新增：" + row.getBarCode());
            if (row.getStatus() == null || (row.getStatus() != 0 && row.getStatus() != 1))
                throw new IllegalArgumentException(prefix + "产品状态必须为 0（开启）或 1（关闭）");
            if (row.getCategoryName() == null || row.getUnitName() == null)
                throw new IllegalArgumentException(prefix + "产品分类、单位不能为空");
            var categories = categoryMapper.selectList(ErpProductCategoryDO::getName, row.getCategoryName());
            var units = unitMapper.selectList(ErpProductUnitDO::getName, row.getUnitName());
            if (categories.size() != 1 || units.size() != 1)
                throw new IllegalArgumentException(prefix + "分类或单位不存在或名称不唯一，请先维护基础资料");
            ErpProductDO product = BeanUtils.toBean(row, ErpProductDO.class);
            product.setId(null);
            product.setCreateTime(null);
            product.setCategoryId(categories.get(0).getId());
            product.setUnitId(units.get(0).getId());
            productMapper.insert(product);
        }
        return rows.size();
    }
}
