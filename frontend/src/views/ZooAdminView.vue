<template>
  <div class="admin-page">
    <el-tabs v-model="tab">
      <el-tab-pane label="动物园管理" name="zoo">
        <div class="toolbar">
          <el-button type="primary" @click="openCreate">新增动物园</el-button>
        </div>
        <el-table :data="zoos" stripe>
          <el-table-column prop="name" label="动物园" min-width="160" />
          <el-table-column prop="city" label="城市" width="100" />
          <el-table-column label="成人票" width="90">
            <template #default="{ row }">¥{{ fmt(row.ticketAdult) }}</template>
          </el-table-column>
          <el-table-column prop="openTime" label="开放时间" width="120" />
          <el-table-column prop="starAnimals" label="明星动物" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog v-model="dialogVisible" :title="form.id ? '编辑动物园' : '新增动物园'" width="640px">
          <el-form :model="form" label-width="90px">
            <el-row :gutter="12">
              <el-col :span="12"><el-form-item label="名称"><el-input v-model="form.name" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="城市"><el-input v-model="form.city" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="省份"><el-input v-model="form.province" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="地址"><el-input v-model="form.address" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="成人票(元)"><el-input-number v-model="form.ticketAdult" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="儿童票(元)"><el-input-number v-model="form.ticketChild" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="开放时间"><el-input v-model="form.openTime" placeholder="如 8:30-18:00" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="时间备注"><el-input v-model="form.openHoursNote" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="门票备注"><el-input v-model="form.ticketNote" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="官网"><el-input v-model="form.website" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="明星动物"><el-input v-model="form.starAnimals" placeholder="如：大熊猫萌兰、金丝猴" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item></el-col>
            </el-row>
          </el-form>
          <template #footer>
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submit">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="知识库管理" name="knowledge">
        <div class="toolbar">
          <el-button type="primary" @click="openKCreate">新增文档</el-button>
          <el-button @click="doReindex">重建向量索引</el-button>
        </div>
        <el-table :data="docs" stripe>
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column label="正文" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">{{ row.content }}</template>
          </el-table-column>
          <el-table-column prop="docSource" label="来源" width="120" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button size="small" @click="openKEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="kRemove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog v-model="kDialogVisible" :title="kForm.id ? '编辑文档' : '新增文档'" width="640px">
          <el-form :model="kForm" label-width="70px">
            <el-form-item label="标题"><el-input v-model="kForm.title" /></el-form-item>
            <el-form-item label="分类"><el-input v-model="kForm.category" placeholder="如：熊猫攻略/Tips/路线/亲子" /></el-form-item>
            <el-form-item label="正文"><el-input v-model="kForm.content" type="textarea" :rows="10" /></el-form-item>
            <el-form-item label="来源"><el-input v-model="kForm.docSource" /></el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="kDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="kSubmit">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listZoos, createZoo, updateZoo, deleteZoo } from '../api/zoo'
import { listKnowledge, createKnowledge, updateKnowledge, deleteKnowledge, reindex } from '../api/knowledge'

const tab = ref('zoo')
const zoos = ref([])
const dialogVisible = ref(false)
const emptyForm = () => ({
  id: null, name: '', city: '', province: '', address: '',
  ticketAdult: null, ticketChild: null, ticketNote: '',
  openTime: '', openHoursNote: '', website: '', description: '', starAnimals: ''
})
const form = ref(emptyForm())

const docs = ref([])
const kDialogVisible = ref(false)
const emptyKForm = () => ({ id: null, title: '', category: '', content: '', docSource: '' })
const kForm = ref(emptyKForm())

async function loadZoos() {
  const data = await listZoos({ page: 1, size: 100 })
  zoos.value = data.records
}
async function loadDocs() {
  docs.value = await listKnowledge()
}
function openCreate() {
  form.value = emptyForm()
  dialogVisible.value = true
}
function openEdit(row) {
  form.value = { ...row }
  dialogVisible.value = true
}
async function submit() {
  if (!form.value.name || !form.value.city) {
    ElMessage.warning('名称和城市必填')
    return
  }
  if (form.value.id) {
    await updateZoo(form.value.id, form.value)
    ElMessage.success('已更新')
  } else {
    await createZoo(form.value)
    ElMessage.success('已新增')
  }
  dialogVisible.value = false
  await loadZoos()
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.name}」？`, '提示', { type: 'warning' })
  await deleteZoo(row.id)
  ElMessage.success('已删除')
  await loadZoos()
}

function openKCreate() {
  kForm.value = emptyKForm()
  kDialogVisible.value = true
}
function openKEdit(row) {
  kForm.value = { ...row }
  kDialogVisible.value = true
}
async function kSubmit() {
  if (!kForm.value.title) {
    ElMessage.warning('标题必填')
    return
  }
  if (kForm.value.id) {
    await updateKnowledge(kForm.value.id, kForm.value)
    ElMessage.success('已更新')
  } else {
    await createKnowledge(kForm.value)
    ElMessage.success('已新增')
  }
  kDialogVisible.value = false
  await loadDocs()
}
async function kRemove(row) {
  await ElMessageBox.confirm(`确认删除「${row.title}」？`, '提示', { type: 'warning' })
  await deleteKnowledge(row.id)
  ElMessage.success('已删除')
  await loadDocs()
}
async function doReindex() {
  await reindex()
  ElMessage.success('向量索引重建完成')
}

function fmt(v) {
  return v == null ? '待定' : Number(v)
}

onMounted(() => {
  loadZoos()
  loadDocs()
})
</script>

<style scoped>
.admin-page { padding: 20px; }
.toolbar { margin-bottom: 14px; display: flex; gap: 10px; }
</style>
