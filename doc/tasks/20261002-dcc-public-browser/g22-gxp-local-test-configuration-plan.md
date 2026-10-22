# G22 本机 tenant1 DCC 审计配置准备方案

状态：ready_for_closeout，供 Root 审查准备包。共享任务仍为 in_progress。没有连接数据库、执行 SQL、启停服务或运行 E2E；全部写授权和实际质量批准输入均未提供。

## 真实缺口与源码依据

Root 只读运行证据 `doc/tasks/20261001-dcc-integration-unblock/g21-gxp-active-policy.log` 显示 tenant1 仅有 active `dcc.controlled-file.publish`，版本 `2026-09-approved-01`。候选文件有 26 个 DCC operation，余下 25 个缺登记。`g21-audit-approval-readiness.log` 仅有旧版本批准行，没有新版批准登记。两个运行证据来自 main 树，不表示子 Agent 重新连接过数据库。

正式实现依据：

| 仓库相对路径与锚点 | 本方案采用的事实 |
| --- | --- |
| `IntRuoyiBackend/script/gxp_audit_coverage_gate.py:43`，`parse_policy` | 使用正式 parser 读取所有字段，不自建平行 YAML 解释规则 |
| 同文件 `source_locator_exists:94` / `canonical_report:113` | 逐项解析真实源定位，生成现有规则的规范源映射报告 |
| `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/gxpaudit/GxpAuditServiceImpl.java:42`，`append` | 缺 active operation 直接抛 GXP_AUDIT_POLICY_NOT_FOUND，真实业务写不会以空审计成功 |
| `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/mysql/gxpaudit/GxpAuditPolicyOperationMapper.java:11`，`selectActive` | 正式 tenant+operation+active+GXP 策略，不能用前端 fallback 补齐 |
| `IntRuoyiBackend/sql/mysql/20260908_gxp_audit_trail_core.sql:60` / `:74` | 正式 version/operation 表字段、唯一键和 InnoDB 结构；本包不重跑 core SQL |
| `IntRuoyiBackend/sql/mysql/ruoyi-vue-pro.sql`，`CREATE TABLE system_users` | 启用用户的正式 id/tenant_id/status/deleted 结构；旧 schema snapshot 未覆盖该表，执行前必须只读复核 |
| `docs/csv-validation/12-qms-training-approval.md`，职责分工 | QA/合规负责人批准策略；“不能由开发自批准” |
| `docs/system/gxp-audit-trail-config-security-deployment.md`，Configuration/Permissions | 配置变更需系统审计，策略生效需质量电子签名，policy-manage 不能自批准 |

只读检查 `IntRuoyiFronted/src` 和 system Controller，未找到 GxP audit policy-operation/version 配置或质量批准的正式页面/接口。`IntRuoyiFronted/src/views/signature-governance/components/PolicyGovernanceListPane.vue:72` 调用 `getCurrentSignatureGovernancePolicy`，属于电子签名策略状态读取，不能登记本表或替代新版质量批准。本包不新增质量业务节点，也不伪造签名或系统配置审计事件。

## 固定候选与批准输入

| 项目 | 固定内容 |
| --- | --- |
| 策略版本 | `2026-10-dcc-integration-01` |
| 文件 | `IntRuoyiBackend/config/gxp-audit-policy.yaml` |
| 原始字节 SHA-256 | `776905347c7726983db317eda110908f6762aa1351a8564e1e130da19b0d59df` |
| 规范源映射报告 | `g22-formal-policy-source-coverage.txt` |
| 报告 SHA-256 | `319fc04a677dcb8670b420c9791bf5e4584d6708e0e765e58dbb637d9ab6b558` |
| 报告范围 | 正式 parser 的全部 33 项登记 / 11 个已发现 annotation；其中 26 项 DCC |
| 候选文件中的 approvalReference | `PENDING-REVIEW-20261001`，未修改、不能作真实批准依据 |

