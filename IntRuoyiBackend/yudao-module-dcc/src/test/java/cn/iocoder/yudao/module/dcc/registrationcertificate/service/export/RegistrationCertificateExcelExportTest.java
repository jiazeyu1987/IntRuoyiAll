package cn.iocoder.yudao.module.dcc.registrationcertificate.service.export;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.io.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegistrationCertificateExcelExportTest {
    DccRegistrationCertificateQueryService queries;
    DccRegistrationCertificateAuditMapper audits;
    PermissionApi permissions;
    MutableClock clock;
    DccRegistrationCertificateExportService service;
    DccRegistrationCertificatePageItem domestic;
    @BeforeEach void setup() {
        queries = mock(DccRegistrationCertificateQueryService.class);
        audits = mock(DccRegistrationCertificateAuditMapper.class);
        permissions = mock(PermissionApi.class);
        when(permissions.hasAnyPermissions(eq(2L), any(String[].class))).thenReturn(true);
        when(audits.insert(any(DccRegistrationCertificateAuditDO.class))).thenReturn(1);
        clock = new MutableClock();
        service = new DccRegistrationCertificateExportService(queries, audits, permissions, new DccRegistrationCertificateBusinessClock(), clock);
        domestic = DccRegistrationCertificatePageItem.builder().versionId(11L).certificateId(21L).ownerCompanyId(31L)
                .ownerCompanyName("测试公司").productName("产品A").projectCode("00012")
                .factsJson("{\"certificateType\":\"DOMESTIC\",\"certificateNo\":\"0001234567890123456789\",\"effectiveDate\":\"2026-01-02\",\"remark\":\"=1+1\\n第二行\"}").build();
        when(queries.getExportRows(eq(1L), eq(2L), any(), any(), anyInt())).thenReturn(List.of(domestic));
        when(queries.getExportRow(1L, 2L, 11L)).thenReturn(domestic);
    }
    DccRegistrationCertificateExportService.Preview preview() {
        return service.preview(1L, 2L, new DccRegistrationCertificateExportService.Scope("current", "FILTERED", null, List.of()));
    }
    @Test void threeSheetsTextDatesAndAuditOnce() throws Exception {
        var p = preview(); service.generate(1L, 2L, p.snapshotId(), p.requestId());
        service.generate(1L, 2L, p.snapshotId(), p.requestId());
        var result = new ByteArrayOutputStream();
        try (var stream = service.open(1L, 2L, p.requestId())) { stream.write(result); }
        try (var book = new XSSFWorkbook(new ByteArrayInputStream(result.toByteArray()))) {
            assertEquals(List.of("国内注册证", "CE证书", "FDA证书"), java.util.stream.IntStream.range(0,3).mapToObj(book::getSheetName).toList());
            var row = book.getSheetAt(0).getRow(1);
            assertEquals("00012", row.getCell(1).getStringCellValue());
            assertEquals(CellType.STRING, row.getCell(3).getCellType());
            assertEquals("0001234567890123456789", row.getCell(3).getStringCellValue());
            assertEquals(CellType.NUMERIC, row.getCell(5).getCellType());
            assertEquals("=1+1\n第二行", row.getCell(8).getStringCellValue());
            assertEquals(CellType.BLANK, book.getSheetAt(2).getRow(2).getCell(0).getCellType());
        }
        var audit = ArgumentCaptor.forClass(DccRegistrationCertificateAuditDO.class);
        verify(audits, times(1)).insert(audit.capture());
        assertTrue(audit.getValue().getDetailJson().contains("ordered_record_ids"));
        assertEquals("EXCEL_GENERATED", audit.getValue().getEventType());
    }
    @Test void fixedWindowActiveStreamAndPermissionInvalidation() throws Exception {
        var p = preview(); service.generate(1L, 2L, p.snapshotId(), p.requestId());
        var expires = service.status(1L, 2L, p.requestId()).expiresAt();
        var active = service.open(1L, 2L, p.requestId());
        clock.advance(299);
        try (var retry = service.open(1L, 2L, p.requestId())) { assertEquals(expires, retry.status().expiresAt()); }
        clock.advance(1); service.cleanup();
        assertThrows(ServiceException.class, () -> service.open(1L, 2L, p.requestId()));
        var output = new ByteArrayOutputStream(); active.write(output); active.close();
        assertTrue(output.size() > 0);
        assertEquals("EXPIRED", service.status(1L, 2L, p.requestId()).state());
    }
    @Test void revokedPermissionInvalidatesEvenWithActiveStream() throws Exception {
        var p = preview(); service.generate(1L, 2L, p.snapshotId(), p.requestId());
        try (var active = service.open(1L, 2L, p.requestId())) {
            when(queries.getExportRow(1L,2L,11L)).thenReturn(null);
            assertThrows(ServiceException.class, () -> service.open(1L,2L,p.requestId()));
            active.write(new ByteArrayOutputStream());
        }
        when(queries.getExportRow(1L,2L,11L)).thenReturn(domestic);
        assertEquals("INVALID", service.status(1L,2L,p.requestId()).state());
        assertThrows(ServiceException.class, () -> service.open(1L,2L,p.requestId()));
    }
    @Test void snapshotExpiryWrongOwnerAndTabDenied() {
        var p = preview(); clock.advance(1800);
        assertThrows(ServiceException.class, () -> service.generate(1L,2L,p.snapshotId(),p.requestId()));
        assertThrows(ServiceException.class, () -> service.status(9L,2L,p.requestId()));
        assertThrows(ServiceException.class, () -> service.preview(1L,2L,new DccRegistrationCertificateExportService.Scope("old","FILTERED",null,List.of())));
    }
    @Test void auditFailureNeverMakesFileAvailable() {
        var p=preview(); when(audits.insert(any(DccRegistrationCertificateAuditDO.class))).thenReturn(0);
        assertThrows(ServiceException.class, () -> service.generate(1L,2L,p.snapshotId(),p.requestId()));
        assertEquals("FAILED",service.status(1L,2L,p.requestId()).state());
        assertThrows(ServiceException.class, () -> service.open(1L,2L,p.requestId()));
    }
    @Test void auditLatencyDoesNotExtendRetentionAndMenuRevocationInvalidatesPermanently() throws Exception {
        var p=preview();var generated=clock.instant();
        when(audits.insert(any(DccRegistrationCertificateAuditDO.class))).thenAnswer(i->{clock.advance(30);return 1;});
        service.generate(1L,2L,p.snapshotId(),p.requestId());
        assertEquals(generated.plusSeconds(300),service.status(1L,2L,p.requestId()).expiresAt());
        try(var active=service.open(1L,2L,p.requestId())) {
            when(permissions.hasAnyPermissions(eq(2L),any(String[].class))).thenReturn(false);
            assertThrows(ServiceException.class,()->service.status(1L,2L,p.requestId()));
            when(permissions.hasAnyPermissions(eq(2L),any(String[].class))).thenReturn(true);
            assertEquals("INVALID",service.status(1L,2L,p.requestId()).state());
            active.write(new ByteArrayOutputStream());
        }
        assertThrows(ServiceException.class,()->service.open(1L,2L,p.requestId()));
    }
    @Test void mappingBlockIsExplicitAndSourceCountsReconcile() throws Exception {
        var ce=DccRegistrationCertificatePageItem.builder().versionId(12L).factsJson("{\"certificateType\":\"OTHER\"}").build();
        when(queries.getExportRows(eq(1L),eq(2L),any(),any(),anyInt())).thenReturn(List.of(domestic,ce));
        when(queries.getExportRow(1L,2L,12L)).thenReturn(ce);
        var p=preview(); service.generate(1L,2L,p.snapshotId(),p.requestId());
        var s=service.status(1L,2L,p.requestId()); assertEquals(p.count(),s.exported()+s.skipped()); assertEquals(1,s.skipped());
        when(queries.getExportRows(eq(1L),eq(2L),any(),any(),anyInt())).thenReturn(List.of(ce));
        var onlyCe=preview();
        var failure=assertThrows(ServiceException.class,()->service.generate(1L,2L,onlyCe.snapshotId(),onlyCe.requestId()));
        assertTrue(failure.getMessage().contains("注册证类型未配置导出模板"));
    }
    @Test void templateDynamicCeSectionsFdaGroupingAndExpandedRows() throws Exception {
        var lines=new ArrayList<RegistrationCertificateExportWorkbook.Line>();
        for(int i=0;i<3;i++) { var values=new Object[19]; values[1]="公司A"; values[2]="CE产品"+i;
            lines.add(new RegistrationCertificateExportWorkbook.Line(i,"CE",0,"A",values,null)); }
        var second=new Object[19]; second[1]="公司B";
        lines.add(new RegistrationCertificateExportWorkbook.Line(4,"CE",1,"B",second,null));
        for(int i=0;i<2;i++) { var values=new Object[13];values[0]="K holder";values[2]="0001";values[3]="000"+i;
            lines.add(new RegistrationCertificateExportWorkbook.Line(5,"FDA",0,"A",values,"https://example.com/record/1")); }
        var bytes=new ByteArrayOutputStream();new RegistrationCertificateExportWorkbook().write(lines,bytes,100,()->{});
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(bytes.toByteArray()))) {
            var ce=book.getSheetAt(1);assertEquals("No",ce.getRow(6).getCell(0).getStringCellValue());
            assertTrue(ce.getMergedRegions().stream().anyMatch(r->r.formatAsString().equals("B4:B6")));
            assertEquals("公司B",ce.getRow(9).getCell(1).getStringCellValue());
            var fda=book.getSheetAt(2);assertEquals("0001",fda.getRow(2).getCell(2).getStringCellValue());
            assertEquals("https://example.com/record/1",fda.getRow(3).getCell(2).getHyperlink().getAddress());
            assertTrue(fda.getMergedRegions().stream().anyMatch(r->r.formatAsString().equals("A3:A4")));
        }
        assertThrows(IOException.class,()->new RegistrationCertificateExportWorkbook().write(lines,new ByteArrayOutputStream(),5,()->{}));
    }
    @Test void benchmarkRepresentativeWorkbook() throws Exception {
        var writer=new RegistrationCertificateExportWorkbook();var line=writer.map(domestic);
        var lines=new ArrayList<RegistrationCertificateExportWorkbook.Line>();
        for(int i=0;i<1000;i++)lines.add(line);
        var bytes=new ByteArrayOutputStream();long start=System.nanoTime(); writer.write(lines,bytes,10000,()->{});
        System.out.println("EXPORT_BENCHMARK records=1000 rows=1000 bytes="+bytes.size()+" millis="+(System.nanoTime()-start)/1_000_000);
        var expanded=new ArrayList<RegistrationCertificateExportWorkbook.Line>();
        for(int i=0;i<1000;i++) {
            expanded.add(line);
            var ceValues=new Object[19];ceValues[1]="公司";ceValues[2]="CE product"+i;
            expanded.add(new RegistrationCertificateExportWorkbook.Line(i,"CE",i%2,"company",ceValues,null));
            for(int n=0;n<10;n++) {var values=new Object[13]; values[0]="holder";values[2]="000"+i;values[3]="000"+n;
                if(i*10+n<9998)
                    expanded.add(new RegistrationCertificateExportWorkbook.Line(i,"FDA",0,"holder",values,null));}
        }
        bytes=new ByteArrayOutputStream();start=System.nanoTime();writer.write(expanded,bytes,10000,()->{});
        System.out.println("EXPORT_BENCHMARK template_lines="+expanded.size()+" bytes="+bytes.size()+" millis="+(System.nanoTime()-start)/1_000_000);
    }

    @Test void oneUserCanOnlyGenerateOneAndDuplicateRequestDoesNotGenerateTwice() throws Exception {
        var first=preview();var second=preview();
        var started=new java.util.concurrent.CountDownLatch(1);var finish=new java.util.concurrent.CountDownLatch(1);
        when(queries.getExportRow(1L,2L,11L)).thenAnswer(invocation->{started.countDown();assertTrue(finish.await(5,java.util.concurrent.TimeUnit.SECONDS));return domestic;});
        var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var task=executor.submit(()->{service.generate(1L,2L,first.snapshotId(),first.requestId());return null;});
            assertTrue(started.await(5,java.util.concurrent.TimeUnit.SECONDS));
            service.generate(1L,2L,first.snapshotId(),first.requestId());
            assertEquals("GENERATING",service.status(1L,2L,first.requestId()).state());
            assertThrows(ServiceException.class,()->service.generate(1L,2L,second.snapshotId(),second.requestId()));
            finish.countDown();task.get(5,java.util.concurrent.TimeUnit.SECONDS);
            verify(audits,times(1)).insert(any(DccRegistrationCertificateAuditDO.class));
        } finally {finish.countDown();executor.shutdownNow();}
    }

    @Test void acceptedRequestSurvivesSnapshotExpiryAndFieldsReadAtGeneration() throws Exception {
        var p=preview();clock.advance(1799);
        domestic.setProductName("生成时新名称");
        service.generate(1L,2L,p.snapshotId(),p.requestId());clock.advance(2);
        var bytes=new ByteArrayOutputStream();
        try(var stream=service.open(1L,2L,p.requestId())) {stream.write(bytes);}
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals("生成时新名称",book.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
        }
    }

    @Test void overLimitPreviewDisablesConfirmation() {
        when(queries.getExportRows(eq(1L),eq(2L),any(),any(),anyInt())).thenThrow(new DccRegistrationCertificateQueryService.ExportLimitExceeded());
        assertFalse(preview().allowed());
    }

    @Test void unsupportedTypeAndNewFdaDoNotFabricateMissingData() {
        var writer=new RegistrationCertificateExportWorkbook();
        var row=DccRegistrationCertificatePageItem.builder().versionId(1L).factsJson("{\"certificateType\":\"FDA\",\"listingNumber\":\"123\"}").build();
        var line=writer.map(row);
        assertEquals("123",line.values()[3]);
        assertNull(line.values()[10]); assertNull(line.values()[11]); assertNull(line.link());
        row.setFactsJson("{\"certificateType\":\"OTHER\"}");
        assertThrows(RegistrationCertificateExportWorkbook.MappingUnavailable.class,()->writer.map(row));
    }

    @Test void ceAndFdaKeepRecordsWithBlanksAndUseOnlyVerifiedSources() throws Exception {
        var ce=DccRegistrationCertificatePageItem.builder().versionId(12L).certificateId(22L).ownerCompanyId(31L)
                .ownerCompanyName("真实公司").productName("CE实际产品").factsJson("""
                {"certificateType":"CE","historicalSheet":"CE证书","classification":"不能推定MDD",
                 "productNameEn":"CE real","tradeName":"商标","notifiedBodyCode":"0123",
                 "expiryDate":"2029-01-01","ceCertificates":[{"revision":"自由Rev","certificateNo":"不可推定MDR"}],
                 "historicalValues":{"公司名称":"旧公司不使用","MDR分类":"IIa","MDR规则":"Rule 5",
                   "MDR证书编号":"00012","MDR签发日期":"2026-01-02","MDD首次签发日期":"2024-03",
                   "EMDN代码":"001234","延期函编号":"EXT-1","CE标志":"0123"}}
                """).build();
        var fda=DccRegistrationCertificatePageItem.builder().versionId(13L).certificateId(23L).ownerCompanyId(31L)
                .factsJson("""
                {"certificateType":"FDA","historicalSheet":"FDA证书","certificateHolder":"实际持有人",
                 "kpNumber":"K0001","listingNumber":"00099","productNameEn":"FDA real",
                 "productCode":"ABC","classification":"II","regulationCode":"21 CFR",
                 "approvalDate":"2026-01-03","registrationLink":"https://www.accessdata.fda.gov/real",
                 "factoryRegistrationStatus":"不能作为Listing状态","historicalValues":{"文件编号":"00001234567890123456789","列名状态":"Active"}}
                """).build();
        var emptyCe=DccRegistrationCertificatePageItem.builder().versionId(14L).certificateId(24L).ownerCompanyId(31L)
                .factsJson("{\"certificateType\":\"CE\",\"classification\":\"III\",\"expiryDate\":\"2029-01-01\",\"notifiedBodyCode\":\"不得推断CE标志\"}").build();
        var emptyFda=DccRegistrationCertificatePageItem.builder().versionId(15L).certificateId(25L).ownerCompanyId(31L)
                .factsJson("{\"certificateType\":\"FDA\",\"listingNumber\":\"L2\",\"factoryRegistrationStatus\":\"ACTIVE\",\"registrationLink\":\"javascript:alert(1)\"}").build();
        var rows=List.of(domestic,ce,fda,emptyCe,emptyFda);
        when(queries.getExportRows(eq(1L),eq(2L),any(),any(),anyInt())).thenReturn(rows);
        for(var row:rows) when(queries.getExportRow(1L,2L,row.getVersionId())).thenReturn(row);
        var p=preview(); assertEquals(2,p.structureNotices().size());
        service.generate(1L,2L,p.snapshotId(),p.requestId());
        var status=service.status(1L,2L,p.requestId()); assertEquals(5,status.exported());assertEquals(0,status.skipped());
        var bytes=new ByteArrayOutputStream();try(var stream=service.open(1L,2L,p.requestId())) {stream.write(bytes);}
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(bytes.toByteArray()))) {
            var c=book.getSheetAt(1).getRow(3);
            assertEquals("真实公司",c.getCell(1).getStringCellValue()); assertEquals("CE real",c.getCell(2).getStringCellValue());
            assertEquals("IIa",c.getCell(5).getStringCellValue()); assertEquals("Rule 5",c.getCell(6).getStringCellValue());
            assertEquals("00012",c.getCell(7).getStringCellValue()); assertEquals(CellType.NUMERIC,c.getCell(8).getCellType());
            assertEquals("001234",c.getCell(10).getStringCellValue()); assertEquals("2024-03",c.getCell(13).getStringCellValue());
            assertEquals(CellType.BLANK,c.getCell(11).getCellType()); assertEquals(CellType.BLANK,c.getCell(14).getCellType());
            assertEquals("EXT-1",c.getCell(15).getStringCellValue());assertEquals("0123",c.getCell(18).getStringCellValue());
            for(int i=5;i<=18;i++) assertEquals(CellType.BLANK,book.getSheetAt(1).getRow(4).getCell(i).getCellType());
            var f=book.getSheetAt(2).getRow(2);
            assertEquals("00099",f.getCell(3).getStringCellValue());assertEquals("00001234567890123456789",f.getCell(10).getStringCellValue());
            assertEquals("Active",f.getCell(11).getStringCellValue()); assertEquals("https://www.accessdata.fda.gov/real",f.getCell(2).getHyperlink().getAddress());
            var missing=book.getSheetAt(2).getRow(3);assertEquals("L2",missing.getCell(3).getStringCellValue());
            assertEquals(CellType.BLANK,missing.getCell(10).getCellType());assertEquals(CellType.BLANK,missing.getCell(11).getCellType());
            assertNull(missing.getCell(2).getHyperlink());
        }
    }
    static class MutableClock extends Clock {
        Instant instant=Instant.parse("2026-11-24T00:00:00Z");
        void advance(long seconds){instant=instant.plusSeconds(seconds);}
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return instant;}
    }
}
