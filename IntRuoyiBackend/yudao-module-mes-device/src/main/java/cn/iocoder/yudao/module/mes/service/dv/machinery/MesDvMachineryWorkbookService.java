package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.ExcelReader;
import cn.idev.excel.read.metadata.ReadSheet;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.*;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.workshop.MesMdWorkshopSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkshopMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.*;

/** 设备类型、车间与设备三表导入导出，编码为跨环境关联键。 */
@Service
public class MesDvMachineryWorkbookService {
    @Resource private MesMdWorkshopMapper workshopMapper;
    @Resource private MesMdWorkshopService workshopService;
    @Resource private MesDvMachineryService machineryService;
    @Resource private SecurityFrameworkService security;
    @Resource private MesDvMachineryTypeWorkbookService typeWorkbookService;

    public void write(HttpServletResponse response, Class<?> deviceHead, List<?> devices,
                      List<MesDvWorkshopExcelVO> workshops, List<MesDvMachineryTypeExcelVO> types) throws IOException {
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream()).autoCloseStream(false)
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                .registerConverter(new LongStringConverter()).build()) {
            writer.write(types, FastExcelFactory.writerSheet(0, "设备类型").head(MesDvMachineryTypeExcelVO.class).build());
            writer.write(workshops, FastExcelFactory.writerSheet(1, "车间列表").head(MesDvWorkshopExcelVO.class).build());
            writer.write(devices, FastExcelFactory.writerSheet(2, "设备台账").head(deviceHead).build());
        }
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8("设备台账.xlsx"));
        response.addHeader("X-Excel-Export-Record-Count", String.valueOf(devices.size()));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    public List<MesDvWorkshopExcelVO> workshops() {
        if (!security.hasPermission("mes:md-workshop:export")) {
            throw new AccessDeniedException("缺少车间导出权限");
        }
        return BeanUtils.toBean(workshopMapper.selectList(), MesDvWorkshopExcelVO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    public MesDvMachineryImportRespVO read(MultipartFile file, boolean updateSupport) throws IOException {
        byte[] bytes = file.getBytes();
        try (ExcelReader reader = FastExcelFactory.read(new ByteArrayInputStream(bytes)).build()) {
            Set<String> names = new HashSet<>();
            for (ReadSheet sheet : reader.excelExecutor().sheetList()) names.add(sheet.getSheetName());
            if (!names.containsAll(List.of("设备类型", "设备台账", "车间列表"))) {
                throw new ServiceException(400, "请使用包含设备类型、车间列表、设备台账三个工作表的新模板");
            }
        }
        List<MesDvWorkshopExcelVO> workshops = FastExcelFactory.read(new ByteArrayInputStream(bytes))
                .head(MesDvWorkshopExcelVO.class).sheet("车间列表").doReadSync();
        List<MesDvMachineryImportExcelVO> devices = FastExcelFactory.read(new ByteArrayInputStream(bytes))
                .head(MesDvMachineryImportExcelVO.class).sheet("设备台账").doReadSync();
        List<MesDvMachineryTypeExcelVO> types = FastExcelFactory.read(new ByteArrayInputStream(bytes))
                .head(MesDvMachineryTypeExcelVO.class).sheet("设备类型").doReadSync();
        typeWorkbookService.importTypes(types, updateSupport);
        Set<String> codes = new HashSet<>();
        int created = 0, updated = 0, reused = 0;
        for (MesDvWorkshopExcelVO row : workshops) {
            if (row.getCode() == null || row.getCode().isBlank() || row.getName() == null || row.getName().isBlank()
                    || row.getStatus() == null || (row.getStatus() != 0 && row.getStatus() != 1)) {
                throw new ServiceException(400, "车间编码、名称和有效状态（0启用1停用）必须填写");
            }
            if (!codes.add(row.getCode())) throw new ServiceException(400, "车间编码重复：" + row.getCode());
            MesMdWorkshopDO existing = workshopMapper.selectByCode(row.getCode());
            MesMdWorkshopSaveReqVO req = BeanUtils.toBean(row, MesMdWorkshopSaveReqVO.class);
            if (existing == null) {
                requirePermission("mes:md-workshop:create");
                workshopService.createWorkshop(req);
                created++;
            } else if (updateSupport) {
                requirePermission("mes:md-workshop:update");
                req.setId(existing.getId());
                workshopService.updateWorkshop(req);
                updated++;
            } else {
                if (!Objects.equals(existing.getName(), row.getName()) || !Objects.equals(existing.getStatus(), row.getStatus())
                        || !Objects.equals(existing.getRemark(), row.getRemark())
                        || (existing.getArea() == null ? row.getArea() != null
                        : row.getArea() == null || existing.getArea().compareTo(row.getArea()) != 0)) {
                    throw new ServiceException(400, "车间[" + row.getCode() + "]已存在且内容不同，请勾选更新已有数据");
                }
                reused++;
            }
        }
        MesDvMachineryImportRespVO result = machineryService.importMachineryList(devices, updateSupport);
        if (!result.getFailureCodes().isEmpty()) {
            throw new ServiceException(400, "导入失败，本次设备类型、车间和设备修改均已回滚：" + result.getFailureCodes());
        }
        result.setWorkshopCreateCount(created);
        result.setWorkshopUpdateCount(updated);
        result.setWorkshopReuseCount(reused);
        return result;
    }

    private void requirePermission(String permission) {
        if (!security.hasPermission(permission)) throw new AccessDeniedException("缺少车间操作权限：" + permission);
    }
}
