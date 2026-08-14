# Execution Log: PQC组长表单修改与放行前复核链路

## Intent

用户要求在 worktree 内完成开发验证，先提交，再融合进 int_main。业务规则为：PQC 组长可修改 PQC 组员提交数据；PQC 管理列表修改/复核常驻；PQC 历史保留；复核通过更新活跃订单审核进度；放行前可改；放行后管理表单移除但历史保留。

## BDD Scenarios

BDD: PQC 管理列表操作常驻 -> Given PQC 组员提交的检验记录仍未放行 When PQC 组长打开 PQC 管理列表 Then 每行展示详情、复核、修改，且修改入口连接正式原始记录修改能力。

BDD: PQC 历史只读保留 -> Given PQC 提交已经进入历史查询口径 When PQC 组长打开 PQC 历史 Then 记录仍保留但不展示复核或修改操作。

BDD: 放行前可修改 -> Given PQC 提交已被复核通过但对应活跃订单尚未放行 When PQC 组长提交原始记录修改 Then 后端接受修改并保留字段级修订记录。

BDD: 放行后管理表单移除且历史保留 -> Given PQC 提交关联的活跃订单已有 RELEASED 放行事务 When 查询 PQC 管理列表 Then 该提交不返回；When 查询 PQC 历史 Then 该提交仍返回。

BDD: 复核通过更新活跃订单审核进度 -> Given PQC 提交存在正式逐件明细 When PQC 组长提交 APPROVED 复核 Then 后端写入最新复核记录并触发正式过程检验汇集，使活跃订单放行链路读取 CONFIRMED/PQC 汇集事实。

## Evidence

- 2026-08-10：读取适用技能、项目任务、前端、后端、E2E、编码、Git 和 worktree 触发规则。
- 2026-08-10：确认工作目录为 `D:\IntRuoyiWorktree\pqc-leader-form-edit-release-flow`，分支为 `codex/pqc-leader-form-edit-release-flow`，初始工作树干净。
- 2026-08-10：前端将 PQC 当前列表的“复核/修改”能力从 `PENDING/REJECTED` 状态条件改为“当前页且未放行”，历史页仅保留详情。
- 2026-08-10：后端增加 `pqcFormView` 当前/历史查询语义；当前页排除正式放行交易，历史页保留复核通过记录。
- 2026-08-10：增加 PQC 正式修改接口，更新检验任务、逐件明细、PQC 记录、事件修订审计；已汇集记录修改后重新汇集审核进度。
- 2026-08-10：创建实现提交 `d9b338596 fix: align PQC leader form edit and release flow`。
- 2026-08-10：将当时最新 `int_main` 合入任务分支，生成提交 `9e1cc4af5 merge: integrate current int_main into PQC leader flow`，无内容冲突；随后重新通过全部定向验证。
- 2026-08-10：`int_main` 存在与本任务重叠的并行未提交文件；用户明确授权手工三方融合并保留并行改动。
- 2026-08-10：仅暂存重叠路径后，将 `int_main` 从 `9c32e265c` 快进到 `9e1cc4af5`，再恢复并行改动；业务页面并行改动无冲突恢复，3 份本任务文档完成语义合并。
- 2026-08-10：使用 `project-experience-consolidation`，将“并行脏主工作区仅保存重叠路径、三方融合后恢复并行改动”的门禁合并到既有 `docs/worktree-memory.md`。
- 2026-08-10：`task-closeout-cleanup` preview/apply 均通过；保留 `task.md`、`execution-log.md`、`verification-report.md`，删除 3 份本任务中间 evidence 文件。
- 2026-08-10：确认任务分支为 `int_main` 祖先、worktree 干净、8091/48091 无监听且无目标进程后移除 Git worktree；清空依赖残留后验证物理路径不存在。
- 2026-08-10：端口登记表中仅目标条目已更新为 `active=false`，slot 10、8091/48091 已释放，其他任务登记项未修改。

## Command Log

- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-pqc-leader-form-release-flow-static.spec.cjs` -> FAIL，缺少 `pqcFormView` 和正式放行筛选合同。
- RED: `node IntRuoyiFronted\tests\e2e\pqc-leader-form-edit-release-flow-static.spec.cjs` -> FAIL，缺少当前/历史视图参数和放行前常驻按钮合同。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-pqc-leader-form-release-flow-static.spec.cjs` -> PASS。
- GREEN: `node IntRuoyiFronted\tests\e2e\pqc-leader-form-edit-release-flow-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，`BUILD SUCCESS`，MES 及 reactor 依赖编译通过。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS，退出码 0。
- REGRESSION: `git diff --check` -> PASS。
- GIT: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS；worktree 使用 8091/48091，`int_main` 使用 8081/48081。
- GIT: `git merge --ff-only codex/pqc-leader-form-edit-release-flow` -> PASS，`int_main` 快进到 `9e1cc4af5`。
- REGRESSION: 融合并恢复并行改动后，后端 PQC 静态合同、前端 PQC 静态合同和 PQC 管理默认提交日期静态合同均 PASS。
- REGRESSION: 融合并恢复并行改动后，`$env:NODE_OPTIONS='--max-old-space-size=8192'; node node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS，退出码 0。
- CLOSEOUT: `task_closeout.py --task-id 20260810-pqc-leader-form-edit-release-flow --mode preview/apply` -> PASS。
- CLOSEOUT: `git worktree list --porcelain` 不再包含目标，`Test-Path D:\IntRuoyiWorktree\pqc-leader-form-edit-release-flow` -> False，登记条目 `active=false`。

## Blockers

- 无。
