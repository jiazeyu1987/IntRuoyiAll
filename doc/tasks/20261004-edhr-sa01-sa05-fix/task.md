# SA01—SA05 repair
## Task Goal
在独立 worktree 顺序修复五项正式签名、上传授权及作废/放行状态问题，交管理者独立 review。
## Milestones
- M1 completed：规则、基线、指纹及槽位。
- M2 completed：SA01/SA03 签名身份和统一证据。
- M3 completed：SA02 正式上传上下文和授权。
- M4 completed：SA04/SA05 NCR、独立作废和放行门禁。
- M5 completed：定向回归、编译、变更映射和 review_ready。
- M6 awaiting_review：管理者独立复核及审核后本地集成。
## Expected Verification
本地隔离服务单测与跨模块 Maven 编译；必要前端契约检查；记录退出码及 Surefire 数量，不连接共享业务数据库，不执行真实 E2E。
## Acceptance
SA01：选中系统签名人B认证并持久化B，登录操作者A独立保留；本人正常，错密和未授权拒绝。
SA03：员工档案凭据与独立身份域生成统一签名，读取核验一致，失败无残留，不迁移历史。
SA02：批次入口可信解析唯一正式PQC申请，权限与数据范围校验，伪造/跨租户/终态拒绝，详情入口一致。
SA04：待审NCR阻止独立作废各入口和审批生效，QA正式NCR作废仍可结束评审。
SA05：待审作废阻止PQC批准、上市初始化和最终批准，撤回/驳回恢复，旧申请不可覆盖不协调终态。
## 设计约束检查
唯一执行者、不创建子Agent；仅改本 worktree 和本任务资产。执行者不自行提交/推送/合并/删除worktree，不启动业务服务、不写共享数据库、不执行真实E2E。管理者独立审核通过后可按用户授权执行必要本地提交与融合，并保护主干重叠P1成果；源报告只读。仓库规则取消默认强制BDD/TDD，使用业务回归验证。
## Current Status
review_ready：R1/R2/R3/V1整改和最终JDK17定向组合回归已全部通过，业务文件停止编辑，交管理者独立复核。

## Review Handoff
实现与本地必需验证完成；M6独立复核待管理者执行。本阶段不是集成收尾，不执行cleanup apply、提交、融合、删除worktree或标记completed。保留本地日志与辅助编辑脚本便于审查；原始大输出和凭据不得提交。管理者只提交task-owned-files.txt确认的本任务源码/测试/经验改动以及需要保留的精简交付记录。

## Round 1 remediation
目标：一次性处理R1批准后资料授权、R2真实PQC操作者写读契约、R3删除操作授权；V1同步正式writer测试夹具并独立回归。
里程碑：R1/R2/R3/V1全部完成。最终同一精确定向组合在Microsoft JDK17.0.20、Maven3.9.16运行，2026-10-04 16:14:09 +08:00退出0；统一签名17+MES538=555项，40个实际JUnit测试类，失败0/错误0/跳过0。精确命令、逐类及Surefire运行时核对见verification-report.md、round1-final-test-summary.json；不将历史JDK21结果作为最终依据。无真实E2E或共享服务/业务库操作。仍仅交review_ready，不自行融合/推送/部署。

## Round 1 Allow-list
追加文件与授权依据见round1-allow-list.md，包含R2事件服务必要相邻修正、必要回归夹具及仅测试getFirst()→get(0)的JDK17编译前置修正。完整精确源码资产清单task-owned-files.txt含72个文件（64修改、8新增）；review-ready-source-fingerprints.json保存最终原始字节SHA256。没有改动生产NCR处置服务、共享环境或主干并行资产。
