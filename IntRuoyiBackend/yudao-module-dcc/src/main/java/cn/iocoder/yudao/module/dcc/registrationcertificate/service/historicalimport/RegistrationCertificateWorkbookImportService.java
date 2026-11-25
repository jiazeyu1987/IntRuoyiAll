package cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService.RowCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService.WorkbookRow;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@RequiredArgsConstructor
public class RegistrationCertificateWorkbookImportService {
    private final JdbcTemplate jdbc;
    private final DccRegistrationCertificateMigrationService migration;

    public record ImportResult(Map<String,Integer> sheetCounts, int inserted, int existing) {}

    public ImportResult importFile(MultipartFile file, Long actor) throws IOException {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
            throw invalidParamException("请选择不超过 10 MB 的注册证 Excel 文件");
        byte[] bytes = file.getBytes();
        List<RegistrationCertificateWorkbook.Entry> entries;
        try {
            entries = new RegistrationCertificateWorkbook().read(new ByteArrayInputStream(bytes));
        } catch (IOException | org.apache.poi.UnsupportedFileFormatException | org.apache.poi.ooxml.POIXMLException ex) {
            throw invalidParamException("无法读取工作簿，请上传有效的 .xlsx 文件：" + ex.getMessage());
        }
        long tenant = TenantContextHolder.getRequiredTenantId();
        Map<String,Long> companies = new HashMap<>();
        Map<String,Integer> counts = new LinkedHashMap<>();
        List<WorkbookRow> rows = new ArrayList<>();
        for (var entry : entries) {
            Long owner = companies.computeIfAbsent(entry.company(), name -> {
                var ids = jdbc.queryForList("SELECT id FROM mdm_enterprise WHERE tenant_id=? AND deleted=0 AND status='ENABLE' AND type='OWNED_COMPANY' AND name=?",
                        Long.class, tenant, name);
                if (ids.size()!=1) throw invalidParamException(entry.sheet()+" 第"+entry.row()+"行：所属公司须唯一匹配已启用的自有企业："+name);
                return ids.get(0);
            });
            var f = entry.facts();
            if (entry.product().length()>255 || tooLong(f.getCertificateNo(),128) || tooLong(f.getClassification(),64)
                    || tooLong(f.getRemark(),1024)) throw invalidParamException(entry.sheet()+" 第"+entry.row()+"行：字段长度超出注册证限制");
            var command = new RowCommand(entry.row(), owner, null, null, entry.product(),
                    "DOMESTIC".equals(f.getCertificateType()) ? entry.company() : null, f.getCertificateNo(),
                    f.getFirstObtainedDate(), f.getApprovalDate(), f.getEffectiveDate(), f.getExpiryDate(),
                    f.getClassification(), null, null, null, null, null, null, null, null,
                    List.of(), List.of("NO_ATTACHMENT_POLICY","HISTORICAL_WORKBOOK"));
            rows.add(new WorkbookRow(command,f));
            counts.merge(entry.sheet(),1,Integer::sum);
        }
        String hash = HexFormat.of().withUpperCase().formatHex(org.apache.commons.codec.digest.DigestUtils.sha256(bytes));
        var result = migration.commitWorkbookRows(tenant, actor, hash, UUID.randomUUID().toString(), rows);
        return new ImportResult(counts,result.committedCount(),result.replayedCount());
    }

    private static boolean tooLong(String value,int max) {
        return value!=null && value.length()>max;
    }
}
