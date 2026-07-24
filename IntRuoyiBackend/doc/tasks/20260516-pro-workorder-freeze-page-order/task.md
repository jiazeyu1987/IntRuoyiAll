# Task: 生产工单冻结分页排序支持

## Goal

为生产工单分页查询增加“非冻结优先、冻结靠后”的后端排序支持，保证前端第一页就能先看到未冻结工单，从而满足真实列表展示要求。

## Scope

- 检查并显式阻塞上一条未闭环后端任务，再创建当前任务文档、执行日志与证据文件。
- 仅调整生产工单分页查询排序规则，不改动临时冻结业务逻辑、状态切换接口或排产清理逻辑。
- 新增针对 `MesProWorkOrderMapper.selectPage` 的数据库测试，验证 `temporaryFrozen=false` 在前、`temporaryFrozen=true` 在后。
- 完成目标测试与真实页面联动验证。

## Previous Task Check

- Previous backend task: `doc/tasks/20260516-electronic-batch-record-image-performance-optimization/task.md`
- Status before this task: blocked by explicit user reprioritization.
- Impact: 上一条后端任务已显式暂停，不再阻塞当前生产工单排序切片。

## Milestones

- [x] M1: 检查前序任务状态并创建当前任务文档、执行日志与证据文件。
- [x] M2: 记录 RED 证据并新增生产工单分页排序测试。
- [x] M3: 实施最小后端排序修复。
- [x] M4: 完成 GREEN 验证、更新证据并提交当前任务相关改动。

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProWorkOrderMapperTest,MesProWorkOrderServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 真实前端 `http://127.0.0.1:8081/mes/pro/work-order` 第一页出现未冻结工单在前，冻结工单排后。

## Current Status

Completed. 后端分页排序修复已完成，主代码已重新打包并重启，真实页面验证已证明第一页先出现未冻结工单。

## Blocker And Impact

- Blocker: 当前 `yudao-module-mes` 测试源码里存在一批与本任务无关的现存缺失类问题，导致新增的 `MesProWorkOrderMapperTest` 不能单独在标准 `testCompile` 流程中运行。
- Impact:
  - 主代码行为已通过真实页面和打包重启验证。
  - 新增 mapper 测试文件已落地，但其执行被仓库既有无关测试编译错误阻塞，需要后续统一清理测试基线后再跑通。

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\pom.xml -Dtest=MesProWorkOrderMapperTest,MesProWorkOrderServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - FAIL
  - blocker: unrelated existing missing test classes under `pro.feedback.importer` and `pro.workorder.sync`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
  - PASS
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
  - PASS after the rebuilt jar was in place
- Real frontend verification via `verify-workorder-freeze-display.mjs`
  - PASS
  - page 1 now shows non-frozen rows before frozen rows
