<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" theme="dark">
      <div class="logo">{{ collapsed ? 'CP' : '内容流水线' }}</div>
      <a-menu theme="dark" mode="inline" v-model:selectedKeys="selectedKeys" @click="onMenuClick">
        <a-menu-item key="/dashboard"><pie-chart-outlined />控制台</a-menu-item>
        <a-menu-item key="/topics"><file-text-outlined />选题管理</a-menu-item>
        <a-menu-item key="/scripts"><unordered-list-outlined />脚本审核</a-menu-item>
        <a-menu-item key="/tasks"><unordered-list-outlined />任务列表</a-menu-item>
        <a-menu-item key="/analytics"><bar-chart-outlined />数据分析</a-menu-item>
        <a-menu-item key="/templates"><copy-outlined />模板管理</a-menu-item>
        <a-menu-item key="/materials"><folder-outlined />素材库</a-menu-item>
        <a-menu-item key="/publish"><send-outlined />发布管理</a-menu-item>
        <a-menu-item key="/voice"><audio-outlined />配音管理</a-menu-item>
        <a-menu-item key="/edits"><scissor-outlined />剪辑合成</a-menu-item>
        <a-sub-menu key="settings" title="系统设置">
          <template #icon><setting-outlined /></template>
          <a-menu-item key="/settings/models">模型配置</a-menu-item>
          <a-menu-item key="/settings/platform-accounts">平台账号</a-menu-item>
          <a-menu-item key="/settings/roles">角色管理</a-menu-item>
          <a-menu-item key="/settings/users">用户管理</a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header">
        <menu-unfold-outlined v-if="collapsed" @click="collapsed = !collapsed" />
        <menu-fold-outlined v-else @click="collapsed = !collapsed" />
        <span style="margin-left: 16px">内容生产流水线系统</span>
        <div class="header-right">
          <a-dropdown>
            <a class="user-name">{{ username }}</a>
            <template #overlay>
              <a-menu @click="handleLogout">
                <a-menu-item key="logout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  PieChartOutlined, FileTextOutlined, UnorderedListOutlined,
  SendOutlined, SettingOutlined, MenuUnfoldOutlined, MenuFoldOutlined,
  BarChartOutlined, CopyOutlined, FolderOutlined, AudioOutlined, ScissorOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const collapsed = ref(false)
const selectedKeys = ref([route.path])
const username = ref(localStorage.getItem('username') || '管理员')

function onMenuClick({ key }: { key: string }) {
  router.push(key)
}

function handleLogout({ key }: { key: string }) {
  if (key === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    router.push('/login')
  }
}
</script>

<style scoped>
.logo {
  height: 64px;
  line-height: 64px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}
.header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-right { display: flex; align-items: center; }
.user-name { color: rgba(0, 0, 0, 0.85); cursor: pointer; }
.content { margin: 24px; min-height: 280px; }
</style>