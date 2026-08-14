# DCC 文控签核追溯真实 E2E 验证

## Task Goal

使用非 admin 账号，通过真实 Playwright 页面路径验证 DCC 文控受控文件的上传人、审批人、签名人、签名时间、签名方式、证据状态、文件 hash、盖章文件和发布文件可在页面追溯，并与只读后端数据一致。

## Milestones

- [x] 读取必需规则、创建任务目录并记录适用门禁
- [x] 确认本机运行态、非 admin 测试账号、密码环境变量和页面入口前置
- [ ] 通过真实页面创建或复用任务自有原版发布/升版文件并完成审批签名链路
- [ ] 通过上传人、审批/签名人、DCC/QA/文控查看账号分别完成页面核验
- [ ] 使用只读 API/DB 核验页面展示与后端证据一致
- [x] 输出 `verification-report.md`，记录文件 ID、版本、上传人、审批人和 BLOCKED 影响

## Expected Verification

- Playwright 操作真实前端页面，不能用 API/SQL 制造审批记录或签名记录。
- 使用非 admin 账号；密码只能通过环境变量注入，任务日志不记录明文。
- 页面证据必须证明“谁上传、谁审核、谁签名”用户可见。
- 只读 API/DB 只用于最终核验页面展示的人、时间、任务状态、签名证据、文件 hash/盖章文件/发布文件 ID 一致。
- 若缺页面入口、权限、测试数据、签名授权、运行态或导出能力，记录 `E2E BLOCKED` 与影响，不做 API-only、SQL 改状态或 admin 绕过。

## Applicable Gates

- DCC 文控审批处理入口门禁：审批必须从 DCC 页面进入非只读处理态，看到签名按钮和目标写接口；若只有 viewer 只读、无签名按钮、BPM 原生审批 403 或下一审批人缺失，必须 BLOCKED。
- Playwright 浏览器门禁：优先使用本机 Chrome/Edge 可执行文件；浏览器缺失是前置缺口，不得改成 API-only。
- 目标链路归因门禁：区分本机前端、后端、目标 DCC/API 请求和外部资源错误；目标链路失败必须 BLOCKED。
- Playwright artifact 门禁：任务结束前清理或脱敏本任务 `.playwright-cli`、截图、trace 中可能含账号/敏感字段的产物。
- 用户补充边界：只验证本场景，不修其它场景；缺入口、权限、测试数据或运行态问题先记录 BLOCKED 和影响。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本任务为验证，不允许 fallback、API-only、SQL 改状态或 admin 绕过。
- `是否从根因和长期维护角度解决`：不适用；本任务只做真实 E2E 验证与缺口归因，不做实现修复。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

## Blocked Reason

`DCC_E2E_PASSWORD` 或等价本场景非 admin 密码环境变量未注入。按用户要求，不能使用 admin、不能把密码明文写入命令/日志、不能用 API-only/SQL 改状态绕过，因此本轮无法登录上传人、审批/签名人或查看账号进入真实页面完成签核追溯验证。
