<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">
        <el-icon :size="22"><Guide /></el-icon>
        <span>动物园旅游助手</span>
      </div>
      <el-menu :default-active="$route.path" router class="menu">
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>智能对话</span>
        </el-menu-item>
        <el-menu-item index="/zoo">
          <el-icon><OfficeBuilding /></el-icon>
          <span>动物园浏览</span>
        </el-menu-item>
        <el-menu-item index="/admin">
          <el-icon><Setting /></el-icon>
          <span>数据管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; }
.layout { height: 100vh; }
.aside { background: #1f2d3d; }
.aside .logo {
  display: flex; align-items: center; gap: 8px;
  color: #fff; font-size: 16px; font-weight: 600;
  padding: 18px 16px;
}
.aside .menu { border-right: none; }
.aside .el-menu { --el-menu-bg-color: #1f2d3d; --el-menu-text-color: #cfd8e3; }
.aside .el-menu-item.is-active { background: #337ecc; color: #fff; }
.main { padding: 0; background: #f5f7fa; }
</style>
