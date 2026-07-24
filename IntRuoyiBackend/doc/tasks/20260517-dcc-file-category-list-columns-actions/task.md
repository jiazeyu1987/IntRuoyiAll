# Task: DCC 文件类别列表删除接口补齐

## Goal

为 DCC 文件类别列表页补齐支持真实删除的后端能力，并确保删除前置校验会明确暴露子类别或受控文件引用等阻塞原因。

## Scope

- 核对上一条后端任务状态后再开始本次改动。
- 在开始生产代码修改前创建本任务文档、执行日志和后端验证记录。
- 为 DCC 文件类别提供真实删除接口，不引入 fallback、假删除或静默成功。
- 删除前必须校验至少以下真实阻塞：存在子类别、存在受控文件引用。
- 删除时同步清理该类别的目录绑定、权限规则、分发规则、培训规则和审批路线关联数据。

## Previous Task Check

- Previous backend task: `doc/tasks/20260517-compare-machinery-ledger-with-final-excel/task.md`
- Status before this task: completed.
- Impact: no unfinished backend task blocks this DCC category deletion change.

## BDD

BDD: 未被引用的文件类别可以删除 -> Given 某文件类别没有子类别且没有受控文件引用 / When 管理员发起删除 / Then 后端删除该类别及其关联治理配置并返回成功。

BDD: 有子类别的文件类别禁止删除 -> Given 某文件类别下仍存在子类别 / When 管理员发起删除 / Then 后端明确返回删除阻塞错误，而不是部分删除。

BDD: 被受控文件引用的文件类别禁止删除 -> Given 某文件类别已经被受控文件或主文件记录引用 / When 管理员发起删除 / Then 后端明确返回删除阻塞错误，而不是静默成功。

## Milestones

- [x] M1: 记录 RED 测试，覆盖允许删除与阻塞删除场景。
- [x] M2: 实现删除接口、服务逻辑和错误码。
- [x] M3: 运行 GREEN 测试并更新验证证据。
- [x] M4: 与前端联调删除流程。
- [x] M5: 在验证通过后整理本任务相关变更并准备提交。

## Expected Verification

- `mvn -pl yudao-module-dcc -am test -Dtest=DccFileCategoryAdminServiceImplTest`

## Current Status

Completed. The backend now exposes `DELETE /dcc/file-categories/{id}`, blocks deletion when child categories or controlled-file references exist, and supplies the category list with signoff/approval position ids for frontend approver rendering.

## Final Verification

- `mvn -pl yudao-module-dcc -am test -Dtest=DccFileCategoryAdminServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS
- `mvn -pl yudao-server -am -DskipTests package` -> PASS
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS
- Runtime integration result:
  - backend list API served approver position ids used by the frontend
  - live page `http://127.0.0.1:8081/dcc/controlled-file/categories` rendered 审核/批准 users and opened the 删除 confirmation dialog

## Blockers

None.
