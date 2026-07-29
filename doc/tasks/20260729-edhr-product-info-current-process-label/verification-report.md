# Verification Report

## Result

PASS。选择“产品信息”工序后，填写页顶部“工序”显示“产品信息”，不再显示来源“粗洗工序”。

## Root Cause

产品信息任务为保持业务追溯，仍保存来源工序的 `routeProcessId/processName`。旧顶部标签直接读取 `execution.value.processName/processCode`，没有按当前 `batchTaskId` 使用产品信息虚拟工序显示名称，因此卡片已切换而标签仍显示粗洗。

## Fix

- 新增可选的当前批次任务解析函数，继续按 `batchTaskId`、execution、work task 和正式任务映射识别当前任务。
- 顶部标签解析当前任务后复用 `resolveAssistProcessSwitchItemName`，与工序卡片、填写人范围使用同一产品信息虚拟工序口径。
- 保留必选任务解析的 fail-fast 异常；未引入 fallback、异常吞噬或后端来源字段改写。

## Automated Verification

- 聚焦静态合同：PASS。
- 5 项相邻填写页合同：PASS。
- 批次详情产品信息虚拟工序合同：PASS。
- `pnpm ts:check`：PASS。
- Bug regression evidence validator：PASS。

## Real E2E

- 环境：本机 `int_main`，前端 `8081` HTTP 200，后端 `48081` health `UP`。
- 身份：`芋道源码/admin`，只读路径，官方登录前置通过。
- 样本：批次执行 `900000000910`，粗洗任务 `7231`，产品信息任务 `7232`。
- 页面路径：从粗洗填写页点击“切换工序”，选择唯一“产品信息”卡片。
- 断言：
  - 顶部标签从“粗洗工序”更新为“产品信息”。
  - 产品信息卡片切换后为当前项。
  - 3 个填写人候选全部属于任务 `7232`。
  - MES 写请求 `0`。
  - MES HTTP 错误 `0`。
  - console error `0`。

## Artifacts

- E2E 结果 JSON、成功截图和首次等待条件不足的失败截图已在任务记录写入关键断言后，由 `task-closeout-cleanup` 按任务候选清理。
- 保留本报告中的样本、页面路径、断言和无写请求证据。

## Git Evidence

- 开始前脏工作区基线：`067f0ce3`。
- 实现和聚焦测试实际进入并行基线提交：`83191bd4`。
- 本报告、任务日志和经验门禁由收尾提交 `807c2b25` 承载。
- `git push origin int_main` 已成功，远端已快进包含上述提交。

## Residual Risk

本次未修改后端任务来源、批记录表单绑定、表单槽位或填写权限。剩余风险限于未来新增虚拟工序时未复用统一显示工序解析；长期门禁已补充该约束。
