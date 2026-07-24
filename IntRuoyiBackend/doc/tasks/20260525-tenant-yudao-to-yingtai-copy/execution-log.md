# 执行日志：复制芋道源码租户数据到瑛泰医疗租户

BDD: 租户数据复制预检 -> Given 源租户 `芋道源码` 与目标租户 `瑛泰医疗` 需要在同一明确数据库环境中唯一存在, When 执行只读预检, Then 输出租户编号、tenant_id 表清单、表级记录数和冲突风险，且不修改任何业务数据。

BDD: 源租户保持只读 -> Given 源租户保存当前业务数据, When 执行复制任务, Then 源租户所有表级记录数与关键业务数据保持不变。

BDD: 目标租户接收复制数据 -> Given 用户确认目标环境、目标已有数据处理策略和回滚方案, When 执行复制, Then 目标租户可通过真实登录和核心业务列表看到复制后的数据。

## 证据

- 2026-05-25：任务文档已创建，当前仅允许只读预检。
- RED: `python -X utf8 -` 只读前置检查 -> FAIL, 目标租户 `瑛泰医疗` 已有 1436 行 `tenant_id` 隔离数据；缺少覆盖/合并策略和回滚方案，写入必须阻塞。
- 只读预检：本机隔离库 `127.0.0.1:23306/ruoyi-vue-pro` 中 `芋道源码 id=1`、`瑛泰医疗 id=162`，源租户 105 张非空租户表合计 104338 行，目标租户 8 张表合计 1436 行。
- 只读风险检查：105 张源侧非空租户表均存在主键；直接复制保留主键会冲突，生成新主键则需要跨表引用重映射。
- 证据校验：`python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence ...\database-schema-evidence.md` -> FAIL，缺少 GREEN 通过记录；由于写库前置条件未确认，当前不允许伪造 GREEN。
- 2026-05-25：用户确认本机库执行，并确认先删除 `瑛泰医疗` 下的数据再复制。
- RED: `python -X utf8 -` 唯一索引结构预检 -> FAIL, 发现 26 个源侧非空租户表存在不含 `tenant_id` 的唯一索引；即使清空目标租户，精确复制源租户原值仍会与源租户自身唯一键冲突。
- 2026-05-25：review round 1 -> FAIL，阻塞项包括缺少正式可执行租户复制设计、M3 状态与证据不一致、BDD/TDD 缺少 GREEN/REGRESSION、API/命令合同不清晰、Subagent Driven 分工缺失。
- BDD: 租户全量可复制数据设计补齐 -> Given reviewer 要求补齐复制源租户所有可复制数据到目标租户且源租户保持不变的正式设计, When worker 仅修订任务文档和设计文档, Then 文档必须覆盖 schema 租户化、复制契约、ID 映射、目标清空备份回滚、文件对象、BPM 边界、排除策略、API/命令、验证矩阵和 Subagent Driven 分工。
- RED: reviewer round 1 -> FAIL, 现有文档不能放行实现或执行复制。
- GREEN: 文档修订 -> PASS, 已新增 `tenant-clone-design.md` 并同步 `task.md` 当前状态，等待 reviewer 复审。
- REGRESSION: 文档边界检查 -> PASS, 本轮仅修改任务文档、执行日志、正式设计文档和 worker 结果报告，不修改生产代码、不改数据库、不提交 git。
