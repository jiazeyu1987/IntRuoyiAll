# 公共附件组件卸载竞态交接

## 当前结果

Root 已解除生产前端构建冻结，并把 ControlledFileAttachments.vue 的最小卸载失效 hunk 唯一交本线程维护。现已在该组件 import onBeforeUnmount，并在卸载时 generation++、清空原选择/预览/下载状态；不改 API、hash 或权限规则。专属测试实际 GREEN 5/5；原共享组件/API 受影响回归 8/8。下面 RED 与等待转交描述为实施前历史，不表示当前仍待修复。

## BDD 与边界

Given 版本 A 的两个普通附件已显示，下载确认或下载响应仍在等待；When 附件组件随详情刷新/离开而卸载，旧确认或响应随后到达；Then 不再发送旧下载请求、不调用浏览器文件保存，不影响当前页面。

当前源码 `IntRuoyiFronted/src/views/dcc/controlled-file/detail/ControlledFileAttachments.vue` 的 download 仅在 props watch 中增加 generation；没有卸载失效处理。本轮新增独立测试已取得真实 RED。

## 实际 RED

在 `IntRuoyiFronted` 执行：

```powershell
node --test tests/unit/dcc-attachment-download-disposal.test.cjs
```

初次实际 exit 1：2 tests、0 PASS、2 FAIL、0 skip；补三项对照后同命令 RED 5 tests、3 PASS、2 FAIL。用例均编译并挂载真实 Vue SFC，调用实际 download handler；ElMessageBox 与下载网络/浏览器保存为明确测试端口，不连接真实账号或接口。

1. 两普通附件中的第二项 `(file=20, attachment=701)` 在确认 Promise 等待时调用 Vue app.unmount；旧确认随后接受，实际 requests=1，期望 0。
2. 同一精确附件请求已发起、文件响应 Promise 等待时卸载；旧响应随后返回，实际 browser saves=1，期望 0。

已有共享组件测试覆盖了 props 版本变更和取消确认，未覆盖真实 app.unmount；本次未改动或重跑该测试宿主。新测试自带宿主，使用生产 SFC 和生产 attachment-facts helper。

## 最小修复及 GREEN

Root 明确转交后，在卸载钩子中使 generation 失效并清空预览/下载状态；已有确认后和回包后的 generation 校验拒绝旧操作。没有吞掉服务端权限失败，没有把旧响应标为当前版本，也没有新增 API。

GREEN：新增专属测试 5/5，包含两个原 RED、cancel/close 零请求且保留双附件、合法当前版本下载/服务器名、撤销权限零请求。受影响回归：`node --test tests/unit/dcc-shared-attachments-component.test.cjs tests/unit/dcc-attachment-download.test.cjs` 实际 8/8，0 skip。更广的构建/类型/Maven 仍由 Root 唯一执行；旧 hash/权限断言和测试宿主保持原样。

## Owner 边界（移交前历史）

- 本组件不属于本线程当前独占的 AttachmentService/RevisionService/CheckinAttachmentsPanel；生产前端同时处于 Root Vite 构建冻结。本线程不会修改它或旧测试宿主。
- 复现只执行新建 `tests/unit/dcc-attachment-download-disposal.test.cjs` 的专属用例；不重跑旧 211/129 或 Root 125/181，不启动 Maven/fulltypes/build，不写 shared target。
- 若 RED 成立，建议 Root 给组件 owner 分配最小卸载失效处理，然后运行新增用例 GREEN 与该组件的受影响回归；不要放宽服务端下载授权。
- 该问题首先是已离开/刷新详情的旧操作仍执行，不据此宣称服务端权限被绕过；服务端仍按精确文件 ID、附件 ID 和当前权限校验。
