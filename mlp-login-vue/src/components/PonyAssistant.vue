<template>
  <!-- 小马 AI 助手：内嵌在登录/注册页，点击开聊，可更换助手形象 -->
  <div class="pa-root" ref="root">
    <!-- 悬浮球 -->
    <button ref="fab" class="pa-fab" :title="'和' + cur.name + '聊天'" @click="toggle">
      <span class="pa-fab-face"><PonyFace :a="cur" /></span>
      <span class="pa-fab-name">{{ cur.name }}</span>
      <span v-if="!open" class="pa-fab-pulse"></span>
    </button>

    <!-- 聊天面板：Teleport 到 body，避免被卡片 overflow 裁剪 -->
    <Teleport to="body">
      <Transition name="pa-pop">
        <section v-if="open" class="pa-panel" :style="panelStyle">
        <header class="pa-head" :style="{ background: cur.color }">
          <span class="pa-head-face"><PonyFace :a="cur" /></span>
          <div class="pa-head-txt">
            <b>{{ cur.name }}</b>
            <span>{{ cur.title }}</span>
          </div>
          <button class="pa-switch" title="更换助手" @click="picking = !picking">🔄</button>
          <button class="pa-close" title="收起" @click="open = false">✕</button>
        </header>

        <!-- 助手选择器 -->
        <div v-if="picking" class="pa-picker">
          <p class="pa-picker-tip">选择你的小马助手：</p>
          <button
            v-for="a in ASSISTANTS"
            :key="a.id"
            class="pa-pick"
            :class="{ on: a.id === cur.id }"
            @click="choose(a)"
          >
            <span class="pa-pick-face"><PonyFace :a="a" /></span>
            <b>{{ a.name }}</b>
            <span>{{ a.title }}</span>
          </button>
        </div>

        <!-- 消息列表 -->
        <div v-else class="pa-msgs" ref="msgBox">
          <div
            v-for="(m, i) in msgs"
            :key="i"
            class="pa-msg"
            :class="m.role === 'me' ? 'me' : 'ai'"
          >
            <span v-if="m.role === 'ai'" class="pa-msg-face"><PonyFace :a="cur" /></span>
            <div class="pa-bubble">
              <p v-text="m.role === 'ai' && m.typing ? shownText(i) : m.text"></p>
              <button
                v-if="m.action && !m.typing"
                class="pa-jump"
                @click="jump(m.action)"
              >{{ m.actionLabel }} →</button>
            </div>
          </div>
        </div>

        <!-- 快捷问题 -->
        <div v-if="!picking" class="pa-quick">
          <button v-for="(q, i) in cur.quick" :key="i" @click="send(q)">{{ q }}</button>
        </div>

        <!-- 输入区 -->
        <form v-if="!picking" class="pa-input" @submit.prevent="send(input)">
          <input v-model="input" :placeholder="'和' + cur.name + '说点什么…'" maxlength="60" />
          <button type="submit" :disabled="!input.trim()">发送</button>
        </form>
        </section>
      </Transition>
    </Teleport>
  </div>
</template>

<script>
import sound from '../sound'
import { getSession } from '../auth'
import PonyFace from './PonyFace.vue'

/** 可更换的小马助手 */
const ASSISTANTS = [
  {
    id: 'spike', name: '穗龙', emoji: '🐲', title: '小马国首席助理',
    color: 'linear-gradient(135deg, #a5e0bf, #bda6f2)',
    greet: '嘿！我是穗龙，宝石保管专家兼头号跑腿，有求必应！',
    quick: ['怎么加好友？', '学院有什么好玩的？', '推荐个小游戏']
  },
  {
    id: 'luna', name: '月亮公主', emoji: '🌙', title: '夜之守护者', color: '#9b8fd8',
    greet: '夜安……我是露娜。今夜，你想了解些什么？',
    quick: ['怎么换背景主题？', '今日运势', '打卡有什么用？']
  },
  {
    id: 'celestia', name: '宇宙公主', emoji: '☀️', title: '小马国导师', color: '#f0d078',
    greet: '欢迎，我的学生。有任何疑问，都可以尽情问我。',
    quick: ['积分怎么赚？', '学院有什么好玩的？', '碧琪是谁？']
  },
  {
    id: 'twilight', name: '紫悦', emoji: '📚', title: '和谐之元 · 魔法', color: '#f2a8c8',
    greet: '友谊是魔法！有什么我可以帮你的吗？',
    quick: ['推荐个小游戏', '珍奇是谁？', '怎么换背景主题？']
  },
  {
    id: 'rarity', name: '珍奇', emoji: '💎', title: '坎特洛特时装设计师', color: '#c9a0e8',
    greet: 'Bonjour～我是珍奇，旋转木马时装店的主理人。穿搭、配色、礼仪，尽管问我！',
    quick: ['学院有什么好玩的？', '积分怎么赚？', '怎么加好友？']
  }
]

