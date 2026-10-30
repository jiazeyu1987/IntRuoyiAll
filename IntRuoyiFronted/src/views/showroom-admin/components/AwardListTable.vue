<template>
  <div class="showroom-award-list">
    <UnifiedListTemplate
      @query="emitSearch"
      @reset="resetSearch"
      :query-loading="loading"
      table-key="showroom.award.main"
      v-model:limit="localPageSize"
      v-model:page="localPageNo"
      :total="pageTotal"
      @pagination="emitPageChange"
      :load-filter-rows="loadColumnFilterRows0"
      :list-rows="awards"
      @filtered-rows="emit('filteredRows', $event)"
      @column-filter-reset="emitPageChange"
      :columns="listFilterColumns0"
      :show-column-settings="false"
      :column-filter-definitions="[...listColumnFilterDefinitions0, ...allColumnDisplayFilters0]"
    >
      <template #filters>
        <div>
          <el-input
            v-model="draftKeyword"
            class="showroom-award-list__search"
            clearable
            placeholder="搜索奖项名称或编码"
            @keyup.enter="emitSearch"
          />
        </div>
      </template>

      <template #table="{ resolveRows }">
        <el-table stripe v-loading="loading" :data="resolveRows(awards)" border row-key="awardId">
          <el-table-column label="奖项编码" width="130" prop="awardCode" show-overflow-tooltip />
          <el-table-column label="中文名称" min-width="180" prop="nameCn" show-overflow-tooltip />
          <el-table-column label="英文名称" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.nameEn || '未填写' }}</template>
          </el-table-column>
          <el-table-column label="颁发单位" min-width="170" prop="issuer" show-overflow-tooltip />
          <el-table-column label="日期/期限" width="130" prop="awardDateText" show-overflow-tooltip />
          <el-table-column label="封面" width="104">
            <template #default="{ row }">
              <el-image
                v-if="row.coverImageUrl"
                :src="row.coverImageUrl"
                :preview-src-list="[row.coverImageUrl]"
                class="showroom-award-list__cover"
                fit="cover"
                preview-teleported
              />
              <el-tag v-else type="danger" effect="plain">缺封面</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.incomplete ? 'warning' : 'success'" effect="plain">
                {{ row.incomplete ? '待完善' : '已发布' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="emit('edit', row)">编辑</el-button>
              <el-button
                link
                type="primary"
                :loading="props.generatingCoverAwardId === row.awardId"
                :disabled="props.generatingCoverAwardId === row.awardId"
                @click="emit('generateCover', row)"
              >
                生图
              </el-button>
              <el-button link type="danger" @click="emit('delete', row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </div>
</template>

<script setup lang="ts">
import { loadAllListPages, type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'

import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import type { ShowroomAwardPageRowRespVO } from '@/api/showroom-admin'

defineOptions({ name: 'AwardListTable' })

const props = defineProps<{
  awards: ShowroomAwardPageRowRespVO[]
  loadFilterRows: () => Promise<ShowroomAwardPageRowRespVO[]>
  loading?: boolean
  generatingCoverAwardId?: number | null
  keyword?: string
  pageNo: number
  pageSize: number
  pageTotal: number
}>()

const emit = defineEmits<{
  search: [keyword: string]
  filteredRows: [rows: ShowroomAwardPageRowRespVO[]]
  pageChange: [pagination: { pageNo: number; pageSize: number }]
  edit: [award: ShowroomAwardPageRowRespVO]
  generateCover: [award: ShowroomAwardPageRowRespVO]
  delete: [award: ShowroomAwardPageRowRespVO]
}>()

const draftKeyword = ref(props.keyword || '')
const localPageNo = ref(props.pageNo)
const localPageSize = ref(props.pageSize)

watch(() => props.keyword, (value) => {
  draftKeyword.value = value || ''
})
watch(() => props.pageNo, (value) => {
  localPageNo.value = value
})
watch(() => props.pageSize, (value) => {
  localPageSize.value = value
})

const emitSearch = () => emit('search', draftKeyword.value.trim())
const resetSearch = () => {
  draftKeyword.value = ''
  emitSearch()
}
const emitPageChange = () => {
  emit('pageChange', { pageNo: localPageNo.value, pageSize: localPageSize.value })
}

const loadColumnFilterRows0 = async () => {
  return props.loadFilterRows()
}

const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'incomplete',
    label: '状态',
    type: 'text',
    getValue: (row) => [row.incomplete ? '待完善' : '已发布'].join(' ')
  }
])

const listFilterColumns0 = [
  {
    key: 'awardCode',
    label: '奖项编码',
    visible: true
  },
  {
    key: 'nameCn',
    label: '中文名称',
    visible: true
  },
  {
    key: 'nameEn',
    label: '英文名称',
    visible: true
  },
  {
    key: 'issuer',
    label: '颁发单位',
    visible: true
  },
  {
    key: 'awardDateText',
    label: '日期/期限',
    visible: true
  },
  {
    key: 'coverImageUrl',
    label: '封面',
    visible: true
  },
  {
    key: 'incomplete',
    label: '状态',
    visible: true
  }
]

const allColumnDisplayFilters0: ListColumnFilterDefinition[] = [
  {
    key: 'nameEn',
    label: '英文名称',
    type: 'text',
    getValue: (row) => [row.nameEn || '未填写'].join(' ')
  },
  {
    key: 'coverImageUrl',
    label: '封面',
    type: 'select',
    options: [
      { label: '有', value: true },
      { label: '无', value: false }
    ],
    getValue: (row) => Boolean(row.coverImageUrl)
  },
  {
    key: 'incomplete',
    label: '状态',
    type: 'text',
    getValue: (row) => [row.incomplete ? '待完善' : '已发布'].join(' ')
  }
]
</script>

<style scoped>
.showroom-award-list {
  display: flex;
  flex-direction: column;
}

.showroom-award-list__toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
}

.showroom-award-list__search-group {
  display: flex;
  flex: 1;
  gap: 8px;
}

.showroom-award-list__search {
  max-width: 360px;
}

.showroom-award-list__table-shell {
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-top: 0;
}

.showroom-award-list__cover {
  width: 64px;
  height: 44px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
}

.showroom-award-list__footer {
  display: flex;
  justify-content: flex-end;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-top: 0;
  border-radius: 0 0 8px 8px;
}
</style>
