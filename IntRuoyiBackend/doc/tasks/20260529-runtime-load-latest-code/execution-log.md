# Execution Log

BDD: Runtime backend loads latest code -> Given IntRuoyi repository HEAD is `f89118240f`, When the local backend runtime is restarted, Then port `48081` must expose healthy backend endpoints from that worktree.

BDD: Website scoped release path serves JSON -> Given Website has the scoped release proxy/fetch fix, When `/showroom/sites/yingtai-showroom/stages/TEST/release/current` is requested through the Website origin, Then it must return release JSON rather than the Vite/app `index.html`.

START: Created runtime verification task record before additional runtime/config work.

CHECK: backend repository HEAD -> `f89118240f 任务: 导入产品清单同步展柜映射`.

CHECK: Website repository HEAD -> `c9ed5c8 任务: 记录展厅发布数据源诊断`.

BLOCKER: local backend restart before explicit DCC config -> FAIL, `DCC electronic signature evidence configuration is missing`; impact: local backend `48081` could not load latest code until explicit runtime config was supplied.

GREEN: local backend restart with explicit runtime config `DCC_SIGNATURE_EVIDENCE_HMAC_SECRET=CODEX-DCC-E2E-HMAC-SECRET-20260526`, `DCC_SIGNATURE_EVIDENCE_KEY_VERSION=dcc-hmac-v1` -> PASS, `GET http://127.0.0.1:48081/actuator/health` returned `{"status":"UP"}`.

GREEN: local scoped current-release probe -> PASS, backend `48081`, Website `4173`, and Website `5188` all returned JSON release `20260529T062609Z-2c8e98f943b3`.

RED: clean detached build runtime on `48082` -> FAIL, pure `f89118240f` startup threw `No default constructor found` for `ShowroomPublicReleaseReadbackVerifier`.

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomPublicReleaseReadbackVerifierSpringWiringTest,ShowroomPublicReleaseReadbackVerifierTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests.

RED: test Website scoped current before nginx refresh -> FAIL, `GET http://172.30.30.58:8083/showroom/sites/yingtai-showroom/stages/TEST/release/current` returned `text/html` `index.html`.

INSPECT: remote `/opt/intruoyi/runtime/website/nginx.conf` lacked the `/showroom/sites/` proxy locations even though the current local template includes them.

GREEN: remote Website nginx refresh -> PASS, uploaded current template-rendered nginx config to `172.30.30.58:/opt/intruoyi/runtime/website/nginx.conf`, recreated `intruoyi-website`, and `nginx -t` succeeded in the container.

GREEN: test Website scoped current after nginx refresh -> PASS, backend `172.30.30.58:48081` and Website `172.30.30.58:8083` both returned release `20260528T213138Z-0bd139dadc8f` and manifest hash `9bb4622a55ccc86e2dfbb422066d5dfd9ae2e67353a6d1958d6b34441c93d992`.
