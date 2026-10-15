import axios from 'axios'

/**
 * SSE 流式对话：fetch + ReadableStream
 * 注意：必须复用同一个 TextDecoder('utf-8') 实例逐 chunk 解码，避免多字节汉字乱码
 */
export async function sendChat(sessionId, message, onEvent, signal) {
  const res = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId, message }),
    signal
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buf = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    let idx
    while ((idx = buf.indexOf('\n\n')) >= 0) {
      const raw = buf.slice(0, idx)
      buf = buf.slice(idx + 2)
      for (const line of raw.split('\n')) {
        if (line.startsWith('data:')) {
          const payload = line.slice(5).trim()
          if (payload) {
            try {
              onEvent(JSON.parse(payload))
            } catch (e) {
              /* 忽略无法解析的行 */
            }
          }
        }
      }
    }
  }
  decoder.decode() // 冲刷尾部缓冲
}

export const getHistory = (sessionId) =>
  axios.get('/api/chat/history', { params: { sessionId } }).then(r => r.data.data)

export const clearHistory = (sessionId) =>
  axios.delete('/api/chat/history', { params: { sessionId } }).then(r => r.data)
