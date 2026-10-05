# SA06 身份域读取修复

## Current Status
ready_for_closeout

## Goal
修复系统用户与临时工档案同数字 ID 导致正式详情、时间线、签名列表/证据及归档追溯错名。完成后 review_ready 交管理者复审，不自行提交或融合。

## Baseline
HEAD 64f07c9ea2fd10968dd2ab85f64d065dc5956cdb；隔离 worktree C:/Users/BJB110/.codex/worktrees/a37a/IntRuoyi，起始工作区干净。四个核心 reader UTF-8/LF SHA256 均与管理者交接一致。
已完整读取 E:/IntRuoyi/doc/tasks/20261003-edhr-thread-management/sa06-repair-handoff.md 与 post-merge-static-audit-round2.md。

## Milestones
1. 基线和槽位核验：完成；slot 4，8085/48085，无服务启动。
2. 最小读取修复与同根因下游核对：完成；包含预审明确授权的模拟签名单元格展示。
3. 实际 JDK17 定向回归及证据：完成，最终16类232例零失败/错误/跳过，前端7项合同通过。
4. review_ready 交接：完成；保留源码、日志和验证报告，待管理者复审。

## Expected Verification
- 可执行 H2/MyBatis mapper 合同覆盖同租户双域同号的生产/PQC实际人员。
- 当前/归档详情实际人员与正式冻结签名者分离，签名证据名称一致。
- 错租户/身份域/签名关联仍拒绝；签名、资料上传、放行生命周期定向回归。
- 实际 Java17 runtime/version/vendor/home、命令、Surefire 计数、退出码、源码输入指纹。

## 设计约束检查
不增加 schema、不回填旧数据、不跨域猜测、不引入 fallback；保留现有签名/租户/操作者/模块/来源/哈希校验。实际员工与签名者保持现有一致性；A登录操作者与B实际员工/签名者可不同，不扩大代签能力。P1未提交主干增量不在本 worktree，融合时管理者另核对。
不启动/重启服务，不执行真实 E2E、业务 API/DB、数据库写入或远程推送。本地 H2测试夹具属于已明确要求的持久化合同验证；不连接业务库。
用户当轮明确要求 review_ready 停手，覆盖默认 closeout 提交/推送/融合要求；本轮不标 completed、不删除 worktree，保留复审证据。
## 预审补充
明确 SIMULATION_SESSION 无正式 signatureId 时展示模拟记录、姓名及时间；正式无 ID 仍未签名，所有无 ID 跳转禁用。不扩大 UI 或全量 TypeScript 检查。

## 管理者复审与本地集成门禁（2026-10-04）
管理者完整源码复审 PASS；16项最终指纹一致。隔离环境加入主干实际两项P1增量后，实际Java17定向16类247例，失败/错误/跳过0；管理者前端7项合同PASS。两项临时输入逐字节恢复，不纳入本修复提交。管理者批准必要本地实现提交与融合；无远程推送、共享服务、数据库或E2E操作。工作线程232例与组合247例独立记账，不能累加。实现和验证已通过，保留待融合复审证据；尚未completed。

## Cleanup Keep
- doc/tasks/20261004-edhr-sa06-identity-read-fix/source-fingerprints.md