const PICK_KEY = 'mlp_assistant'

/** 本地规则引擎：关键词 → 回答 + 可选跳转 */
function reply(text, cur) {
  const q = text.trim()
  const has = (...ks) => ks.some(k => q.includes(k))

  if (has('加好友', '好友', '朋友', '认识'))
    return {
      text: '去底部导航的「好友」页：点「添加好友」，输入名字或 ID 搜索，发送申请；对方在「新的朋友」里点同意，你们就是好友啦，之后就能聊天！同一个 Wi-Fi 下的朋友用局域网地址也能直接访问哦。',
      action: 'friends', actionLabel: '去加好友'
    }
  if (has('学院', '课程', '学习', '上课'))
    return {
      text: '小马学院有 6 门课，每门 6 小节 + 小测，学完发专属徽章！每天打卡连击还有积分拿。点击导航栏的「🎓 学院」就能开始。',
      action: 'learn', actionLabel: '去学院'
    }
  if (has('游戏', '玩'))
    return {
      text: '学院里有九款小游戏：魔法问答、苹果农庄、闪电反应、派对翻牌、森林喂食、宝石工坊、甜品工坊、派对布置、时尚装扮。每只小马都有自己擅长的一款，得分还能换积分！',
      action: 'learn', actionLabel: '去玩游戏'
    }
  if (has('运势', '运气', '今日'))
    return {
      text: '「幸运小马」页每天可以抽一张运势卡，有今日幸运色、宜忌和开运小马。试试看今天会遇见谁！',
      action: 'fortune', actionLabel: '抽张运势'
    }
  if (has('主题', '背景', '颜色', '变色', '彩虹'))
    return {
      text: '右上角那一排彩色圆点可以切换 6 套主题；最左边的彩虹圆点是「自动变色」，每 8 秒平滑过渡一套。旁边的 🔊 按钮可以开关音效。背景里漂浮的可爱标志也可以点，会直达对应小马的课程！',
      action: 'home', actionLabel: '回图鉴试试'
    }
  if (has('打卡', '连击'))
    return { text: '学院页顶部每天可以打卡：首日 +20 积分，连击每天多 +2，断了要重新累计。坚持就是胜利！' }
  if (has('积分', '徽章', '分'))
    return { text: '积分获取：学完小节 +10、答对小测 +5、通关课程 +50、每日打卡 +20（连击加成）、玩游戏按得分结算。徽章墙在学院页下方，集齐 6 枚就是全能学员！' }
  if (has('碧琪'))
    return { text: '碧琪·派（Pinkie Pie）：地球马，可爱标志是三个气球（两蓝一黄）。天生乐天派，最爱派对和纸杯蛋糕，在学院教派对布置和甜品设计！' }
  if (has('珍奇', '小玛'))
    return { text: '珍奇（Rarity）：独角兽，可爱标志是三颗钻石。坎特洛特时装设计师，慷慨又讲究，学院里教配色、穿搭和礼仪。' }
  if (has('紫悦'))
    return { text: '紫悦·斯帕克（Twilight Sparkle）：独角兽，可爱标志是六芒星，代表和谐之元「魔法」。塞拉斯蒂亚的学生，友谊魔法的研究者——就是我（如果你选了我当助手的话）！' }
  if (has('云宝', '云宝黛西'))
    return { text: '云宝黛西（Rainbow Dash）：飞马，可爱标志是白云配彩虹闪电。彩虹音爆镇发明人，学院教气象与飞行课，急性子但超级靠谱。' }
  if (has('苹果嘉儿', '苹果姐', '苹果嘉'))
    return { text: '苹果嘉儿（Applejack）：地球马，可爱标志是三个红苹果。甜苹果园一把手，诚实又勤快，学院教农业和耐力课。' }
  if (has('柔柔'))
    return { text: '柔柔（Fluttershy）：飞马，可爱标志是三只粉蝴蝶。温柔羞怯，却能和动物对话，发起脾气来连龙都怕——学院教生物课。' }
  if (has('月亮公主', '露娜'))
    return { text: '月亮公主露娜：掌管夜晚与梦境，可爱标志是深蓝月牙。她曾因嫉妒被放逐千年，如今回到妹妹身边，守护每晚安睡。' }
  if (has('宇宙公主', '塞拉斯蒂亚'))
    return { text: '宇宙公主塞拉斯蒂亚：掌管太阳，小马国的导师，可爱标志是太阳。她创办了友谊学院，相信每一位学生都是希望。' }
  if (has('穗龙', '斯派克'))
    return { text: '正是在下！紫悦的头号助手，管宝石、跑腿、写友谊报告。虽然我是龙，但心是小马的心。' }
  if (has('你好', '嗨', 'hello', 'hi', '在吗'))
    return { text: cur.id === 'luna' ? '夜安，旅人。我一直都在。' : cur.id === 'celestia' ? '你好，我亲爱的学生。请说。' : cur.id === 'twilight' ? '你好！友谊魔法随时待命。' : '嘿！我在这儿呢，说吧！' }
  if (has('谢谢', '感谢', '3q'))
    return { text: cur.id === 'luna' ? '不必言谢，这是我应该做的。' : '不客气！能帮上忙就好。' }
  if (has('你是谁', '名字', '介绍'))
    return { text: `我是${cur.name}，${cur.title}。点我头顶的 🔄 可以换成月亮公主、宇宙公主或紫悦来陪你聊天。` }
  if (has('吃', '饿'))
    return { text: '说到吃……碧琪的纸杯蛋糕学院里有教做法，顺便还能玩「甜品工坊」小游戏！' }
  return {
    text: '这个我还真不清楚……试试问我：加好友、学院课程、小游戏、运势、背景主题，或者任何一只小马的名字！'
  }
}

