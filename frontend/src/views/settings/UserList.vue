<template>
  <a-card title="用户管理">
    <template #extra>
      <a-button type="primary" @click="showAdd = true">新增用户</a-button>
    </template>
    <a-table :dataSource="users" :columns="columns" :loading="loading" rowKey="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'ENABLED' ? 'green' : 'red'">{{ statusLabel(record.status) }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="handleEdit(record)">编辑</a>
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
              <a style="color: red">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:visible="showAdd" :title="editingId ? '编辑用户' : '新增用户'" @ok="handleCreate" ok-text="保存">
      <a-form :model="form" layout="vertical">
        <a-form-item label="用户名"><a-input v-model:value="form.username" /></a-form-item>
        <a-form-item label="密码"><a-input-password v-model:value="form.password" /></a-form-item>
        <a-form-item label="昵称"><a-input v-model:value="form.nickname" /></a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="form.roleId" allowClear>
            <a-select-option v-for="r in roles" :key="r.id" :value="r.id">{{ r.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getUsers, createUser, updateUser, deleteUser } from '../../api/user'
import { getRoles } from '../../api/role'

const users = ref<any[]>([])
const roles = ref<any[]>([])
const loading = ref(false)
const showAdd = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ username: '', password: '', nickname: '', roleId: undefined })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '用户名', dataIndex: 'username' },
  { title: '昵称', dataIndex: 'nickname' },
  { title: '角色 ID', dataIndex: 'roleId' },
  { title: '状态', key: 'status' },
  { title: '创建时间', dataIndex: 'createdAt' },
  { title: '操作', key: 'action' },
]

function statusLabel(s: string) {
  const map: Record<string, string> = { ENABLED: '启用', DISABLED: '禁用', ACTIVE: '活跃' }
  return map[s] || s
}

async function load() {
  loading.value = true
  try {
    const [userRes, roleRes]: any = await Promise.all([getUsers({}), getRoles({})])
    users.value = userRes.data?.records || userRes.data || []
    roles.value = roleRes.data?.records || roleRes.data || []
  } finally { loading.value = false }
}

function handleEdit(record: any) {
  editingId.value = record.id
  form.value = { username: record.username, password: '', nickname: record.nickname || '', roleId: record.roleId }
  showAdd.value = true
}

async function handleCreate() {
  if (editingId.value) {
    const data: any = { nickname: form.value.nickname, roleId: form.value.roleId }
    if (form.value.password) data.password = form.value.password
    await updateUser(editingId.value, data)
    message.success('更新成功')
  } else {
    await createUser(form.value)
    message.success('创建成功')
  }
  showAdd.value = false
  editingId.value = null
  form.value = { username: '', password: '', nickname: '', roleId: undefined }
  load()
}

async function handleDelete(id: number) {
  await deleteUser(id)
  message.success('已删除')
  load()
}

onMounted(() => load())
</script>