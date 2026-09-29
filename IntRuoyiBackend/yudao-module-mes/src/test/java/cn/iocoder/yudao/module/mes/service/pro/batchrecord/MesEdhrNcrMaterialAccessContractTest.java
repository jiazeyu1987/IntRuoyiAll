package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesEdhrNcrMaterialAccessContractTest {
    @Test
    void deletedOnlyMaterialCannotAcquireActiveReference() {
        var provider = providerFor(70002L,
                "{\"activeMaterials\":[{\"fileId\":70001,\"url\":\"first.pdf\"}],"
                        + "\"reviewMaterialEvents\":[{\"fileId\":70002,\"action\":\"DELETE\"}]}");
        assertThrows(IllegalStateException.class, () -> provider.resolve(70002L));
    }

    @Test
    void duplicateOrFractionalIdentityIsRejected() {
        var duplicate = providerFor(70002L, "{\"activeMaterials\":["
                + "{\"fileId\":70002,\"url\":\"a\"},{\"fileId\":70002,\"url\":\"b\"}]}");
        assertThrows(IllegalStateException.class, () -> duplicate.resolve(70002L));
        var fractional = providerFor(70002L,
                "{\"activeMaterials\":[{\"fileId\":70002.5,\"url\":\"a\"}]}");
        assertThrows(IllegalStateException.class, () -> fractional.resolve(70002L));
    }

    @Test
    void exactLargeStringIdentityIsPreserved() {
        long fileId = 9007199254740993L;
        var provider = providerFor(fileId,
                "{\"activeMaterials\":[{\"fileId\":\"9007199254740993\",\"url\":\"a\"}]}");
        assertTrue(provider.resolve(fileId).orElseThrow().versionKey()
                .startsWith("NCR-MATERIAL-1001-9007199254740993-"));
    }

    @Test
    void historicalPrimaryWithoutJsonRemainsReadable() {
        assertTrue(providerFor(70001L, null).resolve(70001L).isPresent());
        assertThrows(IllegalStateException.class, () -> providerFor(70002L, null).resolve(70002L));
    }

    private MesEdhrNonconformanceReviewMaterialBusinessFileAccessProvider providerFor(Long requestedId, String json) {
        var mapper = mock(MesProEdhrNonconformanceReviewMapper.class);
        var review = new MesProEdhrNonconformanceReviewDO().setId(1001L)
                .setActiveOrderId(8101L).setReviewStatus("closed")
                .setReviewMaterialFileId(70001L).setReviewMaterialUrl("https://storage.example/first.pdf")
                .setReviewMaterialsJson(json);
        review.setTenantId(122L);
        when(mapper.selectListByReviewMaterialFileId(requestedId)).thenReturn(List.of(review));
        return new MesEdhrNonconformanceReviewMaterialBusinessFileAccessProvider(mapper);
    }

    @Test
    void secondActiveMaterialHasItsOwnFormalReference() {
        var mapper = mock(MesProEdhrNonconformanceReviewMapper.class);
        var review = new MesProEdhrNonconformanceReviewDO().setId(1001L)
                .setActiveOrderId(8101L).setReviewStatus("closed")
                .setReviewMaterialFileId(70001L).setReviewMaterialUrl("https://storage.example/first.pdf")
                .setReviewMaterialsJson("{\"activeMaterials\":["
                        + "{\"fileId\":70001,\"url\":\"https://storage.example/first.pdf\",\"fileName\":\"first.pdf\"},"
                        + "{\"fileId\":70002,\"url\":\"https://storage.example/second.pdf\",\"fileName\":\"second.pdf\"}]}");
        review.setTenantId(122L);
        when(mapper.selectListByReviewMaterialFileId(70002L)).thenReturn(List.of(review));
        var provider = new MesEdhrNonconformanceReviewMaterialBusinessFileAccessProvider(mapper);
        var reference = assertDoesNotThrow(() -> provider.resolve(70002L)).orElseThrow();
        assertEquals(1001L, reference.businessId());
        assertEquals(122L, reference.tenantId());
        assertTrue(reference.versionKey().startsWith("NCR-MATERIAL-1001-70002-"));
    }
}
