# 任务：实现租户全量复制离线工具

## 任务目标

- 在独立 worktree `D:\ProjectPackage\Int\IntRuoyi\worktrees\tenant-clone-implementation-20260525` 中实现租户全量可复制数据复制的本机离线工具第一版。
- 以 `doc/tasks/20260525-tenant-yudao-to-yingtai-copy/tenant-clone-design.md` 为实现依据，交付命令层、预检、复制契约校验、作业/映射表 schema、离线 JSON 数据源的备份/清空/复制/ID 映射/引用重写/回滚闭环，以及非离线路径严格失败门禁。
- 当前实现不得直接执行生产/测试服写库，不得修改源租户数据，不得静默跳过唯一索引、未分类表、未声明引用、缺失备份或缺失确认。

## 维护性与方案评估

- 采用离线命令优先，避免在业务后端 API 中直接暴露高风险数据复制能力。
- 采用复制契约驱动，不做“扫描到表就无脑复制”的短期方案。
- 采用预检先行：schema 唯一索引未租户化、契约缺失、目标非空未确认、运行态未声明时必须失败。
- 分片开发由 test、schema/contract、CLI/workflow、cleanup、fail-fast、offline clone、conflict fix 多个 worker 子 agent 执行，主 agent 只做 reviewer 和集成门禁。

## 前序任务检查

- 已检查最近任务 `doc/tasks/20260525-nas-backup-root/task.md`，状态为已完成。
- 已确认设计文档任务 `doc/tasks/20260525-tenant-yudao-to-yingtai-copy/task.md` 已提交并通过 review 设计门禁。

## BDD 场景

- BDD: 预检必须阻止未租户化唯一索引 -> Given 数据库存在含 `tenant_id` 的源侧非空表且唯一索引不包含 `tenant_id`, When 执行 tenant clone precheck, Then 命令返回 `TENANT_CLONE_SCHEMA_UNIQUE_INDEX_NOT_TENANT_SCOPED` 且不写业务数据。
- BDD: 复制契约必须覆盖所有候选表 -> Given 数据库存在含 `tenant_id` 的表, When 契约缺失表分类或引用声明, Then 契约校验失败并列出缺失项。
- BDD: 目标清空必须要求确认与备份 -> Given 目标租户存在数据, When execute 缺少 `--confirm-clear-target` 或备份路径, Then 命令失败且不删除目标数据。
- BDD: 作业与 ID 映射 schema 可审计 -> Given 执行 create-job, When 生成作业表和映射表 DDL, Then 字段、唯一键、状态机符合设计文档。
- BDD: 回滚接口必须 fail fast -> Given 作业没有备份索引, When 调用 rollback, Then 返回 `TENANT_CLONE_BACKUP_MISSING` 且不改写数据。
- BDD: 离线作业按 job code 幂等创建并可查询 -> Given 本机 job store 为空且提供复制契约, When 连续两次执行 `create-job --job-code` 并按相同 job code 执行 `status`, Then 只持久化一个 READY 作业且两次返回同一作业。
- BDD: 离线复制生成 ID 映射并重写引用 -> Given 子表按契约引用父表主键, When 复制源租户父表和子表, Then tenant_data 表生成一对一 ID 映射且目标子表引用目标父表 ID。
- BDD: 离线 rollback 使用备份恢复且可重复 -> Given execute 已备份并改写目标租户数据, When 连续两次执行 `rollback --confirm-restore-target`, Then 目标租户恢复原始数据，重复 rollback 不再次改变数据。
- BDD: job code 复用必须校验请求一致性 -> Given job store 已存在同一 job code, When 使用不同源/目标租户、profile、mode 或 contract 再次 create-job, Then 返回 `TENANT_CLONE_JOB_CODE_CONFLICT` 且不改写已有作业。

## 里程碑

- [x] M1：创建独立 worktree、同名分支和实现任务文档。
- [x] M2：RED 测试覆盖 CLI 合同、契约校验、schema 预检、备份确认、回滚缺失备份。
- [x] M3：实现 schema/契约 worker 负责的预检和 DDL 产物。
- [x] M4：实现 clone worker 负责的 CLI、作业状态、离线清空/备份/复制/回滚和非离线路径 fail-fast。
- [x] M5：实现 test worker 负责的测试夹具和回归验证命令。
- [x] M6：reviewer 集成复审，循环修复至符合设计文档。
- [x] M7：运行验证、执行 cleanup 预览、提交当前任务改动。

## 预期验证

- `python -m pytest script/tests/test_tenant_clone_cli.py -q`
- `python -m pytest script/tests/test_tenant_clone_contract.py -q`
- `python -m pytest script/tests/test_tenant_clone_schema.py -q`
- `python -m pytest script/tests/test_tenant_clone_workflow.py -q`
- `python -m pytest script/tests/test_tenant_clone_offline_clone.py -q`
- `python -m pytest script/tests/test_tenant_clone_offline_clone.py script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q`

## 当前状态

状态：已完成。实现与 reviewer 回归已通过，工作分支已 rebase 到当前 `int_main`，并已手动 ff-only 合并回主后端工作区；自动 cleanup apply 曾被主 worktree dirty 阻塞，未处理 unrelated dirty 文件。

## 最终验证记录

- GREEN: `python -m pytest script/tests/test_tenant_clone_offline_clone.py script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q` -> PASS, 18 passed。
- Reviewer 结论：离线 JSON 数据源路径已覆盖 create-job/status、备份、清空、复制、ID 映射、引用重写、缺失映射失败不改写数据源、rollback 幂等恢复、job code 冲突 fail-fast；未提供 `--offline-data-store` 时仍保持明确失败门禁，不假成功。
- Rebase: `git rebase int_main` -> PASS，工作分支可 ff-only 合并到当前 `int_main`。
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-tenant-clone-implementation --mode preview` -> BLOCKED；无 delete 候选，剩余阻塞原因是主 worktree dirty。
- Merge: `git merge --ff-only tenant-clone-implementation-20260525` -> PASS，已合并到主后端工作区，未暂存或提交 unrelated dirty 文件。
- Post-merge regression: `python -m pytest script/tests/test_tenant_clone_offline_clone.py script/tests/test_tenant_clone_cli.py script/tests/test_tenant_clone_contract.py script/tests/test_tenant_clone_schema.py script/tests/test_tenant_clone_workflow.py -q` -> PASS, 18 passed。
