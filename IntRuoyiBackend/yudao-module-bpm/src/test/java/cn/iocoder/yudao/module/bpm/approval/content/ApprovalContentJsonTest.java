package cn.iocoder.yudao.module.bpm.approval.content;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ApprovalContentJsonTest {
    @Test void canonicalObjectsSortRecursivelyButArrayOrderAndTextArePreserved() {
        var first=ApprovalContentJson.parse("{\"z\":[\"02\",\"01\"],\"a\":{\"b\":null,\"a\":\"  汉字  \"}}");
        var reordered=ApprovalContentJson.parse("{\"a\":{\"a\":\"  汉字  \",\"b\":null},\"z\":[\"02\",\"01\"]}");
        assertEquals(ApprovalContentJson.hash(first),ApprovalContentJson.hash(reordered));
        assertNotEquals(ApprovalContentJson.hash(first),ApprovalContentJson.hash(ApprovalContentJson.parse("{\"a\":{\"a\":\"  汉字  \",\"b\":null},\"z\":[\"01\",\"02\"]}")));
        assertTrue(ApprovalContentJson.canonical(first).contains("  汉字  "));
    }
    @Test void rejectsDuplicateKeysTrailingDocumentsAndNativeDecimal() {
        assertThrows(IllegalArgumentException.class,()->ApprovalContentJson.parse("{\"a\":1,\"a\":2}"));
        assertThrows(IllegalArgumentException.class,()->ApprovalContentJson.parse("{} {}"));
        assertThrows(IllegalArgumentException.class,()->ApprovalContentJson.canonical(ApprovalContentJson.parse("{\"amount\":1.25}")));
    }
    @Test void receiptLocatorIsBoundToOneRealReceiptIdentity() {
        assertEquals("ApprovalCenterReviewExecutor.execute#receiptId=71",ApprovalCenterReceiptService.sourceLocator(71L));
        assertNotEquals(ApprovalCenterReceiptService.sourceLocator(71L),ApprovalCenterReceiptService.sourceLocator(72L));
        assertThrows(IllegalArgumentException.class,()->ApprovalCenterReceiptService.sourceLocator(0L));
    }
}
