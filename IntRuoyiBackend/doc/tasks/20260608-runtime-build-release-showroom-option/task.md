# 任务：构建发布包增加展厅构筑包选项

## 任务目标

在运行控制台“构建发布包”弹窗中增加“发布展厅构筑包”选项，默认不选中；勾选时提示当前选中的展厅构筑包会覆盖服务器的展厅数据，确认后才允许带 Website/展厅构筑包构建。未勾选时发布包只包含后端和管理前端，不包含 Website。

## 前置任务状态

- 已检查最近运行控制台任务 `20260608-runtime-console-empty-repo-root`，状态为 completed。
- 已检查后端未完成任务 `20260608-backup-incremental-manifest-short-loop`，状态为 blocked；阻塞项为测试服真实 B1-B5 备份恢复闭环证据缺失，不影响本次本地构建发布包参数与脚本行为修改。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；构建发布包必须显式传入是否包含展厅构筑包，发布脚本缺少组件范围信息时 fail fast。
- `是否从根因和长期维护角度解决`：是；从前端请求、后端校验、命令参数和发布包 manifest 全链路记录组件范围。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 默认构建发布包不包含展厅构筑包 -> Given 运维打开“构建发布包”弹窗 / When 未勾选“发布展厅构筑包”并提交 / Then 请求携带 `includeShowroomBuildPackage=false`，后端构建命令使用 `-Component intruoyi`。
- BDD: 勾选展厅构筑包需要覆盖确认 -> Given 运维打开“构建发布包”弹窗 / When 勾选“发布展厅构筑包” / Then 前端提示“当前选中的展厅构筑包会覆盖服务器的展厅数据，是否继续？”，取消时恢复未选中，确认时保持选中。
- BDD: 选中展厅构筑包构建完整发布包 -> Given 运维已确认发布展厅构筑包 / When 提交构建发布包 / Then 请求携带 `includeShowroomBuildPackage=true`，后端构建命令使用 `-Component full`，manifest 记录包含展厅构筑包。
- BDD: 发布包部署按 manifest 组件范围执行 -> Given ReleasePackage manifest 记录 `component=intruoyi` / When 执行 deploy-release / Then 脚本只部署后端和管理前端，不复制、不重启、不校验 Website。
- BDD: 缺少组件范围信息不得静默按 full 发布 -> Given ReleasePackage manifest 缺少 `component` 且未显式传入 `-Component` / When 执行 deploy-release / Then 脚本 fail fast 并提示重建发布包或显式指定组件。

## 里程碑

- [x] M1：写入任务文档、RED 测试和静态脚本断言。
- [x] M2：实现后端请求字段、校验、命令参数和发布包列表 manifest 字段。
- [x] M3：实现发布脚本 `intruoyi` 组件、manifest 记录和 deploy-release 组件解析。
- [x] M4：实现前端弹窗复选项、确认回滚和请求参数。
- [x] M5：运行目标验证、记录证据、执行收尾预览并提交本任务改动。

## 预期验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" test`
- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `node tests\e2e\runtime-control-build-release-showroom-option-static.spec.js`
- Playwright 打开 `http://localhost:8081`，验证构建发布包弹窗默认未选、勾选确认、取消回滚和确认保持选中。

## Cleanup Keep

- `doc/tasks/20260608-runtime-build-release-showroom-option/backend-api-evidence.md`
- `doc/tasks/20260608-runtime-build-release-showroom-option/frontend-feature-evidence.md`
- `doc/tasks/20260608-runtime-build-release-showroom-option/ci-cd-evidence.md`

## Verification Result

- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，71 passed。
- `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" test` -> PASS，53 tests。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- `node tests\e2e\runtime-control-build-release-showroom-option-static.spec.js` -> PASS。
- Playwright 本机 `http://localhost:8081` 登录 `芋道源码/admin` 验证“构建发布包”弹窗 -> PASS；未点击提交，未执行真实构建或发布。
- `task-closeout-cleanup --mode preview` -> PASS，delete/blocked/warnings 均为 none。

## 当前状态

completed: 已完成本地代码实现、目标验证、证据校验和收尾预览；本任务未访问、发布或修改测试服、备份服务器或正式服务器。
