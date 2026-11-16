package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryTypeExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.type.MesDvMachineryTypeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryTypeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryTypeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;

@Service
public class MesDvMachineryTypeWorkbookService {
    @Resource private MesDvMachineryTypeMapper mapper;
    @Resource private MesDvMachineryTypeService service;
    @Resource private SecurityFrameworkService security;

    public List<MesDvMachineryTypeExcelVO> exportTypes() {
        requirePermission("mes:dv-machinery-type:query");
        List<MesDvMachineryTypeDO> types = mapper.selectList();
        Map<Long, String> codes = new HashMap<>();
        types.forEach(type -> codes.put(type.getId(), type.getCode()));
        return types.stream().map(type -> {
            MesDvMachineryTypeExcelVO row = BeanUtils.toBean(type, MesDvMachineryTypeExcelVO.class);
            if (type.getParentId() != null && type.getParentId() != 0) {
                String parentCode = codes.get(type.getParentId());
                if (parentCode == null) throw new ServiceException(400, "设备类型父节点不存在：" + type.getCode());
                row.setParentCode(parentCode);
            }
            return row;
        }).toList();
    }

    // Called inside the workbook transaction; parents are imported before children regardless of row order.
    public void importTypes(List<MesDvMachineryTypeExcelVO> rows, boolean updateSupport) {
        Map<String, MesDvMachineryTypeExcelVO> pending = new LinkedHashMap<>();
        for (MesDvMachineryTypeExcelVO row : rows) {
            if (row.getCode() == null || row.getCode().isBlank() || row.getName() == null || row.getName().isBlank()
                    || row.getSort() == null || row.getStatus() == null || (row.getStatus() != 0 && row.getStatus() != 1)) {
                throw new ServiceException(400, "设备类型编码、名称、排序及有效状态必须填写");
            }
            if (pending.putIfAbsent(row.getCode(), row) != null) throw new ServiceException(400, "设备类型编码重复：" + row.getCode());
        }
        while (!pending.isEmpty()) {
            boolean progressed = false;
            Iterator<MesDvMachineryTypeExcelVO> iterator = pending.values().iterator();
            while (iterator.hasNext()) {
                MesDvMachineryTypeExcelVO row = iterator.next();
                Long parentId = 0L;
                if (row.getParentCode() != null && !row.getParentCode().isBlank()) {
                    if (pending.containsKey(row.getParentCode())) continue;
                    MesDvMachineryTypeDO parent = mapper.selectByCode(row.getParentCode());
                    if (parent == null) throw new ServiceException(400, "父设备类型编码不存在：" + row.getParentCode());
                    parentId = parent.getId();
                }
                MesDvMachineryTypeSaveReqVO req = BeanUtils.toBean(row, MesDvMachineryTypeSaveReqVO.class);
                req.setParentId(parentId);
                MesDvMachineryTypeDO existing = mapper.selectByCode(row.getCode());
                if (existing == null) {
                    requirePermission("mes:dv-machinery-type:create");
                    service.createMachineryType(req);
                } else if (updateSupport) {
                    requirePermission("mes:dv-machinery-type:update");
                    req.setId(existing.getId());
                    service.updateMachineryType(req);
                } else if (!Objects.equals(existing.getName(), row.getName())
                        || !Objects.equals(existing.getParentId(), parentId)
                        || !Objects.equals(existing.getStatus(), row.getStatus())
                        || !Objects.equals(existing.getSort(), row.getSort())
                        || !Objects.equals(existing.getRemark(), row.getRemark())) {
                    throw new ServiceException(400, "设备类型[" + row.getCode() + "]已存在且内容不同，请勾选更新已有数据");
                }
                iterator.remove();
                progressed = true;
            }
            if (!progressed) throw new ServiceException(400, "设备类型父子关系存在循环：" + pending.keySet());
        }
    }

    private void requirePermission(String permission) {
        if (!security.hasPermission(permission)) throw new AccessDeniedException("缺少设备类型权限：" + permission);
    }
}
