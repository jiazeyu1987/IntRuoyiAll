# 执行日志：实现租户全量复制离线工具

BDD: 预检必须阻止未租户化唯一索引 -> Given 数据库存在含 `tenant_id` 的源侧非空表且唯一索引不包含 `tenant_id`, When 执行 tenant clone precheck, Then 命令返回 `TENANT_CLONE_SCHEMA_UNIQUE_INDEX_NOT_TENANT_SCOPED` 且不写业务数据。

BDD: 复制契约必须覆盖所有候选表 -> Given 数据库存在含 `tenant_id` 的表, When 契约缺失表分类或引用声明, Then 契约校验失败并列出缺失项。

BDD: 目标清空必须要求确认与备份 -> Given 目标租户存在数据, When execute 缺少 `--confirm-clear-target` 或备份路径, Then 命令失败且不删除目标数据。

BDD: 作业与 ID 映射 schema 可审计 -> Given 执行 create-job, When 生成作业表和映射表 DDL, Then 字段、唯一键、状态机符合设计文档。

BDD: 回滚接口必须 fail fast -> Given 作业没有备份索引, When 调用 rollback, Then 返回 `TENANT_CLONE_BACKUP_MISSING` 且不改写数据。

## 证据

- 2026-05-25：已创建实现任务文档，等待 worker 子 agent 按分片写 RED 测试和实现。
- RED: `python -m pytest script/tests/test_tenant_clone_cli.py -q` -> FAIL, 3 failed，预期原因：生产模块 `script.tenant_clone` 尚不存在，CLI 无法输出 JSON/errorCode。
- RED: `python -m pytest script/tests/test_tenant_clone_contract.py -q` -> FAIL, 2 failed，预期原因：生产模块 `script.tenant_clone` 尚不存在，契约校验入口未实现。
- RED: `python -m pytest script/tests/test_tenant_clone_schema.py -q` -> FAIL, 2 failed，预期原因：生产模块 `script.tenant_clone` 尚不存在，schema 预检和 DDL 入口未实现。
- RED: `python -m pytest script/tests/test_tenant_clone_workflow.py -q` -> FAIL, 3 failed，预期原因：生产模块 `script.tenant_clone` 尚不存在，execute/rollback fail-fast 入口未实现。
- GREEN: schema/contract worker `python -m pytest script/tests/test_tenant_clone_contract.py -q` -> PASS, 2 passed。
- GREEN: schema/contract worker `python -m pytest script/tests/test_tenant_clone_schema.py -q` -> PASS, 2 passed。
- GREEN: CLI/workflow worker `python -m pytest script/tests/test_tenant_clone_cli.py -q` -> PASS, 3 passed。
- GREEN: CLI/workflow worker `python -m pytest script/tests/test_tenant_clone_workflow.py -q` -> PASS, 3 passed。
- REGRESSION: reviewer `python -m pytest script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q` -> PASS, 10 passed。
- REVIEW-FAIL: `python -X utf8 -m script.tenant_clone schema-ddl --name tenant-clone-job` -> FAIL 门禁，stdout JSON 正常但 stderr 出现 RuntimeWarning；`__init__.py` 与 `__main__.py` 存在重复 CLI/loader 入口，需修复为单一清晰入口。
- GREEN: CLI cleanup worker `python -m pytest script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q` -> PASS, 11 passed。
- GREEN: reviewer Python subprocess capture `python -X utf8 -m script.tenant_clone schema-ddl --name tenant-clone-job` -> PASS, returncode=0, stderr 为空，stdout 为有效 JSON。
- REVIEW-FAIL: static review `rg "not implemented|skeleton" script/tenant_clone` -> FAIL, `execute` 和 `rollback` 在前置条件通过后返回 `success: true` 但实际写入/恢复路径未实现，违反 no default-success/no fallback 策略。
- BDD: CLI 成功命令不得输出 RuntimeWarning -> Given `script.tenant_clone` 包仅由 `__main__.py` 提供 CLI 入口, When 执行 `python -X utf8 -m script.tenant_clone schema-ddl --name tenant-clone-job`, Then 命令 stdout 输出 JSON 且 stderr 为空。
- GREEN: cleanup worker `python -m pytest script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q` -> PASS, 11 passed；新增覆盖 `schema-ddl` 成功命令 `stderr == ""`。
- GREEN: cleanup worker `python -X utf8 -m script.tenant_clone schema-ddl --name tenant-clone-job` -> PASS, returncode=0，stdout 为 JSON，stderr 为空。
- 2026-05-25：Test worker 已补充 RED 测试设计，仅使用临时文件与假 schema/contract 数据，不连接真实 MySQL、不修改数据库、不写生产实现代码。
- RED: `python -m pytest script/tests/test_tenant_clone_cli.py -q` -> FAIL, 预期原因：`script.tenant_clone` 生产模块尚不存在或 CLI 尚未按合同输出 JSON/errorCode；覆盖 precheck JSON 合同、未租户化唯一索引、缺少目标清空确认、源目标租户相同。
- RED: `python -m pytest script/tests/test_tenant_clone_contract.py -q` -> FAIL, 预期原因：契约校验入口尚不存在或未实现未分类表/未声明引用 fail-fast；覆盖 `TENANT_CLONE_CONTRACT_MISSING_TABLE` 与 `TENANT_CLONE_CONTRACT_INVALID_REFERENCE`。
- RED: `python -m pytest script/tests/test_tenant_clone_schema.py -q` -> FAIL, 预期原因：schema 预检与作业/映射表 DDL 输出入口尚不存在或未实现；覆盖 `TENANT_CLONE_SCHEMA_UNIQUE_INDEX_NOT_TENANT_SCOPED`、`infra_tenant_clone_job`、`infra_tenant_clone_id_map` 关键字段、唯一键和状态机。
- RED: `python -m pytest script/tests/test_tenant_clone_workflow.py -q` -> FAIL, 预期原因：execute/rollback fail-fast 工作流尚不存在或未实现；覆盖目标清空确认、备份路径缺失、rollback 缺失备份索引。
- RED: 已实际运行上述 4 条命令，当前失败证据均为 `No module named script.tenant_clone` 导致 stdout 不是 JSON；符合生产模块尚不存在阶段的 RED 预期。
- GREEN: `python -m pytest script/tests/test_tenant_clone_cli.py -q` -> PASS, 3 passed；CLI 入口支持 `precheck`、`execute`、`rollback` 并在失败时向 stdout 输出 JSON 对象。
- GREEN: `python -m pytest script/tests/test_tenant_clone_workflow.py -q` -> PASS, 3 passed；execute 缺确认/缺备份与 rollback 缺备份索引均 fail-fast，`clearedRows/restoredRows` 保持 0。
- GREEN: `python -m pytest script/tests/test_tenant_clone_contract.py -q` -> PASS, 2 passed；入口扩展后契约校验命令仍保持现有 worker 行为。
- GREEN: `python -m pytest script/tests/test_tenant_clone_schema.py -q` -> PASS, 2 passed；入口扩展后 schema 检查与 DDL 命令仍保持现有 worker 行为。
- 2026-05-25：Schema/contract worker 已实现 `script/tenant_clone` 纯函数层：契约校验、schema 唯一索引检查、job/id_map DDL 生成；实现仅读取 JSON 文件和内存字典，不连接真实 MySQL，不写业务数据。
- GREEN: `python -m pytest script/tests/test_tenant_clone_contract.py -q` -> PASS, 2 passed；覆盖未分类候选表返回 `TENANT_CLONE_CONTRACT_MISSING_TABLE`、未声明引用返回 `TENANT_CLONE_CONTRACT_INVALID_REFERENCE`。
- GREEN: `python -m pytest script/tests/test_tenant_clone_schema.py -q` -> PASS, 2 passed；覆盖非主键唯一索引缺少 `tenant_id` 返回 `TENANT_CLONE_SCHEMA_UNIQUE_INDEX_NOT_TENANT_SCOPED`，并验证 `infra_tenant_clone_job` 与 `infra_tenant_clone_id_map` DDL 字段、唯一键和状态机。
- BDD: 未实现复制写入路径必须 fail fast -> Given execute 前置条件、目标清空确认与备份索引均满足, When 实际 clone write path 尚未实现, Then 命令返回非 0、`success=false`、`TENANT_CLONE_WRITE_PATH_NOT_IMPLEMENTED` 且 `clearedRows=0`。
- BDD: 未实现回滚恢复路径必须 fail fast -> Given rollback 已提供备份索引且确认恢复, When 实际 restore path 尚未实现, Then 命令返回非 0、`success=false`、`TENANT_CLONE_RESTORE_PATH_NOT_IMPLEMENTED` 且 `restoredRows=0`。
- RED: `python -m pytest script/tests/test_tenant_clone_workflow.py -q` -> FAIL, 2 failed，预期原因：`execute`/`rollback` 前置条件通过后仍返回 `success=true`，且消息声明写入/恢复路径未实现。
- GREEN: `python -m pytest script/tests/test_tenant_clone_workflow.py -q` -> PASS, 5 passed；未实现写入/恢复路径改为明确 errorCode 的 fail-fast，`clearedRows/restoredRows` 保持 0。
- GREEN: `python -m pytest script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q` -> PASS, 13 passed。
- GREEN: `rg "not implemented|skeleton" script/tenant_clone` -> PASS，无命中；生产代码不再包含未实现 skeleton 成功路径提示。
- BDD: 离线作业按 job code 幂等创建并可查询 -> Given 本机 job store 为空且提供复制契约, When 连续两次执行 `create-job --job-code` 并按相同 job code 执行 `status`, Then 只持久化一个 READY 作业且两次返回同一作业。
- BDD: 离线 execute 完成 clear_target_then_clone 闭环 -> Given 本机 JSON 数据源同时包含源租户、目标租户和其他租户数据, When 执行 `execute --offline-data-store --confirm-clear-target`, Then 先备份目标租户原始行，再只删除目标 tenant_id 行，保留源租户行，按契约复制源租户行到目标租户。
- BDD: 离线复制必须生成 ID 映射并重写子表引用 -> Given 子表按契约引用父表主键, When 复制源租户父表和子表, Then tenant_data 表生成一对一 ID 映射且目标子表引用目标父表 ID。
- BDD: 缺失父表 ID 映射必须失败 -> Given 源子表引用不存在的源父表主键且契约要求重写, When 执行离线复制, Then 命令返回 `TENANT_CLONE_MISSING_ID_MAPPING` 且不改写离线 JSON 数据源。
- BDD: 离线 rollback 使用备份恢复且可重复 -> Given execute 已备份并改写目标租户数据, When 连续两次执行 `rollback --confirm-restore-target`, Then 目标租户恢复原始数据，作业状态保持 `ROLLED_BACK`，重复 rollback 不再次改变数据。
- RED: `python -m pytest script/tests/test_tenant_clone_offline_clone.py -q` -> FAIL, 4 failed；预期原因：当前 CLI 尚未实现 `create-job`/`status` 子命令，`execute` 尚未支持 `--contract` 与 `--offline-data-store` 离线 JSON 数据源，`rollback` 离线恢复闭环因此无法进入。
- REGRESSION: `python -m pytest script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q` -> PASS, 13 passed；确认新增 fixture 未改变既有 tenant clone 测试语义。
- GREEN: implementation worker `python -m pytest script/tests/test_tenant_clone_offline_clone.py -q` -> PASS, 4 passed；实现 `create-job`/`status`、离线 JSON 数据源 clear+clone、备份索引、ID 映射、引用重写、缺失映射失败不改写 data store、rollback 恢复与幂等。
- REGRESSION: implementation worker `python -m pytest script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q` -> PASS, 13 passed；确认未提供 `--offline-data-store` 时既有 execute/rollback 未实现路径仍保持明确 fail-fast。
- BDD: 复用 job code 必须校验创建请求一致性 -> Given 已存在同一 `job_code` 的 READY 作业, When 再次执行 `create-job` 但 `targetTenantId` 或 `contractPath` 与已存在作业不一致, Then 命令返回非 0、`success=false`、`TENANT_CLONE_JOB_CODE_CONFLICT`、phase `JOB_CREATE`，且持久化 job 文件不被改写。
- RED: `python -m pytest script/tests/test_tenant_clone_offline_clone.py -q` -> FAIL, 1 failed，预期原因：`create-job` 发现同名 job 文件后直接返回已有作业成功，未校验 `sourceTenantId`、`targetTenantId`、`profile`、`mode`、`contractPath` 是否与请求一致。
- GREEN: `python -m pytest script/tests/test_tenant_clone_offline_clone.py -q` -> PASS, 5 passed；新增同名 job code 冲突覆盖不同目标租户与不同 contract path，冲突时返回 `TENANT_CLONE_JOB_CODE_CONFLICT` 且作业文件保持不变。
- GREEN: `python -m pytest script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q` -> PASS, 13 passed；确认既有 CLI、契约、schema 与工作流 fail-fast 行为未回归。
- REGRESSION: reviewer `python -m pytest script/tests/test_tenant_clone_offline_clone.py script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q` -> PASS, 18 passed。
- CLEANUP-PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-tenant-clone-implementation --mode preview` -> BLOCKED；无 delete 候选，阻塞原因是主 worktree dirty 且当前 linked worktree 不能 ff-only 合并到 `int_main`。
- REBASE: `git rebase int_main` -> PASS；工作分支更新到当前 `int_main` 之后，`git merge-base --is-ancestor int_main HEAD` -> PASS。
- REGRESSION: rebase 后 reviewer `python -m pytest script/tests/test_tenant_clone_offline_clone.py script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q` -> PASS, 18 passed。
- CLEANUP-PREVIEW: rebase 后 `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-tenant-clone-implementation --mode preview` -> BLOCKED；无 delete 候选，剩余阻塞原因是主 worktree dirty。
- MERGE: 主后端工作区 `git merge --ff-only tenant-clone-implementation-20260525` -> PASS；unrelated dirty 文件 `script/deploy/int-ruoyi-test/website.nginx.conf` 与 `doc/tasks/20260525-full-test-publish-intruoyi-website/` 未暂存、未提交。
- REGRESSION: 主后端工作区合并后 `python -m pytest script/tests/test_tenant_clone_offline_clone.py script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q` -> PASS, 18 passed。
