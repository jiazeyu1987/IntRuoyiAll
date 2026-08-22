# 流程修复 2：一线生产提交与生产组长复核边界

## 任务目标

实现一线生产提交、生产组长复核、工序分配只形成正式生产来源事实；达到工序目标只更新进度投影，不触发活跃订单完成、三类回填、批次执行、材料上传或放行。

## 修改边界

- 修改流程2相关后端服务和测试，并修复阻断该专项完整编译的 ERP 合同测试、MES QA 响应/保存 DTO 与其测试契约；任务证据保留在本目录。
- 不修改数据库、不启动服务、不运行写入型 E2E。
- 流程4拥有唯一完成命令和三类回填；流程6建批；流程7映射追溯；流程8上传来料检报告、灭菌报告、成品检报告、成品检记录；流程9约束多入口；流程10最终放行；流程11总体验证、迁移和回归门禁。

## 里程碑

- [x] M1 规则、现状和任务契约审计
- [x] M2 流程2代码边界修复
- [x] M3 BDD/TDD 与跨线程合同验证
- [x] M4 独立 worktree 验证与证据收尾
- [x] M5 完整 reactor 编译阻断修复与回归

## 预期验证

- 提交、复核、分配阶段各自只写本阶段事实，并具备版本、幂等和正式来源 ID。
- 复核/分配达到目标时不调用 completeAndBackfill，不新增或修改本次三类回填和批次执行。
- 一线提交不写正式批记录；记录簿 payload 只保留为流程2来源快照，正式批记录延迟至流程4完成节点。
- 跨线程字段包含 productionFactEventId、reviewEventId、allocationEventId、submissionVersion、reviewVersion、allocationVersion、payloadHash、signatureSnapshot、activeOrderId、workOrderId、pickListBindingId、routeVersionId。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；恢复缺失的正式 QA 设备字段/类型并修正错误的测试契约调用。
- 是否存在临时补丁或绕过：否；隔离 POM 已清理。

## Current Status

ready_for_closeout

流程2边界实现与编译阻断修复已完成。流程2来源快照已与正式记录簿写入 service 解耦；ERP test-compile、MES 2783 个主源码编译、MES 488 个测试源码编译，以及流程2/相邻 QA 测试共 108 项均通过。服务、数据库和写入型 E2E 仍按范围未运行。task-closeout-cleanup preview/apply 已执行，删除项为 0；两个预置临时路径均不存在。提交/融合尚未执行：强制端口保护门禁因官方注册表存在其它任务的越界活动槽位 `slot=31` 而失败，禁止绕过或修改该并行任务登记。

## Cleanup Keep

- `doc/tasks/20260821-flow-repair-02-production-submit-review-boundary/development-plan.md`
- `doc/tasks/20260821-flow-repair-02-production-submit-review-boundary/test-plan.md`
- `doc/tasks/20260821-flow-repair-02-production-submit-review-boundary/backend-api-evidence.md`

## Cleanup Candidates

- `IntRuoyiBackend/yudao-module-mes/flow2-verification-pom.xml`
- `IntRuoyiBackend/yudao-module-mes/target/flow2-verification/`
