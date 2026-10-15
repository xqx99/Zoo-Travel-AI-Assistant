<template>
  <div class="chat-page">
    <div class="chat-header">
      <span class="title">动物园旅游智能规划助手</span>
      <el-button size="small" :disabled="store.streaming" @click="store.reset()">新会话</el-button>
    </div>

    <div class="chat-body" ref="bodyRef">
      <div v-for="(m, i) in store.messages" :key="i" class="msg" :class="m.role">
        <div class="avatar" :class="m.role">{{ m.role === 'user' ? '我' : 'AI' }}</div>
        <div class="bubble">
          <div v-if="m.quotes && m.quotes.length" class="quotes">
            <el-tooltip
              v-for="(q, qi) in m.quotes"
              :key="qi"
              :content="q.text"
              placement="top"
            >
              <el-tag size="small" effect="plain" class="quote-tag">
                {{ q.kind === 'zoo' ? '🏠 ' : '📖 ' }}{{ q.title }}
              </el-tag>
            </el-tooltip>
          </div>
          <div class="text" v-html="render(m.text)"></div>
          <div v-if="store.streaming && m.role === 'assistant' && !m.text" class="loading">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="input"
        type="textarea"
        :rows="2"
        resize="none"
        placeholder="例如：周末带小孩去南京红山动物园看熊猫；或：北京动物园门票多少钱，几点开放"
        :disabled="store.streaming"
        @keydown.enter.exact.prevent="send"
      />
      <el-button type="primary" :loading="store.streaming" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue'
import { marked } from 'marked'
import { useChatStore } from '../stores/chatStore'

marked.setOptions({ gfm: true, breaks: true })

const store = useChatStore()
const input = ref('')
const bodyRef = ref(null)

function render(text) {
  return marked.parse(text || '')
}

async function send() {
  const text = input.value.trim()
  if (!text) return
  input.value = ''
  await store.send(text)
}

function scrollBottom() {
  nextTick(() => {
    const el = bodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

watch(() => [store.messages.length, store.streaming], scrollBottom)

onMounted(async () => {
  await store.initHistory()
  scrollBottom()
})
</script>

<style scoped>
.chat-page { display: flex; flex-direction: column; height: 100%; }
.chat-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; background: #fff; border-bottom: 1px solid #e4e7ed;
}
.chat-header .title { font-size: 16px; font-weight: 600; color: #303133; }
.chat-body {
  flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 14px;
}
.msg { display: flex; gap: 10px; }
.msg.user { flex-direction: row-reverse; }
.avatar {
  width: 34px; height: 34px; border-radius: 8px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; color: #fff;
}
.avatar.user { background: #337ecc; }
.avatar.assistant { background: #67c23a; }
.bubble {
  max-width: 72%; padding: 10px 14px; border-radius: 10px;
  background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,.08); line-height: 1.7;
}
.msg.user .bubble { background: #e6f0ff; }
.quotes { margin-bottom: 6px; display: flex; flex-wrap: wrap; gap: 6px; }
.text :deep(p) { margin: 6px 0; }
.text :deep(pre) { background: #f5f7fa; padding: 10px; border-radius: 6px; overflow-x: auto; }
.text :deep(h1), .text :deep(h2), .text :deep(h3) { margin: 10px 0 6px; }
.loading { display: flex; gap: 4px; padding: 4px 0; }
.dot { width: 6px; height: 6px; border-radius: 50%; background: #a8abb2; animation: blink 1s infinite; }
.dot:nth-child(2) { animation-delay: .2s; }
.dot:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%, 100% { opacity: .3; } 50% { opacity: 1; } }
.chat-input {
  display: flex; gap: 10px; align-items: flex-end;
  padding: 14px 20px; background: #fff; border-top: 1px solid #e4e7ed;
}
.chat-input .el-textarea { flex: 1; }
</style>
