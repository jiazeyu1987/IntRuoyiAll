# 任务：测试服发布后端堆内存防回归

## 任务目标

- 防止测试服发布脚本再次把后端 JVM 堆内存覆盖回 `-Xmx512m`。
- 固定测试服发布写入的 `JAVA_OPTS` 为 `-Xms1g -Xmx2g -Djava.security.egd=file:/dev/./urandom`。
- 用脚本契约测试覆盖该运维事故回归风险。

## 非目标

- 不执行真实测试服发布。
- 不修改展厅发布业务逻辑或资产处理链路。
- 不引入流式/分批发布改造。

## 前置任务检查

- 当前 worktree 分支：`task/20260525-runtime-control-ops-console`。
- 前一任务 `20260525-runtime-control-publish-scope` 状态为 `completed`。
- 当前分支未包含既有修复提交 `b8235c5403`，`publish-int-ruoyi-to-test.ps1` 仍写入 `-Xmx512m`，存在回归风险。

## 里程碑

- [x] M1：建立任务文档和 BDD 场景。
- [x] M2：补充测试服发布脚本 JVM 堆内存 RED 契约测试。
- [x] M3：调整测试服发布脚本写入的 `JAVA_OPTS`。
- [x] M4：运行脚本契约、bug evidence validator 和 closeout 预览。
- [x] M5：提交本任务改动。

## BDD 场景

- BDD: 测试服发布保留 2G 后端堆 -> Given 运维人员通过脚本或运行控制台发布测试服, When 脚本生成测试服 `.env`, Then `JAVA_OPTS` 必须为 `-Xms1g -Xmx2g -Djava.security.egd=file:/dev/./urandom`。
- BDD: 测试服发布不得回退到 512m -> Given 展厅手动发布需要处理图片和音频资产, When 测试服发布脚本生成后端运行参数, Then 不得写入 `-Xms512m -Xmx512m`。

## 预期验证

- RED：`python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` 先失败，证明当前分支仍可能覆盖为 512m。
- GREEN：同一脚本契约通过。
- GREEN：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260525-test-publish-backend-heap-guard\bug-regression-evidence.md` 通过。

## 当前状态

- 状态：completed
- 已完成：
  - 已建立任务文档和 BDD 场景。
  - 已补充测试服发布脚本 JVM 堆内存 RED 契约测试。
  - 已将测试服发布脚本生成的 `JAVA_OPTS` 调整为 `-Xms1g -Xmx2g -Djava.security.egd=file:/dev/./urandom`。
  - 脚本契约测试和 bug evidence validator 已通过。
  - 已运行 task-closeout-cleanup 预览；预览因主 worktree 状态阻止 apply，未执行清理应用。
- 阻塞与影响：
  - task-closeout-cleanup apply 阶段未执行：后端主 worktree dirty 且当前 linked worktree 不能快进合并到 `int_main`；这只影响自动清理/合并 worktree，不影响本任务代码交付。
