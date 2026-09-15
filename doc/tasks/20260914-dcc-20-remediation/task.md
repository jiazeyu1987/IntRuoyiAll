# DCC 20项修复

## Goal
在当前 int_qms 分支逐个修复用户提供的 DCC-MAIN-01 至 DCC-MAIN-20，修复后逐项静态代码分析，持续修正直到20项逻辑错误全部消除。以当前分支代码为准，不用 int_main 的历史核查替代当前证据。提交推送仅在当前轮明确授权时执行。

## Current Status
ready_for_closeout

## Latest main integration
User-requested integration completed: remote int_main 70b53cf27 merged into int_qms as e2b7eac93 and pushed to origin/int_qms. Frontend build and 349 targeted backend tests plus 15 frontend checks passed. Checkout/checkin and project-template implementations are now available. The 20-item goal is not complete and must be audited against this integrated baseline.

## Milestones
1. 核对当前实现、建立20项验收矩阵、保存既有脏改动基线。
2. 按 BDD / RED / GREEN 修复批准生效、版本检入、权限及界面状态问题。
3. 定向回归、真实用户路径验收、逐项完成审计。
   静态审计须逐项追踪前端入口、API、授权、服务、事务、持久化、签名/附件归属和失败重放；测试通过不能替代调用链审计。发现遗漏继续修复并复测。
4. 经验合并、清理、分别提交实现与收尾，推送 origin/int_qms。

## Expected verification
后端定向单元/并发测试；前端行为与契约测试；类型和构建检查；按已确认测试租户执行真实页面流程。不能以静态检索替代行为验证。

## Current-turn authorization
用户已在 2026-09-15 当前轮授权提交并推送。数据库写入、远端服务器操作、服务重启和真实页面 E2E 未在本轮授权或要求；本任务收尾基于本地单元/契约/构建验证、静态调用链审计和 Git 提交推送证据。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；统一正式来源、版本身份、授权与事务状态，不做显示绕过。
- 是否存在临时补丁或绕过：否。

## Requirements
| ID | 验收目标 | 状态 |
|---|---|---|
| 01 | 批准自动生效，不要求分发，不建立强制分发任务，保留PDF和签名校验 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 02 | 普通文件不受历史培训配置阻断，不删除历史资料 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 03 | CAD检入新源件及同会话PDF，支持大小版本并重新审批 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 04 | 仅备注检入若保留，PDF仍具有明确版本归属和独立访问校验 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 05 | 检出与送审锁内当前读、条件更新、互斥 | implemented; H2 mapper race, static closure and local regression passed; implementation committed; real-page E2E not requested |
| 06 | 固定必签人失效明确阻断，禁止少签替代完成 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 07 | 页面、审批中心与后端禁止新退回/转办，历史只读 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 08 | 禁止前后加签，批准保留签名和盖章PDF完整性 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 09 | 上传中/失败不允许提交旧内容，异步结果归属当前会话 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 10 | 管理端可读取修复失效模板，上传端严格阻断非法项 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 11 | 上传只新建且支持合法初始版本；驳回重传单独关联 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 12 | 正式默认目录自动落位，项目和编号不同的同名文件独立 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 13 | 拒绝自身/后代父节点，已有循环有限时间明确失败 | implemented; static closure and local regression passed; implementation committed; live DB/E2E not requested |
| 14 | 关联候选完整分页/检索，名称权限不泄漏内容，关联独立授权 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 15 | 产品DCC编号前后端同源，失效关联明确失败 | implemented; UNBOUND/null policy, static closure and local regression passed; implementation committed; real-page E2E not requested |
| 16 | 每版本完整动作权限，名称/内容访问独立投影 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 17 | 并发旧转办/加签请求全部拒绝且无写入 | implemented; static closure and local no-write regression passed; implementation committed; real-page E2E not requested |
| 18 | 列表成功、错误、loading、分页、缓存、路由与当前上下文一致 | implemented; static closure and frontend behavior regression passed; implementation committed; real-page E2E not requested |
| 19 | 批准PDF上传按任务、办理人、用途、版本授权 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |
| 20 | 未绑定附件正式清理且可恢复，重传不得复用旧票据 | implemented; static closure and local regression passed; implementation committed; real-page E2E not requested |

## Applicable gates
- 已读当前 docs/backend-development.md、frontend-development.md、powershell-encoding.md、powershell-memory.md、task-closeout-rules.md、e2e-rules.md。
- experience-index.md 存在；命中前端经验路径 D:/ProjectPackage/Int/IntPP/FRONTEND_STYLE.md，待检查可用性。
- 既有基线文件需先检查秘密和大小，禁止遗漏、强推或重写历史。
- 无运行态验证证据时不得关闭条目。基线和本任务实现必须分开。

## Blockers
用户已明确允许排除 .runtime/ 并继续，其文件保留磁盘；.runtime/ 已由 .gitignore 排除，未进入暂存区。经验索引指向的 D:/ProjectPackage/Int/IntPP/FRONTEND_STYLE.md 在本机不存在；不套用其他项目路径。20项代码修复、静态合同、后端定向回归、前端类型检查和前端构建已通过；实现提交 7e7f3a080 已完成。task-closeout-cleanup 可执行文件在当前检出中不存在，已按文件归属直接复核 keep/delete 边界；等待收尾提交和 git push origin int_qms 验证。
