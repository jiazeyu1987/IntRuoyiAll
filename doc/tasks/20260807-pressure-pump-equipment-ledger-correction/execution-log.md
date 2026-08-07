# Execution Log

## User Intent

- 在上一轮设备台账比对结论基础上，用户要求新增 `C01017 / 撤压机`。
- 用户要求将当前光固机编码 `A05059` 改为 `A05075`。
- 用户要求将当前箱型干燥机编码 `B09041` 改为 `B04091`。
- 变更范围限定为本地 `int_main` 运行库的租户 `122`，不修改其它租户或其它设备。

## BDD Scenarios

BDD: 新增撤压机 -> Given 租户 122 不存在未删除编码 C01017 且正式设备类型和车间已核对, When 新增设备 C01017 / 撤压机, Then 台账存在唯一正式记录；设备条码配置缺失时保持无条码，不伪造记录。

BDD: 光固机编码修正 -> Given 租户 122 的设备 A05059 唯一存在且 A05075 不存在, When 将该设备编码改为 A05075, Then 原设备 ID 和非编码业务字段不变，设备工序关系和全局 MES 工序目录不再残留 A05059。

BDD: 箱型干燥机编码修正 -> Given 租户 122 的设备 B09041 唯一存在且 B04091 不存在, When 将该设备编码改为 B04091, Then 原设备 ID 和非编码业务字段不变，设备工序关系和全局 MES 工序目录不再残留 B09041。

BDD: 任一前置条件失败时整体回滚 -> Given 三项变更在同一事务中执行, When 任一唯一性、关联、租户或影响行数断言失败, Then 三项变更均不提交且现有数据保持原状。

## Command Intent And Evidence

