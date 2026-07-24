# 20260611 DCC 恢复验证下载权限语义修正

## 任务目标

修正 DCC 恢复验证脚本对下载接口的硬断言。恢复后当前测试用户可能可查看详情和预览 metadata，但因 DCC 权限规则无法直接下载受控文件；验证脚本必须真实记录该权限状态，不得把下载失败伪装为内容成功，也不得因此否定已通过详情、预览和对象恢复链证明的恢复点。

## 里程碑

- [x] M1 记录真实流程失败原因和 BDD 场景。
- [x] M2 补充 RED 测试，要求验证脚本支持显式 `allowDownloadAccessDenied`。
- [x] M3 修正验证脚本与真实流程期望。
- [x] M4 运行静态验证并提交本任务改动。

## 预期验证

- `node tests\e2e\dcc-restore-verify-download-permission.test.cjs`
- `node --check tests\e2e\dcc-restore-verify.e2e.js`
- `node --check scripts\dcc-incremental-backup-restore-real-flow-gate.mjs`
- `git diff --check`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只有期望显式允许时才记录下载权限受限；其他 JSON 下载响应仍失败。
- `是否从根因和长期维护角度解决`：是。验证语义匹配 DCC 受控文件权限模型。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：已完成。
- 阻塞：无。

## 完成记录

- `dcc-restore-verify.e2e.js` 支持显式 `allowDownloadAccessDenied`，并输出 `downloadAccess=denied`。
- 完整真实流程对 B3/B4 的下载期望显式声明该权限语义。
- 验证结果：静态测试和 Node 语法检查通过。
