package cn.iocoder.yudao.module.dcc.service.file;

import java.util.Map;
import java.util.Objects;

/** The one immutable full-model catalog; no mutable deployment version or key/latest inference. */
public final class DccWorkflowDefinitionCatalog {
    public enum FlowRule { LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL, PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL, PUBLIC_V5_OBSOLETE_APPROVAL_END, PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM }
    public record Graph(String executionContract,String processKey,String definitionDigest,FlowRule flowRule) { }
    private static final Map<String,Graph> GRAPHS=Map.of(
            "a8222aa8aa09dba8aba4bc2ff3e2b296b1b824849e48dca3f9a4d1a5809d99c6",new Graph("LEGACY_V4_ACTION",DccControlledFileProcessDefinitionKeys.UPLOAD,"a8222aa8aa09dba8aba4bc2ff3e2b296b1b824849e48dca3f9a4d1a5809d99c6",FlowRule.LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL),
            "42aff38a44dec91c5cd64cae5a19507263260d4d5fed7ec0f28b3e4632805cc4",new Graph("LEGACY_V4_ACTION",DccControlledFileProcessDefinitionKeys.REVISION,"42aff38a44dec91c5cd64cae5a19507263260d4d5fed7ec0f28b3e4632805cc4",FlowRule.LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL),
            "dbf44a1bd3afd792a27b195f43f50f4efc886ea85ec8c35b1c05730e9221e162",new Graph("PUBLIC_MATRIX_NATIVE_V1",DccControlledFileProcessDefinitionKeys.UPLOAD,"dbf44a1bd3afd792a27b195f43f50f4efc886ea85ec8c35b1c05730e9221e162",FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL),
            "9922d53710050318a4827b3b61449727ff87e3c62f3146619be4d19c7cffcd13",new Graph("PUBLIC_MATRIX_NATIVE_V1",DccControlledFileProcessDefinitionKeys.REVISION,"9922d53710050318a4827b3b61449727ff87e3c62f3146619be4d19c7cffcd13",FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL),
            "36bc2379b56d0ada0571fccd573024fb05f951632e0963cbcd9ab1702af7b2c2",new Graph("PUBLIC_MATRIX_NATIVE_V2",DccControlledFileProcessDefinitionKeys.UPLOAD,"36bc2379b56d0ada0571fccd573024fb05f951632e0963cbcd9ab1702af7b2c2",FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM),
            "84d039f7fab6c81159fb049637a0ff70b6eb3e57d6dfc6b47a64475e421935d5",new Graph("PUBLIC_MATRIX_NATIVE_V2",DccControlledFileProcessDefinitionKeys.REVISION,"84d039f7fab6c81159fb049637a0ff70b6eb3e57d6dfc6b47a64475e421935d5",FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM),
            "a3fec88d08ba5a1b4c6830ac05e653b5bc2faa72e9e11b3b54c1fa8006d2f79d",new Graph("PUBLIC_MATRIX_NATIVE_V1",DccControlledFileProcessDefinitionKeys.OBSOLETE,"a3fec88d08ba5a1b4c6830ac05e653b5bc2faa72e9e11b3b54c1fa8006d2f79d",FlowRule.PUBLIC_V5_OBSOLETE_APPROVAL_END));
    private DccWorkflowDefinitionCatalog() { }
    public static Graph find(String key,String digest){var graph=GRAPHS.get(digest);return graph!=null&&Objects.equals(key,graph.processKey())?graph:null;}
    public static Graph require(String contract,String key,String digest){
        var graph=GRAPHS.get(digest);
        if(graph==null||!Objects.equals(contract,graph.executionContract())||!Objects.equals(key,graph.processKey()))
            throw new IllegalStateException("EXECUTION_CONTRACT_UNREGISTERED_DEFINITION");
        return graph;
    }
}
