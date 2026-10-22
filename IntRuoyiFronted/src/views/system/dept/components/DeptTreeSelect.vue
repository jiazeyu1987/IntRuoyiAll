<!--
  部门树选择组件（通用复用）

  功能：搜索输入框 + 部门树，支持点击同一节点取消选中
  Props:
    filterPlaceholder — 搜索输入框占位文字（默认 '请输入部门名称'）
  Events:
    node-click(deptId: number | undefined) — 点击节点后触发，取消选中时传 undefined
  Expose:
    reset() — 清空选中状态（供外部重置按钮调用）
-->
<template>
  <div class="h-full" :class="{ 'with-supervisors': showSupervisor }">
    <div class="flex items-center gap-8px p-[15px]">
      <el-input v-model="filterText" class="flex-1" clearable :placeholder="filterPlaceholder">
        <template #prefix>
          <Icon icon="ep:search" />
        </template>
      </el-input>
      <TreeExpandActions @expand="expandAll" @collapse="collapseAll" />
    </div>
    <el-scrollbar class="!h-[calc(100%-32px-30px)]">
      <el-tree
        ref="treeRef"
        :data="deptList"
        :expand-on-click-node="false"
        :filter-node-method="filterNode"
        :props="defaultProps"
        default-expand-all
        highlight-current
        node-key="id"
        @node-click="handleNodeClick"
      >
        <template #default="{ data }">
          <span class="dept-tree-node">
            <span>{{ data.name }}</span>
            <DeptSupervisorLabel v-if="showSupervisor" :supervisor="data.supervisor" />
          </span>
        </template>
      </el-tree>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { ElTree } from 'element-plus'
import * as DeptApi from '@/api/system/dept'
import DeptSupervisorLabel from './DeptSupervisorLabel.vue'
import { defaultProps, handleTree } from '@/utils/tree'
import TreeExpandActions from '@/components/TreeExpandActions/index.vue'
import { setElementTreeExpandState } from '@/utils/treeExpand'

defineOptions({ name: 'DeptTreeSelect' })

const props = withDefaults(
  defineProps<{
    filterPlaceholder?: string // 搜索输入框占位文字
    departments?: DeptApi.DeptVO[]
    showSupervisor?: boolean
  }>(),
  {
    filterPlaceholder: '请输入部门名称'
  }
)

const emit = defineEmits<{
  'node-click': [deptId: number | undefined]
}>()

const filterText = ref('') // 过滤关键字
const deptList = ref<Tree[]>([]) // 部门树数据
const treeRef = ref<InstanceType<typeof ElTree>>() // 树 Ref
let currentNodeId: number | null = null // 当前选中的节点 ID

/** 加载部门树 */
const loadTree = async () => {
  const res = props.departments !== undefined ? props.departments : await DeptApi.getSimpleDeptList()
  deptList.value = handleTree(res)
}

watch(() => props.departments, (value) => {
  if (value !== undefined) deptList.value = handleTree(value)
})

/** 基于名字过滤 */
const filterNode = (name: string, data: Tree) => {
  if (!name) return true
  return data.name.includes(name)
}

/** 监听过滤关键字 */
watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

/** 处理节点点击：支持点击同一节点取消选中 */
const handleNodeClick = (row: { [key: string]: any }) => {
  if (currentNodeId === row.id) {
    // 再次点击同一节点 → 取消选中
    treeRef.value?.setCurrentKey(undefined)
    currentNodeId = null
    emit('node-click', undefined)
  } else {
    // 选中新节点
    currentNodeId = row.id
    emit('node-click', row.id)
  }
}

/** 清空选中状态（供外部重置按钮调用） */
const reset = () => {
  currentNodeId = null
  filterText.value = ''
  treeRef.value?.setCurrentKey(undefined)
}

const setCurrent = (deptId: number) => {
  treeRef.value?.setCurrentKey(deptId)
}

const expandAll = () => {
  setElementTreeExpandState(treeRef as any, true)
}

const collapseAll = () => {
  setElementTreeExpandState(treeRef as any, false)
}

defineExpose({ reset, setCurrent })

/** 初始化 */
onMounted(async () => {
  await loadTree()
})
</script>

<style scoped>
.with-supervisors .dept-tree-node { display: flex; flex-direction: column; padding: 5px 0; line-height: 20px; }
.with-supervisors :deep(.el-tree-node__content) { height: auto; min-height: 30px; }
</style>
