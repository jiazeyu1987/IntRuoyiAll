BDD: bat wrapper should offer explicit preset publish modes -> Given operators may launch the wrapper by double-click without command-line arguments / When the bat file starts with no arguments / Then it should present a Chinese menu for default publish, skip-database publish, skip-MinIO publish, skip-all-data publish, and cancel.
BDD: direct command mode should remain stable -> Given automation or operators may still call the bat file with explicit arguments / When the first argument is a known preset like `default` or `skip-db` / Then the wrapper must map that preset to the same verified PowerShell publish script without changing release semantics.
- RED: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, the original `.bat` wrapper had no interactive menu or preset mode mapping.
- GREEN: enhanced `publish-int-ruoyi-to-test.bat` with:
  - no-argument menu mode
  - preset modes `default / skip-db / skip-minio / skip-data`
  - invalid-choice failure
  - explicit cancel path
  - preserved direct passthrough behavior for custom arguments
- GREEN: updated `publish-int-ruoyi-to-test.ps1` command resolution so Windows wrapper executions prefer a sibling `.cmd` launcher when a tool like `pnpm` is exposed as both `pnpm.ps1` and `pnpm.cmd`.
- GREEN: converted the `.bat` file to Windows `CRLF` line endings and kept the menu text ASCII-safe so `cmd.exe` no longer mis-parses the script.
- GREEN: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, `5 passed`.
- GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.bat default` -> PASS, the preset mode completed a full real publish and printed the final frontend/backend URLs.
