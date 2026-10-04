package cn.iocoder.yudao.module.dcc.service.file;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import javax.sql.DataSource;
import java.nio.file.*;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccLegacyMaintenanceGateTest {

    @TempDir
    Path temp;

    final DataSource source = mock(DataSource.class);

    final org.springframework.mock.env.MockEnvironment environment = new org.springframework.mock.env.MockEnvironment()
            .withProperty("spring.profiles.active", "local-maintenance");

    DccLegacyMaintenanceGate gate() {
        environment.setActiveProfiles("local-maintenance");
        return new DccLegacyMaintenanceGate(source, environment);
    }

    ObjectNode descriptor(Path path) throws Exception {
        var d = DccLegacyMaintenanceGate.JSON.createObjectNode();
        d.put("path", path.toString());
        d.put("sha256", DccLegacyMaintenanceGate.hash(Files.readAllBytes(path)));
        d.put("bytes", Files.size(path));
        return d;
    }

    ObjectNode request() throws Exception {
        Path manifest = temp.resolve("manifest.json");
        Files.writeString(manifest, "{}");
        Path data = temp.resolve("proof.json");
        Files.writeString(data, "{}");
        ObjectNode auth = DccLegacyMaintenanceGate.JSON.createObjectNode();
        auth.put("status", "EXACT_LEGACY_SCOPE_REGISTRATION_USER_APPROVED");
        auth.put("userAnswerReceived", true);
        auth.put("actualAnswer", "授权按此方案执行");
        auth.put("sourceQuestionId", "real-user-question-id");
        auth.put("sourceMessageReference", "real-user-message-reference");
        auth.put("scopeId", "sealed-root-scope");
        auth.put("manifestSha256", DccLegacyMaintenanceGate.hash(Files.readAllBytes(manifest)));
        auth.put("claimCount", 25);
        auth.put("versionCount", 39);
        auth.put("nameGroupCount", 13);
        auth.put("tenantId", "1");
        auth.put("database", DccLegacyMaintenanceGate.DATABASE);
        auth.put("serverUuid", DccLegacyMaintenanceGate.UUID);
        auth.put("preserveHistoricalRows", true);
        auth.put("allowObjectRecovery", false);
        auth.put("allowAuditPolicyConfiguration", false);
        auth.put("allowSchemaMigration", false);
        Path authority = temp.resolve("authority.json");
        Files.writeString(authority, DccLegacyMaintenanceGate.JSON.writeValueAsString(auth));
        ObjectNode r = DccLegacyMaintenanceGate.JSON.createObjectNode();
        r.put("schemaVersion", 1);
        r.set("authorization", descriptor(authority));
        r.set("manifest", descriptor(manifest));
        for (String k : java.util.List.of("facts", "bytesReceipt", "bytesResults", "historicalDecision", "developmentPolicy", "policyFile", "schemaProof")) r.set(k, descriptor(data));
        r.put("scopeId", "sealed-root-scope");
        r.put("reason", "reviewed actual originals");
        r.put("requestId", "sealed-root-request");
        return r;
    }

    DccLegacyMaintenanceGate.Input read(ObjectNode input) throws Exception {
        Path request = temp.resolve("request.json");
        Files.writeString(request, DccLegacyMaintenanceGate.JSON.writeValueAsString(input));
        return gate().read(temp, request, DccLegacyMaintenanceGate.hash(Files.readAllBytes(request)));
    }

    @Test
    void developmentRequestWithoutQualityApprovalIsAccepted() throws Exception {
        var request = request();
        request.remove("developmentPolicy");
        var development = DccLegacyMaintenanceGate.JSON.createObjectNode();
        development.put("status", "DEVELOPMENT_TEST_ONLY");
        development.put("tenantId", "1");
        development.put("policyVersion", "2026-10-dcc-integration-01");
        development.put("policyHash", DccLegacyMaintenanceGate.POLICY_SHA);
        development.put("coverageReportHash", DccLegacyMaintenanceGate.COVERAGE_SHA);
        request.set("developmentPolicy", save("development-policy.json", development));
        assertEquals("sealed-root-scope", read(request).scopeId());
        verifyNoInteractions(source);
    }

    @Test
    void exactPermissionInputValidatesReadOnlyAndAllDescriptorHashes() throws Exception {
        var input = read(request());
        assertEquals("sealed-root-scope", input.scopeId());
        verifyNoInteractions(source);
    }

    ObjectNode withActualConditionalAnswer() throws Exception {
        var request = request();
        Path authorization = Path.of(request.get("authorization").get("path").asText());
        var facts = (ObjectNode) DccLegacyMaintenanceGate.JSON.readTree(Files.readAllBytes(authorization));
        facts.put("actualAnswer", "授权条件齐备后按方案执行（建议）");
        Files.writeString(authorization, DccLegacyMaintenanceGate.JSON.writeValueAsString(facts));
        request.set("authorization", descriptor(authorization));
        return request;
    }

    @Test
    void actualConditionalAuthorizationReadsExactScopeButDeclinedAnswerStillRejects() throws Exception {
        var request = withActualConditionalAnswer();
        assertEquals("sealed-root-scope", read(request).scopeId());
        Path authorization = Path.of(request.get("authorization").get("path").asText());
        var facts = (ObjectNode) DccLegacyMaintenanceGate.JSON.readTree(Files.readAllBytes(authorization));
        facts.put("actualAnswer", "尚未批准");
        Files.writeString(authorization, DccLegacyMaintenanceGate.JSON.writeValueAsString(facts));
        request.set("authorization", descriptor(authorization));
        assertThrows(IllegalArgumentException.class, () -> read(request));
        verifyNoInteractions(source);
    }

    @Test
    void actualConditionalAnswerUsesDevelopmentDeclarationWithoutQualityFiles() throws Exception {
        var request = withActualConditionalAnswer();
        request.set("developmentPolicy", save("development.json", developmentPolicy()));
        request.set("policyFile", descriptor(copyPolicy()));
        gate().requireDevelopmentPolicy(read(request));
        verifyNoInteractions(source);
    }

    @Test
    void declinedActualQuestionAndOldDdlAuthorityNeverAuthorizeScope() throws Exception {
        var r = request();
        Path authority = Path.of(r.get("authorization").get("path").asText());
        var a = (ObjectNode) DccLegacyMaintenanceGate.JSON.readTree(Files.readAllBytes(authority));
        a.put("actualAnswer", "尚未批准");
        a.put("userAnswerReceived", false);
        Files.writeString(authority, DccLegacyMaintenanceGate.JSON.writeValueAsString(a));
        r.set("authorization", descriptor(authority));
        assertThrows(IllegalArgumentException.class, () -> read(r));
        verifyNoInteractions(source);
    }

    @Test
    void forgedDescriptorHashRejectedEvenWhenUnderlyingManifestFactoryWouldReadOtherHashes() throws Exception {
        var r = request();
        ((ObjectNode) r.get("facts")).put("sha256", "0".repeat(64));
        assertThrows(IllegalArgumentException.class, () -> read(r));
        verifyNoInteractions(source);
    }

    @Test
    void pathEscapeUnknownFieldsAndOversizedRequestRejectBeforeDataSource() throws Exception {
        var r = request();
        r.put("actorId", "1");
        assertThrows(IllegalArgumentException.class, () -> read(r));
        var oversized = temp.resolve("oversize.json");
        Files.writeString(oversized, "x".repeat(65537));
        assertThrows(IllegalArgumentException.class, () -> gate().read(temp, oversized, "a".repeat(64)));
        verifyNoInteractions(source);
    }

    @Test
    void incompletePolicyFactsAnd35Of39CannotBeReady() throws Exception {
        var input = read(request());
        assertThrows(IllegalArgumentException.class, () -> gate().requireDevelopmentPolicy(input));
        assertThrows(Exception.class, () -> gate().verifiedScope(input));
        verifyNoInteractions(source);
    }

    @Test
    void showCreateNormalizationOnlyRemovesTableAutoIncrementMetadata() {
        assertEquals("CREATE TABLE t (`id` bigint AUTO_INCREMENT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", DccLegacyMaintenanceGate.normalizeShowCreate("CREATE TABLE t (`id` bigint AUTO_INCREMENT) ENGINE=InnoDB AUTO_INCREMENT=123 DEFAULT CHARSET=utf8mb4"));
    }

    @Test
    void formalThreeFieldDriverDescriptorsRequireExactNonnegativeIntegerBytes() throws Exception {
        Path data = temp.resolve("driver-artifact.txt");
        Files.writeString(data, "driver descriptor bytes");
        var d = descriptor(data);
        assertEquals(data, DccLegacyMaintenanceGate.descriptor(d, temp).path());
        var two = d.deepCopy();
        two.remove("bytes");
        assertEquals(data, DccLegacyMaintenanceGate.descriptor(two, temp).path());
        for (String invalid : List.of("wrong", "negative", "fractional", "bool", "extra")) {
            var bad = d.deepCopy();
            switch(invalid) {
                case "wrong" ->
                    bad.put("bytes", Files.size(data) + 1);
                case "negative" ->
                    bad.put("bytes", -1);
                case "fractional" ->
                    bad.put("bytes", 1.5);
                case "bool" ->
                    bad.put("bytes", true);
                case "extra" ->
                    bad.put("ignored", true);
            }
            assertThrows(IllegalArgumentException.class, () -> DccLegacyMaintenanceGate.descriptor(bad, temp));
        }
        verifyNoInteractions(source);
    }

    @Test
    void fullSourceSnapshotOverNineMiBLoadsExactlyButAboveSixteenMiBRejects() throws Exception {
        Path snapshot = temp.resolve("full-source-snapshot.json");
        byte[] bytes = new byte[9_423_066];
        java.util.Arrays.fill(bytes, (byte) ' ');
        Files.write(snapshot, bytes);
        var d = DccLegacyMaintenanceGate.descriptor(descriptor(snapshot), temp);
        assertArrayEquals(bytes, DccLegacyMaintenanceGate.load(d, temp));
        Files.write(snapshot, new byte[16 * 1024 * 1024 + 1]);
        assertThrows(IllegalArgumentException.class, () -> DccLegacyMaintenanceGate.descriptor(descriptor(snapshot), temp));
        verifyNoInteractions(source);
    }

    ObjectNode save(String name, ObjectNode body) throws Exception {
        Path path = temp.resolve(name);
        Files.writeString(path, DccLegacyMaintenanceGate.JSON.writeValueAsString(body));
        return descriptor(path);
    }

    ObjectNode developmentPolicy() {
        var declaration = DccLegacyMaintenanceGate.JSON.createObjectNode();
        declaration.put("status", "DEVELOPMENT_TEST_ONLY");
        declaration.put("tenantId", "1");
        declaration.put("policyVersion", "2026-10-dcc-integration-01");
        declaration.put("policyHash", DccLegacyMaintenanceGate.POLICY_SHA);
        declaration.put("coverageReportHash", DccLegacyMaintenanceGate.COVERAGE_SHA);
        return declaration;
    }

    Path copyPolicy() throws Exception {
        Path repository = Path.of("").toAbsolutePath();
        while (repository != null && !Files.exists(repository.resolve("IntRuoyiBackend/config/gxp-audit-policy.yaml")))
            repository = repository.getParent();
        assertNotNull(repository);
        Path policy = temp.resolve("policy.yaml");
        Files.copy(repository.resolve("IntRuoyiBackend/config/gxp-audit-policy.yaml"), policy, StandardCopyOption.REPLACE_EXISTING);
        assertEquals(DccLegacyMaintenanceGate.POLICY_SHA, DccLegacyMaintenanceGate.hash(Files.readAllBytes(policy)));
        return policy;
    }

    DccLegacyMaintenanceGate.Input withDevelopmentPolicy(ObjectNode declaration) throws Exception {
        var request = request();
        request.set("developmentPolicy", save("development.json", declaration));
        request.set("policyFile", descriptor(copyPolicy()));
        return read(request);
    }

    @Test
    void exactDevelopmentPolicyNeedsNoApprovalPersonTimeOrRegistry() throws Exception {
        var input = withDevelopmentPolicy(developmentPolicy());
        gate().requireDevelopmentPolicy(input);
        assertFalse(Files.exists(temp.resolve("signed-fixture.txt")));
        verifyNoInteractions(source);
    }

    @Test
    void developmentDeclarationCannotChangeVersionHashTenantStatusOrInventApproval() throws Exception {
        for (String field : List.of("status", "tenantId", "policyVersion", "policyHash", "coverageReportHash")) {
            var changed = developmentPolicy();
            changed.put(field, "different");
            var error = assertThrows(IllegalArgumentException.class,
                    () -> gate().requireDevelopmentPolicy(withDevelopmentPolicy(changed)));
            assertEquals("DCC_MAINTENANCE_EXACT_DEVELOPMENT_POLICY_REQUIRED", error.getMessage());
        }
        var approval = developmentPolicy();
        approval.put("approvedBy", "admin");
        assertThrows(IllegalArgumentException.class,
                () -> gate().requireDevelopmentPolicy(withDevelopmentPolicy(approval)));
        verifyNoInteractions(source);
    }

    DccLegacyMaintenanceGate actualDevelopmentReadPorts(String url) throws Exception {
        var connection = mock(java.sql.Connection.class);
        var metadata = mock(java.sql.DatabaseMetaData.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getURL()).thenReturn(url);
        var target = gate();
        var reader = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        when(reader.queryForMap(anyString())).thenReturn(Map.of("database_name", DccLegacyMaintenanceGate.DATABASE,
                "server_uuid", DccLegacyMaintenanceGate.UUID, "time_zone", "+08:00"));
        org.springframework.test.util.ReflectionTestUtils.setField(target, "jdbc", reader);
        return target;
    }

    @Test
    void developmentEnvironmentRequiresActualLoopbackMetadataAndSourceUuid() throws Exception {
        var target = actualDevelopmentReadPorts("jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false");
        target.requireDevelopmentEnvironment();
        for (String url : List.of("jdbc:mysql://remote:23306/ruoyi-vue-pro", "jdbc:mysql://localhost:3306/ruoyi-vue-pro",
                "jdbc:mysql://localhost:23306/foreign", "jdbc:h2:mem:test")) {
            target = actualDevelopmentReadPorts(url);
            var rejected = target;
            var error = assertThrows(IllegalArgumentException.class, rejected::requireDevelopmentEnvironment);
            assertEquals("DCC_MAINTENANCE_DEVELOPMENT_LOCAL_DATASOURCE_REQUIRED", error.getMessage());
        }
        target = actualDevelopmentReadPorts("jdbc:mysql://localhost:23306/ruoyi-vue-pro");
        when(target.jdbc.queryForMap(anyString())).thenReturn(Map.of("database_name", DccLegacyMaintenanceGate.DATABASE,
                "server_uuid", "foreign", "time_zone", "+08:00"));
        assertThrows(IllegalArgumentException.class, target::requireDevelopmentEnvironment);
    }

    @Test
    void nonDevelopmentAndProductionProfilesRejectBeforeOpeningDataSource() throws Exception {
        for (String profile : List.of("local", "prod", "production", "backup", "test", "unit-test")) {
            var target = gate();
            if (profile.equals("local")) environment.setActiveProfiles("local");
            else environment.setActiveProfiles("local-maintenance", profile);
            var error = assertThrows(IllegalArgumentException.class, target::requireDevelopmentEnvironment);
            assertEquals("DCC_MAINTENANCE_DEVELOPMENT_PROFILE_REQUIRED", error.getMessage());
        }
        verifyNoInteractions(source);
    }

    @Test
    void currentDeveloperRuntimeRequiresAll26PayloadsWithoutReadingQualityRegistry() throws Exception {
        var input = withDevelopmentPolicy(developmentPolicy());
        var target = spy(actualDevelopmentReadPorts("jdbc:mysql://localhost:23306/ruoyi-vue-pro"));
        // Complete schema contract has separate actual Gate tests; here isolate its read-only port.
        doNothing().when(target).verifyCurrentSchemaProof(input);
        when(target.jdbc.queryForObject(contains("information_schema.TABLES"), eq(Long.class))).thenReturn(3L);
        when(target.jdbc.queryForObject(contains("infra_release_migration"), eq(Long.class))).thenReturn(1L);
        Map<?, ?> policy = new org.yaml.snakeyaml.Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(
                new org.yaml.snakeyaml.LoaderOptions())).load(Files.readString(input.policy().path()));
        String[] fields = { "operationId", "sourceType", "sourceLocator", "domain", "subjectType", "actionType", "reasonPolicy",
                "signaturePolicy", "statePolicy", "retentionClass", "testIds", "owner", "applicability" };
        String[] columns = { "operation_id", "source_type", "source_locator", "domain", "subject_type", "action_type", "reason_policy",
                "signature_policy", "state_policy", "retention_class", "test_ids", "owner", "applicability" };
        var runtime = new java.util.HashMap<String, Map<String, Object>>();
        for (Object item : (List<?>) policy.get("operations")) {
            var operation = (Map<?, ?>) item;
            String id = operation.get("operationId").toString();
            if (!id.startsWith("dcc.") || id.equals("dcc.controlled-file.publish")) continue;
            var row = new java.util.HashMap<String, Object>();
            row.put("policy_version", DccLegacyMaintenanceGate.POLICY_VERSION);
            row.put("active", true);
            row.put("deleted", false);
            for (int i = 0; i < fields.length; i++) {
                Object value = operation.get(fields[i]);
                if (value instanceof List<?> values) value = "[" + String.join(", ", values.stream().map(String::valueOf).toList()) + "]";
                row.put(columns[i], value);
            }
            runtime.put(id, row);
            when(target.jdbc.queryForList("SELECT * FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id=?", id))
                    .thenReturn(List.of(row));
        }
        assertEquals(26, runtime.size());
        target.currentDevelopmentPolicy(input);
        verify(target.jdbc, times(26)).queryForList(contains("gxp_audit_policy_operation"), any(Object.class));
        verify(target.jdbc, never()).queryForList(contains("gxp_audit_policy_version"), any(Object.class));
        runtime.get(DccLegacySourceNameRegistrationService.OPERATION).put("reason_policy", "wrong");
        assertThrows(IllegalArgumentException.class, () -> target.currentDevelopmentPolicy(input));
    }

    ObjectNode schemaProof() throws Exception {
        var p = DccLegacyMaintenanceGate.JSON.createObjectNode();
        p.put("status", "ACTUAL_SOURCE_SIDECAR_SCHEMA_FIRST_REPEAT_VERIFIED");
        p.put("database", DccLegacyMaintenanceGate.DATABASE);
        p.put("serverUuid", DccLegacyMaintenanceGate.UUID);
        p.put("migrationId", "20261003_dcc_legacy_source_name_occupancy");
        p.put("migrationSqlSha256", "621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a");
        Path repository = Path.of("").toAbsolutePath();
        while (repository != null && !Files.exists(repository.resolve("doc/tasks/20261002-dcc-detail-integration/g28-schema-contract.json"))) repository = repository.getParent();
        assertNotNull(repository, "formal frozen contract required for isolated protocol test");
        Path contract = temp.resolve("contract.json");
        Files.copy(repository.resolve("doc/tasks/20261002-dcc-detail-integration/g28-schema-contract.json"), contract, StandardCopyOption.REPLACE_EXISTING);
        p.set("validatedContract", descriptor(contract));
        var journal = DccLegacyMaintenanceGate.JSON.createObjectNode();
        journal.put("status", "G28_SOURCE_ONE_MIGRATION_FIRST_REPEAT_PASS_NOT_APPLICATION_READINESS");
        journal.put("database", DccLegacyMaintenanceGate.DATABASE);
        journal.put("databaseWriteAttempted", true);
        journal.put("migrationId", p.get("migrationId").asText());
        journal.put("sqlSha256", p.get("migrationSqlSha256").asText());
        journal.put("operationId", "isolated-protocol-fixture");
        journal.set("originalBefore", save("before.json", DccLegacyMaintenanceGate.JSON.createObjectNode()));
        var steps = journal.putArray("steps");
        for (String phase : List.of("first", "repeat")) {
            var step = steps.addObject();
            step.put("phase", phase);
            step.put("phaseStatus", "POSTFLIGHT_AND_ORIGINAL_ROWS_PASS");
            for (String field : List.of("material", "postflight", "originalAfter", "newLedger")) step.set(field, save(phase + "-" + field + ".json", DccLegacyMaintenanceGate.JSON.createObjectNode()));
            step.put("materialSha256", step.get("material").get("sha256").asText());
            var execution = step.putObject("execution");
            execution.put("exitCode", 0);
            execution.put("database", DccLegacyMaintenanceGate.DATABASE);
            execution.put("sqlSha256", step.get("materialSha256").asText());
            Path stdout = temp.resolve(phase + "-stdout.jsonl"), stderr = temp.resolve(phase + "-stderr.txt");
            Files.writeString(stdout, "isolated protocol fixture; not an actual DDL receipt");
            Files.writeString(stderr, "");
            execution.set("stdout", descriptor(stdout));
            execution.set("stderr", descriptor(stderr));
        }
        p.set("completedSourceJournal", save("source-journal.json", journal));
        var shapes = p.putObject("showCreateTableSha256");
        for (String table : List.of("dcc_legacy_source_name_scope", "dcc_legacy_source_name_evidence", "dcc_source_name_reservation")) shapes.put(table, "a".repeat(64));
        var validation = DccLegacyMaintenanceGate.JSON.createObjectNode();
        validation.put("status", "ROOT_REPLAYED_CURRENT_SCHEMA_DRIVER_PASS");
        validation.put("database", DccLegacyMaintenanceGate.DATABASE);
        validation.put("serverUuid", DccLegacyMaintenanceGate.UUID);
        validation.put("sourceJournalSha256", p.get("completedSourceJournal").get("sha256").asText());
        validation.put("schemaContractSha256", p.get("validatedContract").get("sha256").asText());
        validation.set("showCreateTableSha256", shapes.deepCopy());
        validation.put("validatorSourceSha256", "667a81edf468e1212afef8a01ae5cb996bd62737cfba7db6138e30259e0db2d4");
        p.set("validatorReceipt", save("schema-validator.json", validation));
        return p;
    }

    DccLegacyMaintenanceGate.Input withSchema(ObjectNode proof) throws Exception {
        var r = request();
        r.set("schemaProof", save("schema-proof.json", proof));
        return read(r);
    }

    @Test
    void arbitrarySelfConsistentContractHashRejectsBeforeAnySchemaQuery() throws Exception {
        var p = schemaProof();
        Path wrong = temp.resolve("wrong-contract.json");
        Files.writeString(wrong, "{}");
        p.set("validatedContract", descriptor(wrong));
        var error = assertThrows(IllegalArgumentException.class, () -> gate().verifyCurrentSchemaProof(withSchema(p)));
        assertEquals("DCC_MAINTENANCE_FIXED_SCHEMA_CONTRACT_REQUIRED", error.getMessage());
        verifyNoInteractions(source);
    }

    @Test
    void sourceJournalEmptyPhaseObjectsCannotBeFirstRepeatPass() throws Exception {
        var p = schemaProof();
        Path path = Path.of(p.get("completedSourceJournal").get("path").asText());
        var journal = (ObjectNode) DccLegacyMaintenanceGate.JSON.readTree(Files.readAllBytes(path));
        journal.putArray("steps").addObject();
        journal.withArray("steps").addObject();
        p.set("completedSourceJournal", save("empty-phase-journal.json", journal));
        var error = assertThrows(IllegalArgumentException.class, () -> gate().verifyCurrentSchemaProof(withSchema(p)));
        assertEquals("DCC_MAINTENANCE_ACTUAL_SOURCE_SCHEMA_PHASE_REQUIRED", error.getMessage());
        verifyNoInteractions(source);
    }

    @Test
    void foreignDatabaseOrForgedValidatorDigestCannotAuthorizeCurrentShape() throws Exception {
        var p = schemaProof();
        p.put("database", "foreign");
        var foreign = p;
        assertThrows(IllegalArgumentException.class, () -> gate().verifyCurrentSchemaProof(withSchema(foreign)));
        p = schemaProof();
        Path path = Path.of(p.get("validatorReceipt").get("path").asText());
        var validation = (ObjectNode) DccLegacyMaintenanceGate.JSON.readTree(Files.readAllBytes(path));
        validation.put("sourceJournalSha256", "0".repeat(64));
        p.set("validatorReceipt", save("wrong-validator.json", validation));
        var wrong = p;
        assertThrows(IllegalArgumentException.class, () -> gate().verifyCurrentSchemaProof(withSchema(wrong)));
        verifyNoInteractions(source);
    }

    @Test
    void oldReceiptWithArbitraryContractAndEmptyPhaseObjectsNeverReachesLiveSchemaQueries() throws Exception {
        var p = schemaProof();
        p.remove(List.of("validatedContract", "validatorReceipt"));
        p.put("validatedContractSha256", "a".repeat(64));
        var journal = DccLegacyMaintenanceGate.JSON.createObjectNode();
        journal.put("status", "G28_SOURCE_ONE_MIGRATION_FIRST_REPEAT_PASS_NOT_APPLICATION_READINESS");
        journal.put("database", DccLegacyMaintenanceGate.DATABASE);
        journal.put("databaseWriteAttempted", true);
        journal.putArray("steps").addObject();
        journal.withArray("steps").addObject();
        p.set("completedSourceJournal", save("old-empty-journal.json", journal));
        var reader = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        String wrongDdl = "CREATE TABLE wrong_shape (id bigint) ENGINE=InnoDB";
        for (String table : List.of("dcc_legacy_source_name_scope", "dcc_legacy_source_name_evidence", "dcc_source_name_reservation")) {
            ((ObjectNode) p.get("showCreateTableSha256")).put(table, DccLegacyMaintenanceGate.hash(wrongDdl.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            when(reader.queryForList("SHOW CREATE TABLE `" + table + "`")).thenReturn(List.of(Map.of("Create Table", wrongDdl)));
        }
        var target = gate();
        org.springframework.test.util.ReflectionTestUtils.setField(target, "jdbc", reader);
        assertThrows(IllegalArgumentException.class, () -> target.verifyCurrentSchemaProof(withSchema(p)));
        verifyNoInteractions(reader, source);
    }

    @Test
    void supersededValidatorProofRejectsBeforeReadingAnyCurrentSchema() throws Exception {
        var proof = schemaProof();
        var validation = (ObjectNode) DccLegacyMaintenanceGate.JSON.readTree(
                Files.readAllBytes(Path.of(proof.get("validatorReceipt").get("path").asText())));
        validation.put("validatorSourceSha256", "1560ee9ad223b4f65b6393428b84e949422fcc433ce92242285dd762d71c2f56");
        proof.set("validatorReceipt", save("superseded-validator.json", validation));
        var reader = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        var target = gate();
        org.springframework.test.util.ReflectionTestUtils.setField(target, "jdbc", reader);
        var error = assertThrows(IllegalArgumentException.class,
                () -> target.verifyCurrentSchemaProof(withSchema(proof)));
        assertEquals("DCC_MAINTENANCE_ROOT_ACTUAL_SCHEMA_VALIDATION_REQUIRED", error.getMessage());
        verifyNoInteractions(reader, source);
    }

    @Test
    void reviewedCurrentCompleteThreeFieldDriverProtocolLoadsBeforeExactCurrentShapes() throws Exception {
        var p = schemaProof();
        var reader = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        Path repository = Path.of("").toAbsolutePath();
        while (repository != null && !Files.exists(repository.resolve("IntRuoyiBackend/sql/mysql/20261003_dcc_legacy_source_name_occupancy.sql"))) repository = repository.getParent();
        assertNotNull(repository);
        String sql = Files.readString(repository.resolve("IntRuoyiBackend/sql/mysql/20261003_dcc_legacy_source_name_occupancy.sql"));
        for (String table : List.of("dcc_legacy_source_name_scope", "dcc_legacy_source_name_evidence", "dcc_source_name_reservation")) {
            var matcher = java.util.regex.Pattern.compile("(?s)CREATE TABLE IF NOT EXISTS " + table + " \\(.*?\\) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;").matcher(sql);
            assertTrue(matcher.find());
            String ddl = matcher.group();
            ((ObjectNode) p.get("showCreateTableSha256")).put(table, DccLegacyMaintenanceGate.hash(ddl.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            when(reader.queryForList("SHOW CREATE TABLE `" + table + "`")).thenReturn(List.of(Map.of("Create Table", ddl)));
        }
        var receipt = (ObjectNode) DccLegacyMaintenanceGate.JSON.readTree(Files.readAllBytes(Path.of(p.get("validatorReceipt").get("path").asText())));
        receipt.set("showCreateTableSha256", p.get("showCreateTableSha256").deepCopy());
        p.set("validatorReceipt", save("matched-schema-validator.json", receipt));
        var target = gate();
        org.springframework.test.util.ReflectionTestUtils.setField(target, "jdbc", reader);
        target.verifyCurrentSchemaProof(withSchema(p));
        for (String table : List.of("dcc_legacy_source_name_scope", "dcc_legacy_source_name_evidence", "dcc_source_name_reservation")) verify(reader).queryForList("SHOW CREATE TABLE `" + table + "`");
        verifyNoInteractions(source);
    }
}
