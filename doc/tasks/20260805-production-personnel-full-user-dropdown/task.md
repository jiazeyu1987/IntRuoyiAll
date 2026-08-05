# 生产人员正式工全量用户下拉

## Task Goal

将生产组长“新增人员”中的正式工远程下拉从“当前组长负责部门及子部门用户”调整为“全量系统用户”，并保证下拉选中的有效系统用户可以完成正式工关联。

## Milestones

- [x] M0：建立任务记录，读取后端、任务收尾和适用经验门禁。
- [ ] M1：补充全量用户搜索与跨部门正式工关联的 BDD/RED 测试。
- [ ] M2：实现系统全量用户候选搜索和正式工关联校验。
- [ ] M3：运行定向后端回归、技能证据校验和差异检查。
- [ ] M4：完成经验沉淀、cleanup、提交并推送。

## Expected Verification

- RED：现有实现无法返回当前生产组长负责部门之外的系统用户，且跨部门正式工关联被范围校验拒绝。
- GREEN：`MesTeamLeaderRuntimeConfigServiceTest` 覆盖全量系统用户候选搜索、关键字过滤、跨部门有效用户关联、无效用户失败和重复关联保护。
- REGRESSION：`MesProcessPoolTeamLeaderControllerTest` 与生产人员相关后端定向测试通过。
- CONTRACT：后端 API evidence validator、`git diff --check` 通过。
- CLOSEOUT：任务证据完成、cleanup preview/apply 通过、提交并推送当前 `int_main` 到 `origin`。

## Current Status

in_progress

已确认现有实现直接调用 `AdminUserApi.getUserListBySubordinate`，导致候选和关联均被当前组长部门下属范围限制。

## Applicable Gates

- BDD/TDD：先记录 Given/When/Then，再执行 RED -> GREEN -> REGRESSION。
- 全量候选与写入契约必须一致：下拉可见的有效系统用户必须能够关联，不能只放开前端或候选接口。
- 正式工重复关联必须在写库前返回业务错误，不得退化为数据库唯一键异常。
- 权限不变：Controller 继续要求 `mes:pro-process-pool-team-leader:maintain`。
- 无 fallback：不得在全量查询失败时回退到下属列表、空成功或前端本地过滤。
- 当前主工作区已有未提交改动，按项目规则先创建独立脏工作区基线提交并记录文件清单。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，统一调整正式工候选读取与关联校验的数据范围。
- `是否存在临时补丁或绕过`：否。
