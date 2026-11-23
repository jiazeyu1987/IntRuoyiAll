# CE注册证发布登记与数据边界

基线 bd01f08cdf8c36a443664d750842705d41cb3632。仅修改应用仓发布输入，不操作服务器。

三条20261123 CE原SQL作为正式schema迁移登记；原始字节不变。新增20261123_dcc_registration_certificate_ce_release_preflight为可执行schema迁移（不是evidence-only preflight），依赖既有reminder、lifecycle、regulatory_facts。三条原SQL通过release-sources.json的带SHA256依赖声明，均在新检查之后执行；正式解析器合并原有依赖，不替换它们。

新迁移 requiresTargetPreflight=true，提供同名target-preflight只读SELECT，正式migration_inputs绑定其SHA256、readOnly=true与包内引用。检查全部租户和历史行（不按deleted过滤）：四个原CHECK存在且ENFORCED；支持材料status仅EFFECTIVE/REJECTED/VOIDED，类型、文件种类、提醒阈值符合三个原SQL的目标枚举。任意NULL/不支持值阻断，不转换PENDING_CONFIRMATION/CONFIRMED，不写业务数据。

停服前正式目标预检执行只读SELECT；停写后执行schema迁移内相同谓词并SIGNAL阻断，随后三个不可变原SQL自身CHECK继续保护数据约束。发现遗留状态需要独立业务审计与明确处置，不能猜测状态或自动回填。

本机解析验证只确认发布合同，不代表目标库数据通过。维护端必须从最终提交重新运行migration_inputs及目标预检；未通过不得部署。新检查不更新既有材料或通知配置。原三条SQL未改名、不覆盖、不作为排除来源处理。

验证命令：
- python scripts/preflight/test_release_source_registration.py
- python C:/SpugHandoff/release/source/ops/release/release_source_catalog.py --sql-root <checkout>/IntRuoyiBackend/sql/mysql
- 载入 C:/SpugHandoff/release/source/ops/spug/release-center/intruoyi_build.py，仅调用migration_inputs(Path(<fixed-checkout>))，不调用构建/服务器；包括实际policy gate、依赖排序、只读预检合同校验和artifact绑定。
- mvn -pl yudao-module-dcc -am '-Dtest=DccRegistrationCertificateSupportingDocumentServiceTest,DccRegistrationCertificateReminderServiceTest,DccRegistrationCertificateReminderSchemaTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

执行结果和最终提交由交付回复提供；本机日志保存在.runtime/ce-release-*，不将完整发布清单输出到对话。固定验证工作区由本任务新建，维护端preflight-20261123-bd01f08工作区保持不动。
