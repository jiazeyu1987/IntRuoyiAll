# 任务：MES 报工归属草稿保存与再次归属后端改造

## 任务目标

- 后端首次归属只保存归属结果并生成草稿正式报工，不自动提交，不在归属时回写排产进度。
- 为正式报工增加 `source_import_record_id` 稳定关联，为缓存池消费补齐可回滚分配台账，并提供再次归属接口。
- 严格限制“修改归属”只发生在链路完整、正式报工仍为草稿、且本记录创建缓存池未被他人继续消费的场景。
- 自动重排 / 重排发布阶段必须正确处理“存在受保护旧任务且仍有剩余报工量”的场景：保留旧任务，同时为剩余量继续生成活动任务；只有确实无法生成承接任务时才前置阻断，避免用户看到重排成功后再在归属时报错。
- 本机标准重启 / schema 保底流程必须自动补齐 `mes_pro_feedback.source_import_record_id` 及索引，避免代码已升级但运行库仍停留旧结构时再次触发 `Unknown column`。

## 当前状态

COMPLETED

## 上一任务检查

- 后端上一相关任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-feedback-attribution-active-task-candidate-regression\task.md`
- 当前状态：`已完成`
- 处理说明：该任务已修复候选误暴露无活动任务工序问题，本次在其基础上继续扩展归属保存与修改逻辑。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 本轮先做本机后端代码、SQL 与定向单测，不做服务器写入或真实 E2E。
  - 若后续进入真实写入、长链路验证或本机数据库 schema 写入，必须先在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。历史链路缺失、正式报工状态不允许修改或缓存池已被他人消费时必须直接失败。
- `是否从根因和长期维护角度解决`：是。通过持久化来源关联、统一应用归属逻辑与完整回滚台账解决“保存即提交、不可修改”根因。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 首次归属生成草稿正式报工 -> Given 导入记录首次归属到一个或多个排产工序 / When 调用归属保存接口 / Then 系统创建 source_import_record_id 关联的 PREPARE 正式报工，不自动 submit，也不触发排产进度同步。`
- `BDD: 再次归属整体回滚重建 -> Given 导入记录已归属且关联正式报工全部为 PREPARE、链路完整 / When 调用再次归属接口 / Then 系统先删除旧草稿正式报工、恢复被本记录消费的缓存池、删除本记录创建的池与分配记录，再按新选择重建。`
- `BDD: 非草稿态拒绝修改 -> Given 某导入记录关联的任一正式报工状态不是 PREPARE / When 调用再次归属接口 / Then 系统拒绝修改并返回明确原因。`
- `BDD: 关联缓存池已被他人消费时拒绝修改 -> Given 本记录创建的残余池或超产池已被其他导入记录继续消费 / When 调用再次归属接口 / Then 系统拒绝修改并返回明确原因。`
- `BDD: 提交正式报工后同步排产进度 -> Given 正式报工处于 PREPARE 并关联排产工单 / When 用户手动提交正式报工 / Then 状态进入 APPROVING 后触发排产进度同步。`
- `BDD: 受保护旧任务存在时仍可对剩余量续排 -> Given 某排产工单工序已有已完成/已报工等受保护任务，且 remainingQuantity 仍大于 0 / When 用户预览或发布重排结果 / Then 系统保留原受保护任务，同时为剩余量继续生成新的活动任务，不得把整次重排直接阻断。`
- `BDD: 仅在剩余量无法生成承接任务时阻断重排 -> Given 某排产工单工序 remainingQuantity 仍大于 0，且系统在当前范围内无法为该剩余量生成活动任务 / When 用户预览或发布重排结果 / Then 系统必须在排产阶段直接阻断，避免用户到归属保存时才发现无法正式报工。`
- `BDD: 本机重启必须补齐正式报工来源列 -> Given 本机历史 MES 库缺少 mes_pro_feedback.source_import_record_id / When 执行标准本机重启脚本或本地 schema 保底流程 / Then 系统先幂等补齐该列与索引，再启动后端，正式报工查询不再因 Unknown column 失败。`

## 里程碑

1. M1：补后端任务包与 RED 测试。
2. M2：实现 schema / DO / Mapper / VO / 接口扩展。
3. M3：实现首次归属草稿化、再次归属回滚重建、提交后进度同步。
4. M4：补自动重排“活动任务承接剩余量”源头阻断回归。
5. M5：运行 GREEN 测试并回写证据。
6. M6：补本机 runtime schema guard、修复真实本地库并回写缺陷 / schema 证据。

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT source_import_record_id FROM mes_pro_feedback LIMIT 1;"`

## 最终验证结果

- `mvn --% -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest,MesProFeedbackServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，33 个报工归属 / 正式报工回归通过。
- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，33 个自动排产 / 重排回归通过。
- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldKeepFinishedTaskAndGenerateNewActiveTaskForRemainingQuantity+replanApply_shouldKeepFinishedTaskAndCreateNewActiveTaskForRemainingQuantity -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，验证“剩余报工量仍可重排续排”的定向缺陷回归。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-mes-feedback-attribution-inline-edit\bug-regression-evidence.md` -> PASS。
- `python -X utf8 -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q` -> PASS，`11 passed`。
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "DESCRIBE mes_pro_feedback; SELECT source_import_record_id FROM mes_pro_feedback LIMIT 5; SHOW INDEX FROM mes_pro_feedback WHERE Key_name = 'idx_mes_pro_feedback_source_import_record_id';"` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260626-mes-feedback-attribution-inline-edit --mode preview` -> PASS，`status=ready`。

## 阻塞与影响

- 当前无新增外部阻塞。
- 本次恢复推进范围仅限 MES 报工归属 / 自动重排相关后端代码、定向单测、任务文档与证据；不混入菜单重组或其他模块改动。
