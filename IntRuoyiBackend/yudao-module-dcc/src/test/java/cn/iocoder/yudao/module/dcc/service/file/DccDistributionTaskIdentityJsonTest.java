package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionRecipientReceiptRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionResultRespVO;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Actual JsonUtils wire contract for my-page consumers that later address exact body/receipt identities. */
class DccDistributionTaskIdentityJsonTest {
    private DccDistributionTaskRespVO row(long id) {
        var row=new DccDistributionTaskRespVO();
        row.setRecipientId(id);row.setDistributionId(id-1);row.setControlledFileId(id-2);
        row.setCategoryId(id-3);row.setUserId(id-4);row.setDepartmentId(id-5);
        row.setFileName("精确正文.pdf");row.setTitle("精确版本");row.setFileNumber("SOP-EXACT");
        row.setVersionNo("B/1");row.setFileStatus("ACTIVE");row.setDistributionMedium("PUBLIC_FOLDER");
        row.setStatus("READY_TO_ACKNOWLEDGE");return row;
    }
    private Map<String,Long> expected(DccDistributionTaskRespVO row) {
        var values=new LinkedHashMap<String,Long>();values.put("recipientId",row.getRecipientId());values.put("distributionId",row.getDistributionId());
        values.put("controlledFileId",row.getControlledFileId());values.put("categoryId",row.getCategoryId());values.put("userId",row.getUserId());values.put("departmentId",row.getDepartmentId());return values;
    }
    private void exactStrings(JsonNode json,DccDistributionTaskRespVO original) {
        for(var field:expected(original).entrySet()){
            assertTrue(json.get(field.getKey()).isTextual(),field.getKey()+" must survive ordinary JavaScript JSON parsing as a string");
            assertEquals(field.getValue().toString(),json.get(field.getKey()).textValue(),field.getKey());
        }
    }
    @Test void actualJsonUtilsPreservesAllSixLongMaxIdentitiesAsExactStrings() {
        var original=row(Long.MAX_VALUE);exactStrings(JsonUtils.parseTree(JsonUtils.toJsonString(original)),original);
    }
    @Test void actualMyPageCommonResultEnvelopePreservesUnsafeIdentitiesAndOriginalTaskState() {
        var original=row(9007199254741010L);
        var response=CommonResult.success(new PageResult<>(List.of(original),1L));
        var json=JsonUtils.parseTree(JsonUtils.toJsonString(response));var task=json.get("data").get("list").get(0);
        exactStrings(task,original);assertEquals(1L,json.get("data").get("total").longValue());
        assertEquals("B/1",task.get("versionNo").textValue());assertEquals("READY_TO_ACKNOWLEDGE",task.get("status").textValue());
        assertEquals("PUBLIC_FOLDER",task.get("distributionMedium").textValue());assertEquals("ACTIVE",task.get("fileStatus").textValue());
    }
    @Test void originalLowIdentityAndNullableFactsAreNotFilledOrReinterpreted() {
        var original=row(100L);original.setCategoryId(null);original.setDepartmentId(null);
        var json=JsonUtils.parseTree(JsonUtils.toJsonString(original));
        for(String field:List.of("recipientId","distributionId","controlledFileId","userId"))assertEquals(expected(original).get(field).toString(),json.get(field).textValue());
        for(String field:List.of("categoryId","departmentId","readAt","acknowledgedAt","publishedTime"))assertTrue(json.path(field).isNull() || json.path(field).isMissingNode(),field);
        assertEquals("SOP-EXACT",json.get("fileNumber").textValue());assertEquals("READY_TO_ACKNOWLEDGE",json.get("status").textValue());
    }
    @Test void actualJsonUtilsPreservesAllSixRecipientAndSignatureIdentitiesAsExactStrings() {
        var row=new DccDistributionRecipientReceiptRespVO();row.setControlledFileId(Long.MAX_VALUE);row.setDistributionId(Long.MAX_VALUE-1);
        row.setRecipientId(Long.MAX_VALUE-2);row.setDepartmentId(Long.MAX_VALUE-3);row.setUserId(Long.MAX_VALUE-4);row.setSignatureId(Long.MAX_VALUE-5);
        row.setVersionNo("B/1");row.setMessageStatus("SENT");row.setSignerNameSnapshot("实际签名人");
        var json=JsonUtils.parseTree(JsonUtils.toJsonString(CommonResult.success(List.of(row)))).get("data").get(0);
        var expected=Map.of("controlledFileId",Long.MAX_VALUE,"distributionId",Long.MAX_VALUE-1,"recipientId",Long.MAX_VALUE-2,
                "departmentId",Long.MAX_VALUE-3,"userId",Long.MAX_VALUE-4,"signatureId",Long.MAX_VALUE-5);
        for(var field:expected.entrySet()){assertTrue(json.get(field.getKey()).isTextual(),field.getKey());assertEquals(field.getValue().toString(),json.get(field.getKey()).textValue(),field.getKey());}
        assertEquals("B/1",json.get("versionNo").textValue());assertEquals("SENT",json.get("messageStatus").textValue());assertEquals("实际签名人",json.get("signerNameSnapshot").textValue());
    }
    @Test void unsignedReceiptNullSignatureAndTimesRemainNullWithoutManufacturedFacts() {
        var row=new DccDistributionRecipientReceiptRespVO();row.setControlledFileId(42L);row.setDistributionId(51L);row.setRecipientId(71L);row.setUserId(99L);
        row.setMessageStatus("FAILED");row.setErrorMessage("真实发送失败");
        var json=JsonUtils.parseTree(JsonUtils.toJsonString(row));
        assertEquals("42",json.get("controlledFileId").textValue());assertEquals("51",json.get("distributionId").textValue());
        assertEquals("71",json.get("recipientId").textValue());assertEquals("99",json.get("userId").textValue());
        for(String field:List.of("departmentId","signatureId","sentAt","readAt","acknowledgedAt","signedAt","distributionCompletedAt"))
            assertTrue(json.path(field).isNull() || json.path(field).isMissingNode(),field);
        assertEquals("FAILED",json.get("messageStatus").textValue());assertEquals("真实发送失败",json.get("errorMessage").textValue());
    }
    @Test void existingAutomaticResultSafeIdentityAndNullBatchContractRemainUnchanged() {
        var row=new DccAutomaticDistributionResultRespVO();row.setControlledFileId(Long.MAX_VALUE);row.setBatchId(9007199254740993L);row.setStatus("FAILED");
        var json=JsonUtils.parseTree(JsonUtils.toJsonString(row));
        assertTrue(json.get("controlledFileId").isTextual());assertEquals(Long.toString(Long.MAX_VALUE),json.get("controlledFileId").textValue());
        assertTrue(json.get("batchId").isTextual());assertEquals("9007199254740993",json.get("batchId").textValue());
        row.setBatchId(null);row.setStatus("NOT_CREATED");json=JsonUtils.parseTree(JsonUtils.toJsonString(row));
        assertTrue(json.path("batchId").isNull() || json.path("batchId").isMissingNode());assertEquals("NOT_CREATED",json.get("status").textValue());
    }
}
