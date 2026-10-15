import { defineStore } from 'pinia'
import { sendChat, getHistory, clearHistory } from '../api/chat'

export const useChatStore = defineStore('chat', {
  state: () => ({
    sessionId: localStorage.getItem('zootravel-session') || crypto.randomUUID(),
    messages: [],
    streaming: false
  }),
  actions: {
    setSessionId(id) {
      this.sessionId = id
      localStorage.setItem('zootravel-session', id)
    },
    async initHistory() {
      try {
        const history = await getHistory(this.sessionId)
        this.messages = history.map(m => ({
          role: m.role === 'user' ? 'user' : 'assistant',
          text: m.text,
          quotes: []
        }))
      } catch (e) {
        this.messages = []
      }
    },
    async reset() {
      try {
        await clearHistory(this.sessionId)
      } catch (e) {
        /* ignore */
      }
      this.setSessionId(crypto.randomUUID())
      this.messages = []
    },
    async send(text) {
      if (this.streaming || !text.trim()) return
      this.messages.push({ role: 'user', text })
      const bot = { role: 'assistant', text: '', quotes: [] }
      this.messages.push(bot)
      this.streaming = true
      try {
        await sendChat(this.sessionId, text, (ev) => {
          if (ev.type === 'meta') {
            bot.quotes = ev.quotes || []
          } else if (ev.type === 'delta') {
            bot.text += ev.content || ''
          } else if (ev.type === 'done') {
            this.streaming = false
          } else if (ev.type === 'error') {
            bot.text = `出错了：${ev.message || '未知错误'}`
            this.streaming = false
          }
        })
      } catch (e) {
        if (e.name === 'AbortError') {
          bot.text = bot.text || '（已中断）'
        } else {
          bot.text = `请求失败：${e.message}`
        }
        this.streaming = false
      } finally {
        this.streaming = false
      }
    }
  }
})
