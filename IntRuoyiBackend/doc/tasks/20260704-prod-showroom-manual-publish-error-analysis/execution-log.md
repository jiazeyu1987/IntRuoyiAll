# 执行日志

BDD: manual showroom publish failure analysis -> Given production admin page shows “展厅发布失败：系统异常” after clicking manual publish, When we inspect the backend route, production logs and release configuration without repeating the write action, Then the root cause is identified with concrete evidence and a safe fix path.

GREEN: experience-preflight -> PASS, 已读取 PowerShell、服务器访问、登录、发布备份恢复门禁；本阶段只做本地代码和正式服只读日志/配置排查。

GREEN: production-status-readonly -> PASS, 正式服运行版本为 `release-20260703-2335-codeonly-three-env`，后端健康检查 HTTP 200，前端 HTTP 200，Website 容器运行中；本次未重启、未发布、未再次触发手动发布。

GREEN: frontend-entrypoint-trace -> PASS, 前端“手动发布展厅”按钮位于 `CompanyWorkbench.vue`，调用 `ShowroomAdminApi.publishRelease({ siteKey: 'yingtai-showroom', stage: 'TEST' })`，接口为 `POST /admin-api/showroom/release/publish`。

GREEN: backend-route-trace -> PASS, 后端调用链为 `ShowroomAdminController.publishRelease` -> `ShowroomApiRuntime.publishRelease` -> `ShowroomReleaseAutoPublishService.publishNow` -> `ShowroomReleasePublisherService.publishRelease` -> `ShowroomReleaseAssembler.resolveSourceSnapshot`。

GREEN: production-readonly-log-root-cause -> PASS, 正式服 `/admin-api/showroom/release/publish` 在 `ShowroomReleaseAssembler.resolveSourceSnapshot` 阶段失败，异常为 `SHOWROOM_RELEASE_HALL_BLOCKED: hallId=1 hallCode=hall_01 reason=SHOWROOM_REQUIRED_FIELD_MISSING: hall canvas layout is required`，底层由 `ShowroomHallCanvasLayoutPolicy.requireCanvasLayout` 抛出。

GREEN: production-readonly-showroom_hall_item-check -> PASS, 正式库真实展项映射表为 `showroom_hall_item`；`hall_01` 的展项映射行存在，但 `layout_x/layout_y/layout_width/layout_height` 全部为 NULL。汇总显示 hall_id 1-8 与 10-17 的展项映射均存在缺失布局，其中 `hall_01` 是本次发布栈首先命中的失败点。

## 根因结论

- 失败接口：`POST /admin-api/showroom/release/publish`。
- 直接根因：正式服展柜 `hall_01` 的 `showroom_hall_item` 展项映射缺少完整画布布局字段，release 组装强制要求每个展项都有 `layout_x/layout_y/layout_width/layout_height`，所以 fail fast。
- 影响范围：不只 `hall_01`，只读汇总显示多个展柜的展项映射布局字段为空；修完 `hall_01` 后，后续展柜也可能继续阻断发布，需一次性补齐全部展柜布局。
- 非根因：不是前端按钮问题，也不是 Website readback 校验失败；异常发生在 release 生成前的源快照组装阶段。
- 旁路问题：自动发布定时任务持续报 `Dirty showroom release requires configured auto-publish site key and stage`，说明自动发布 scope 配置缺失；这会导致 dirty 状态不能自动发布，但本次手动发布的直接失败原因是展柜 canvas layout 缺失。

## 修复建议

- 数据修复路径：通过展柜管理页面或受控 SQL 为所有正式服 `showroom_hall_item` 映射补齐完整且不重叠、覆盖 100% 画布的布局字段。
- 代码/产品改进：在管理端展柜管理页发布前置检查中直接暴露“哪些展柜缺 layout”，不要只让全局发布返回“系统异常”。
- 发布链路：同时补齐正式服 `showroom.release.auto-publish.site-key=yingtai-showroom` 与 `showroom.release.auto-publish.stage=TEST/PROD` 的配置策略，避免自动发布定时任务持续报错。