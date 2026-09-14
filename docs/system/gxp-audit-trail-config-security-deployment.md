# 统一 GxP 审计追踪配置、安全与部署设计

## Purpose and Scope

定义策略配置、秘密管理、权限分离、不可变控制、可信时间、备份归档、部署顺序、监控和失败关闭规则。

## Evidence Reviewed

- 本地 `yudao.access-log.enable=false`，说明 API 访问日志不能承担强制 GxP 审计。
- 签名留存 S3 校验只有显式启用并提供真实验证源时可用。
- 现有可信时间最小方案要求正式服/审查服配置 chrony、只追加证据目录和定时采集。
- 发布 Migration 台账已保存文件 SHA-256、环境、状态和 operationId。
- `docs/release-backup-restore.md` 明确区分灾备备份和法规长期归档。

## Configuration

- `gxp.audit.enabled` 在包含 GxP 功能的正式 profile 中必须为强制 `true`，不得通过普通环境变量关闭；缺失或 false 时应用启动失败。
- `gxp.audit.policy.location` 指向版本化、hash 固定且有批准引用的策略文件。
- 策略权威路径固定为 `config/gxp-audit-policy.yaml`，必须通过版本化 schema 校验；启动时验证策略 hash、有效批准和实际启用 GxP 模块，不能仅凭 profile 名称决定是否需要审计。
- `gxp.audit.incremental-seal.max-age`、`manifest.schedule`、`archive.schedule`、`review.schedule`、`trusted-time.max-age` 和偏差阈值只能使用已批准值；缺失时启动或治理门禁失败。
- `retention.class.*` 保存期限来自批准矩阵，不提供默认年限。
- 配置变更必须产生系统审计事件；策略生效需要质量电子签名。

## Secrets

- WORM 对象存储密钥、数据库专用凭据和签名密钥只从正式 secret store 或受控环境注入。
- 密钥不写入 Git、数据库审计 payload、命令日志、导出包或错误文本。
- 历史验签所需密钥版本必须可恢复；轮换不能使旧事件失去验证能力。
- 读取、轮换和吊销归档凭据本身属于系统级审计事件。

## Permissions

- `system:gxp-audit:query`：租户内查询。
- `system:gxp-audit:export`：导出固定范围证据。
- `system:gxp-audit:verify`：执行完整性校验。
- `system:gxp-audit:review`：创建和处理周期审查。
- `system:gxp-audit:review-sign`：质量签名。
- `system:gxp-audit:policy-manage`：提交策略变更，但不能自批准。
- `system:gxp-audit:cross-tenant-query`：独立高风险权限，使用即记录审计。

应用业务账号只有审计表 INSERT 权限，查询账号只有 SELECT 权限；任何应用账号均无 UPDATE、DELETE、TRUNCATE、ALTER 权限。Migration 账号和紧急 DBA 账号独立托管、限时启用、双人批准并记录工单。被审特权账号不得控制特权审计外送目标或删除其保护对象。

## Security Controls

- 身份取自认证上下文，禁止客户端提交或覆盖正式操作人。
- 正式时间取自受控服务器；签名时间、审计时间和业务选择时间分字段保存。
- before/after 按数据分类策略过滤，严禁密码、令牌、私钥和完整连接串落入审计。
- hash 算法和规范化版本显式保存；算法升级创建新策略版本，不重写旧事件。
- 每日清单写入启用 versioning 和 Object Lock 的正式 bucket，并验证对象 versionId、retainUntil、legal hold 和 hash。
- 增量密封按批准最大时长固定已提交事件水位；每日清单执行最终封存。任一水位超时、序号缺口或清单任务失败时治理状态和发布门禁失败。
- WORM 中保存的是自包含法规归档包而非仅 manifest/hash；包必须含完整规范事件、证据关联、签名、可信时间和必需领域证据。只有主副本均返回匹配 versionId、包 hash 和保存属性后才标记 VERIFIED。
- 主存储之外保留独立故障域副本；复制不得缩短保存期或降低 hold。
- 启用数据库原生审计或经批准的等价特权审计来源，覆盖审计表 DML/DDL、授权、账号使用、审计配置变化和关闭审计尝试，并在批准时限内外送到独立保护存储。具体产品未确认时保持设计 blocker，不使用普通应用日志替代。

## Deployment

1. M0：只读盘点、机器可读登记表、策略批准、实际写入口覆盖报告、数据库账号和正式存储前置检查。
2. M1：部署 additive schema、权限和只读验证，不启用业务接入。
3. M2：部署审计内核，使用专用自检事务验证 append、回滚和 hash，不产生假业务记录。
4. M3：按模块维护窗口切换；每个模块切换后禁止旧路径独立成功。
5. M4：启用查询、自动周期审查、增量密封、每日清单、完整法规归档、特权审计外送和时间采集。
6. M5：在清空的隔离环境仅用法规归档包完成真实恢复，再完成篡改检测、连续周期审查和经授权 Playwright 验收后进入发布门禁。

任何必需配置、权限、NTP、WORM、备份或策略缺失均停止部署，不切换到访问日志、内存记录、本地文件或默认策略。

## Observability

- 指标：append 成功/失败、业务回滚、幂等冲突、链冲突、清单延迟、完整性失败、归档失败、时间偏差、证据过期、审查逾期。
- 补充指标：未封存最大事件年龄、归档包 required item 缺失数、主副本差异、特权审计外送延迟/断链、应审周期缺口、覆盖登记缺口和过期不适用决定。
- 告警：任一生产 GxP append 失败立即高优先级告警；完整性失败和 WORM 验证失败按发布阻塞处理。
- 日志只记录 event UUID、对象稳定身份、错误码和 traceId，不重复输出 before/after 敏感内容。
- 治理状态页不得将“未采集、未配置、无法连接”展示为正常或不适用。

## Open Questions

- 企业允许的 NTP 地址、偏差阈值和采集频率。
- 正式/审查环境的 WORM 产品、bucket、复制拓扑和责任人。
- 审计查询和导出的性能容量目标。
- 数据库特权审计正式产品、外送协议和被审 DBA 无法改写的保护边界。

## Design Blockers

- 未提供法规/质量批准的保存期限和审查规则。
- 未提供真实 WORM bucket、对象锁定和独立副本证据。
- 未提供正式服务器 chrony/NTP 及普通应用账号不可修改时间的证据。
- 未完成数据库账号最小权限和紧急 DBA 操作审批流程。
- 未批准法规归档包内容矩阵、最大未封存时长和长期格式可读性抽检周期。
- 未证明全写入口发现器能够覆盖当前 Controller、Service、Job、消费者、Migration 和脚本。
