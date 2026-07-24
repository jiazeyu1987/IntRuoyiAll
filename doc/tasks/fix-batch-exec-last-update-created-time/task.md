# 修复批次执行列表初始最后更新时间

## Task Goal

修复批次执行列表中“最后更新时间”列的初始数据：批次执行 row 刚创建且尚未被业务更新时，应显示该 row 的创建时间。

## Milestones

- [x] 定位批次执行列表、接口或数据模型中最后更新时间字段的来源。
- [x] 用回归测试复现“新建 row 初始最后更新时间不等于创建时间”的问题。
- [x] 实施最小正式修复，不引入 fallback、降级或吞异常。
- [x] 运行针对性回归验证并记录 RED/GREEN 证据。

## Expected Verification

- 相关单元测试或静态契约测试先失败后通过。
- 若存在对应前端展示逻辑，确认列字段与修复后的数据契约一致。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；后端列表响应契约补齐 `createTime/updateTime` 并由服务层从批次执行 row 审计字段映射，前端继续读取 `updateTime`。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 任务启动时 `docs\experience-index.md` 缺失；本任务为局部 bug 修复，继续前记录该事实并避免高风险发布、环境或数据操作。
- 追加真实 E2E 收尾时已识别 `docs\experience-index.md`、`docs\e2e-rules.md`、`docs\login-access.md`、`docs\local-runtime.md` 和 `docs\worktree-restrictions.md`，并将本地重启脚本路径门禁沉淀到 `docs\local-runtime.md`。

## Final Verification Result

- 后端目标单测、前端静态契约、diff 空白检查、回归证据校验和 task-closeout-cleanup 均通过。
- 追加真实 E2E 复验通过：本地后端重启到当前 jar 后，浏览器登录批次执行列表，确认“最后更新时间”列可见，真实分页响应 20 行均包含 `createTime/updateTime`，且本页 20 行 `createTime === updateTime`。
- 追加经验沉淀完成：`docs\experience-index.md` 可通过 `Missing int_main frontend path` / `restart-int-ruoyi-local` 路由到 `docs\local-runtime.md#2026-07-24-本地重启脚本路径门禁`。
- Cleanup apply 无删除项，当前为主工作区 `int_main`，未执行 worktree 合并或移除。

## Cleanup Keep

- `doc/tasks/fix-batch-exec-last-update-created-time/bug-regression-evidence.md`
