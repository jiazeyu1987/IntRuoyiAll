BDD: app-config should read the current company live narration bundle -> Given the current live company revision has matching published ZH/EN company narrations / When anonymous `GET /showroom/display/app-config` is requested / Then the endpoint must return 200 and aggregate the current company narration text and audio URLs.

BDD: runtime should fail fast on narration revision drift -> Given the current company revision and live company narration revisions no longer match / When anonymous `GET /showroom/display/app-config` is requested / Then the backend must return the exact `source revision mismatch` error instead of falling back.

RED: `Invoke-WebRequest http://127.0.0.1:48081/showroom/display/app-config` -> FAIL, `SHOWROOM_TARGET_NOT_FOUND: live company ZH narration source revision mismatch` followed by `SHOWROOM_TARGET_NOT_FOUND: live product ZH narration source revision mismatch`.

GREEN: `Get-Content -Raw -Encoding utf8 ...\\restore-local-app-config-minimal.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro` -> PASS

GREEN: `Invoke-WebRequest http://127.0.0.1:48081/showroom/display/app-config` -> PASS, HTTP 200 with `code=0`

INFO: 本地联调用产品被收敛到当前 live revision 已对齐讲解的 product `251`，并为其补齐 preview asset 与 hall mapping。
