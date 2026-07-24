# Execution Log

BDD: 运行控制台构建发布包 -> Given 本机前后端运行的是当前仓库最新状态且用户授权访问测试服 / When 通过 Playwright 登录运行控制台并触发构建发布包 / Then 必须生成明确的发布包标识，不能用直接命令替代真实前端路径。

BDD: 发布包部署到测试服 -> Given 已通过运行控制台生成发布包 / When 将该发布包部署到测试服 / Then 发布脚本必须成功完成并通过测试服健康检查，不能静默跳过失败门禁。

BDD: 测试服媒体不挂 -> Given 发布包已部署到测试服 / When 登录测试服产品管理并加载产品列表 / Then 封面图片不显示 `加载失败`，图片请求返回 `200 image/*`，中英音频请求返回 `200 audio/*`。

INFO: 用户授权 -> 当前任务允许访问测试服 `172.30.30.58`，不包含正式服。

INFO: M1 工作区状态 -> 后端 HEAD `97695bebc486d0c3061529d03316d55e9387c278`；前端 HEAD `d8606ffe12d6632d322efb01903ed36403710c45`；后端既有未跟踪 `runtime/` 不纳入本任务；前端既有未跟踪历史任务目录不纳入本任务。

GREEN: 本机最新运行状态 -> PASS，已重启本机后端与前端；`curl http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`，`curl -I http://localhost:8081/` 返回 `HTTP/1.1 200 OK`。

RED: 运行控制台构建脚本响应等待 -> FAIL，Playwright 已点击 `构建发布包`，但既有脚本按 `POST /admin-api/infra/runtime-control/actions` 等待 60 秒超时；后端已生成操作 `37ce0b94-8327-4b46-89a3-401dd8787d51` 并继续执行，未重复提交构建。

GREEN: 运行控制台构建发布包 -> PASS，操作 `37ce0b94-8327-4b46-89a3-401dd8787d51` 状态 `succeeded`，参数 `releaseTag=26-06-02 00:12:30`、`publishScope=code-only`；日志显示后端 Maven `BUILD SUCCESS`，前端镜像 `intruoyi-frontend:26-06-02_00-12-30` 构建成功，镜像包 `2351177728` bytes，NAS 上传路径 `Backup/ReleasePackage/26-06-02_00-12-30`。

RED: 发布包选择器初版脚本 -> FAIL，Element Plus 的 `ReleasePackage` placeholder 不在 input 属性上；修正为按 `发布包` 表单项内 `.el-select__input` 选择，并校验来源目录绑定到 `26-06-02 00:12:30`。

GREEN: 部署发布包到测试服 -> PASS，Playwright 在运行控制台显式选择 `26-06-02 00:12:30` 并提交；操作 `7d586c0a-d777-4c7c-8f6a-71296203338e` 状态 `succeeded`，命令为 `deploy-release -Environment test -ReleaseTag "26-06-02 00:12:30"`，日志结尾 `Publish completed for test.`。

GREEN: 测试服健康门禁 -> PASS，`http://172.30.30.58:48081/actuator/health` 返回 `{"status":"UP"}`；`http://172.30.30.58:8081/` 返回 `HTTP 200 OK`；`http://172.30.30.58:8083/showroom` 返回 `HTTP 200 OK`；测试服容器 `intruoyi-backend` 与 `intruoyi-frontend` 镜像标签均为 `26-06-02_00-12-30`。

GREEN: 测试服产品管理封面与状态 -> PASS，Playwright 登录 `http://172.30.30.58:8081/showroom/product`，租户 `测试租户`、用户 `aoteman`；第一页 `product_001` 至 `product_020` 共 20 条产品，20 张封面请求全部 `HTTP 200 image/png`，图片 `naturalWidth/naturalHeight > 0`，页面无 `加载失败`；中音频 OK 状态 20 个、英音频 OK 状态 20 个，缺失音频状态 0 个。

GREEN: 测试服音频 HTTP 抽样 -> PASS，`product_001` 至 `product_005` 中英共 10 条已发布音频；`http://172.30.30.58:8081/admin-api/infra/file/28/get/...` 全部 `HTTP 200 audio/vnd.wave`，`http://172.30.30.58:9000/yudao/...` 全部 `HTTP 200 audio/x-wav`。

GREEN: 测试服文件配置绑定 -> PASS，`infra_file_config.id=28` 为 `endpoint=http://host.docker.internal:9000`、`domain=http://172.30.30.58:9000/yudao`、`has_localhost=0`。

GREEN: task-closeout-cleanup 预览 -> PASS，`mode=preview` 仅计划删除 `doc/tasks/20260602-runtime-console-build-deploy-test-media/artifacts/` 下临时 SQL、脚本、日志、截图与 JSON；保留 `task.md`、`execution-log.md`；blocked/warnings 均为 none。

GREEN: task-closeout-cleanup apply -> PASS，已删除本任务 `artifacts/` 下临时 SQL、脚本、日志、截图与 JSON；保留 `task.md`、`execution-log.md`；blocked/warnings 均为 none。
