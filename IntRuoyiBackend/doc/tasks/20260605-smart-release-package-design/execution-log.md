# 执行日志：20260605-smart-release-package-design

BDD: 小改动快速构建 -> Given 后端、前端、Website、SQL、数据、资源中只有一个组件变化 / When 构建 `smart-release` / Then 只重建变化组件，未变化组件复用已验证产物并校验 checksum/digest。

BDD: 表结构变化不导致部署失败 -> Given 本地代码依赖新的表或字段 / When 构建发布包 / Then 发布包必须包含版本化、幂等的数据库迁移及执行顺序，目标环境部署前能预检并在缺失时 fail fast。

BDD: 必要数据变化可随发布到测试服和备份服 -> Given 菜单权限、字典、配置或业务基础数据变化 / When 构建发布包 / Then 必要数据被声明为数据迁移包并带 checksum、执行历史和租户范围，禁止覆盖目标环境非发布数据。

BDD: 发布不搬运海量文件但检查引用一致性 -> Given 发布包不包含 DCC/展厅真实文件 / When 部署到测试服或备份服 / Then 发布前后必须校验数据库文件引用、文件配置和目标 MinIO 对象一致，不因引用正式服域名或缺对象而放行。

BDD: 备份恢复支持大文件增量 -> Given DCC 模块有 10000 个文件且只新增少量文件 / When 执行备份或恢复 / Then 只同步新增或变化文件，数据库关系和资源 manifest 保持一致，并通过 size/sha256 校验。

RED: 主 agent 第一轮严格评审 proposal-worker-draft.md -> FAIL，草案未充分定义本地数据库手工改表/改数据的构建前捕获机制，required-data 可发布边界不够可执行，发布不搬真实大文件时缺少 resourceSnapshot/resourceDeltaPrepared 前置门禁，大文件 hash 策略和缓存复用位置仍需收紧。

GREEN: 子 agent Bohr 修订 proposal-worker-draft.md -> PASS，补充 `release-intake`、`data-ownership-registry`、新增文件引用资源准备门禁、大文件 hash 策略和 immutable artifact cache 规则。

GREEN: 主 agent 严格复审 final-design.md 与 review-report.md -> PASS，最终方案明确更快构建、更稳发布、表结构/必要数据/资源引用变化捕获、大文件增量备份恢复、无 fallback 门禁和阶段实施顺序。

GREEN: task-closeout-cleanup preview -> PASS，无阻塞；预览建议删除 final-design.md、proposal-worker-draft.md、review-report.md，但这些是本任务正式交付物和评审证据，本次保留不删除。