该 coverage hash 仅证明固定候选的正式登记/源定位映射，不能表示对应业务测试已执行、全部写入口已覆盖或 QA 已批准。所有 operation 的 `testIds` 按正式 parser 原始列表字符串存入 varchar；它们是策略测试编号，不是本批测试 PASS 记录。

`g22-quality-approval-input.template.json` 是空事实模板。Root 向用户确认后，应在另一个副本输入实际质量批准人姓名、QA/质量角色依据、tenant1 启用系统账号的精确 ID、实际批准依据、质量签名证据引用以及批准时间。时间明确使用 Asia/Shanghai；数据库 DATETIME 由 Root 在已核实的会话时区下登记。禁止填写密码/令牌，禁止从 admin、账号1或开发者身份推定 QA。旧登记 `CODEX-IMPLEMENTATION-20260908` 属于旧文件，本包明确拒绝把它作为新文件批准依据。

源 policy 原始字节、正式 parser、coverage report 或待批准范围变化时，旧包的批准不能继续沿用；先重新生成、Review，并重新确认实际批准事实。

## 三个独立授权范围

| 范围 | 本机永久写入上限 | 当前状态 |
| --- | --- | --- |
| Root 已提问的 19 个正式 SQL migration | 由 Root 原迁移计划确定 | 独立 pending；本包不执行或默认包含 |
| 新版质量批准登记 | tenant1 `gxp_audit_policy_version` 最多新增 1 行 | 单独 pending；需真实批准与单独写授权 |
| 25 个 DCC 审计动作配置 | tenant1 `gxp_audit_policy_operation` 最多新增 25 行 | 单独 pending；需固定版本真实批准登记与单独写授权 |

批准登记模板只记录已经真实发生、独立确认的质量批准事实。它不进行审批，不赋予 QA 资格，也不能把一次本机测试配置授权宣称为生产 CSV 批准或正式上线就绪。正式质量签名/配置审计机制缺口如未通过真实依据补齐，应保持明确限制；不得补写假业务事件。

## 25 项精确白名单

完整 14 个登记字段逐行保存在 `g22-gxp-25-operation-impact.json` 和 operation SQL 中，包括 `policyVersion`、`operationId`、`sourceType`、完整 `sourceLocator`、`domain`、`subjectType`、`actionType`、`reasonPolicy`、`signaturePolicy`、`statePolicy`、`retentionClass`、`testIds`、`owner`、`applicability`。以下仅便于审查定位，不代替完整 payload。

| operationId | 源方法简写 | 正式 testIds |
| --- | --- | --- |
| dcc.controlled-file.activate | DccWorkflowFileStateAudit#recordActivation | [CC2-A-LEDGER-02, CC2-A-LEDGER-03] |
| dcc.controlled-file.auto-obsolete | DccWorkflowFileStateAudit#recordAutomaticObsolete | [CC2-A-LEDGER-02, CC2-A-LEDGER-03] |
| dcc.controlled-file.control | DccWorkflowFileStateAudit#recordControl | [CC2-A-LEDGER-01, CC2-A-LEDGER-03, CC2-A-LEDGER-06] |
| dcc.controlled-file.obsolete | DccWorkflowFileStateAudit#recordApprovedObsolete | [CC2-A-LEDGER-04, CC2-A-LEDGER-05] |
| dcc.folder-template.delete | DccFolderTemplateService#delete | [B-08] |
| dcc.folder-template.save | DccFolderTemplateService#save | [B-08] |
| dcc.project-attributes.configure | DccProjectAttributesController#configure | [B-04] |
| dcc.project-file-placement.bind | DccProjectFilePlacementService#bind | [B-R03, B-PLACEMENT] |
| dcc.project-folder.create | DccProjectFolderMaintenanceService#save | [B-R03, B-FOLDER-CREATE] |
| dcc.project-folder.delete | DccProjectFolderMaintenanceService#delete | [D2-02, CC2-B-FOLDER-DELETE] |
| dcc.project-folder.update | DccProjectFolderMaintenanceService#save | [B-R03, B-FOLDER-UPDATE] |
| dcc.project-product.approve | DccProjectProductCreateStateService#markApprovalDecision | [B-REV-01, D2-03] |
| dcc.project-product.complete | DccProjectProductCreateWriteService#writeApprovedRequest | [B-REV-01, D2-03] |
| dcc.project-product.create | DccProjectProductCreateServiceImpl#createRequest | [B-REV-01, D2-03] |
| dcc.project-product.resubmit | DccProjectProductCreateServiceImpl#resubmitRejectedRequest | [B-01] |
| dcc.project-product.retry | DccProjectProductCreateStateService#markRetryWriting | [B-REV-01, D2-03] |
| dcc.project-product.review | DccProjectProductCreateServiceImpl#review | [B-REV-01, D2-03] |
| dcc.project-product.reviewer-config | DccProjectReviewerConfigurationService#save | [P07] |
| dcc.project-product.write-failed | DccProjectProductCreateFailureService#markWriteFailed | [B-REV-01, D2-03] |
| dcc.project-reference.cancel | DccProjectReferenceService#cancel | [D-10] |
| dcc.project-reference.create | DccProjectReferenceService#create | [D-08] |
| dcc.relation.arrange | DccRelationRemediationService#saveArrangements | [D-06] |
| dcc.relation.controlled | DccRelationRemediationService#recordControlled | [D-07] |
| dcc.relation.notification.retry | DccRelationNotificationRecoveryService#requestRetry | [D-07] |
| dcc.relation.replace | DccControlledFileRelatedFileServiceImpl#replaceCurrentRelations | [D-01] |

