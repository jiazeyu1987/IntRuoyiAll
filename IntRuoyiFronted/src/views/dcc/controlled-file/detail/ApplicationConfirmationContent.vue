<template>
  <div class="application-confirmation">
    <template v-for="section in sections" :key="section.title">
      <details v-if="section.collapsed" class="application-confirmation__section">
        <summary>{{ section.title }}</summary>
        <dl
          ><template v-for="item in section.items" :key="item.label">
            <dt>{{ item.label }}</dt
            ><dd
              ><ul v-if="Array.isArray(item.value)"
                ><li v-for="(value, index) in item.value" :key="index">{{ value }}</li></ul
              ><span v-else>{{ item.value }}</span></dd
            >
          </template></dl
        >
      </details>
      <section v-else class="application-confirmation__section">
        <h3>{{ section.title }}</h3>
        <dl
          ><template v-for="item in section.items" :key="item.label">
            <dt>{{ item.label }}</dt
            ><dd
              ><ul v-if="Array.isArray(item.value)"
                ><li v-for="(value, index) in item.value" :key="index">{{ value }}</li></ul
              ><span v-else>{{ item.value }}</span></dd
            >
          </template></dl
        >
      </section>
    </template>
    <p class="application-confirmation__notice"
      >生效与受控分别记录；新版受控且已生效时才启用并作废旧版。取消保留已填写内容。</p
    >
  </div>
</template>
<script setup lang="ts">
defineProps<{
  sections: Array<{
    title: string
    collapsed?: boolean
    items: Array<{ label: string; value: string | string[] }>
  }>
}>()
</script>
<style scoped>
:global(.el-message-box.dcc-application-confirmation) {
  width: min(760px, calc(100vw - 32px));
  max-width: calc(100vw - 32px);
}
.application-confirmation {
  max-height: 65vh;
  overflow-y: auto;
  padding-right: 12px;
  overflow-wrap: anywhere;
}
.application-confirmation__section {
  margin-bottom: 16px;
}
h3,
summary {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
summary {
  cursor: pointer;
}
dl {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: 8px 12px;
  margin: 12px 0 0;
}
dt {
  color: var(--el-text-color-secondary);
}
dd {
  margin: 0;
  white-space: pre-wrap;
}
ul {
  margin: 0;
  padding-left: 18px;
}
.application-confirmation__notice {
  padding: 12px;
  background: var(--el-color-warning-light-9);
  border-radius: 4px;
}
@media (max-width: 600px) {
  dl {
    grid-template-columns: 1fr;
    gap: 4px;
  }
  dd {
    margin-bottom: 8px;
  }
}
</style>
