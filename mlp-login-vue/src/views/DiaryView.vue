<template>
  <div class="diary">
    <!-- 顶部 -->
    <div class="diary-head">
      <router-link to="/home" class="diary-back" title="回首页">«</router-link>
      <div class="diary-title">
        <b>📔 小马日记本</b>
        <p>今天的小事，值得记下来 ✨</p>
      </div>
    </div>

    <!-- 写日记 -->
    <form class="diary-editor" @submit.prevent="save">
      <div class="mood-row">
        <button
          v-for="m in MOODS" :key="m.e" type="button"
          class="mood" :class="{ on: mood === m.e }"
          @click="mood = m.e"
        >{{ m.e }}<i>{{ m.t }}</i></button>
      </div>
      <textarea
        v-model.trim="text"
        rows="3" maxlength="200"
        placeholder="今天遇到了什么好玩的事呀…（最多 200 字）"
      ></textarea>
      <div class="diary-actions">
        <span class="count" :class="{ warn: text.length >= 180 }">{{ text.length }}/200</span>
        <button type="submit" class="save" :disabled="!text">写进日记</button>
      </div>
    </form>

    <!-- 日记列表 -->
    <div class="diary-list">
      <div v-for="d in entries" :key="d.id" class="diary-item">
        <div class="diary-meta">
          <span class="dmood">{{ d.mood }}</span>
          <time>{{ d.time }}</time>
          <button class="del" title="撕掉这页" @click="del(d.id)">🗑️</button>
        </div>
        <p>{{ d.text }}</p>
      </div>
      <div v-if="!entries.length" class="empty">还没有日记，写下第一篇吧 🌈</div>
    </div>
  </div>
</template>

<script>
import { getSession } from '../auth'

const MOODS = [
  { e: '😊', t: '开心' },
  { e: '🤩', t: '兴奋' },
  { e: '😴', t: '累了' },
  { e: '🌧️', t: '难过' },
  { e: '😤', t: '生气' }
]

export default {
  name: 'DiaryView',
  data() {
    return {
      MOODS,
      mood: '😊',
      text: '',
      entries: []
    }
  },
  created() {
    this.load()
  },
  methods: {
    key() {
      const s = getSession()
      return 'mlp_diary_' + ((s && s.id) || 'guest')
    },
    load() {
      try {
        this.entries = JSON.parse(localStorage.getItem(this.key()) || '[]')
      } catch (e) {
        this.entries = []
      }
      // 新的在前
      this.entries.sort((a, b) => b.id - a.id)
    },
    persist() {
      localStorage.setItem(this.key(), JSON.stringify(this.entries))
    },
    save() {
      if (!this.text) return
      const now = new Date()
      const pad = n => String(n).padStart(2, '0')
      this.entries.unshift({
        id: Date.now(),
        mood: this.mood,
        text: this.text,
        time: `${now.getFullYear()}/${now.getMonth() + 1}/${now.getDate()} ${pad(now.getHours())}:${pad(now.getMinutes())}`
      })
      this.text = ''
      this.mood = '😊'
      this.persist()
    },
    del(id) {
      if (!confirm('把这一页撕掉？撕了就找不回来了哦')) return
      this.entries = this.entries.filter(d => d.id !== id)
      this.persist()
    }
  }
}
</script>

<style scoped>
.diary-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.diary-back {
  width: 34px; height: 34px; border-radius: 50%;
  background: var(--card); border: 1.5px solid var(--border);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: var(--muted); text-decoration: none;
  transition: all .15s;
}
.diary-back:hover { color: var(--primary); border-color: var(--primary); }
.diary-title b { font-size: 17px; }
.diary-title p { margin: 2px 0 0; font-size: 12px; color: var(--muted); }

/* 编辑器 */
.diary-editor {
  background: var(--card); border: 1.5px solid var(--border);
  border-radius: 16px; padding: 14px; margin-bottom: 16px;
}
.mood-row { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }
.mood {
  border: 1.5px solid var(--border); background: var(--panel);
  border-radius: 999px; padding: 5px 12px; cursor: pointer;
  font-size: 14px; color: var(--muted); transition: all .15s;
}
.mood i { font-style: normal; font-size: 11px; margin-left: 3px; }
.mood.on {
  border-color: var(--primary); color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, #fff); font-weight: 700;
}
.diary-editor textarea {
  width: 100%; border: 1.5px solid var(--border); border-radius: 12px;
  padding: 10px 12px; font-size: 13px; font-family: inherit;
  resize: vertical; box-sizing: border-box; background: var(--panel);
  color: var(--text);
}
.diary-editor textarea:focus { outline: none; border-color: var(--primary); }
.diary-actions {
  display: flex; align-items: center; justify-content: space-between; margin-top: 8px;
}
.count { font-size: 11px; color: var(--muted); }
.count.warn { color: var(--danger); font-weight: 700; }
.save {
  border: none; cursor: pointer; border-radius: 999px;
  padding: 8px 22px; font-size: 13px; font-weight: 800; color: #fff;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 4px 12px rgba(160,108,213,.3);
}
.save:disabled { background: var(--track); color: var(--muted); cursor: not-allowed; box-shadow: none; }

/* 列表 */
.diary-item {
  background: var(--card); border: 1.5px solid var(--border);
  border-radius: 14px; padding: 12px 14px; margin-bottom: 10px;
}
.diary-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.dmood { font-size: 16px; }
.diary-meta time { font-size: 11.5px; color: var(--muted); }
.del {
  margin-left: auto; border: none; background: none; cursor: pointer;
  font-size: 13px; opacity: .45; transition: opacity .15s;
}
.del:hover { opacity: 1; }
.diary-item p { margin: 0; font-size: 13px; line-height: 1.7; color: var(--text); word-break: break-word; }
.empty {
  text-align: center; padding: 30px 0; font-size: 12.5px; color: var(--muted);
}
</style>
