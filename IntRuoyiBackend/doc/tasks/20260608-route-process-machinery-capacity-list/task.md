# 工艺路线设备列表显示产能

## 任务目标

在 MES 工艺路线工序的设备列表弹窗中，展示每台设备的设备工序单台产能和按绑定数量计算的总产能，便于用户从设备列表直接核对产能预算。

## 里程碑

1. 建立任务文档并记录 BDD 场景。
2. 为工艺路线工序设备列表接口补充失败测试，证明设备列表应返回单台产能和总产能。
3. 后端在设备列表 VO 中返回设备工序单台产能与总产能，产能只来自 `mes_dv_machinery_process` 的设备+工序记录。
4. 前端设备列表弹窗新增 `单台产能/h` 和 `总产能/h` 两列。
5. 运行后端目标测试、前端类型检查和真实页面验证。
6. 更新任务记录并提交本次改动。

## 预期验证

- `MesProRouteProcessControllerWorkstationViewTest` 覆盖设备列表产能字段。
- 前端类型检查通过。
- 登录本机前端，打开 `/mes/pro/route`，进入工艺流程详情的设备列表弹窗，可看到单台产能和总产能。

## 当前状态

已完成。

## 完成记录

- 后端 `MesProRouteProcessMachineryRespVO` 已新增设备工序单台标准小时产能与按绑定数量计算的总标准小时产能字段。
- 后端工艺路线工序列表接口只按 `machineryId + processId` 从 `mes_dv_machinery_process` 汇总设备产能；缺少设备工序产能时返回空值，不使用设备主档或产品产能兜底。
- 前端工艺路线工序设备列表弹窗已新增 `单台产能/h` 与 `总产能/h` 两列，空值显示 `未配置`。
- 本机 48081 后端已用当前源码重建并重启；真实页面验证命中 `B010 吹球囊成型`、设备 `A03190`，单台产能和总产能均显示 `9.52381`。

## 验证结果

- RED：`mvn -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest test` 先失败，原因是设备列表 VO 尚未提供产能字段 getter。
- GREEN：`mvn -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest test` 通过，6 个用例全绿。
- GREEN：`node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` 通过。
- GREEN：`mvn -pl yudao-server -am -DskipTests package` 通过，生成包含新字段的 `yudao-server.jar`。
- GREEN：`powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` 通过，本机 `48081` 健康检查 `{"status":"UP"}`。
- GREEN：Playwright 登录本机 `芋道源码/admin` 打开 `/mes/pro/route?openId=900026`，点击 `B010` 行设备列表，弹窗显示 `单台产能/h`、`总产能/h` 和 `A03190` 的 `9.52381`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少设备工序产能时仅显示未配置，不改用设备主档产能。
- `是否从根因和长期维护角度解决`：是。设备列表展示直接读取设备+工序产能，与资源大表和排程预算来源保持一致。
- `是否存在临时补丁或绕过`：否。