这些候选统一保留正式 `REQUIRED_CATEGORY_AND_TEXT`、`NOT_REQUIRED`、`GXP_CONTROLLED_DOCUMENT`、`DCC/GXP`；状态策略按各 operation 原值保留。`signaturePolicy=NOT_REQUIRED` 仅是该审计登记原始字段，不删除或替代业务服务已经独立要求的电子签名，也不等于本次策略配置无需质量批准。

## SQL 行为与业务影响

`g22-gxp-25-operation-config.review.sql` 的永久 DML 只有一个 guarded INSERT，目标只限上述 tenant1 25 项。已存在完全相同 payload 的行跳过；全部完全重复时不执行 INSERT，结果 0 insert / 0 update / 0 delete。部分精确配置仅插入缺项。相同 operation 的其它版本、停用/删除标记、大小写或任意字段差异、重复行都整批失败，禁止覆盖。旧 publish 必须仍为原 active/GXP/旧版本及源定位，绝不插入新版 publish 或改写它。

`g22-gxp-quality-version-registration.review.sql` 只允许一个 guarded version INSERT；实际批准人必须为 tenant1 正式启用账号，且质量角色由外部实际批准事实单独确认。精确版本/hash/person/time/reference/coverage 重复时 0 变化，冲突直接失败。它不会修改旧批准记录或 operation 表。模板所需授权变量默认未设置，不能默认成功。

两份模板先验证目标库及 InnoDB。操作配置还校验已存在且 hash/reference/person/coverage 完全匹配的新版批准行；不会在缺批准时顺手建登记。使用同一命名锁和 SERIALIZABLE 事务。无 UPDATE、DELETE、TRUNCATE、INSERT IGNORE、ON DUPLICATE KEY UPDATE 或替代策略。其它租户/operation/publish 现有行及其时间戳保持原值。

配置的业务效果是允许现有服务在合法业务写期间按声明策略追加真实 GxP 审计；不授予角色、签名、业务权限，不创建项目、文件、模板、引用、受控、生效或废止业务数据。Root 后续仍须由真实前端页面执行被验收动作；配置准备和离线测试不能代替业务 E2E。

## Root 获批后的执行前置与复核

