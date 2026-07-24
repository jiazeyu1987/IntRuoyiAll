# Execution Log

BDD: local runtime-control starts a stable Website readback target -> Given showroom publish verification targets `http://127.0.0.1:4173` / When runtime-control starts the Website component / Then it must launch a stable preview runtime and wait until the scoped current-release endpoint becomes reachable.

BDD: publish verification should not depend on the Vite dev server event loop -> Given the backend readback verifier only needs the public Website contract / When local startup provisions the Website component / Then the provisioned runtime should serve built static assets plus proxied release JSON instead of a hot-reload development server.

RED: `python -m pytest script/tests/test_runtime_control_scripts.py -q` -> FAIL, `restart-int-ruoyi-local.ps1` had no `Wait-WebsiteReadbackReady` contract and did not assert Website readback readiness before returning.

GREEN: `python -m pytest script/tests/test_runtime_control_scripts.py -q` -> PASS, `6` tests.

GREEN: `powershell -ExecutionPolicy Bypass -File .\script\deploy\restart-int-ruoyi-local.ps1 -Component website` -> PASS, startup now injects `WEBSITE_RUNTIME_MODE=preview` and waits until `http://127.0.0.1:4173/showroom/sites/yingtai-showroom/stages/TEST/release/current` returns JSON.
