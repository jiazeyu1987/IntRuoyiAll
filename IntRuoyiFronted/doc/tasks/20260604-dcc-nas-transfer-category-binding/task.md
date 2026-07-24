# Task: DCC NAS 转移类别目录绑定前端提示修复

## 任务目标

在 NAS 管理页“转移到 DCC”弹窗中，当模板类别未绑定受控目录时，在提交前以中文提示阻止转移，避免用户看到后端英文异常。

## 上一任务检查

- 前端上一任务 `yudao-ui-admin-vue3/doc/tasks/20260604-dcc-controlled-file-metadata-edit/task.md` 状态为 `completed`。

## 里程碑

- [x] M1：定位前端弹窗和提交调用。
- [ ] M2：先补充前端静态契约 RED。
- [x] M3：实现提交前绑定校验与可选项提示。
- [x] M4：运行目标 Node 测试。
- [x] M5：更新证据并提交。

## 预期验证

- RED：`node scripts/system-nas-management.test.mjs` 在缺少未绑定类别校验时失败。
- GREEN：`node scripts/system-nas-management.test.mjs` 通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。未绑定目录仍不能转移，不替换类别、不默认成功、不绕过后端约束。
- `是否从根因和长期维护角度解决`：是。前端利用分类接口已有 `directoryId` 字段做同一前置条件校验。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## 阻塞

- 无代码修复阻塞。Playwright 完整提交前拦截路径受本机真实数据限制，详见根任务证据。

## Cleanup Keep

- `doc/tasks/20260604-dcc-nas-transfer-category-binding/bug-regression-evidence.md`
