# 20260917 DCC Main Flow Static Subagent Review

## Task Goal

用 gpt-5.5 high 子线程静态检查 DCC 四个主流程的主流程代码逻辑；主线程复核子线程反馈，确认真实问题后修复，误报跳过，并循环到子线程确认无静态逻辑问题。

## Milestones

- [ ] 读取任务相关规则与 DCC 既有流程文档。
- [ ] 创建四个流程静态检查子线程并收集反馈。
- [ ] 主线程逐项复核反馈，确认真实问题和误报。
- [ ] 对真实主流程逻辑问题执行 RED/GREEN 修复。
- [ ] 复跑子线程静态检查，直到无剩余静态逻辑问题。
- [ ] 完成定向验证、记录结果并进入收尾。

## Expected Verification

- 子线程静态审计：受控浏览、升版、文件生命周期、上传到受控保存四条流程均无剩余主流程逻辑问题。
- RED/GREEN：每个确认修复项记录 Given/When/Then、失败证据和修复后通过证据。
- 后端/前端定向验证：按实际改动范围运行 Maven、前端静态合同或类型检查；如环境阻塞则 fail fast 记录真实原因。
- 不执行 E2E，除非用户当轮明确要求。

## Current Status

blocked - 重复准备记录，已收口至 doc/tasks/20260917-dcc-main-flow-static-logic-audit/；以主任务 verification-report.md 为准，不独立声明完成。Git 提交推送及正式 cleanup 尚未执行。

## Design Constraints Check

- 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/worktree-restrictions.md`、`docs/database-rules.md`、`docs/dcc-main-flow-20-issues-handoff.md`、`docs/dcc-main-flow-no-training-distribution.md`、`docs/bugs/20260912-dcc-90-step-static-audit.md`。
- 默认禁止 fallback、降级、吞异常、模拟成功和兼容补丁。
- 未获本轮明确授权，不执行 Git 提交/推送、远程服务器、数据库写入、发布、服务停止/重启或 E2E。
- “工序开始”“批记录表单”“表单槽位”三条链路独立，不互相补齐或推断。

## BDD

BDD: controlled browsing permission split -> Given 用户只有名称查看权限 When 浏览、搜索或选择关联候选 Then 只能发现名称级候选，不能读取正文、详情或预览；切换目录、文件或版本时重新校验权限与上下文。

BDD: revision via checkout/checkin -> Given 已有受控文件需要修改且用户具备当前项目和类别权限 When 检出并检入 Then 只有检出人能修改该版本，检入填写说明并生成需审批的新版本；小版本不触发大版本关联通知，大版本按确认时点通知有权限处理人且不自动切换既有关联。

BDD: controlled file lifecycle -> Given 项目、模板文件夹、默认目录、审批路线和权限已配置 When 新建、审批、驳回、盖章 PDF、生效、修改和替代 Then 版本正文在审批期间锁定，驳回保留原因并要求重新上传，最终批准且盖章 PDF 入默认目录成功后才成为受控文件。

BDD: upload to controlled save -> Given 上传人选择项目和有效模板文件夹 When 选择模板允许的文件名、填写编号/初始版本、上传源文件和必要 PDF 并提交审批 Then 上传入口只创建首次文件，已有同身份文件不得创建新版本，最终批准前校验盖章 PDF、上传人、用途、目标版本和默认目录，生效失败显示真实原因。
