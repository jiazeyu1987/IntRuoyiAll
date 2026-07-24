# 任务：融合发布门禁验证工具到 int_main

## 任务目标

- 将后端任务分支 `task/20260524-release-readiness-gates-dev` 中的 G6-G11 发布门禁验证工具、测试和任务证据融合进 `int_main`。
- 确认前端同名任务分支是否仍需合并，避免重复提交或引入无关变更。
- 合并后在 `int_main` 上重新运行发布门禁脚本契约验证，并保持整体发布结论为真实状态。

## 非目标

- 不执行正式服发布、回滚、恢复数据、重启服务或发送真实告警 webhook。
- 不修改 G6-G11 当前业务结论；缺少真实前置条件时继续失败关闭并记录 `BLOCKED`。
- 不把其他未授权 worktree 分支融合进 `int_main`。

## 前置任务检查

- 后端主工作区分支：`int_main`。
- 前端主工作区分支：`int_main`。
- 后端任务分支和 `int_main` 已分叉，不能快进，需要受控 merge。
- 前端任务分支已是 `int_main` 的祖先，无需再次合并。
- 最近检查到的后端任务 `20260525-runtime-control-ops-console`、`20260525-runtime-control-publish-scope`、`20260525-test-publish-backend-heap-guard` 均为 `completed`。

## BDD 场景

- BDD: 后端发布门禁工具进入 int_main -> Given reviewer 要在主分支复用 G6-G11 发布门禁, When 后端任务分支融合进 `int_main`, Then `script/release-readiness` 工具、模板和对应 pytest 契约测试必须存在并通过。
- BDD: 前端无重复融合 -> Given 前端任务分支已经被 `int_main` 包含, When 执行融合检查, Then 不产生新的前端 merge commit 或重复改动。
- BDD: 发布结论不被合并掩盖 -> Given G6-G11 仍缺真实登录、责任人、webhook 或高风险动作确认, When 合并后运行阻塞态验证, Then 工具必须返回 `BLOCKED` 且不得发送 webhook 或执行生产动作。

## 里程碑

- [x] M1：建立任务文档，完成前置分支和前一任务状态检查。
- [x] M2：记录 RED 证据，证明当前 `int_main` 缺少发布门禁契约测试。
- [x] M3：将后端任务分支受控 merge 到 `int_main`，前端确认无需合并。
- [x] M4：在合并后的 `int_main` 上运行发布门禁契约与阻塞态验证。
- [x] M5：提交前执行 closeout 预览并报告结论。

## 预期验证

- RED：`python -X utf8 -m pytest script\tests\test_release_readiness_g6_g7_tooling.py script\tests\test_release_readiness_g8_g9_contracts.py script\tests\test_release_readiness_g10_g11_contracts.py -q` 在合并前失败，原因是测试文件不存在。
- GREEN：合并后运行同一 pytest 命令通过。
- GREEN：合并后运行包含发布 go/no-go 与测试服发布脚本契约的扩展回归通过。
- GREEN：合并后运行 G6/G7、G8/G9、G10/G11 阻塞态验证，确认缺少真实前置条件时失败关闭且不执行生产动作。

## 当前状态

- 状态：completed
- 已完成：
  - 已确认前端同名任务分支为 `int_main` 祖先，无需合并。
  - 已确认后端同名任务分支与 `int_main` 分叉，需要受控 merge。
  - 已将后端任务分支以 `--no-commit` 方式融合到 `int_main`，自动合并无冲突。
  - 合并后的发布门禁 pytest 契约已通过，54 个测试通过。
  - G6/G7、G8/G9、G10/G11 阻塞态验证均按预期返回 `BLOCKED`，未发送 webhook，未执行发布、回滚或恢复动作。
  - 项目根 BDD/TDD acceptance plan 校验已通过。
- 阻塞与影响：
  - 无合并阻塞。
  - 发布业务结论仍为 `BLOCKED`，因为 G6-G11 的真实登录证据、责任人确认、webhook 配置和高风险动作确认仍未补齐。
