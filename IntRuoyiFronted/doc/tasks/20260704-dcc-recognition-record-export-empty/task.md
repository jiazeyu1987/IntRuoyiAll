# DCC 识别记录导出空数据前端修复

## 任务目标

修复受控浏览当前目录模式下导出识别记录未带子目录范围参数的问题。

## 里程碑

1. 补充前端静态契约，锁定导出参数必须携带 `includeDescendantDirectories`。
2. 更新受控浏览导出请求构造。
3. 运行 DCC 批量识别静态契约验证。

## 预期验证

- `buildBrowserRequestParams()` 在当前目录模式下设置 `includeDescendantDirectories: true`。
- `pnpm.cmd e2e:dcc:browser-batch-recognition:static` 通过。

## 当前状态

已完成：`buildBrowserRequestParams()` 当前目录模式已携带 `includeDescendantDirectories`，静态契约和 lint 已通过。

## 经验门禁

- `docs/powershell-memory.md`：PowerShell 命令和中文输出显式 UTF-8，不使用 `&&`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。
- 是否存在临时补丁或绕过：否。

## 验证结论

- RED: `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 e2e:dcc:browser-batch-recognition:static` -> FAIL，`buildBrowserRequestParams()` 缺少 `includeDescendantDirectories`。
- GREEN: `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 e2e:dcc:browser-batch-recognition:static` -> PASS。
- GREEN: `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 exec eslint src/views/dcc/controlled-file/browser/index.vue tests/e2e/dcc-browser-batch-recognition-static.spec.js --format stylish` -> PASS。
