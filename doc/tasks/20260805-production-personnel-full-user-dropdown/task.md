# 生产人员正式工全量用户下拉

## Task Goal

将生产组长“新增人员”中的正式工远程下拉从“当前组长负责部门及子部门用户”调整为“全量系统用户”，并保证下拉选中的有效系统用户可以完成正式工关联。

## Milestones

- [x] M0：建立任务记录，读取后端、任务收尾和适用经验门禁。
- [x] M1：补充全量用户搜索与跨部门正式工关联的 BDD/RED 测试。
- [x] M2：实现系统全量用户候选搜索和正式工关联校验。
- [x] M3：运行定向后端回归、技能证据校验和差异检查。
- [ ] M4：完成经验沉淀、cleanup、提交并推送。

## Expected Verification

- RED：现有实现无法返回当前生产组长负责部门之外的系统用户，且跨部门正式工关联被范围校验拒绝。
- GREEN：`MesTeamLeaderRuntimeConfigServiceTest` 覆盖全量系统用户候选搜索、关键字过滤、跨部门有效用户关联、无效用户失败和重复关联保护。
- REGRESSION：`MesProcessPoolTeamLeaderControllerTest` 与生产人员相关后端定向测试通过。
- CONTRACT：后端 API evidence validator、`git diff --check` 通过。
- CLOSEOUT：任务证据完成、cleanup preview/apply 通过、提交并推送当前 `int_main` 到 `origin`。

## Current Status

blocked

功能实现、定向回归、evidence validator 和 cleanup 已通过，且实现已由并发提交 `633361dde` 推送到 `origin/int_main`。最终记录提交被 `E:\IntRuoyi\.git\index.lock` 阻塞：锁文件非空（1,441,792 字节），项目规则禁止删除非空锁；待共享仓库索引恢复后继续 M4。

## Blockers

- `git add -- <task-owned paths>` 返回 `Unable to create 'E:/IntRuoyi/.git/index.lock': File exists`。
- 该锁由并发 Git 操作产生，持续存在且非空；已等待并确认原并发 commit/worktree-add 进程退出，但不得按“零字节陈旧锁”流程删除。
- 影响：本任务 closeout 文档与经验文档尚未形成最终本地提交和推送；不影响已进入 `origin/int_main` 的功能实现与测试代码。

## Applicable Gates

- BDD/TDD：先记录 Given/When/Then，再执行 RED -> GREEN -> REGRESSION。
- 全量候选与写入契约必须一致：下拉可见的有效系统用户必须能够关联，不能只放开前端或候选接口。
- 正式工重复关联必须在写库前返回业务错误，不得退化为数据库唯一键异常。
- 权限不变：Controller 继续要求 `mes:pro-process-pool-team-leader:maintain`。
- 无 fallback：不得在全量查询失败时回退到下属列表、空成功或前端本地过滤。
- 当前主工作区已有未提交改动，按项目规则先创建独立脏工作区基线提交并记录文件清单。
- 共享分支并发：实现被并行基线提交 `633361dde` 纳入后，必须保留该事实，不得把并行提交伪装成本任务独立实现提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，统一调整正式工候选读取与关联校验的数据范围。
- `是否存在临时补丁或绕过`：否。
