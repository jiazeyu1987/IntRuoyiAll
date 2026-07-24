import pytest

from script.release.release_operation_lock import (
    ReleaseOperationLockError,
    ReleaseOperationLockStore,
    acquire_release_operation_lock,
    release_release_operation_lock,
)


def test_lock_acquire_blocks_existing_running_operation_for_same_environment() -> None:
    store = ReleaseOperationLockStore()
    acquire_release_operation_lock(store, environment="test", operation_id="op-1", release_tag="20260613")

    with pytest.raises(ReleaseOperationLockError, match="running release operation exists"):
        acquire_release_operation_lock(store, environment="test", operation_id="op-2", release_tag="20260614")


def test_lock_allows_different_environment_and_release_after_completion() -> None:
    store = ReleaseOperationLockStore()
    acquire_release_operation_lock(store, environment="test", operation_id="op-1", release_tag="20260613")
    acquire_release_operation_lock(store, environment="backup", operation_id="op-2", release_tag="20260613")

    release_release_operation_lock(store, environment="test", operation_id="op-1", status="APPLIED")
    acquire_release_operation_lock(store, environment="test", operation_id="op-3", release_tag="20260614")

    assert store.current("test")["operationId"] == "op-3"
    assert store.current("backup")["operationId"] == "op-2"


def test_lock_release_requires_matching_running_operation() -> None:
    store = ReleaseOperationLockStore()
    acquire_release_operation_lock(store, environment="test", operation_id="op-1", release_tag="20260613")

    with pytest.raises(ReleaseOperationLockError, match="does not match running operation"):
        release_release_operation_lock(store, environment="test", operation_id="op-other", status="FAILED")

    assert store.current("test")["status"] == "RUNNING"
