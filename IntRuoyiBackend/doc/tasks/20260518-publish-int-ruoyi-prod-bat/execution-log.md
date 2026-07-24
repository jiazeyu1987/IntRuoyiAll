BDD: production wrapper should require explicit intent -> Given production release is higher risk than the test release / When an operator launches the production `.bat` wrapper / Then the wrapper must target the production host by default and require an explicit `PROD` confirmation unless the operator intentionally passes a bypass flag.
BDD: production wrapper should keep a safe verification path -> Given this task only adds the wrapper and does not require a real production release / When the operator runs the wrapper with `cancel` / Then it should exit cleanly without calling the PowerShell publish script.
- RED: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, no dedicated `publish-int-ruoyi-to-prod.bat` wrapper existed yet.
- GREEN: inspected production server `172.30.30.57` and confirmed there was no existing `/opt/intruoyi/runtime` directory and no current `intruoyi-*` containers, so a dedicated production wrapper could safely pin the same isolated runtime path and ports `8081 / 48081`.
- GREEN: added `script/deploy/publish-int-ruoyi-to-prod.bat` with:
  - fixed production host `172.30.30.57`
  - fixed remote runtime dir `/opt/intruoyi/runtime`
  - fixed ports `8081 / 48081`
  - `cancel` safety mode
  - explicit `PROD` confirmation before real publish
  - `default / skip-db / skip-minio / skip-data` preset support
- GREEN: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, `6 passed`.
- GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat cancel` -> PASS, wrapper returned safely with `[INFO] Publish cancelled.` and did not trigger a production release.
