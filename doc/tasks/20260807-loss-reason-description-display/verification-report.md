# 验证报告

## 验证结论

- PASS：工序配置“损耗原因”列现在只显示正式损耗描述 `reasonName`，不显示 `reasonCode`。
- 变更未引入 fallback、降级、吞异常、默认成功或兼容分支。

## BDD / TDD 证据

- BDD: 工序配置显示损耗描述 -> Given 工序已关联具有正式描述和编码的损耗原因；When 用户查看工序配置表格；Then “损耗原因”列只显示正式损耗描述，不显示编码。
- RED: `node tests/e2e/team-leader-process-config-unified-static.spec.cjs` -> FAIL，旧模板仍渲染 `reason.reasonCode`。
- GREEN: `node tests/e2e/team-leader-process-config-unified-static.spec.cjs` -> PASS。
- Bug regression evidence validator -> PASS；validator self-test -> PASS。

## 初始回归验证

- 并发改动到达前，`node tests/e2e/production-leader-function-tabs-static.spec.js` -> PASS。
- 并发改动到达前，`node tests/e2e/team-leader-loss-reason-auto-code-dialog-static.spec.cjs` -> PASS。
- `pnpm ts:check` -> PASS。
- `git diff --check -- <task-owned paths>` -> PASS；仅有 LF/CRLF 提示，无空白错误。

## 并发改动后的最终验证

- `node tests/e2e/team-leader-loss-reason-description-static.spec.cjs` -> PASS；当前展示块只显示 `reasonName`，负向禁止 `reasonCode`。
- `pnpm ts:check` -> PASS。
- 任务范围 `git diff --check` -> PASS；仅有 LF/CRLF 提示。
- 共享工序配置合同当前先失败于并发任务新增的 `standardText/targetValue` API 断言；两个旧损耗操作合同当前失败于并发任务改成统一“维护损耗”弹窗。上述失败不触及本任务独立展示块合同，未修改或回滚并发实现。

## 真实页面验证

- 运行态：`int_main`，前端 `8081` HTTP 200，后端 `48081` health `UP`；监听进程归属 `E:\IntRuoyi`。
- 身份：本机 `芋道源码/admin`，只读验证。
- 路径：`/mes/pro/process-pool/team-leader` -> “工序配置”。
- 正式接口返回损耗原因 267 条；页面可见描述命中 267 条；页面显示编码命中 0 条。
- MES 写请求 0；`pageErrors=[]`；`consoleErrorCount=0`。
- 截图：`IntRuoyiFronted/output/playwright/loss-reason-description-display/process-config-loss-reasons.png`（收尾临时证据，cleanup 删除）。

## 非本任务失败

- `node tests/e2e/process-loss-reason-maintenance-static.spec.cjs` -> FAIL：旧合同仍要求已被统一工序配置表替代的 `data-team-leader-loss-reason-tab`。该失败不触及本次展示块，已保留证据且未扩大修改范围。
- cleanup 后并发任务修改了同一页面与共享合同；最终复查还发现共享工序配置合同及两个旧损耗操作合同与并发实现暂时不一致，已由独立聚焦合同隔离并在执行日志记录精确失败点。

## 最终结果

- 本次需求验证通过；第一次 cleanup 已删除临时脚本、缺陷临时证据和截图目录。并发改动后增加正式独立聚焦合同并完成重新验证；第二次 cleanup preview/apply 均为 `delete=<none>`、`blocked=<none>`、`warnings=<none>`。
