<template>
  <a-card title="平台账号管理">
    <template #extra>
      <a-button type="primary" @click="showAdd = true">新增账号</a-button>
    </template>
    <a-table :dataSource="accounts" :columns="columns" :loading="loading" rowKey="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'platform'">
          <a-tag>{{ platformLabel(record.platform) }}</a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <a-switch v-model:checked="record.status" checkedValue="ENABLED" unCheckedValue="DISABLED" @change="(checked: boolean) => handleToggleStatus(record.id, checked)" />
        </template>
        <template v-if="column.key === 'action'">
          <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
            <a style="color: red">删除</a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="showAdd" title="新增平台账号" @ok="handleCreate" ok-text="创建">
      <a-form :model="form" layout="vertical">
        <a-form-item label="平台">
          <a-select v-model:value="form.platform">
            <a-select-option value="douyin">抖音</a-select-option>
            <a-select-option value="kuaishou">快手</a-select-option>
            <a-select-option value="xiaohongshu">小红书</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="账号名称"><a-input v-model:value="form.accountName" /></a-form-item>
        <a-form-item label="Cookies"><a-textarea v-model:value="form.cookiesEncrypted" :rows="4" /></a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getPlatformAccounts, createPlatformAccount, updatePlatformAccount, deletePlatformAccount } from '../../api/platform-account'

const accounts = ref<any[]>([])
const loading = ref(false)
const showAdd = ref(false)
const form = ref({ platform: 'douyin', accountName: '', cookiesEncrypted: '' })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '平台', key: 'platform' },
  { title: '账号名称', dataIndex: 'accountName' },
  { title: '状态', key: 'status' },
  { title: '创建时间', dataIndex: 'createdAt' },
  { title: '操作', key: 'action' },
]

function platformLabel(s: string) {
  const map: Record<string, string> = { douyin: '抖音', kuaishou: '快手', xiaohongshu: '小红书', bilibili: 'B站', weibo: '微博' }
  return map[s] || s
}

async function load() {
  loading.value = true
  try {
    const res: any = await getPlatformAccounts({})
    accounts.value = res.data?.records || res.data || []
  } finally { loading.value = false }
}

async function handleCreate() {
  await createPlatformAccount(form.value)
  message.success('创建成功')
  showAdd.value = false
  form.value = { platform: 'douyin', accountName: '', cookiesEncrypted: '' }
  load()
}

async function handleToggleStatus(id: number, checked: string | boolean) {
  // checked 为字符串 "ENABLED" / "DISABLED"（由 checkedValue / unCheckedValue 决定）
  await updatePlatformAccount(id, { status: checked === 'ENABLED' ? 'ENABLED' : 'DISABLED' })
}

async function handleDelete(id: number) {
  await deletePlatformAccount(id)
  message.success('已删除')
  load()
}

onMounted(() => load())
</script>