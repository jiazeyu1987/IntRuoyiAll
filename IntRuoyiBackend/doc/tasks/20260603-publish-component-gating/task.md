# 任务：统一发布脚本按组件门禁

## 任务目标

改造 `script/deploy/publish-int-ruoyi.ps1`，让统一发布脚本支持按组件发布与按组件门禁。DCC 后端类改动在执行 backend-only 发布时，不再被 website/showroom 构建与校验阻断；展厅手动发布仍可单独成功执行。

## 上一任务检查

- 上一个后端任务 `20260603-restore-data-without-rehearsal-gate` 已因用户切换需求标记为 `blocked`。
- 当前任务只改发布脚本与测试，不直接发布到服务器。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。目标是拆清组件边界，不是放宽失败条件；缺少所选组件的必要前置时仍必须 fail-fast。
- `是否从根因和长期维护角度解决`：是。通过显式组件选择与组件级 preflight/build/deploy 路径，避免 DCC/backend 发布被无关 showroom/website 构建拖住。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: backend-only 发布不构建 website -> Given 用户只需要发布后端修复 / When 执行统一发布脚本并显式选择 backend 组件 / Then 脚本只校验并构建 backend 所需内容，不执行 website/showroom 构建、同步与健康检查。

BDD: website-only 发布不要求 DCC backend 运行时门禁 -> Given 用户只需要手动发布展厅 / When 执行统一发布脚本并显式选择 website 组件 / Then 脚本只校验 website 所需前置，不因 DCC viewer token、OnlyOffice 或下载加密配置缺失而失败。

BDD: full 发布保持现有全量校验 -> Given 用户执行全量发布 / When 选择 full 组件 / Then backend、frontend、website 与相关运行时门禁、构建和部署流程保持现有严格校验。

## 里程碑

- [x] M1：记录任务并确认上一任务状态。
- [x] M2：补充 RED 测试，证明当前脚本仍无组件边界，backend 发布会触发 website 路径。
- [x] M3：最小修改统一发布脚本，引入组件选择与组件级门禁。
- [x] M4：运行目标脚本测试并验证 backend-only / website-only / full 合同。
- [x] M5：更新任务记录与验证结论。

## 预期验证

- `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "component or website or backend"`
- 如需补充新测试文件，则运行对应目标 pytest 命令。

## 当前状态

completed

## 最终结果

统一发布脚本已支持 `-Component full|backend|frontend|website`。其中：

- `backend`：只执行 backend 相关运行时门禁、jar/image 构建、数据库与后端健康检查，不再触发 website/showroom 构建与网站容器切换。
- `frontend`：只执行 admin 前端相关构建、镜像导出、frontend 容器切换与前端可用性校验。
- `website`：只执行展厅 website 构建、website 目录切换与网站 readback/smoke check，不再要求 DCC/eDHR backend runtime secret。
- `full`：保持原有全量严格发布路径。

为避免 `frontend-only` 或 `website-only` 发布把远端现有 backend 运行时配置写空，脚本在非 backend 组件路径会先读取远端 `.env` 并回填已有值。

`NasShare` 现在默认留空，由配置文件或显式参数决定，不再在脚本中写死共享名。

## 补充修复

2026-06-03 的真实 website-only 发布包部署发现 `deploy-release` 仍无条件校验 backend `required-sql` 包内容，已补充回归测试并改为仅 `$publishBackend` 时执行。

## 最终验证结果

- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py` -> PASS，45 passed。
- `python -m pytest script/tests/test_publish_int_ruoyi_deploy_services.py` -> PASS，6 passed。
- `python -m pytest script/tests/test_edhr_protected_storage_publish_tooling.py` -> PASS，4 passed。
