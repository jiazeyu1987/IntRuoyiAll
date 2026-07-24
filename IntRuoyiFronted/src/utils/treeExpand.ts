import { nextTick, ref, type Ref } from 'vue'

export const setElementTreeExpandState = (treeRef: Ref<any>, expanded: boolean) => {
  const nodesMap = treeRef.value?.store?.nodesMap
  if (!nodesMap) {
    return
  }
  Object.values(nodesMap).forEach((node: any) => {
    if (node && typeof node.expanded === 'boolean') {
      node.expanded = expanded
    }
  })
}

export const useTreeTableExpand = (initialExpanded = true) => {
  const isExpandAll = ref(initialExpanded)
  const refreshTable = ref(true)

  const setExpandState = async (expanded: boolean) => {
    if (isExpandAll.value === expanded && refreshTable.value) {
      return
    }
    refreshTable.value = false
    isExpandAll.value = expanded
    await nextTick()
    refreshTable.value = true
  }

  return {
    isExpandAll,
    refreshTable,
    expandAll: () => setExpandState(true),
    collapseAll: () => setExpandState(false)
  }
}
