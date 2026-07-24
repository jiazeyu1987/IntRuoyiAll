import json
from pathlib import Path


EVIDENCE = Path(
    "doc/tasks/20260605-with-data-release-package-dcc-e2e/"
    "with-data-release-package-evidence.json"
)


def test_with_data_release_package_carries_dcc_object_snapshot_and_inventory():
    data = json.loads(EVIDENCE.read_text(encoding="utf-8"))

    assert data["releaseTag"].startswith("20260605_with_data_dcc_verify_")
    assert data["component"] == "backend"
    assert data["publishScope"] == "with-data"
    assert data["nasConfigName"] == "manual-backup-publish-20260601-005831.json"
    assert data["nasReleaseRoot"] == "Backup/ReleasePackage"
    assert data["nasReleasePackageUploaded"] is True
    assert data["localReleaseDirRemoved"] is True

    package = data["package"]
    assert package["releaseManifestPresent"] is True
    assert package["releaseManifestPublishScope"] == "with-data"
    assert package["databaseDumpPresent"] is True
    assert package["minioYudaoSnapshotPresent"] is True
    assert package["minioYudaoObjectCount"] > 0
    assert package["dccObjectInventoryPresent"] is True
    assert package["dccObjectInventoryObjectCount"] > 0
    assert package["dccObjectInventoryObjectCount"] <= package["minioYudaoObjectCount"]
    assert package["dccObjectInventoryMissingObjectCount"] == 0
    assert package["dccObjectInventorySha256Present"] is True
