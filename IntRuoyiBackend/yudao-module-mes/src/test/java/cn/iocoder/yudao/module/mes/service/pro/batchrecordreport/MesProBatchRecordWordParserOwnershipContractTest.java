package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordWordParserOwnershipContractTest {

    private static final String SHARED = "SHARED_RAW_STRUCTURE";
    private static final String MES = "MES_ADAPTER_SEMANTICS";
    private static final Set<String> ALLOWED_OWNERS = Set.of(SHARED, MES);
    private static final String CONTRACT_RESOURCE = "contracts/shared-word-parser-ownership.json";
    private static final String REAL_DOC_FIXTURE = "fixtures/pressure-pump-record.doc";

    private static final Map<String, Class<?>> MODEL_TYPES = Map.of(
            MesProBatchRecordParsedCell.class.getSimpleName(), MesProBatchRecordParsedCell.class,
            MesProBatchRecordParsedTable.class.getSimpleName(), MesProBatchRecordParsedTable.class,
            MesProBatchRecordDocumentFrame.class.getSimpleName(), MesProBatchRecordDocumentFrame.class);

    @Test
    void ownershipContract_coversEveryModelFieldAndParserHelperBidirectionally() throws Exception {
        JsonNode contract = readContract();

        assertModelFieldInventory(contract.path("modelFields"));
        assertParserHelperInventory(contract.path("parserHelpers"));
        assertExternalMesSemantics(contract.path("externalMesSemantics"));
        assertRealDocFixtureReadable();
    }

    private void assertModelFieldInventory(JsonNode modelFields) {
        assertTrue(modelFields.isObject(), "modelFields must be a JSON object");
        assertEquals(MODEL_TYPES.keySet(), fieldNames(modelFields),
                "contract must cover exactly the three MES parsed models");

        MODEL_TYPES.forEach((modelName, modelType) -> {
            Map<String, String> declaredOwnership = ownershipMap(modelFields.path(modelName));
            Set<String> actualFields = Arrays.stream(modelType.getDeclaredFields())
                    .filter(field -> !field.isSynthetic())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .map(Field::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            assertEquals(actualFields, declaredOwnership.keySet(),
                    modelName + " field ownership must match reflection in both directions");
            assertOnlyApprovedOwners(declaredOwnership, "model field " + modelName);
        });

        assertMesField(modelFields, "MesProBatchRecordParsedCell", "fillable");
        assertMesField(modelFields, "MesProBatchRecordParsedCell", "visualBlank");
        assertMesField(modelFields, "MesProBatchRecordParsedCell", "borderless");
        assertMesField(modelFields, "MesProBatchRecordParsedCell", "reviewedCellRule");
        assertMesField(modelFields, "MesProBatchRecordParsedCell", "cellRuleSource");
        assertMesField(modelFields, "MesProBatchRecordParsedCell", "documentFrameRole");
        assertMesField(modelFields, "MesProBatchRecordParsedCell", "placeholder");
        assertMesField(modelFields, "MesProBatchRecordParsedCell", "inputType");
        assertMesField(modelFields, "MesProBatchRecordParsedTable", "sourceSplitIndex");
        assertMesField(modelFields, "MesProBatchRecordParsedTable", "sourceTableIndex");
        assertMesField(modelFields, "MesProBatchRecordParsedTable", "tableTitle");
        assertMesField(modelFields, "MesProBatchRecordParsedTable", "preserveSourceGrid");
        assertMesField(modelFields, "MesProBatchRecordParsedTable", "routeBSource");
    }

    private void assertParserHelperInventory(JsonNode parserHelpers) {
        Map<String, String> declaredOwnership = ownershipMap(parserHelpers);
        Set<String> actualHelpers = Arrays.stream(MesProBatchRecordDocParser.class.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .filter(method -> !method.isBridge())
                .map(this::methodSignature)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertOnlyApprovedOwners(declaredOwnership, "parser helper");
        Set<String> mesAdapterHelpers = helpersOwnedBy(declaredOwnership, MES);
        Set<String> migratedSharedHelpers = helpersOwnedBy(declaredOwnership, SHARED);
        assertEquals(actualHelpers, mesAdapterHelpers,
                "MES adapter helper ownership must match reflection in both directions");
        assertFalse(migratedSharedHelpers.isEmpty(), "pre-migration shared raw helper inventory cannot be empty");
        assertTrue(actualHelpers.stream().noneMatch(migratedSharedHelpers::contains),
                "shared raw extraction helpers must be absent from the MES adapter");

        assertEquals(SHARED, declaredOwnership.get("collectTopLevelTables(Range)"));
        assertEquals(SHARED, declaredOwnership.get("resolveVisualColumnBoundaries(Table)"));
        assertEquals(SHARED, declaredOwnership.get("resolveDocxCellText(XWPFTableCell)"));
        assertEquals(MES, declaredOwnership.get("parseShared(byte[],String)"));
        assertEquals(MES, declaredOwnership.get("toMesRawTable(WordTable,boolean)"));
        assertEquals(MES, declaredOwnership.get("toMesDocumentFrame(WordDocumentFrame,String)"));
        assertEquals(MES, declaredOwnership.get("toMesRows(List,boolean)"));
        assertEquals(MES, declaredOwnership.get("toMesCell(WordCell,boolean,Integer)"));
        assertEquals(MES, declaredOwnership.get("splitTemplates(MesProBatchRecordParsedTable)"));
        assertEquals(MES, declaredOwnership.get("extractTemplateTitle(String)"));
        assertEquals(MES, declaredOwnership.get("normalizeRowsToVisualGrid(List,int)"));
        assertEquals(MES, declaredOwnership.get("normalizeRowsToLogicalGrid(List,List)"));
        assertEquals(MES, declaredOwnership.get("isPackedLabelGridText(String)"));
    }

    private Set<String> helpersOwnedBy(Map<String, String> ownership, String owner) {
        return ownership.entrySet().stream()
                .filter(entry -> owner.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void assertExternalMesSemantics(JsonNode externalMesSemantics) throws ClassNotFoundException {
        Map<String, String> declaredOwnership = ownershipMap(externalMesSemantics);
        Set<String> required = Set.of(
                "MesProBatchRecordSharedPageTitleRules",
                "batchSpecificTitleDecision",
                "batchSpecificTableSplitting",
                "batchSpecificGridNormalization",
                "routeRecognition",
                "jimuReportGeneration");
        assertEquals(required, declaredOwnership.keySet(),
                "MES-only external semantics inventory must be explicit and exact");
        assertOnlyApprovedOwners(declaredOwnership, "external MES semantic");
        assertTrue(declaredOwnership.values().stream().allMatch(MES::equals),
                "all external business semantics must remain in MES");
        assertNotNull(Class.forName(MesProBatchRecordSharedPageTitleRules.class.getName()));
    }

    private void assertRealDocFixtureReadable() throws Exception {
        try (InputStream input = resource(REAL_DOC_FIXTURE)) {
            byte[] bytes = input.readAllBytes();
            assertTrue(bytes.length > 0, "mandatory pressure-pump-record.doc fixture must be non-empty");
        }
    }

    private JsonNode readContract() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        try (InputStream input = resource(CONTRACT_RESOURCE)) {
            return mapper.readTree(input);
        }
    }

    private InputStream resource(String path) {
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        assertNotNull(input, "required test resource is missing: " + path);
        return input;
    }

    private Map<String, String> ownershipMap(JsonNode node) {
        assertTrue(node.isObject(), "ownership section must be a JSON object");
        Map<String, String> result = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            assertTrue(entry.getValue().isTextual(), "ownership must be textual for " + entry.getKey());
            result.put(entry.getKey(), entry.getValue().asText());
        });
        return result;
    }

    private Set<String> fieldNames(JsonNode node) {
        Set<String> names = new LinkedHashSet<>();
        Iterator<String> fields = node.fieldNames();
        fields.forEachRemaining(names::add);
        return names;
    }

    private void assertOnlyApprovedOwners(Map<String, String> ownership, String section) {
        assertFalse(ownership.isEmpty(), section + " ownership cannot be empty");
        assertTrue(ownership.values().stream().allMatch(ALLOWED_OWNERS::contains),
                section + " may only use " + ALLOWED_OWNERS + ": " + ownership);
    }

    private void assertMesField(JsonNode modelFields, String model, String field) {
        assertEquals(MES, modelFields.path(model).path(field).asText(), model + "." + field + " must remain in MES");
    }

    private String methodSignature(Method method) {
        String parameters = Arrays.stream(method.getParameterTypes())
                .map(this::simpleTypeName)
                .collect(Collectors.joining(","));
        return method.getName() + "(" + parameters + ")";
    }

    private String simpleTypeName(Class<?> type) {
        if (type.isArray()) {
            return simpleTypeName(type.getComponentType()) + "[]";
        }
        return type.getSimpleName();
    }
}
