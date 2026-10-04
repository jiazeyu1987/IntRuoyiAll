# G43 本机开发维护入口

Status: ready_for_closeout — software frozen for Root review, runtime/package not executed by this agent. 唯一 Java/Maven owner 为 legacy_occupancy_core；实际数据库、Token、服务、打包和 Git 由 Root 管理，本轮不执行。

用户明确取消开发阶段质量批准前置。现有专属 local-maintenance 入口改用 DEVELOPMENT_TEST_ONLY 策略描述；不创建质量批准记录、不编造批准人/时间，不修改普通业务审批或电子签名。

- Given 用户已授权精确历史登记，开发环境与 25/39/13 原件证据完整且当前策略一致，When 提供五字段 developmentPolicy 描述而没有 qualityApproval，Then 请求可进入开发策略校验并通过真实认证、同事务登记和 Gxp 审计。
- Given 缺 local-maintenance、存在正式/测试 Profile 或实际 DataSource 非本机 23306 指定库，When 调用维护入口，Then 在读取 Token/认证/写入前拒绝。
- Given 策略原文/26 项运行配置、固定 Schema 或 39 份正文任一不符，When 执行开发维护，Then 明确拒绝；当前角色与权限和真实 Gxp 追加失败仍使全部登记回滚。
- Given 默认应用启动或正式环境，When 未显式启用专属 Runner，Then 不创建或运行维护动作。

约束：仅 Gate/Command/Executor 和必要专属测试。保持候选策略版本 2026-10-dcc-integration-01、policyHash 661af676e1406e86659806af8be8f46abd17d101d871d3af8d5b3b736873c894、coverageReportHash 3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d。固定 Schema/validator、旧授权回执及 OWNER 四源码不变。

验证顺序：有效行为 RED → 专属定向 GREEN → Gate/Command/Kernel/Entry/Logging 与既有登记/名称占用回归。H2/外部只读端口替身不代表实际数据库或 E2E 已完成。

## 验证结果

有效 RED：Gate 真实拒绝新 developmentPolicy/no qualityApproval（1 case，UNKNOWN_OR_MISSING_FIELD）。首次 GREEN 34/3 中 33 通过，唯一错误为新增测试使用错误 operation-map key 导致 NPE；原失败日志保留。修正为正式 OPERATION 常量后最终回归 130/7 全通过，无跳过。Kernel 24 包含 22 个继承登记场景，因此执行总数含重复。

生产仅 Gate/Command/Executor 三文件，测试仅对应 Gate/Command/Kernel 三文件。原 41 项源码指纹、G39 OWNER 四生产文件、正式 Token Auth/Runner、当前策略原文均零漂移。质量批准注册/资料已从本机开发入口移除；普通业务和签名链未修改。

请求协议：schemaVersion 1 的原 qualityApproval descriptor 改为 developmentPolicy descriptor；descriptor 为受保护路径+raw SHA（可附精确整数 bytes）。内容仅 status、tenantId、policyVersion、policyHash、coverageReportHash 五个字符串字段，不接收 approvedBy/approvedAt 等虚构批准字段。其余原授权/manifest/facts/bytes/历史决定/policyFile/schemaProof/scopeId/reason/requestId 合同保持。

Environment 需显式 active local-maintenance 且不含 prod/production/backup/test/unit-test；DataSource 实际 metadata URL 必须 jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro 或 localhost 同地址（可有 query 参数），实际库/UUID/timezone 严格固定。环境读在 stdin 前与真实登记事务内各执行一次。

最终 fingerprint/verification JSON 记录精确 raw SHA、保留基线、RED/GREEN 日志及边界。Maven 已停，不运行打包、服务、真实数据库、Token、Git 或页面验收；Root 审查和实际执行另行管理。
