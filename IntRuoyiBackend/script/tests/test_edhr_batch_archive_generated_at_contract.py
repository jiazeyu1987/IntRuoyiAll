from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes" / "service" / "pro" / "batchrecord" / "MesProEdhrBatchExecutionServiceImpl.java"


def test_archive_manifest_serializes_generated_at_as_iso_local_datetime():
    text = SOURCE.read_text(encoding="utf-8")
    assert 'manifest.put("generatedAt", generatedAt.toString())' in text
    assert 'manifest.put("generatedAt", generatedAt);' not in text
