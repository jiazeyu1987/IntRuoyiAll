# 任务：DCC 测试根目录 3.DMR 彻底删除

## Goal

删除 `DCC目录管理` 中根目录 `3.DMR` 及其全部子目录、目录内受控文件记录、关联运行态业务记录和底层文件存储引用，确保这些测试数据不再出现在真实运行环境中。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-dcc-test-root-hard-delete\**`
- 真实运行态 DCC 数据表中 `3.DMR` 目录子树相关记录
- 与上述目录子树绑定的真实运行态文件存储引用及物理文件删除验证

## Non-Scope

- 不修改 DCC 正式业务目录结构。
- 不新增 fallback、兼容分支或 mock 清理逻辑。
- 不触碰与 `3.DMR` 子树无关的正式受控文件数据。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-batch-audio-token-fix-retry\task.md`
- Status before this task: `Blocked on 2026-05-22 due to user redirect to DCC runtime cleanup`
- Impact: 已按仓库规则显式阻塞旧任务，不阻塞本次高优先级运行态清理。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增本次清理文档、必要脚本和与本次删除直接相关的最小改动，不覆盖无关改动。

## Milestones

1. [x] 创建任务文档并完成删除前盘点，确认 `3.DMR` 子树、受控文件和底层文件引用范围。
2. [ ] 记录真实 BDD 与 RED 证据，锁定“删除前存在、删除后彻底消失”的运行态契约。
3. [ ] 执行最小且可验证的真实数据清理，删除目录子树、相关业务记录及文件存储引用。
4. [ ] 做删除后 GREEN 校验，确认目录树、受控文件和物理文件均已清空。
5. [ ] 运行 closeout preview，完成任务收尾与结果记录。

## Expected Verification

- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-dcc-test-root-hard-delete\scripts\dcc_test_root_cleanup.py --mode audit`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-dcc-test-root-hard-delete\scripts\dcc_test_root_cleanup.py --mode cleanup`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-dcc-test-root-hard-delete\scripts\dcc_test_root_cleanup.py --mode verify`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-test-root-hard-delete open http://127.0.0.1:8081/login?redirect=%2Fdcc%2Fcontrolled-file%2Fdirectories`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-test-root-hard-delete run-code --filename D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-dcc-test-root-hard-delete\scripts\verify-dcc-root-delete-e2e.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-dcc-test-root-hard-delete --mode preview`

## Current Status

Completed on 2026-05-22. 用户已明确确认执行整棵硬删，真实运行态中的两棵根目录 `3.DMR` 已彻底删除：

- `900003`：删除 `2699` 个目录节点、`96` 条分类绑定、`30` 条受控文件记录、`30` 个流程实例及关联 DCC/Flowable 运行态与历史态数据。
- `902702`：删除 `2699` 个目录节点与重复导入空树。
- 底层文件：删除 `5` 个 `infra_file` 元数据与对应对象存储文件。

删除后数据库校验、真实前端页面核验和 closeout preview 均已完成。按 closeout preview 基线，任务临时脚本与临时验证产物已在收尾时清理，只保留任务记录文档。

## Blockers And Impact

- Blocker:
  - 无。
- Impact:
  - `DCC目录管理` 已不再保留 `3.DMR` 根目录。
  - 原先挂载在 `3.DMR` 子树上的 DCC 分类绑定和测试/联调文件流程记录已一并移除。

## Final Verification

- PASS: `python -X utf8 ...dcc_test_root_cleanup.py --mode audit`
  - 删除前范围命中 `2` 棵根目录、`5398` 个目录节点、`96` 条分类绑定、`30` 条受控文件、`5` 个底层文件对象。
- PASS: `python -X utf8 ...dcc_test_root_cleanup.py --mode cleanup`
  - 已清掉全部 DCC 目录树、分类绑定、受控文件、培训/分发/签名/访问日志、Flowable 流程实例与 `act_ge_bytearray` 数据。
- PASS: `python -X utf8 ...dcc_test_root_cleanup.py --mode verify`
  - 所有目标表与目标主键回查均为 `0`，`remainingTotal=0`。
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-test-root-hard-delete run-code --filename ...verify-dcc-root-delete-e2e.mjs`
  - 真实页面 `http://127.0.0.1:8081/dcc/controlled-file/directories` 与实时目录树接口都已不再返回 `3.DMR`。
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-dcc-test-root-hard-delete --mode preview`
  - `status: ready`，仅建议删除任务临时脚本；收尾时已按建议清理。
