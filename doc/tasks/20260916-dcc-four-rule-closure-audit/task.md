# DCC Four Rule Closure Audit

## Goal
按当前 `int_qms` 代码审计并验证用户指定4条规则：名称/内容权限分离；上传不走升版且初始版本可填写、版本变更只走检出检入；无工作稿概念且统一描述为受控文件；已关联文件大版本变更时通知有权限人员，由其决定是否关联新版，小版本不通知。

## Current Status
ready_for_closeout

## Milestones
1. 读取仓库规则并确认当前分支、工作区和相关历史证据。
2. 对四条规则逐项映射当前源码、接口和前端入口。
3. 运行覆盖四条规则的后端/前端定向合同和静态扫描。
4. 若发现缺口，先修复再复验；若当前已满足，则记录跳过修复依据并收尾。

## Expected Verification
- 当前分支和工作区状态确认。
- DCC 后端静态合同与相关 Maven 定向回归覆盖权限、上传/检入检出、关联通知。
- DCC 前端脚本合同覆盖上传、检入、名称/内容权限、关联分页/选版、术语。
- 精确源码扫描确认用户可见旧术语无生产命中。
- 不执行真实页面 E2E、数据库写入、服务重启或部署，除非用户另行明确要求。

## BDD
- BDD: name/content permission split -> Given a user has only controlled-file name permission; When browsing/searching/indexing/selecting relation candidates; Then the file can be found and related but content preview/download/detail content stays unavailable, while content permission includes name visibility.
- BDD: upload and version governance -> Given a new upload or existing controlled file; When a user uploads or changes version; Then upload creates a new controlled file with explicit legal initial version and existing versions change only through checkout/checkin.
- BDD: controlled-file terminology -> Given user-visible DCC controlled-file pages or backend messages; When they describe file objects and version progress; Then they use controlled-file wording and status progress rather than work-draft/current-version business labels.
- BDD: major-version relation notification -> Given an established relation to a controlled file; When a related file checks in and becomes effective; Then only major-version changes create notification/follow-up for authorized recipients, and recipients decide whether to link the new version.

## 设计约束检查
- 只以当前源码、测试输出和报告文件作为证据，不用历史结论替代当前验证。
- 不引入 fallback、降级、吞异常或模拟成功。
- 本轮不做数据库写入、服务重启、部署或真实页面 E2E。

## Cleanup Keep
- doc/tasks/20260916-dcc-four-rule-closure-audit/task.md
- doc/tasks/20260916-dcc-four-rule-closure-audit/execution-log.md
- doc/tasks/20260916-dcc-four-rule-closure-audit/verification-report.md
