# 20260728 DCC 上传页签受控文件访问权限修复

## Task Goal

修复用户在文件上传页签上传文件时出现 `Current user cannot access this controlled file` 的问题，确保上传流程不会错误走受控文件浏览/下载权限校验。

## Milestones

- [ ] 建立缺陷复现与预期行为记录。
- [ ] 定位上传页签触发受控文件访问校验的根因。
- [ ] 先补 RED 回归测试，再实施最小正式修复。
- [ ] 运行目标 GREEN 与相关回归验证。
- [ ] 完成证据、风险与收尾记录。

## Expected Verification

- 后端或前端最小回归测试覆盖上传页签路径不应触发受控文件访问拒绝。
- 受影响模块的目标测试通过。
- 不引入 fallback、降级、吞异常、mock 成功或默认成功。

## Current Status

in_progress

## 经验门禁

- Trigger: DCC 受控文件上传、`upload-preview`、`Current user cannot access this controlled file`、类别权限、文件类别下拉。
- Preflight check: 先区分菜单权限 `dcc:controlled-file:submit/query` 与类别级 `UPLOAD` 权限；上传页不得展示当前用户无 `UPLOAD` 权限的类别，后端上传预览/提交仍必须 fail-fast 拦截无权限类别。
- Blocker: 缺少类别级上传权限投影、前端仅靠上传接口报错、或为消除报错放宽后端 `UPLOAD` 权限校验时必须停止。
- Verification: 后端类别列表投影 `canUpload`，前端静态契约验证上传页过滤 `canUpload=false`，并运行上传服务原有权限拒绝测试确保后端拦截仍保留。
- Forbidden action: 禁止绕过 `DccControlledFileCategoryPermissionSupport`、禁止把无权限类别当可上传类别展示、禁止吞掉 `CONTROLLED_FILE_ACCESS_DENIED` 或改成默认上传成功。
- Evidence: 本任务 `doc/tasks/20260728-dcc-upload-controlled-file-access/`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：待确认根因后更新。
- `是否存在临时补丁或绕过`：否。