export default {
  name: 'PonyAssistant',
  components: { PonyFace },
  inject: ['toast'],
  data() {
    return {
      ASSISTANTS: () => ASSISTANTS,
      open: false,
      picking: false,
      input: '',
      cur: this.loadCur(),
      msgs: [],
      typeTimer: null,
      panelStyle: {}   // 面板动态定位（fixed，贴住悬浮球）
    }
  },
  watch: {
    cur() {
      // 换助手后重新打招呼
      this.msgs = [this.aiMsg(this.cur.greet)]
    }
  },
  methods: {
    loadCur() {
      try {
        const id = localStorage.getItem(PICK_KEY)
        const a = ASSISTANTS.find(a => a.id === id)
        if (a) return a
      } catch (e) {}
      return ASSISTANTS[0]
    },
    aiMsg(text, action, actionLabel) {
      return { role: 'ai', text, typing: !!text, shown: 0, action, actionLabel }
    },
    /** 打字机效果：当前正在显示的字符数 */
    shownText(i) {
      const m = this.msgs[i]
      return m.text.slice(0, m.shown)
    },
    toggle() {
      this.open = !this.open
      sound.pop()
      if (this.open && !this.msgs.length) {
        this.msgs = [this.aiMsg(this.cur.greet)]
        this.startTyping(0)
      }
      if (!this.open) this.picking = false
      if (this.open) this.$nextTick(this.placePanel)
    },
    /** 面板定位：默认在悬浮球正上方弹出，上方空间不够就改到下方 */
    placePanel() {
      const fab = this.$refs.fab
      if (!fab) return
      const r = fab.getBoundingClientRect()
      const gap = 12
      // 水平方向居中于悬浮球，并限制在视口内（左右各留 12px）
      const panelW = Math.min(340, window.innerWidth - 36)
      let left = r.left + r.width / 2 - panelW / 2
      left = Math.max(12, Math.min(left, window.innerWidth - panelW - 12))
      const style = {
        position: 'fixed',
        left: left + 'px',
        right: 'auto'
      }
      if (r.top > 430) {
        style.bottom = (window.innerHeight - r.top + gap) + 'px'
        style.top = 'auto'
      } else {
        style.top = (r.bottom + gap) + 'px'
        style.bottom = 'auto'
      }
      this.panelStyle = style
    },
    choose(a) {
      this.cur = a
      this.picking = false
      try { localStorage.setItem(PICK_KEY, a.id) } catch (e) {}
      sound.magic()
    },
    send(q) {
      const text = (q || this.input).trim()
      if (!text) return
      this.msgs.push({ role: 'me', text })
      this.input = ''
      sound.click()
      this.$nextTick(() => {
        const box = this.$refs.msgBox
        if (box) box.scrollTop = box.scrollHeight
      })
      // 助手延迟一小会儿再回，显得在"思考"
      setTimeout(() => {
        const r = reply(text, this.cur)
        this.msgs.push(this.aiMsg(r.text, r.action, r.actionLabel))
        this.startTyping(this.msgs.length - 1)
        sound.twinkle()
      }, 420)
    },
    startTyping(i) {
      const m = this.msgs[i]
      if (!m || !m.typing) return
      clearInterval(this.typeTimer)
      this.typeTimer = setInterval(() => {
        m.shown += 2
        const box = this.$refs.msgBox
        if (box) box.scrollTop = box.scrollHeight
        if (m.shown >= m.text.length) {
          m.shown = m.text.length
          m.typing = false
          clearInterval(this.typeTimer)
        }
      }, 28)
    },
    jump(action) {
      sound.click()
      // 登录/注册页没有会话，跳页前先挡一道，让助手别把未登录的人扔到守卫回环里
      if (!getSession()) {
        if (this.toast) this.toast('先登录小马王国，再带你去哦～', 'err')
        this.open = false
        return
      }
      if (this.$route.name !== action) this.$router.push({ name: action })
      this.open = false
    }
  }
}
</script>

