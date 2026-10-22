# SA36—SA38 精确本地融合方案

## Current Status
in_progress — 精确1项经验基线、9项修复相关文件已按本轮授权本地融合，另5份记录独立提交；下一轮完整八方向静态审查待执行。

## 目标与范围

先独立提交当前main中本任务相关docs/backend-development.md经验基线1项，再精确提交融合8项业务实现/测试及1项既有经验文档（共9项），另提交5项验收/融合记录。源码、树及SHA以同名JSON为准；其他前端、docs/worktree-memory.md及其他任务资产均保留。仅本地，不推送、数据库、共享服务、E2E、cleanup apply或worktree删除。

| 精确文件 | 类型 |
|---|---|
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcLeaderPersonnelServiceImpl.java | modified |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java | modified |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderOrderProcessCompletionService.java | modified |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/MesSa36Sa38MapperFixture.java | added |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceTest.java | modified |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandServiceTest.java | modified |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesSa37ReworkReleaseTransactionTest.java | added |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderOrderProcessCompletionServiceTest.java | modified |
| docs/backend-development.md | modified-memory |

## 验证

worker2669例全通过；经理39类709例全通过、12120新输入绑定一致、7462编译来源核验通过；三项语义验收PASS。主干/worker/经理三个端口合同guard实际通过。私有baselineTree只含经验基线1项，candidateTree只含精确9项，diff-tree --check通过；真实index与所有refs不变，没有新commit。

## 记录提交清单

- doc/tasks/20261003-edhr-thread-management/sa36-sa38-manager-verification-report.md
- doc/tasks/20261003-edhr-thread-management/sa36-sa38-manager-review-result.json
- doc/tasks/20261003-edhr-thread-management/sa36-sa38-local-integration-plan.md
- doc/tasks/20261003-edhr-thread-management/sa36-sa38-local-integration-plan.json
- doc/tasks/20261003-edhr-thread-management/sa36-sa38-local-integration-receipt.json

## 授权依据

AGENTS.md要求本轮明确授权Git提交；旧授权范围已消耗，当前方案未执行Git提交或融合。批准后才执行精确本地操作；随后开始完整八方向下一轮静态审查，不能在此标记整个目标完成。

## 当前任务资料预览

当前验收任务only preview，--worktree-closeout off --json，exit0，delete/blocked/warnings均0。经理worktree继续使用，不对整棵旧分支进行cleanup apply/合并/删除；原auto全树拒绝完整保留。精确9项代码融合门禁保持，未执行Git提交。

## 本轮实际授权及融合

用户回复“授权”，精确1＋9＋5本地范围。基线提交：fb82e021d1e08c8ada4ad370ede0732addc25c29；worker实现提交：d5ca77b3474b7e38bb88058c3e79a991f6a5f681；主干融合提交：a1ff60b41adbb5dde8f87d442fd50e8bf8e71620。经理原39类709项及worker原快照2669项证据保留；主干之后另一任务的两项非重叠已办查询输入已重新绑定，最新40类777项全通过，12120输入不变。这两项属于其他任务，工作区保留且不纳入本次提交。无关main/worker资产、stash、其他分支保护通过。5份记录自身提交的hash另外封存，不在尚未产生的提交中自引用。尚未推送、执行E2E、数据库写入、共享服务操作或宣称整个循环完成。
