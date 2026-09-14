# DCC 五账号文档上传 E2E 角色准备执行日志

## 用户意图

用户要求找 5 个不同账号分别扮演文控上传文档 E2E 的 5 个角色；如权限缺失，可以赋予对应权限；5 个账号里不要有 `admin`。用户已单独说明登录密码来源，任务日志不记录密码明文。

## BDD

- BDD: 五个非 admin 账号覆盖文控上传审批链路 -> Given 本机租户存在 5 个启用账号且均不是 `admin` When 补齐文控上传、文控审核、会签审核、会签批准、文控批准的正式权限和岗位 Then 五个账号可分别用于文控上传文档完整 E2E 的角色登录与操作。

## 执行记录

- PRECHECK: 已读取 `docs/login-access.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/task-closeout-rules.md`。
- PRECHECK: `docs/experience-index.md` 存在；本任务命中数据库写入、PowerShell 编码、DCC 上传类别权限、DCC 岗位/审批路线解析门禁。
- PRECHECK: 使用 `quality-assurance-test-suite` 技能约束 QA 证据，已读取 QA evidence contract。
- RED: MySQL 写入事务预检执行 -> FAIL，首次事务因字符集 collation mismatch 失败并回滚，未落库。
- GREEN: MySQL 写入事务重跑 -> PASS，使用 `utf8mb4_unicode_ci` 会话字符集后完成角色、类别上传规则和审批路线写入。
- WRITE: 目标类别 `DCC_OTHER_TEMPLATE_900250` / `其他`，类别 ID `906104`；停用旧启用路线 `906306`，启用新路线 `907390`，版本 `4`。
- WRITE: 账号映射为上传人 `pengyunfeng`，文控审核 `zhaohaichen`，会签审核 `zhaojie`，会签批准 `zhaomingyu`，文控批准 `wangsiyu`。
- GREEN: MySQL 只读复验 -> PASS，5 个账号均启用且非 `admin`，目标类别当前唯一启用路线为 `907390`，路线节点不包含 `admin`。
- GREEN: 本机后端登录/登出预检 -> PASS，5 个账号均可登录并登出；未在日志记录密码、token 或完整凭据。
- GREEN: QA 证据结构校验 -> PASS，`validate_quality_assurance.py --evidence doc/tasks/20260802-dcc-five-account-role-setup/verification-report.md` 通过。
- GREEN: 脱敏复查 -> PASS，任务目录内未发现用户提供的明文密码。
- GREEN: `git diff --check -- doc/tasks/20260802-dcc-five-account-role-setup` -> PASS。
- EXPERIENCE: 已按项目收尾规则检查经验归档；本次命中既有 `docs/database-rules.md` collation 门禁、`docs/login-access.md` 登录门禁和 `docs/frontend-development.md` DCC 上传类别权限门禁，无需新增长期经验文档。

## 验证摘要

- 5 个账号：`pengyunfeng`、`zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu`。
- 审批路线：`907390`，4 个审批节点依次为文控审核、会签审核、会签批准、文控批准。
- 上传权限：`pengyunfeng` 具备文控提交菜单权限和目标类别 `UPLOAD` 权限。
- 注意：目标类别仍保留既有 `admin` 上传权限规则；本任务未删除既有基线权限，选定五账号和审批路线不包含 `admin`。
- Git closeout：当前工作区已有本任务外未提交改动和本地 ahead 状态，本任务未提交或推送。