<style scoped>
/* 内嵌模式：跟随文档流，由使用方决定放在哪（登录/注册页） */
.pa-root { position: relative; z-index: 60; display: flex; justify-content: center; }
.pa-root:empty { display: none; }

/* 悬浮球 */
.pa-fab {
  position: relative; display: flex; align-items: center; gap: 8px;
  padding: 8px 16px 8px 10px; border: none; cursor: pointer;
  border-radius: 999px; background: var(--card);
  box-shadow: 0 8px 24px rgba(120, 90, 160, .28);
  transition: transform .18s cubic-bezier(.34, 1.56, .64, 1);
}
.pa-fab:hover { transform: translateY(-3px) scale(1.04); }
.pa-fab-face { font-size: 24px; line-height: 1; display: flex; align-items: center; justify-content: center; }
.pa-fab-name { font-size: 13px; font-weight: 700; color: var(--text); }
.pa-fab-pulse {
  position: absolute; inset: -3px; border-radius: 999px;
  border: 2px solid var(--primary); opacity: 0;
  animation: paPulse 2.4s ease-out infinite;
}
@keyframes paPulse {
  0% { opacity: .8; transform: scale(1); }
  100% { opacity: 0; transform: scale(1.35); }
}

/* 面板：Teleport 到 body，必须显式高层级，否则会被登录卡片盖住 */
.pa-panel {
  position: absolute; right: 0; bottom: calc(100% + 12px);
  width: min(340px, calc(100vw - 36px));
  background: var(--card); border: 1px solid var(--border);
  border-radius: 16px; box-shadow: 0 18px 48px rgba(120, 90, 160, .3);
  overflow: hidden; display: flex; flex-direction: column;
  z-index: 2000;
}
.pa-pop-enter-active, .pa-pop-leave-active { transition: all .22s cubic-bezier(.22, .61, .36, 1); }
.pa-pop-enter-from, .pa-pop-leave-to { opacity: 0; transform: translateY(12px) scale(.96); }

