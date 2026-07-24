# 执行日志：20260605-smart-release-platform-architecture

BDD: 长期架构不是脚本补丁 -> Given 当前发布链路由 PowerShell 脚本承担大量逻辑 / When 设计长期方案 / Then 文档必须定义 manifest 驱动的发布平台边界，而不是继续让脚本自行猜测发布行为。

BDD: 本地变化必须先进入 release-intake -> Given 开发者手工修改本地表结构、必要数据或资源引用 / When 构建发布包 / Then release-intake 必须生成差异报告并要求绑定 migration、data registry 或资源快照，否则构建失败。

BDD: 更快打包依赖可信缓存 -> Given 本次只变更一个组件 / When 执行 smart-release / Then 未变化组件只能从 immutable artifact cache 复用，并校验 digest、依赖闭包和工具链 hash。

BDD: 大文件不进入普通发布搬运 -> Given DCC 有 10000 个文件且只新增少量文件 / When 普通发布包不携带文件实体 / Then 发布只检查资源引用和已验证 resourceDelta，真实文件同步由备份/恢复 delta 负责。

BDD: 状态机阻止假成功 -> Given precheck、deploy、verify、rollback 任一步失败 / When Runtime Control 展示发布状态 / Then 状态必须停在明确失败或 `deployed_not_verified`，不得标记成功。

GREEN: 架构文档落盘 -> PASS，已新增 ADR、system-design、domain-model-and-contracts、state-machine、implementation-roadmap、test-plan、migration-plan、governance-and-open-decisions、architecture-index。

GREEN: 文档门禁检查 -> PASS，关键文档均存在，并覆盖 Manifest 驱动、release-intake、data-ownership-registry、deployed_not_verified、report-only、RED 测试、legacy 迁移、阶段批准点和阅读顺序。

GREEN: task-closeout-cleanup preview -> PASS，无阻塞；预览建议删除 ADR、系统设计、领域模型、状态机、路线图、测试计划、迁移计划和治理文档，但这些是本任务正式交付物，本次保留不删除。
