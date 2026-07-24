# IntRuoyi Production Publish Evidence

## Environment

Current delivery target is the isolated IntRuoyi production runtime on `172.30.30.57`.

## Commands

Current production publishing uses the unified PowerShell entrypoint with `-Environment prod -ConfirmText PROD` to publish the current local workspace to production `172.30.30.57`. The flow builds the frontend for the production backend origin, transfers backend/frontend images, synchronizes MySQL and MinIO `yudao` data, recreates production containers, and verifies live URLs.

## Secrets

This release requires runtime-only SSH, MySQL, and MinIO credentials. The repository stores no credential values; the shared publish script reads them from running containers or operator access at execution time.

## Pipeline

The effective production release pipeline is:
local package -> production-target frontend build -> image transfer and load -> MinIO mirror -> MySQL reset/import -> production compose start -> remote and external verification.

## Verification

Verification covered promotion execution, a recovered MinIO object transfer, remote service startup, remote and external HTTP health, synchronized MinIO file access, database row-count probes, production backend URL baked into the frontend, real production login, and live production container state.

## Rollback

1. Check the previous production image tags:
   `ssh root@172.30.30.57 "docker images --format '{{.Repository}}:{{.Tag}}' | grep '^intruoyi-'"`.
2. Edit `/opt/intruoyi/runtime/.env` and replace `IMAGE_TAG=<current>` with the previous known-good tag.
3. Recreate only the app containers:
   `ssh root@172.30.30.57 "cd /opt/intruoyi/runtime && docker compose up -d backend frontend"`.
4. If data rollback is required, restore a known-good database dump before recreating the backend.
5. Re-verify:
   `ssh root@172.30.30.57 "curl -fsS http://127.0.0.1:48081/actuator/health"`.

## Blockers

All blockers are currently resolved. During the 2026-05-27 production publish, one MinIO object transfer failed before MySQL reset/import. The failed object was copied with the same source and target MinIO credentials, matching ETag `886d4806fe00ab0aafaa907211d5ff0d`, and the remaining publish steps were continued in the original order.

## Production Target

- Source workspace:
  `D:\ProjectPackage\Int\IntRuoyi`
- Owning backend repository:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Frontend source repository:
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Target environment:
  production server `172.30.30.57`
- Remote application directory:
  `/opt/intruoyi/runtime`
- Remote exposed ports:
  frontend `8081`, backend `48081`
- Remote runtime units:
  `intruoyi-mysql`, `intruoyi-redis`, `intruoyi-backend`, `intruoyi-frontend`, `intruoyi-website`

## Green Evidence

- Source test release:
  `IMAGE_TAG=20260526_234128`
- Production release result:
  `IMAGE_TAG=20260526_234128`
- The verified production publish behavior is now exposed through `script\deploy\publish-int-ruoyi.ps1 -Environment prod -ConfirmText PROD`; the recorded production run reached image transfer and MinIO mirror, one failed MinIO object was copied successfully, and the remaining script steps were continued manually in the same order.
- Production backend health:
  `http://172.30.30.57:48081/actuator/health` -> `{"status":"UP"}`
- Production frontend:
  `http://172.30.30.57:8081/` -> HTTP `200`
- Production Website:
  `http://172.30.30.57:8083/` -> HTTP `200`
  `http://172.30.30.57:8083/showroom` -> HTTP `200`
- Production file-object proof:
  `http://172.30.30.57:9000/yudao/dcc/original/20260523/INT%E2%88%95RE%E2%88%954.2.4-02%EF%BC%88E%E2%88%950%EF%BC%89%E6%96%87%E4%BB%B6%E5%8F%91%E6%94%BE%E2%88%95%E5%9B%9E%E6%94%B6%E8%AE%B0%E5%BD%95%E8%A1%A8%20-%20%E7%91%9B%E6%B3%B0.xls` -> HTTP `200`, size `60928`.
- Production database probes:
  `system_tenant=4`, `infra_file=5735`, `showroom_product=191`.
- Production frontend backend target:
  generated JS contains `172.30.30.57:48081` and does not contain `172.30.30.58`.
- Playwright production login:
  `芋道源码/admin/admin123` reached `http://172.30.30.57:8081/index`; login and permission requests used `172.30.30.57:48081`.
- Playwright production Website:
  `/showroom` title `瑛泰展厅`, release documents and audio assets returned HTTP `200`, console error count `0`.
- Production container state:
  - `intruoyi-backend:20260526_234128`
  - `intruoyi-frontend:20260526_234128`
  - `intruoyi-mysql` healthy
  - `intruoyi-redis` healthy
  - `intruoyi-website` running
