<template>
  <ContentWrap title="代码生成案例" message="用于验证基础设施下的代码生成示例路由入口。">
    <div class="demo-toolbar">
      <div class="demo-summary">
        <span class="demo-summary__title">案例入口</span>
        <span class="demo-summary__text">选择任一案例进入对应的代码生成示例页面。</span>
      </div>
      <div class="demo-toolbar__actions">
        <el-button
          v-for="item in routeItems"
          :key="item.path"
          type="primary"
          plain
          @click="handleGo(item.path)"
        >
          {{ item.title }}
        </el-button>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-table :data="routeItems" row-key="path" :show-overflow-tooltip="true">
      <el-table-column label="案例名称" prop="title" min-width="180" />
      <el-table-column label="路由" prop="path" min-width="240" />
      <el-table-column label="说明" prop="description" min-width="320" />
      <el-table-column label="操作" align="center" width="120">
        <template #default="scope">
          <el-button link type="primary" @click="handleGo(scope.row.path)">进入</el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>
</template>

<script setup lang="ts">
defineOptions({ name: 'InfraTestDemo' })

const { push } = useRouter()

const routeItems = [
  {
    title: '单表（增删改查）',
    path: '/infra/demo/demo01-contact',
    description: '基础单表 CRUD 示例。'
  },
  {
    title: '树表（增删改查）',
    path: '/infra/demo/demo02-category',
    description: '树形分类数据的 CRUD 示例。'
  },
  {
    title: '主子表（标准）',
    path: '/infra/demo/demo03-normal',
    description: '标准主子表关系示例。'
  },
  {
    title: '主子表（ERP）',
    path: '/infra/demo/demo03-erp',
    description: 'ERP 风格的主子表编辑示例。'
  },
  {
    title: '主子表（内嵌）',
    path: '/infra/demo/demo03-inner',
    description: '内嵌子表编辑示例。'
  }
]

const handleGo = (path: string) => {
  push(path)
}
</script>

<style lang="scss" scoped>
.demo-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.demo-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.demo-summary__title {
  font-size: 14px;
  font-weight: 600;
  color: #172033;
}

.demo-summary__text {
  font-size: 13px;
  color: #4b5563;
}

.demo-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
