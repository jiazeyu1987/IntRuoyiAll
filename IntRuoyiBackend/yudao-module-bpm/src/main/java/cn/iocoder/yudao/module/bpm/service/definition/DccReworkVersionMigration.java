package cn.iocoder.yudao.module.bpm.service.definition;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.definition.BpmProcessDefinitionInfoMapper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Publishes a new engine version; never replaces resources used by old instances. */
@Component
@RequiredArgsConstructor
public class DccReworkVersionMigration implements ApplicationRunner, HealthIndicator {
    static final String KEY = "dcc-controlled-file-approval";
    private final RepositoryService repositoryService;
    private final BpmProcessDefinitionInfoMapper definitionMapper;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private volatile boolean ready;

    @Override
    public void run(ApplicationArguments args) {
        var sources = jdbc.queryForList("SELECT tenant_id FROM bpm_dcc_rework_release ORDER BY tenant_id");
        check(sources.size() == 2, "Both registered DCC release origins are required");
        for (Map<String, Object> row : sources) {
            long tenant = ((Number) row.get("tenant_id")).longValue();
            check(tenant == 1L || tenant == 122L, "Unapproved DCC release tenant");
            TenantUtils.execute(tenant, () -> transaction.executeWithoutResult(status -> migrate(tenant)));
        }
        ready = true;
    }

    @Override
    public Health health() {
        return ready ? Health.up().build() : Health.down().withDetail("migration", "dcc-rework-version-pending").build();
    }

    void migrate(long tenant) {
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM bpm_dcc_rework_release WHERE tenant_id=? FOR UPDATE", tenant);
        String baseId = (String) row.get("source_definition_id");
        ProcessDefinition base = repositoryService.getProcessDefinition(baseId);
        check(base != null && KEY.equals(base.getKey()) && Long.toString(tenant).equals(base.getTenantId()), "Source definition identity differs");
        byte[] original;
        try (var stream = repositoryService.getResourceAsStream(base.getDeploymentId(), base.getResourceName())) {
            original = stream.readAllBytes();
        } catch (Exception error) {
            throw new IllegalStateException("Cannot read source definition", error);
        }
        check(digest(original).equals(row.get("source_sha256")), "Source definition bytes changed");
        if ("APPLIED".equals(row.get("status"))) {
            ProcessDefinition deployed = repositoryService.getProcessDefinition((String) row.get("target_definition_id"));
            check(deployed != null && KEY.equals(deployed.getKey()) && base.getTenantId().equals(deployed.getTenantId()), "Applied definition is missing");
            return;
        }
        check("PENDING".equals(row.get("status")), "Unresolved DCC release state");
        ProcessDefinition latest = repositoryService.createProcessDefinitionQuery().processDefinitionKey(KEY)
                .processDefinitionTenantId(base.getTenantId()).latestVersion().singleResult();
        check(latest != null && baseId.equals(latest.getId()), "Latest source definition changed");
        BpmProcessDefinitionInfoDO info = definitionMapper.selectByProcessDefinitionId(baseId);
        check(info != null, "Source definition configuration missing");
        byte[] updated = addRework(original);
        String modelId = (String) row.get("source_model_id");
        Model model = modelId == null ? null : repositoryService.getModel(modelId);
        if (modelId != null) {
            check(model != null && base.getTenantId().equals(model.getTenantId()) && KEY.equals(model.getKey())
                    && base.getDeploymentId().equals(model.getDeploymentId()), "Registered model identity changed");
            check(digest(repositoryService.getModelEditorSource(modelId)).equals(row.get("source_sha256")), "Unpublished model edits require review");
        }
        var deployment = repositoryService.createDeployment().key(KEY).name(base.getName()).category(base.getCategory())
                .tenantId(base.getTenantId()).addBytes(base.getResourceName(), updated).disableSchemaValidation().deploy();
        ProcessDefinition deployed = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        check(deployed != null && KEY.equals(deployed.getKey()) && deployed.getVersion() > base.getVersion(), "New process version was not created");
        repositoryService.setProcessDefinitionCategory(deployed.getId(), base.getCategory());
        BpmProcessDefinitionInfoDO copy = BeanUtils.toBean(info, BpmProcessDefinitionInfoDO.class);
        copy.setId(null);
        copy.setProcessDefinitionId(deployed.getId());
        definitionMapper.insert(copy);
        if (model != null) {
            repositoryService.addModelEditorSource(modelId, updated);
            model.setDeploymentId(deployment.getId());
            repositoryService.saveModel(model);
        }
        // Existing executions retain their definition; only new starts use the new version.
        repositoryService.suspendProcessDefinitionById(baseId, false, null);
        check(jdbc.update("UPDATE bpm_dcc_rework_release SET status='APPLIED',target_definition_id=?,finished_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND status='PENDING'",
                deployed.getId(), tenant) == 1, "DCC release receipt changed");
    }

    static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    static byte[] addRework(byte[] original) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(original));
            String bpmn = "http://www.omg.org/spec/BPMN/20100524/MODEL";
            String flowable = "http://flowable.org/bpmn";
            var processes = doc.getElementsByTagNameNS(bpmn, "process");
            check(processes.getLength() == 1, "Exactly one process is required");
            Element process = (Element) processes.item(0);
            check(KEY.equals(process.getAttribute("id")), "Unexpected process key");
            Set<String> tasks = new HashSet<>();
            var nodes = process.getElementsByTagNameNS(bpmn, "userTask");
            for (int index = 0; index < nodes.getLength(); index++) tasks.add(((Element) nodes.item(index)).getAttribute("id"));
            check(nodes.getLength() == 4 && tasks.equals(Set.of("DOC_CONTROL_REVIEW", "MATRIX_REVIEW", "MATRIX_APPROVAL", "DOC_CONTROL_APPROVAL")), "Unrecognized current task graph");
            var all = process.getElementsByTagName("*");
            for (int index = 0; index < all.getLength(); index++) {
                String id = ((Element) all.item(index)).getAttribute("id");
                check(!Set.of("APPLICANT_REWORK", "flow_applicant_rework_doc_control_review").contains(id), "New graph identity already exists");
            }
            doc.getDocumentElement().setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:flowable", flowable);
            Element task = doc.createElementNS(bpmn, "userTask");
            task.setAttribute("id", "APPLICANT_REWORK");
            task.setAttribute("name", "申请人修改");
            Element extension = doc.createElementNS(bpmn, "extensionElements");
            for (var property : Map.of("candidateStrategy", "36", "approveMethod", "1").entrySet()) {
                Element item = doc.createElementNS(flowable, "flowable:"+property.getKey());
                item.setTextContent(property.getValue());
                extension.appendChild(item);
            }
            task.appendChild(extension);
            process.appendChild(task);
            Element flow = doc.createElementNS(bpmn, "sequenceFlow");
            flow.setAttribute("id", "flow_applicant_rework_doc_control_review");
            flow.setAttribute("sourceRef", "APPLICANT_REWORK");
            flow.setAttribute("targetRef", "DOC_CONTROL_REVIEW");
            process.appendChild(flow);
            var transformers = TransformerFactory.newInstance();
            transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            transformers.newTransformer().transform(new DOMSource(doc), new StreamResult(output));
            return output.toByteArray();
        } catch (Exception error) {
            throw new IllegalStateException("DCC version graph requires review", error);
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
