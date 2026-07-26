# 执行日志

## 用户意图

用户要求根据文控的浏览和上传场景，在测试管理中增加 4 个测试项。

## BDD

- `BDD: 文控受控文件列表浏览 -> Given 测试管理员进入文控受控浏览页面 When 按目录或关键字查看受控文件列表 Then 页面展示匹配文件及其状态和可用操作`
- `BDD: 文控受控文件在线预览 -> Given 受控浏览列表存在当前账号可浏览的文件 When 从可见文件行打开预览 Then 进入只读预览并展示文件标题和受控阅读限制`
- `BDD: 文控普通文件上传并提交 -> Given 测试账号具备文控上传权限且存在可选目录与类别 When 选择受支持文件并完成必填信息和提交前预览 Then 页面提交审批成功并展示明确反馈`
- `BDD: 文控图纸上传缺少 PDF 阻断 -> Given 测试账号在文控上传页选择图纸源文件 When 未同步上传 PDF 即尝试提交 Then 页面明确阻断并提示图纸源文件必须同步上传 PDF`

## 命令与操作意图

- 只读检查项目规则、经验索引、本机端口和文控前端实现，用于确认真实页面入口与测试项内容。
- 检查 Git 脏工作区、当前分支和远端，用于按项目规则保存任务前基线。
- 后续仅通过本机真实前端创建测试项，不记录密码、token 或其他凭据。

## 里程碑记录

- 2026-07-26: 已读取 `docs/task-closeout-rules.md`、`docs/login-access.md`、`docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md` 和 `docs/experience-index.md`。
- 2026-07-26: 已确认本机 `8081` 与 `48081` 均有监听进程。
- 2026-07-26: 已从文控真实前端源码确认受控浏览支持列表、预览和下载，上传页支持提交前预览，且图纸源文件必须同步上传 PDF。
- 2026-07-26: `GREEN: experience-preflight -> PASS`，命中测试管理、真实 E2E、Element Plus 可见业务行定位和脏工作区基线门禁。

## Git 基线

- 当前分支：`int_main`。
- 初始工作区：存在 tracked 与 untracked 并行改动；基线提交待执行。

## 验证证据

- 待补充。

## Blockers

- 无。