/* 头部 */
.pa-head {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px; color: #fff;
}
.pa-head-face {
  width: 38px; height: 38px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255, 255, 255, .28); font-size: 21px;
}
.pa-head-txt { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.pa-head-txt b { font-size: 14.5px; }
.pa-head-txt span { font-size: 11px; opacity: .85; }
.pa-switch, .pa-close {
  border: none; cursor: pointer; flex-shrink: 0;
  width: 28px; height: 28px; border-radius: 8px; font-size: 13px;
  background: rgba(255, 255, 255, .22); color: #fff;
  transition: background .15s;
}
.pa-switch:hover, .pa-close:hover { background: rgba(255, 255, 255, .38); }

/* 助手选择器 */
.pa-picker { padding: 12px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.pa-picker-tip { grid-column: 1 / -1; margin: 0 0 2px; font-size: 11.5px; color: var(--muted); }
.pa-pick {
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  padding: 12px 6px; border: 2px solid var(--border); border-radius: 12px;
  background: var(--panel); cursor: pointer; transition: all .15s;
}
.pa-pick:hover { border-color: var(--primary); transform: translateY(-2px); }
.pa-pick.on { border-color: var(--primary); background: var(--panel); box-shadow: 0 4px 12px rgba(160, 108, 213, .22); }
.pa-pick-face { font-size: 26px; display: flex; justify-content: center; }
.pa-pick b { font-size: 12.5px; color: var(--text); }
.pa-pick span { font-size: 10px; color: var(--muted); }

/* 消息区 */
.pa-msgs {
  height: 300px; overflow-y: auto; padding: 14px 12px;
  display: flex; flex-direction: column; gap: 10px;
}
.pa-msg { display: flex; align-items: flex-end; gap: 7px; max-width: 100%; }
.pa-msg.me { justify-content: flex-end; }
.pa-msg-face {
  width: 26px; height: 26px; border-radius: 8px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: var(--panel); font-size: 15px;
}
.pa-bubble {
  max-width: 78%; padding: 9px 12px; border-radius: 12px;
  font-size: 12.5px; line-height: 1.55;
}
.pa-msg.ai .pa-bubble {
  background: var(--panel); color: var(--text);
  border-bottom-left-radius: 3px;
}
.pa-msg.me .pa-bubble {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: #fff; border-bottom-right-radius: 3px;
}
.pa-bubble p { margin: 0; white-space: pre-wrap; word-break: break-word; }
.pa-jump {
  margin-top: 8px; border: none; cursor: pointer;
  padding: 5px 12px; border-radius: 999px; font-size: 11.5px; font-weight: 700;
  color: #fff; background: var(--primary);
  transition: filter .15s;
}
.pa-jump:hover { filter: brightness(1.1); }

/* 快捷问题 */
.pa-quick {
  display: flex; flex-wrap: wrap; gap: 6px;
  padding: 0 12px 10px;
}
.pa-quick button {
  border: 1px solid var(--border); cursor: pointer;
  padding: 5px 10px; border-radius: 999px;
  background: var(--panel); color: var(--muted);
  font-size: 11.5px; transition: all .15s;
}
.pa-quick button:hover { color: var(--primary); border-color: var(--primary); }

/* 输入区 */
.pa-input {
  display: flex; gap: 8px; padding: 10px 12px;
  border-top: 1px solid var(--border);
}
.pa-input input {
  flex: 1; min-width: 0; border: 1px solid var(--border);
  border-radius: 999px; padding: 8px 14px; font-size: 12.5px;
  background: var(--panel); color: var(--text); outline: none;
}
.pa-input input:focus { border-color: var(--primary); }
.pa-input button {
  border: none; cursor: pointer; flex-shrink: 0;
  padding: 8px 14px; border-radius: 999px; font-size: 12px; font-weight: 700;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}
.pa-input button:disabled { opacity: .5; cursor: not-allowed; }
</style>
