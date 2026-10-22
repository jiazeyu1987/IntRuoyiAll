package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class DccPublicMatrixNativeDerivationTest {
    List<DccCategoryApprovalRouteNodeDO> raw(){return List.of(
        DccCategoryApprovalRouteNodeDO.builder().id(1L).stageType("SIGNOFF").marker("●").subjectType("DEPT").candidateSourceType("DEPT").candidateSourceIds("51").approveMethod("ALL").approveRatio(100).requireAllApprovals(true).required(true).build(),
        DccCategoryApprovalRouteNodeDO.builder().id(2L).stageType("APPROVAL").marker("●").subjectType("USER").candidateSourceType("USER").candidateSourceIds("99").approveMethod("ANY").requireAllApprovals(false).required(true).build(),
        DccCategoryApprovalRouteNodeDO.builder().id(3L).stageNo(4).stageType("DOC_CONTROL").marker("▲").subjectType("DCC_POSITION").candidateSourceType("POSITION").candidateSourceIds("10").approveMethod("ANY").requireAllApprovals(false).required(true).build());}
    @Test void explicitDerivationRetainsDepartmentObligationsAndDoesNotMutatePublicRules(){var raw=raw();var nodes=DccPublicMatrixNativeDerivation.derive(raw,"NEW",List.of(51L,52L));assertEquals("51,52",nodes.get(0).getCandidateSourceIds());assertEquals(List.of("MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW"),nodes.stream().map(DccCategoryApprovalRouteNodeDO::getStageCode).toList());assertNull(raw.get(0).getStageCode());}
    @Test void optionalApprovalAndNonDepartmentSignoffFailRatherThanSkip(){var raw=raw();raw.get(1).setMarker("◎");assertThrows(IllegalArgumentException.class,()->DccPublicMatrixNativeDerivation.derive(raw,"NEW",List.of(51L)));raw.get(1).setMarker("●");raw.get(0).setSubjectType("USER");assertThrows(IllegalArgumentException.class,()->DccPublicMatrixNativeDerivation.derive(raw,"NEW",List.of(51L)));}
    @Test void originalMandatoryDepartmentCanBeRemovedFromThisApplicationWithoutChangingPublicRules(){
        for(var action:List.of("NEW","REVISION","OBSOLETE")){
            var raw=new ArrayList<>(raw());
            raw.add(DccCategoryApprovalRouteNodeDO.builder().id(4L).stageType("SIGNOFF").marker("●").subjectType("DEPT")
                    .candidateSourceType("DEPT").candidateSourceIds("52").approveMethod("ALL").approveRatio(100).requireAllApprovals(true).required(true).build());
            var before=raw.stream().map(DccCategoryApprovalRouteNodeDO::toString).toList();
            var projection=DccPublicMatrixNativeDerivation.project(raw,action,List.of(52L));
            assertEquals("52",projection.nodes().get(0).getCandidateSourceIds());
            assertFalse(projection.groups().stream().filter(g->g.sourceNodeId().equals(1L)).findFirst().orElseThrow().applied());
            assertTrue(projection.groups().stream().filter(g->g.sourceNodeId().equals(4L)).findFirst().orElseThrow().applied());
            assertEquals(before,raw.stream().map(DccCategoryApprovalRouteNodeDO::toString).toList());
            assertEquals("●",projection.groups().get(0).marker());assertTrue(projection.groups().get(0).required());
            assertEquals("ANY",projection.nodes().get(1).getApproveMethod());assertFalse(projection.nodes().get(1).getRequireAllApprovals());
        }
    }
    @Test void retainedAndAddedDepartmentsBothRequireAllApprovalsEvenWhenNoMandatorySourceRemains(){
        var projection=DccPublicMatrixNativeDerivation.project(raw(),"NEW",List.of(53L,52L));
        var signoff=projection.nodes().get(0);
        assertEquals("52,53",signoff.getCandidateSourceIds());assertEquals(52L,signoff.getCandidateSourceId());
        assertEquals("ALL",signoff.getApproveMethod());assertEquals(100,signoff.getApproveRatio());assertTrue(signoff.getRequireAllApprovals());
        assertFalse(projection.groups().get(0).applied());
        DccPublicMatrixNativeDerivation.validateRuntime(projection.nodes(),"NEW");
    }
    @Test void explicitEmptySelectionHasTheExactHintAndNeverRestoresPublicDefaults(){
        var error=assertThrows(cn.iocoder.yudao.module.dcc.service.route.DccApprovalMatrixCapabilityException.class,
                ()->DccPublicMatrixNativeDerivation.project(raw(),"NEW",List.of()));
        assertEquals("APPROVAL_MATRIX_SIGNOFF_EMPTY",error.getReasonCode());
        assertEquals("请至少选择一个会签部门",error.getMessage().substring(error.getMessage().indexOf('：')+1));
        assertEquals("51",DccPublicMatrixNativeDerivation.project(raw(),"NEW",null).nodes().get(0).getCandidateSourceIds());
    }
    @Test void invalidDepartmentIdentitiesAreStillRejectedAfterRemovingRetentionRestriction(){
        for(var selected:List.of(List.of(52L,52L),List.of(0L),List.of(-1L),Arrays.asList(52L,null)))
            assertThrows(IllegalArgumentException.class,()->DccPublicMatrixNativeDerivation.project(raw(),"NEW",selected));
    }
    @Test void firstDefaultPreviewIncludesMandatoryAndOptionalFormalDepartmentsWithoutRewritingMarkers(){
        var raw=new ArrayList<>(raw());
        raw.add(DccCategoryApprovalRouteNodeDO.builder().id(4L).stageType("SIGNOFF").marker("◎").subjectType("DEPT")
                .candidateSourceType("DEPT").candidateSourceIds("52").approveMethod("ALL").approveRatio(100).requireAllApprovals(true).required(true).build());
        var before=raw.stream().map(DccCategoryApprovalRouteNodeDO::toString).toList();
        var projection=DccPublicMatrixNativeDerivation.project(raw,"NEW",null);
        assertEquals("51,52",projection.nodes().get(0).getCandidateSourceIds());
        assertEquals("ALL",projection.nodes().get(0).getApproveMethod());assertEquals(100,projection.nodes().get(0).getApproveRatio());
        assertTrue(projection.groups().stream().filter(g->"SIGNOFF".equals(g.stageType())).allMatch(DccPublicMatrixNativeDerivation.RuleGroup::applied));
        assertEquals(before,raw.stream().map(DccCategoryApprovalRouteNodeDO::toString).toList());
        assertEquals("52",DccPublicMatrixNativeDerivation.project(raw,"NEW",List.of(52L)).nodes().get(0).getCandidateSourceIds());
    }
    @Test void allOptionalDefaultDepartmentsAreFormalDefaultsAndRemainIndividuallyRemovable(){
        var raw=raw();raw.get(0).setMarker("◎");raw.get(0).setRequired(false);raw.get(0).setCandidateSourceIds("52,51");
        var projection=DccPublicMatrixNativeDerivation.project(raw,"REVISION",null);
        assertEquals("51,52",projection.nodes().get(0).getCandidateSourceIds());assertTrue(projection.nodes().get(0).getRequireAllApprovals());
        assertEquals("52,51",raw.get(0).getCandidateSourceIds());assertFalse(projection.groups().get(0).required());
        assertEquals("52",DccPublicMatrixNativeDerivation.project(raw,"REVISION",List.of(52L)).nodes().get(0).getCandidateSourceIds());
    }
    @Test void obsoleteHasNoDocumentControlOrTrainingProjection(){var nodes=DccPublicMatrixNativeDerivation.derive(raw(),"OBSOLETE",List.of(51L));assertEquals(List.of("MATRIX_REVIEW","MATRIX_APPROVAL"),nodes.stream().map(DccCategoryApprovalRouteNodeDO::getStageCode).toList());}
    @Test void historicalMarkerKeepsRawRequiredParticipationWithoutBeingConvertedToMustOrOptional(){
        var raw=raw();raw.get(0).setMarker("▲");raw.get(0).setRequired(true);raw.get(1).setMarker("▲");raw.get(1).setRequired(true);
        var nodes=DccPublicMatrixNativeDerivation.derive(raw,"NEW",List.of(51L));
        assertEquals("51",nodes.get(0).getCandidateSourceIds());assertEquals("ANY",nodes.get(1).getApproveMethod());
        assertEquals("▲",raw.get(0).getMarker());assertEquals("▲",raw.get(1).getMarker());
        raw.get(0).setRequired(false);assertEquals("51",DccPublicMatrixNativeDerivation.derive(raw,"NEW",null).get(0).getCandidateSourceIds());
        assertFalse(DccPublicMatrixNativeDerivation.project(raw,"NEW",null).groups().get(0).required());
        assertEquals("51",DccPublicMatrixNativeDerivation.derive(raw,"NEW",List.of(51L)).get(0).getCandidateSourceIds());
    }
    @Test void allApprovalAndFullRatioRetainTheirStoredPolicyInsteadOfBecomingAny(){
        var raw=raw();var approval=raw.get(1);approval.setCandidateSourceIds("99,100");approval.setApproveMethod("ALL");
        approval.setApproveRatio(100);approval.setRequireAllApprovals(true);
        var node=DccPublicMatrixNativeDerivation.derive(raw,"NEW",List.of(51L)).get(1);
        assertEquals("ALL",node.getApproveMethod());assertEquals(100,node.getApproveRatio());assertTrue(node.getRequireAllApprovals());
        approval.setApproveRatio(60);assertThrows(IllegalArgumentException.class,()->DccPublicMatrixNativeDerivation.derive(raw,"NEW",List.of(51L)));
    }
    @Test void multipleMandatoryAllGroupsHaveExactlyExpressibleAllPolicyAndKeepEverySourceRule(){
        var nodes=new ArrayList<>(raw());nodes.get(1).setCandidateSourceIds("99,100");nodes.get(1).setApproveMethod("ALL");
        nodes.get(1).setApproveRatio(100);nodes.get(1).setRequireAllApprovals(true);
        nodes.add(DccCategoryApprovalRouteNodeDO.builder().id(4L).stageType("APPROVAL").marker("●").subjectType("USER")
                .candidateSourceType("USER").candidateSourceIds("101").approveMethod("ANY").requireAllApprovals(false).required(true).build());
        var approval=DccPublicMatrixNativeDerivation.derive(nodes,"NEW",List.of(51L)).get(1);
        assertEquals("99,100,101",approval.getCandidateSourceIds());assertEquals("ALL",approval.getApproveMethod());
        var projection=DccPublicMatrixNativeDerivation.project(nodes,"NEW",List.of(51L));
        assertEquals(List.of(2L,4L),projection.groups().stream().filter(group->"APPROVAL".equals(group.stageType())).map(DccPublicMatrixNativeDerivation.RuleGroup::sourceNodeId).toList());
    }
    @Test void independentMultipleCandidateAnyGroupsAreBusinessCapabilityErrorsWithOriginalNodeFacts(){
        var nodes=new ArrayList<>(raw());nodes.get(1).setCandidateSourceIds("99,100");
        nodes.add(DccCategoryApprovalRouteNodeDO.builder().id(4L).stageType("APPROVAL").marker("●").subjectType("USER")
                .candidateSourceType("USER").candidateSourceIds("101,102").approveMethod("ANY").requireAllApprovals(false).required(true).build());
        var error=assertThrows(cn.iocoder.yudao.module.dcc.service.route.DccApprovalMatrixCapabilityException.class,
                ()->DccPublicMatrixNativeDerivation.project(nodes,"NEW",List.of(51L)));
        assertEquals("APPROVAL_MATRIX_APPROVAL_GROUP_EXECUTION_REQUIRED",error.getReasonCode());
        assertTrue(error.getMessage().contains("原节点=2"));assertTrue(error.getMessage().contains("审批方式=ANY"));
        assertEquals(4,nodes.size());assertEquals("99,100",nodes.get(1).getCandidateSourceIds());
    }
}
