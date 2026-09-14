# DCC 20项修复

## Goal
在当前 int_qms 分支逐个修复用户提供的 DCC-MAIN-01 至 DCC-MAIN-20，逐项验证并提交推送。以当前分支代码为准，不用 int_main 的历史核查替代当前证据。

## Current Status
in_progress

## Milestones
1. 核对当前实现、建立20项验收矩阵、保存既有脏改动基线。
2. 按 BDD / RED / GREEN 修复批准生效、版本检入、权限及界面状态问题。
3. 定向回归、真实用户路径验收、逐项完成审计。
4. 经验合并、清理、分别提交实现与收尾，推送 origin/int_qms。

## Expected verification
后端定向单元/并发测试；前端行为与契约测试；类型和构建检查；按已确认测试租户执行真实页面流程。不能以静态检索替代行为验证。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；统一正式来源、版本身份、授权与事务状态，不做显示绕过。
- 是否存在临时补丁或绕过：否。

## Requirements
| ID | 验收目标 | 状态 |
|---|---|---|
| 01 | 批准自动生效，不要求分发，不建立强制分发任务，保留PDF和签名校验 | pending |
| 02 | 普通文件不受历史培训配置阻断，不删除历史资料 | pending |
| 03 | CAD检入新源件及同会话PDF，支持大小版本并重新审批 | pending |
| 04 | 仅备注检入若保留，PDF仍具有明确版本归属和独立访问校验 | pending |
| 05 | 检出与送审锁内当前读、条件更新、互斥 | pending |
| 06 | 固定必签人失效明确阻断，禁止少签替代完成 | pending |
| 07 | 页面、审批中心与后端禁止新退回/转办，历史只读 | implemented; unit/contract passed; E2E pending |
| 08 | 禁止前后加签，批准保留签名和盖章PDF完整性 | mutation removal verified; final approval/E2E pending |
| 09 | 上传中/失败不允许提交旧内容，异步结果归属当前会话 | pending |
| 10 | 管理端可读取修复失效模板，上传端严格阻断非法项 | pending |
| 11 | 上传只新建且支持合法初始版本；驳回重传单独关联 | pending |
| 12 | 正式默认目录自动落位，项目和编号不同的同名文件独立 | pending |
| 13 | 拒绝自身/后代父节点，已有循环有限时间明确失败 | implemented; unit/regression passed; concurrent DB/E2E pending |
| 14 | 关联候选完整分页/检索，名称权限不泄漏内容，关联独立授权 | pending |
| 15 | 产品DCC编号前后端同源，失效关联明确失败 | pending |
| 16 | 每版本完整动作权限，名称/内容访问独立投影 | pending |
| 17 | 并发旧转办/加签请求全部拒绝且无写入 | service concurrency tests passed; HTTP/E2E pending |
| 18 | 列表成功、错误、loading、分页、缓存、路由与当前上下文一致 | list/metadata/route guard tests passed; remaining callbacks/E2E pending |
| 19 | 批准PDF上传按任务、办理人、用途、版本授权 | pending |
| 20 | 未绑定附件正式清理且可恢复，重传不得复用旧票据 | pending |

## Applicable gates
- 已读当前 docs/backend-development.md、frontend-development.md、powershell-encoding.md、powershell-memory.md、task-closeout-rules.md、e2e-rules.md。
- experience-index.md 存在；命中前端经验路径 D:/ProjectPackage/Int/IntPP/FRONTEND_STYLE.md，待检查可用性。
- 既有基线文件需先检查秘密和大小，禁止遗漏、强推或重写历史。
- 无运行态验证证据时不得关闭条目。基线和本任务实现必须分开。

## Blockers
用户已明确允许排除 .runtime/ 并继续，其文件保留磁盘；其余既有改动单独基线提交。经验索引指向的 D:/ProjectPackage/Int/IntPP/FRONTEND_STYLE.md 在本机不存在；不套用其他项目路径，前端样式专项修改前需解决该文档缺失。本任务先推进已具备规则与证据的后端工作。
