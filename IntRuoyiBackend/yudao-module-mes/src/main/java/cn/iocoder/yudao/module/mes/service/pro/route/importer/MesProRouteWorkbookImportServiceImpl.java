package cn.iocoder.yudao.module.mes.service.pro.route.importer;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
@Service
public class MesProRouteWorkbookImportServiceImpl implements MesProRouteWorkbookImportService {
    @Resource private MesProRouteResourceImportService resourceImport;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteWorkbookImportResult importWorkbook(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalidParamException("请上传完整流程资源文件");
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            return resourceImport.importWorkbook(workbook);
        } catch (IOException error) { throw invalidParamException("无法读取流程资源文件：" + error.getMessage()); }
    }
}
