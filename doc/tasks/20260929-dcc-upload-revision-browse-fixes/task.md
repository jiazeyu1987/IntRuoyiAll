# DCC 上传、升版、浏览问题修复

## Goal
修复静态审查报告 URB-001 至 URB-017 的 17 项问题，处理 5 项高优先级与 12 项一般问题；主 Agent 逐项 review 代码与静态验证证据。不执行 E2E、浏览器业务操作、数据库写入或服务运行验证。

## Scope And BDD

- URB-001: Given 工作稿数据库培训值与提交值不同，When 同一送审事务提交，Then 持久化状态、文件详情及 BPM 培训变量一致；同幂等键不同载荷被拒绝。
- URB-002: Given 文件待分发且 BPM 节点存在，When 状态更新或 BPM 推进任一失败，Then 文件状态和流程原子回滚；并发重放不重复推进。
- URB-003: Given 指定 BPM 接收节点不存在或不唯一，When DCC 触发节点，Then 明确失败，调用者不能将未推进报告成功。
- URB-004: Given 普通附件仍在上传，When 用户提交上传申请，Then 不提交业务申请；全部附件成功后完整载荷才可提交。
- URB-005: Given 上传请求未完成，When 用户移除文件或改变上传上下文后旧请求返回，Then 旧票据不回写当前申请并按合同清理。
- URB-006: Given 首次请求已提交但响应丢失，When 相同上传会话和幂等载荷重试，Then 返回原业务身份；异键冲突仍拒绝重复创建。
- URB-007: Given 检入已完成且培训值为 false，When 相同票据以 true 重放，Then 载荷冲突；相同载荷只回读原版本。
- URB-008: Given 19 位受控文件 ID 与 supersededByFileId 精确相等，When 计算版本收口摘要，Then 不经浮点转换仍匹配正确前驱。
- URB-009: Given 浏览操作的受控文件 ID 超出 JS 安全整数，When 打开时间维护或审计，Then 传递原始精确 ID。
- URB-010: Given 预览 A 的异步元数据/内容晚于 B 返回，When 当前预览已切换到 B，Then 所有状态和资源仍属于 B。
- URB-011: Given 签名第 1 页请求较慢、第 2 页较快，When 第 1 页最后返回/失败，Then 当前列表、总数、错误和 loading 保持第 2 页状态。
- URB-012: Given 同租户用户有查询菜单但无目标文件查看权限，When 查询纸质分发记录，Then 服务端拒绝且不返回收件人/操作人；有权限用户正常可读。
- URB-013: Given 会签义务已冻结后用户/部门改名或停用，When 查看历史路线快照，Then 显示创建时负责人和部门名称及稳定 ID，不用当前目录值覆盖历史事实。
- URB-014: Given 内容批准、培训/分发、最终文控发生于不同时间，When 最终发布及重试完成，Then approvedTime 记录内容批准完成时刻，不被后续阶段重写。
- URB-015: Given 分发节点已经完成，When 后续文控驳回或生效失败，Then 分发事实仍显示完成，失败状态及原因独立展示。
- URB-016: Given 检入写入成功并返回新版本，When 列表刷新或本地合并失败，Then 用户明确看到检入已成功和新版本身份，并可单独恢复刷新。
- URB-017: Given 已生效 NEW 文件产生小版本工作稿，When 送审，Then 选择 REVISION 流程；未首次受控生效的 NEW 返工仍选择 UPLOAD。

## Milestones
1. 六个子 Agent 按分配实现 17 项问题及针对性回归测试；各组提交方法级改动清单、测试和未解决假设。
2. 主 Agent review 每组差异，重点检查 5 项 P1、事务边界、权限、幂等、历史事实及共享文件互相影响；必要时退回修正。
3. 执行适当的后端单测/前端静态合同/类型检查；严格不跑 E2E、浏览器、数据库写入或服务运行验证。
4. 更新报告状态和验证报告；只有 17 项全有源码与静态验证证据才可进入收尾。

## Expected Verification
- 每项修复均有精准回归保护，覆盖正常、拒绝/失败路径及重放/竞争的静态或单元级场景。
- 运行与修改相称的 Java 单元测试、前端静态合同和类型检查；所有失败原样记录。
- 全部 17 项方法调用链逐项 review，`git diff --check`。
- 不运行 E2E、Playwright 页面流程、数据库迁移/写入、登录或服务启动/重启。

## Current Status
ready_for_closeout

六个子 Agent 已完成分组实现，主 Agent 已完成代码 Review 与定向静态/单元验证。17 项问题均已形成修复证据；未执行 E2E、数据库写入、服务启动或 Git 收尾。用户明确要求只做代码静态分析且不进行 E2E。

## 设计约束检查
- 源码基于当前 int_qms 工作区，不以 HEAD 或运行中的附加 worktree代替；保留既有脏改动。
- 严格按 `doc/tasks/20260928-dcc-upload-revision-browse-static-review/review-report.md` 的 URB 编号追踪；源码指纹改变则就地重审触发/依据。
- 上传与升版区分首次 NEW 返工及已受控 REVISION；培训是实例选择，不用类别默认值推断。
- 所有文件级读取授权由服务端闭环；流程状态推进不得吞异常或默认成功。
- 客户端长 ID 全程字符串；异步回写必须验证当前请求身份。
- 不执行任何 E2E 或数据库/服务运行态动作。

## Cleanup Keep
- doc/tasks/20260929-dcc-upload-revision-browse-fixes/task.md
- doc/tasks/20260929-dcc-upload-revision-browse-fixes/execution-log.md
- doc/tasks/20260929-dcc-upload-revision-browse-fixes/verification-report.md
- doc/tasks/20260928-dcc-upload-revision-browse-static-review/review-report.md
