<template>
  <ContentWrap>
    <el-table
      row-key="id"
      v-loading="loading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="true"
    >
      <el-table-column label="编号" align="center" prop="id" />
      <el-table-column label="名字" align="center" prop="name" />
      <el-table-column label="班主任" align="center" prop="teacher" />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180px"
      />
    </el-table>
  </ContentWrap>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { Demo03Grade, Demo03StudentApi } from '@/api/infra/demo/demo03/inner'

const props = defineProps<{
  studentId?: number
}>()

const loading = ref(false)
const list = ref<Demo03Grade[]>([])

const getList = async () => {
  loading.value = true
  try {
    const data = await Demo03StudentApi.getDemo03GradeByStudentId(props.studentId)
    list.value = data ? [data] : []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