- READ: 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/database-rules.md` 和 `docs/experience-index.md` 适用门禁。
- GIT BASELINE: 任务开始时 `int_main` 比 `origin/int_main` 领先 1 个提交，且存在并发任务日志改动；并发提交 `9c7507e1d` 随后保存了该日志和另一并发任务的初始文档。该提交不作为本任务实现提交，本任务未修改其内容。
- SCHEMA: 上一轮只读核对已确认本地运行库为 Docker `int-ruoyi-mysql` 的 `ruoyi-vue-pro`，正式设备表为 `mes_dv_machinery`；本任务写入前将重新核对 schema、目标行和引用关系。
- SCHEMA: `mes_dv_machinery` 当前无设备编码唯一键，正式服务按租户校验唯一性；事务将按租户、删除标记和精确旧编码加锁并断言目标编码不存在。
- SOURCE: 光固机为 `id=202 / A05059 / 光固机`，箱型干燥机为 `id=198 / B09041 / 箱型干燥机`；两者均属于租户 `122`，设备类型 `5 / DEFAULT-MACHINERY-TYPE`、车间 `900066 / AUTO-WSHOP`、状态 `2 / 生产中`。
- RELATION: 两台设备各有 1 条活动 `mes_dv_machinery_process` 和 1 条按设备 ID 关联的 `mes_md_workstation_machine`；点检、保养、维修、QA 规程、PQC 明细、组长范围和资源调整均无目标设备活动记录。
- CATALOG: 全局只读 MES 工序目录中 `9003131004/9003132004` 使用 `B09041`，`9003131008/9003132008` 使用 `A05059`；它们属于当前页面正式读模型，必须随编码修正同步更新。
- BARCODE: 租户 `122` 没有 `biz_type=400` 的设备条码配置，两台现有设备也没有条码记录；正式 `autoGenerateBarcode` 在配置缺失时直接不生成，本任务保持该正式行为。
- SNAPSHOT: 变更前租户 `122` 未删除设备共 `49` 条；`id=198` 非编码字段 MD5 为 `d03fcab9d173520de79485ab0f4d678e`，`id=202` 为 `9b04a596ce68241a70a32d2a0904d405`。
- CONCURRENCY: 当前存在另一任务的 Playwright `lossreason0807` 页面写入进程，但其目标为损耗原因，不涉及设备台账或本任务目标 ID；本任务不停止、不修改该进程，并使用事务锁和影响行数断言防止目标数据并发漂移。
- RED: desired-state precheck -> FAIL as expected，租户 `122` 的目标编码 `C01017/A05075/B04091` 为 `0` 条，旧编码 `A05059/B09041` 为 `2` 条。
- RED: first transaction attempt -> FAIL as designed，`apply.sql` 第 201 行影响行数断言触发；原因是先读取 `LAST_INSERT_ID()` 的 `SET` 语句将后续 `ROW_COUNT()` 变为 `0`，不是业务前置条件失败。
- ROLLBACK: first transaction attempt -> PASS，连接在 `COMMIT` 前因断言错误退出；复核设备台账仍为 `A05059/B09041`、目标编码仍为 `0` 条、设备工序关系和全局 MES 工序目录仍为旧编码、租户未删除设备仍为 `49` 条。仅 MySQL 自增序列按数据库语义前进到 `980007`，没有生成业务记录。
- FIX: 将插入后的 `ROW_COUNT()` 保存移到 `LAST_INSERT_ID()` 读取之前，保持同一 fail-fast 事务设计。
- GREEN: corrected transaction -> PASS，单事务提交 `id=202 / A05075 / 光固机`、`id=198 / B04091 / 箱型干燥机` 和 `id=980007 / C01017 / 撤压机`；所有事务内精确断言均通过。
- GREEN: post-transaction target uniqueness -> PASS，租户 `122` 的 `A05075/B04091/C01017` 各 `1` 条，旧编码 `A05059/B09041` 均 `0` 条，未删除设备总数由 `49` 增至 `50`。
- GREEN: stable identity and non-code fields -> PASS，光固机仍为 `id=202`、箱型干燥机仍为 `id=198`；非编码字段 MD5 分别保持 `9b04a596ce68241a70a32d2a0904d405` 与 `d03fcab9d173520de79485ab0f4d678e`。
- GREEN: relationship read models -> PASS，`mes_dv_machinery_process` 两条活动关系分别使用 `A05075/B04091`，全局 MES 工序目录及其设备子表共四条活动记录同步为新编码；按稳定设备 ID 的两条工位关系仍各 `1` 条。
- GREEN: barcode and tenant boundary -> PASS，租户 `122` 的设备条码配置仍为 `0`、三台目标设备条码仍为 `0`；租户 `1` 未删除设备仍为 `31`，其它租户不存在五个目标/旧编码的活动记录。
- VERIFY: real frontend E2E -> NOT RUN，当前会话只有 `芋道源码/admin` 的本机默认登录来源；已确认其密码哈希与 `测试租户` 的 `admin/aoteman/codexedhrcell01` 均不相同，历史任务也记录 `测试租户/admin` 登录失败。按 `docs/login-access.md` 未切换租户、未复用数据库令牌、未重置账号密码；本次以事务后真实数据库及正式读模型精确查询作为必需数据验证。
- EXPERIENCE: `project-experience-consolidation` -> PASS，将 DML 后必须先保存 `ROW_COUNT()`、再读取 `LAST_INSERT_ID()` 的可复用门禁合并到 `docs/database-rules.md`，并更新 `docs/experience-index.md`；未新建长期经验文档。
- COMMIT: 并发基线提交 `3cddd9b70` 保存了本任务 `ready_for_closeout` 阶段的三份正式记录；经验门禁提交为 `b30a7f98a`。本任务不重写并发提交历史。
- CLEANUP PREVIEW: `task_closeout.py --task-id 20260807-pressure-pump-equipment-ledger-correction --mode preview` -> PASS，仅计划保留 `task.md/execution-log.md/verification-report.md`，删除临时 `apply.sql`，无 blocked 或 warning。
- CLEANUP APPLY: `task_closeout.py --task-id 20260807-pressure-pump-equipment-ledger-correction --mode apply` -> PASS，仅删除已提交的临时事务脚本 `apply.sql`，三份正式任务记录均保留；当前为主 worktree，无合并或 worktree 删除动作。
- PUSH PREFLIGHT: 默认 GitHub URL 级代理指向未监听的 `127.0.0.1:7890`；Windows 直连 GitHub `443` 可达，一次性清空该 URL 级代理后 `git ls-remote origin HEAD` 成功。未修改全局 Git 配置。
- PUSH BLOCKER: 随后最终推送阶段网络状态恶化；一次性直连 GitHub `443` 超时，一次性使用当前监听的 `127.0.0.1:8902` 则在 TLS 握手阶段失败或连接被重置。多次 `ls-remote/push` 均未成功，未修改全局代理、未切换 remote、未把本地 ahead 状态误报为已推送。

## Milestone Updates

- M1 completed：任务记录、BDD、预期验证、设计约束和并发 Git 基线已记录。
- M2 completed：目标设备、关联表、全局 MES 工序目录、条码配置和新增设备必填元数据已核对。
- M3 completed：变更前精确行、关联计数、非编码字段 MD5、租户总数和回滚条件已记录。
- M4 completed：第二次事务执行通过全部 fail-fast 断言并提交三项主数据变更；第一次脚本错误已验证整体回滚。
- M5 completed：目标唯一性、旧编码清除、稳定 ID、非编码字段、设备工序、全局 MES 工序目录、工位关系、条码和租户边界均通过只读核对。
- M6 pending：长期经验合并、cleanup preview/apply 和正式记录提交已完成；`int_main` 最终推送被 GitHub 网络连接阻塞。

## Blockers

- 非完成阻塞：当前会话缺少 `测试租户` 有效登录凭据，因此未执行真实页面只读复验；该缺口不影响已完成的本地数据库事务和精确数据校验，但不能宣称页面 E2E 已通过。
- Closeout blocker：GitHub 直连和当前本地代理均无法完成 TLS 连接，当前分支仍领先 `origin/int_main`；按项目 Git Policy 保持 `ready_for_closeout`。
