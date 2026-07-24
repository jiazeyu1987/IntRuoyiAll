# 任务：DCC 受控浏览目录仅显示当前层

## 任务目标

将 `src/views/dcc/controlled-file/browser/index.vue` 的目录点击语义从“当前目录递归包含全部子目录文件”改为“像 Windows 文件夹一样只显示当前目录直接文件”；保持目录树、分页、版本切换、详情/预览/下载入口与现有接口字段不变。

## 里程碑

- [x] M1：创建任务文档，记录经验门禁、设计约束检查与 BDD 场景。
- [x] M2：先补前端 RED 静态回归，锁定浏览页当前仍发送递归目录参数的旧行为。
- [x] M3：最小修改受控浏览页请求参数，改为默认仅当前目录。
- [x] M4：运行前端定向静态验证并补齐执行证据。

## 预期验证

- `node tests/e2e/dcc-browser-search-usability-static.spec.js`

## 当前状态

已完成。

## 最终验证结果

- `node tests/e2e/dcc-browser-search-usability-static.spec.js`：PASS

## 前一任务检查

- 前端最近任务 `20260625-dcc-review-matrix-hide-columns` 已标记完成，允许继续本任务。
- 当前前端仓库存在其他未归属脏改动；本任务只修改 DCC 受控浏览页、定向静态测试与本任务文档，不覆盖其他改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：保持现有 DCC 密集操作台表格与目录浏览布局，不新增装饰性结构，不改变目录树与列表骨架。
- `docs/experience-index.md`：本任务仅做本机源码与静态验证，不执行真实 E2E、服务器写入或其他高风险动作，因此不触发 `experience-preflight` 门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。直接把目录浏览正式语义收敛为仅当前目录，不增加双模式或兼容分支。
- `是否从根因和长期维护角度解决`：是。根因是浏览页固定传递归参数；本次直接收口默认请求语义，避免父目录持续混入子目录文件。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 父目录仅显示直属文件 -> Given 父目录下既有直属文件也有子目录文件 When 用户点击父目录 Then 列表只显示父目录直属文件，不显示子目录文件。`
- `BDD: 子目录单独显示自身文件 -> Given 用户已经进入父目录 When 用户继续点击某个子目录 Then 列表切换为只显示该子目录直属文件。`
- `BDD: 浏览页默认非递归请求 -> Given 用户在当前目录模式查看受控浏览 When 页面请求列表 Then 请求只携带当前目录编号，不再显式发送递归目录参数。`

## Cleanup Keep

- `doc/tasks/20260625-dcc-browser-current-directory-only/task.md`
- `doc/tasks/20260625-dcc-browser-current-directory-only/execution-log.md`
