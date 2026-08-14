# Remove Team Scope Employee Process Check

## Task Goal

去掉触发错误文案 `班组长不在该员工或工序的负责范围内` 的判定逻辑，使班组长在人员新增/关联阶段不再被员工或工序负责范围拦截；保留后续真实工序、报工、复核等业务动作的正式范围校验。

## Milestones

- [x] 建立任务记录和 BDD/TDD 验收口径
- [x] 定位并复现负责范围拦截
- [x] 用 RED 回归测试固定“新增/关联不再拦截”
- [x] 移除目标判定逻辑并保持其它范围校验
- [x] 运行聚焦验证并记录结果
- [ ] 收尾提交、推送并更新任务状态

## Expected Verification

- `rg` 定位错误码和调用点
- 目标后端单测 RED/GREEN
- `mvn -pl yudao-module-mes -am "<targeted test args>" test`
- `git diff --check -- <task paths>`

## Current Status

ready_for_closeout

实现和聚焦验证已完成；最终提交/推送暂未执行，因为当前 `int_main` 工作区已有大量并行任务未提交/未跟踪改动，不能把非本任务文件混入本任务提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，删除新增/关联阶段错误的负责范围校验，不用前端绕过或默认成功
- `是否存在临时补丁或绕过`：否

## Applicable Gates

- MES 生产人员档案正式工重复关联门禁：新增正式工/临时工档案只建立当前组长名下档案，不得要求该员工已在负责员工范围内；工序绑定、报工列表/详情、复核和确认才按员工或工序负责范围校验。

## Cleanup Keep

- doc/tasks/20260806-remove-team-scope-employee-process-check/task.md
- doc/tasks/20260806-remove-team-scope-employee-process-check/execution-log.md
- doc/tasks/20260806-remove-team-scope-employee-process-check/verification-report.md
- doc/tasks/20260806-remove-team-scope-employee-process-check/team-scope-denied-contract.cjs
