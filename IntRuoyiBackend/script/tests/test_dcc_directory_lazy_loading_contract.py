from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def test_dcc_directory_children_api_uses_parent_scoped_queries_for_managers():
    service = read_text(
        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/directory/"
        "DccDirectoryAdminServiceImpl.java"
    )
    mapper = read_text(
        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/directory/"
        "DccFileDirectoryMapper.java"
    )

    assert "private List<DccVisibleDirectoryNode> listManagedChildDirectories(Long parentId)" in service
    assert "directoryMapper.selectEnabledListByParentId(parentId)" in service
    assert "directoryMapper.selectEnabledParentIdsByParentIds(childIds)" in service
    assert "default List<Long> selectEnabledParentIdsByParentIds(Collection<Long> parentIds)" in mapper

    list_children_start = service.index("public List<DccVisibleDirectoryNode> listVisibleChildDirectories")
    search_start = service.index("public List<DccVisibleDirectoryNode> searchVisibleDirectories")
    list_children_body = service[list_children_start:search_start]
    manager_branch_index = list_children_body.index(
        "if (accessPermissionService.hasDirectoryManagementPermission(userId))"
    )
    full_tree_query_index = list_children_body.index("directoryMapper.selectEnabledList()")

    assert manager_branch_index < full_tree_query_index
    assert "return listManagedChildDirectories(parentId);" in list_children_body


def test_dcc_directory_search_api_uses_keyword_scoped_queries_for_managers():
    service = read_text(
        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/directory/"
        "DccDirectoryAdminServiceImpl.java"
    )
    mapper = read_text(
        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/directory/"
        "DccFileDirectoryMapper.java"
    )

    assert "private List<DccVisibleDirectoryNode> searchManagedDirectories(String keyword, int limit)" in service
    assert "directoryMapper.selectEnabledListByKeyword(keyword, limit)" in service
    assert "default List<DccFileDirectoryDO> selectEnabledListByKeyword(String keyword, int limit)" in mapper

    search_start = service.index("public List<DccVisibleDirectoryNode> searchVisibleDirectories")
    get_directory_start = service.index("public DccFileDirectoryDO getDirectory")
    search_body = service[search_start:get_directory_start]
    manager_branch_index = search_body.index(
        "if (accessPermissionService.hasDirectoryManagementPermission(userId))"
    )
    full_tree_query_index = search_body.index("directoryMapper.selectEnabledList()")

    assert manager_branch_index < full_tree_query_index
    assert "return searchManagedDirectories(normalizedKeyword, normalizedLimit);" in search_body
