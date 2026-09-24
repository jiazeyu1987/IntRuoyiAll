<template>
  <div class="active-order-detail-layout">
    <slot></slot>
  </div>
</template>

<style scoped>
.active-order-detail-layout {
  display: grid;
  gap: 16px;
  min-width: 0;
  max-width: 100%;
  overflow-x: hidden;
}
</style>
