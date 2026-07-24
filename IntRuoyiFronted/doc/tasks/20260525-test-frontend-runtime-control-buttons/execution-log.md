# 执行日志：发布测试服前端运行控制台按钮

BDD: 测试服前端显示运维按钮 -> Given 测试服前端部署最新 `int_main` 产物且 `admin` 拥有运行控制台运维权限, When `芋道源码/admin` 打开运行控制台, Then 页面显示 `发布测试服`、`提升正式服`、`立即备份`、`回滚版本`、`恢复数据`。

BDD: 前端单独发布不影响后端数据 -> Given 后端仓库存在无关未提交改动, When 修复测试服按钮可见性, Then 只重建并替换 `intruoyi-frontend` 镜像，不重启后端、不修改 `.env` 的 `IMAGE_TAG`。

RED: Playwright real frontend path `http://172.30.30.58:8081/infra/monitors/runtime-control` with `芋道源码/admin` -> FAIL, visible buttons were only `刷新` and `重启`; expected operation buttons were absent.

GREEN: `pnpm ts:check` -> PASS.

GREEN: `node node_modules\vite\bin\vite.js build --mode test` with `VITE_BASE_URL=http://172.30.30.58:48081`, `VITE_BASE_PATH=/`, `VITE_OUT_DIR=dist-intruoyi-test` -> PASS, build output generated.

GREEN: `docker build -t intruoyi-frontend:20260525_135729 -f ruoyi-vue-pro\script\deploy\int-ruoyi-test\Dockerfile.frontend .` -> PASS.

GREEN: `scp intruoyi-frontend-20260525_135729.tar root@172.30.30.58:/opt/intruoyi/runtime/` and remote `docker load && docker compose up -d --no-deps --force-recreate frontend` -> PASS, only `intruoyi-frontend` container was recreated.

GREEN: Playwright real frontend path `http://172.30.30.58:8081/infra/monitors/runtime-control` with `芋道源码/admin` -> PASS, visible buttons: `发布测试服`、`提升正式服`、`立即备份`、`回滚版本`、`恢复数据`; `无运维操作权限` not visible.

GREEN: `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc\tasks\20260525-test-frontend-runtime-control-buttons\ci-cd-evidence.md` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-test-frontend-runtime-control-buttons --mode preview` -> PASS, only temporary `ci-cd-evidence.md` was listed for deletion; no blockers or warnings.