1. 独立记录用户明确的本机测试配置写入授权与真实质量批准事实。确认是否另授权登记 1 条新版批准；已有真实匹配登记时无需重复建行。
2. 从 Root 实际后端进程的运行数据源确认库、宿主端口与服务端端口，不凭默认 YAML 或目录名选库。用 `g22-gxp-readonly-preflight.sql` 刷新真实 schema、引擎、唯一索引、所有现有策略行/批准行及所选用户事实，确认仍是预期 baseline。该文件是只读准备，本批未执行。
3. 对两个策略表做可恢复备份，冻结全部旧行字段/时间戳摘要，暂停普通策略写入。命名锁仅约束本包执行者，不能替代停写。采用全新专用会话、已核实的严格 SQL mode/Asia-Shanghai 会话时区，不能在已有业务事务中调用模板。
4. 核对原始 policy hash、formal parser 和规范 coverage hash 未变；若发生漂移则停止，不能把旧确认用于新 payload。
5. 获得单独相应写授权后，在经授权本机测试目标完成真实 MySQL 语法/首次/重复/冲突/事务回滚验证，并保留受影响行数与完整旧行前后只读摘要。当前 12 个离线测试只证明模型/生成结构，不声称 MySQL 可执行或已回滚。
6. 如需要，先单独执行 quality-version 登记模板，输入真实 person/time/reference，最大 1 行；随后独立执行 operation 配置，最大 25 行。精确重复两者均为零变化。任一冲突失败不得停用旧策略或覆盖。回退也不能擅自删除新增审计配置/批准行；需要 Root 另外审查授权方案。
7. 复核 publish、旧批准及所有非目标行 byte/field/timestamp 摘要不变；复核 25 项全部 active/GXP/完整 payload，无超范围行；再由 Root 继续新后端与真实 UI 验收。

模板会暂时 CREATE/CALL/DROP 自有 procedure：`dcc_gxp1_tenant1_quality` / `dcc_gxp25_tenant1_config`；operation 临时表是 `tmp_dcc_gxp25_tenant1`。无预先 DROP 已存在 routine，重名应直接失败；临时表只有确认由自身创建后才清理。MySQL routine DDL 不属于行事务回滚，失败 CALL 后客户端停止时 routine 可能残留。Root 必须核对其确为本次创建、定义/归属匹配后单独清理，不得使用 mysql `--force` 或预删未知对象。缺 routine 权限、连接/schema/批准/backup/停写前置任一未满足均停止。

## 实际验证与交付边界

BDD/RED 在本任务记录。初始缺生成器 7 FAIL；新增批准登记函数缺失 2 ERROR；自有临时表/事务引擎保护新增断言 1 FAIL；真实生成文件 CRLF 字节漂移 1 FAIL；离线版本身份漏查尾空格冲突 1 FAIL，均为离线有效 RED。最终 `python -B doc/tasks/20261002-dcc-public-browser/test_g22_gxp_configuration.py`：12 PASS，覆盖精确范围、所有字段冲突、重复/部分重放、旧 publish/其它租户/非目标行保留、真实批准缺失/漂移、单独登记权限、SQL 永久 DML 边界及实际生成报告字节。具体原始日志保留在工作区，Git 不纳入 raw stdout；最终摘要在默认任务报告。

规范报告使用原始 UTF-8 字节写入，没有额外换行。任务自有 `.gitattributes` 对这一份报告设置 `-text`，避免本机 `core.autocrlf=true` 在 checkout 时重新改变其批准绑定 hash；不修改其它文件的换行规则。若任何报告实际字节漂移，禁止继续执行旧包。

运行生成器和离线测试无 DB driver、连接、HTTP、shell executor；只读正式源码与 Root 已有证据，输出仅在现有任务目录。G21 Git candidate manifest 保持冻结；没有修改 policy/Java/正式 SQL/生产前端或其它 Owner 文档。

Root 最终向用户单列确认的内容：本机 tenant1 是否授权新增这 25 项审计配置；本次实际 QA/质量批准人姓名与系统账号、质量职责依据、批准依据/签名证据及 Asia/Shanghai 时间；是否另授权登记这一条固定 hash 的新版批准记录。提问依据为真实新增配置写入范围及上述 QA 不可开发自批准规定，不能复用旧版本 reference，也不能把本机配置确认解释成生产 CSV 批准。
