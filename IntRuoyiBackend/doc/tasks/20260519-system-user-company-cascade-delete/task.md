# Task: 用户管理公司树级联删除

## Goal

让系统用户管理中的“公司/部门”删除能力支持真实组织树场景：

- 删除选中的公司时，同时删除该公司及其下所有子部门。
- 前提是该公司树内所有员工都已经提前删除；只要任一层级仍存在员工，必须失败并阻止删除。
- 保持 fail-fast，不引入 fallback、兼容分支或静默跳过。

## Scope

- `yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/dept/**`
- `yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/mysql/user/**`
- `yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/enums/ErrorCodeConstants.java`
- `yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/dept/**`
- `doc/tasks/20260519-system-user-company-cascade-delete/**`

## Non-Goals

- 不改动部门创建、编辑或排序逻辑。
- 不新增 mock 数据、补偿删除或“部分成功”分支。
- 不在后端偷偷忽略仍有员工的部门。

## Previous Task Check

- Previous same-repository task: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-dcc-notify-message-delivery\task.md`
- Status before this task: blocked on 2026-05-19 due to user priority switch in the current thread.
- Companion frontend task: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-system-user-company-cascade-delete\task.md`

## Milestones

- [x] M1: 先检查并处理上一个后端任务状态，创建本任务文档与执行日志。
- [x] M2: 记录 BDD 场景并补后端 RED 测试，覆盖“空公司树可级联删除 / 任一层有员工则失败”。
- [x] M3: 实现最小后端删除逻辑与 fail-fast 错误提示。
- [x] M4: 运行目标后端测试并补齐 evidence。
- [x] M5: 执行收尾预览，准备仅包含本任务改动的提交。

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=DeptServiceImplTest" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-system-user-company-cascade-delete\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-system-user-company-cascade-delete --mode preview`

## Current Status

Completed. Backend tree-delete logic, targeted tests, and companion frontend wiring have all been finished for this task.

## Final Verification Result

- PASS: `mvn --% -pl yudao-module-system -Dtest=DeptServiceImplTest test`
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-system-user-company-cascade-delete\backend-api-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-system-user-company-cascade-delete --mode preview`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-system-user-company-cascade-delete --mode apply`
