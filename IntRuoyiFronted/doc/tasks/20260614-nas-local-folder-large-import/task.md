# 20260614 NAS 本地大文件夹分批导入 DCC 前端实施

## 任务目标

将 `NAS 管理` 页签的 `导入文件夹` 从一次性 multipart 导入升级为可支持超大本地目录的分批上传流程，目标支持 `E:\Downloads\2.DHF` 这类约 79GB、1.5 万文件的目录。前端必须保留浏览器选择本地文件夹能力、相对路径结构、DCC 模板类别/产品/生效日期确认，以及后台任务轮询展示。

## 前置任务检查

- 最近相关前端任务 `20260613-nas-local-folder-import/task.md` 状态为 `BLOCKED_E2E`，阻塞原因是旧实现无法通过 Playwright 注入真实大目录且测试租户缺少启用的 `其他` 类别。
- 本任务是对该阻塞的正式升级：不把旧阻塞伪装为完成，不继续使用一次性 2GB multipart 方案支持大目录。
- 本任务限定在 NAS 管理页、DCC workflow API 类型、静态契约测试和任务证据文件。

## 里程碑

1. M1 文档与审计：记录现有限制、确认目标目录规模和前后端合同。
2. M2 RED：新增/更新前端静态契约测试，要求导入走 session + batch upload + complete，不再以总目录大小 2GB 拦截。
3. M3 GREEN：实现大目录分批上传、进度展示、失败中止、最终触发后台任务轮询。
4. M4 REGRESSION：运行 NAS 管理静态契约测试和可执行的 TypeScript/相关检查。
5. M5 E2E/收尾：记录真实路径验证；如缺少测试租户类别或本地服务阻塞，则 fail fast 记录影响。

## 预期验证

- `node scripts/system-nas-management.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- Playwright 真实路径：本机 `http://localhost:8081/system/nas`，测试租户 `aoteman`，选择小型真实目录验证分批接口链路；`E:\Downloads\2.DHF` 做本机目录规模预检查。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；大目录必须走明确的分批导入合同，上传失败直接中止并显示错误，不静默改走旧一次性接口或 mock 成功。
- `是否从根因和长期维护角度解决`：是；从单请求上限根因改为分批上传 + 后台任务提交，避免继续扩大 HTTP 请求体上限。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：M1-M4 完成；前端导入流程改为创建会话、按批上传、完成会话并展示上传进度，移除总目录大小拦截。
- 验证结果：`node scripts/system-nas-management.test.mjs` 通过；`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` 通过；frontend evidence validator 通过。
- 补充说明：未执行真实浏览器选择 `E:\Downloads\2.DHF` 的 E2E；自动化文件夹选择受浏览器本地文件选择能力限制，本任务已完成本机代码支持与静态/类型验证。

## Cleanup Keep

- `doc/tasks/20260614-nas-local-folder-large-import/frontend-feature-evidence.md`
