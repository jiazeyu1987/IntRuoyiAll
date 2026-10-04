package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRelatedFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRelatedFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRelatedFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.FileVersion;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_RELATED_FILE_DUPLICATE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_RELATED_FILE_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileRelatedFileServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileRelatedFileMapper relatedFileMapper;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Mock private DccLatestControlledFileResolver latestFileResolver;
    @Mock private DccRelationAccessPolicy relationAccessPolicy;
    @Mock private DccRelationStore relationStore;
    @InjectMocks
    private DccControlledFileRelatedFileServiceImpl service;
    @BeforeEach void tenant(){
        TenantContextHolder.setTenantId(1L);
        org.mockito.Mockito.lenient().when(controlledFileMapper.selectByIdAndTenantForUpdate(any(),any()))
                .thenAnswer(call->controlledFileMapper.selectById((Long)call.getArgument(1)));
    }
    @AfterEach void clearTenant(){TenantContextHolder.clear();}

    private void candidate(DccControlledFileDO file) {
        var projection=new FileVersion(1L,file.getId(),file.getMasterId(),file.getDccProjectCodeId(),file.getFileNumber(),file.getFileName(),file.getVersionNo(),file.getStatus(),true,false,true);
        when(latestFileResolver.resolveSelected(file.getId())).thenReturn(projection);
        when(latestFileResolver.resolveLatest(file.getMasterId())).thenReturn(projection);
    }

    @Test
    void validateAndBindRelatedFiles_emptySelectionDoesNotCreateRelations() {
        service.validateAndBindRelatedFiles(100L, 20L, List.of());

        verify(relatedFileMapper, never()).insert(any(DccControlledFileRelatedFileDO.class));
    }

    @Test
    void validateAndBindRelatedFiles_multipleSameProjectFilesCreatesExplicitRelations() {
        when(controlledFileMapper.selectById(100L)).thenReturn(DccControlledFileDO.builder()
                .id(100L).masterId(300L).tenantId(1L).dccProjectCodeId(20L).build());
        DccControlledFileDO first = relatedFile(201L, 301L, "DOC-201", "工艺文件", "V1.0");
        DccControlledFileDO second = relatedFile(202L, 302L, "DOC-202", "检验文件", "V2.0");
        candidate(first); candidate(second);

        service.validateAndBindRelatedFiles(100L, 20L, List.of(201L, 202L));

        ArgumentCaptor<DccControlledFileRelatedFileDO> captor =
                ArgumentCaptor.forClass(DccControlledFileRelatedFileDO.class);
        verify(relatedFileMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(List.of(201L, 202L), captor.getAllValues().stream()
                .map(DccControlledFileRelatedFileDO::getRelatedControlledFileId).toList());
        assertEquals("UPLOAD", captor.getAllValues().get(0).getRelationSource());
    }

    @Test
    void validateAndBindRelatedFiles_missingOwnerCannotCreateOrphanRelations() {

        assertThrows(ServiceException.class,
                () -> service.validateAndBindRelatedFiles(100L, 20L, List.of(201L)));
        verify(relatedFileMapper, never()).insert(any(DccControlledFileRelatedFileDO.class));
    }

    @Test
    void validateAndBindRelatedFiles_missingOrUnauthorizedSelectionFailsFast() {

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.validateAndBindRelatedFiles(100L, 20L, List.of(201L)));

        assertEquals(CONTROLLED_FILE_RELATED_FILE_INVALID.getCode(), exception.getCode());
        verify(relatedFileMapper, never()).insert(any(DccControlledFileRelatedFileDO.class));
    }

    @Test
    void validateAndBindRelatedFiles_duplicateSelectionFailsFast() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.validateAndBindRelatedFiles(100L, 20L, List.of(201L, 201L)));

        assertEquals(CONTROLLED_FILE_RELATED_FILE_DUPLICATE.getCode(), exception.getCode());
        verify(controlledFileMapper, never()).selectAssociatedFilesByProjectCodeId(any(), any());
    }

    @Test
    void listRelatedFiles_returnsFrozenHistoricalMetadataEvenAfterTargetMetadataChanges() {
        when(latestFileResolver.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,20L,"N","源","A/1","ACTIVE",true,false,true));
        DccControlledFileRelatedFileDO first = relation(1L, 100L, 201L, "DOC-201", "旧名称", "V1.0");
        DccControlledFileRelatedFileDO second = relation(2L, 100L, 202L, "DOC-202", "快照名称", "V2.0");
        when(relatedFileMapper.selectListByControlledFileId(100L)).thenReturn(List.of(first, second));

        List<DccControlledFileRelatedFileRespVO> result = service.listRelatedFiles(100L);

        assertEquals(2, result.size());
        assertEquals("旧名称", result.get(0).getFileName());
        assertEquals("V1.0", result.get(0).getVersionNo());
        assertEquals("快照名称", result.get(1).getFileName());
        verify(controlledFileMapper,never()).selectBatchIds(any());
    }

    @Test
    void listForwardRelations_returnsFrozenSourceRowsWithoutProjection() {
        DccControlledFileRelatedFileDO relation = relation(1L, 100L, 201L,
                "DOC-201", "关联文件", "A/1");
        when(relatedFileMapper.selectListByControlledFileId(100L)).thenReturn(List.of(relation));

        assertEquals(List.of(relation), service.listForwardRelations(100L));
    }

    @Test
    void inheritRelatedFiles_copiesSourceRelationsToCheckinIterationWithoutChangingSource() {
        DccControlledFileRelatedFileDO sourceRelation = relation(1L, 100L, 201L,
                "DOC-201", "关联文件", "A/1");
        sourceRelation.setRelatedMasterId(301L);
        sourceRelation.setTenantId(1L);
        when(controlledFileMapper.selectById(100L)).thenReturn(DccControlledFileDO.builder()
                .id(100L).masterId(10L).tenantId(1L).dccProjectCodeId(20L).build());
        when(controlledFileMapper.selectById(101L)).thenReturn(DccControlledFileDO.builder()
                .id(101L).masterId(10L).tenantId(1L).dccProjectCodeId(20L).build());
        when(relatedFileMapper.selectListByControlledFileId(100L)).thenReturn(List.of(sourceRelation));

        service.inheritRelatedFiles(100L, 101L);

        ArgumentCaptor<DccControlledFileRelatedFileDO> captor =
                ArgumentCaptor.forClass(DccControlledFileRelatedFileDO.class);
        verify(relatedFileMapper).insert(captor.capture());
        DccControlledFileRelatedFileDO inherited = captor.getValue();
        assertEquals(101L, inherited.getControlledFileId());
        assertEquals(201L, inherited.getRelatedControlledFileId());
        assertEquals(301L, inherited.getRelatedMasterId());
        assertEquals("DOC-201", inherited.getRelatedFileNumberSnapshot());
        assertEquals("关联文件", inherited.getRelatedFileNameSnapshot());
        assertEquals("A/1", inherited.getRelatedVersionNoSnapshot());
        assertEquals("CHECKIN_INHERITED", inherited.getRelationSource());
        assertEquals(100L, sourceRelation.getControlledFileId());
    }

    @Test
    void resolveCurrentActiveRelatedFileIds_retainedSignatureResolvesLatestControlledIncludingPendingEffect() {
        DccControlledFileRelatedFileDO relation = relation(1L, 100L, 201L,
                "DOC-201", "关联文件", "A/1");
        relation.setRelatedMasterId(301L);
        when(relatedFileMapper.selectListByControlledFileId(100L)).thenReturn(List.of(relation));
        when(latestFileResolver.resolveLatest(301L)).thenReturn(new FileVersion(1L,211L,301L,40L,"DOC-201","关联文件","B/1","CONTROLLED",true,true,false));

        assertEquals(List.of(211L), service.resolveCurrentActiveRelatedFileIds(100L, 20L));
    }

    @Test
    void listReverseCurrentActiveRelations_usesTenantAndRelatedMasterBoundary() {
        DccControlledFileRelatedFileDO relation = relation(2L, 200L, 100L,
                "DOC-100", "被引用文件", "B/1");
        when(relatedFileMapper.selectReverseCurrentActiveRelations(31L, 10L)).thenReturn(List.of(relation));

        assertEquals(List.of(relation), service.listReverseCurrentActiveRelations(31L, 10L));
        verify(relatedFileMapper).selectReverseCurrentActiveRelations(31L, 10L);
    }

    @Test
    void listReverseCurrentActiveRelations_missingIdentityFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> service.listReverseCurrentActiveRelations(null, 10L));
        verify(relatedFileMapper, never()).selectReverseCurrentActiveRelations(any(), any());
    }

    private DccControlledFileDO relatedFile(Long id, Long masterId, String fileNumber, String fileName,
                                             String versionNo) {
        return DccControlledFileDO.builder().id(id).masterId(masterId).fileNumber(fileNumber)
                .fileName(fileName).versionNo(versionNo).dccProjectCodeId(20L).status("ACTIVE").build();
    }

    private DccControlledFileRelatedFileDO relation(Long id, Long controlledFileId, Long relatedFileId,
                                                     String fileNumber, String fileName, String versionNo) {
        return DccControlledFileRelatedFileDO.builder().id(id).controlledFileId(controlledFileId)
                .relatedControlledFileId(relatedFileId).projectCodeId(20L).relatedMasterId(300L)
                .relatedFileNumberSnapshot(fileNumber).relatedFileNameSnapshot(fileName)
                .relatedVersionNoSnapshot(versionNo).relationSource("UPLOAD").build();
    }
}
