<template>
  <div class="adventure">
    <!-- 顶部 -->
    <div class="adv-head">
      <router-link to="/home" class="adv-back" title="回首页">«</router-link>
      <div class="adv-title">
        <b>🧭 探险营地</b>
        <p>每天一次远征：选条路，路上会遇见什么全看运气。</p>
      </div>
    </div>

    <!-- 探险等级 -->
    <div class="adv-rank">
      <div class="rank-main">
        <span class="rank-icon">{{ rank.icon }}</span>
        <div class="rank-txt">
          <b>{{ rank.name }}</b>
          <em>远征 {{ state.trips.length }} 次 · 累计收获 {{ state.totalPoints }} 积分</em>
        </div>
      </div>
      <div class="rank-bar"><i :style="{ width: rankPct + '%' }"></i></div>
      <p class="rank-next" v-if="rankNext">再远征 {{ rankNext.min - state.trips.length }} 次升为「{{ rankNext.name }}」</p>
      <p class="rank-next" v-else>已经是最高等级，整条小马谷都认识你了。</p>
    </div>

    <!-- ===== 阶段一：选路线 / 今日战报 ===== -->
    <template v-if="phase === 'pick'">
      <div v-if="doneToday" class="adv-report">
        <div class="report-top">
          <span class="report-icon">{{ todayTrip.icon }}</span>
          <b>今日远征 · {{ todayTrip.name }}</b>
          <em>+{{ todayTrip.points }}</em>
        </div>
        <ul class="report-list">
          <li v-for="(p, i) in todayTrip.picks" :key="i">
            <span>{{ p.scene }}</span>
            <i>+{{ p.points }}</i>
          </li>
        </ul>
        <p class="report-tip">今天的路已经走完了。明天刷新新的奇遇，营地会给你留一盏灯。</p>
      </div>

      <template v-else>
        <div class="adv-sub">今天走哪条路？</div>
        <div class="route-list">
          <button
            v-for="r in routes"
            :key="r.id"
            class="route-card"
            :style="{ '--rc': r.color }"
            @click="start(r)"
          >
            <span class="route-face">{{ r.icon }}</span>
            <span class="route-main">
              <b>{{ r.name }}</b>
              <em>{{ r.desc }}</em>
            </span>
            <span class="route-meta">{{ r.risk }}</span>
          </button>
        </div>
        <p class="adv-hint">每条路会随机抽 3 段奇遇，选择不同，收获也不同（约 15~35 积分）。</p>
      </template>
    </template>

    <!-- ===== 阶段二：路上遇见的奇遇 ===== -->
    <template v-else-if="phase === 'event' && currentEvent">
      <div class="ev-head">
        <span class="ev-route">{{ route.icon }} {{ route.name }}</span>
        <span class="ev-dots">
          <i v-for="n in events.length" :key="n" :class="{ on: n - 1 <= step }"></i>
        </span>
        <span class="ev-gain">+{{ gained }}</span>
      </div>

      <div class="ev-card" :style="{ '--rc': route.color }">
        <div class="ev-scene">{{ currentEvent.scene }}</div>
        <p class="ev-text">{{ currentEvent.text }}</p>
      </div>

      <div class="opt-list">
        <button v-for="(o, i) in currentEvent.options" :key="i" class="opt" @click="choose(o)">
          <span>{{ o.label }}</span>
          <i>▸</i>
        </button>
      </div>
    </template>

    <!-- ===== 阶段三：结算 ===== -->
    <template v-else-if="phase === 'result'">
      <div class="res-card">
        <div class="res-icon">🎒</div>
        <b class="res-title">远征归来</b>
        <p class="res-gain">本次收获 <span>+{{ gained }}</span> 积分</p>
        <ul class="res-list">
          <li v-for="(p, i) in picks" :key="i">
            <span class="res-scene">{{ p.scene }}</span>
            <span class="res-text">{{ p.text }}</span>
            <i>+{{ p.points }}</i>
          </li>
        </ul>
        <button class="res-btn" @click="backToCamp">回营地</button>
      </div>
    </template>

    <!-- 探险日志 -->
    <div class="trip-log" v-if="phase === 'pick' && state.trips.length">
      <div class="log-title">📜 探险日志</div>
      <div class="log-item" v-for="(t, i) in recentTrips" :key="i">
        <span class="log-date">{{ t.date.slice(5) }}</span>
        <span class="log-route">{{ t.icon }} {{ t.name }}</span>
        <span class="log-points">+{{ t.points }}</span>
      </div>
    </div>

    <router-link to="/learn" class="adv-link">🎮 想玩小游戏？去学院的小马游乐场 →</router-link>
  </div>
</template>

<script>
import { addPoints } from '../learn'
import sound from '../sound'

const KEY = 'mlp_adventure_v1'
const STEPS = 3

/** 探险等级：按累计远征次数 */
const RANKS = [
  { min: 15, name: '传奇探险家', icon: '👑' },
  { min: 7, name: '探险队长', icon: '⛺' },
  { min: 3, name: '资深探险家', icon: '🎒' },
  { min: 1, name: '见习探险家', icon: '🧭' },
  { min: 0, name: '露营新手', icon: '🏕️' }
]

/** 四条路线，每条带一组随机奇遇 */
const ROUTES = [
  {
    id: 'forest', name: '森林边境', icon: '🌲', color: '#7fd4a8', risk: '轻松',
    desc: '柔柔带路，树影里全是小动物的脚步声。',
    events: [
      {
        scene: '🌿 岔路口',
        text: '小径分成两条：左边有一串新鲜的蹄印，右边飘来青草与野莓的香味。',
        options: [
          { label: '跟着蹄印走', points: 9, text: '蹄印尽头是一头迷路的小鹿，你把它送回妈妈身边，柔柔远远地朝你竖起翅膀。' },
          { label: '循着香味走', points: 6, text: '一片野莓丛。你采了满满一篮，还顺手分给了路边探头的小松鼠。' }
        ]
      },
      {
        scene: '🐰 树洞里的哭声',
        text: '树洞里传来细细的哭声，声音很小，像是有谁被卡住了。',
        options: [
          { label: '伸手进去摸', points: 10, text: '是只被藤蔓缠住的小兔子。你耐心解开每一圈，它蹭了蹭你的手才肯走。' },
          { label: '先找树枝探路', points: 7, text: '你用树枝把藤蔓挑开，稳当又安全。小兔子蹦走前回头看了你一眼。' }
        ]
      },
      {
        scene: '🌧 骤雨',
        text: '雨点砸下来，前面有一间破旧的小木屋，旁边是一条抄近路的小道。',
        options: [
          { label: '进木屋避雨', points: 8, text: '屋里躺着一本被遗忘的探险手记，你读完了里面半页手绘路线图。' },
          { label: '冒雨抄近路', points: 11, text: '全身湿透，但你在泥里捡到一块会发光的石头。' }
        ]
      },
      {
        scene: '🦋 挡路的蝴蝶',
        text: '一群蝴蝶停在路中央，像是特意等着你，又像是在引路。',
        options: [
          { label: '跟它们走', points: 9, text: '蝴蝶把你带到一片花海，花心里藏着蜂蜜糖，甜得像偷来的。' },
          { label: '轻轻绕开', points: 6, text: '你安静地绕过，没惊动它们休息。柔柔说，这叫体贴。' }
        ]
      }
    ]
  },
  {
    id: 'mist', name: '云雾山道', icon: '⛰️', color: '#6ec6e6', risk: '刺激',
    desc: '云宝的赛道，风比路还长，脚下全是云。',
    events: [
      {
        scene: '⚡ 乱流',
        text: '云层里的气流乱窜，云宝在前面喊：抓稳了，这段不好走！',
        options: [
          { label: '全速冲过去', points: 12, text: '你在气流里打了个漂亮的滚，落地时彩虹在身后炸开。' },
          { label: '压低身子慢慢挪', points: 7, text: '稳稳通过。慢是慢了点，但一点没磕着。' }
        ]
      },
      {
        scene: '🪜 断了一半的吊桥',
        text: '山道上的吊桥断了一半，对面就是下一个落脚点。',
        options: [
          { label: '荡过去', points: 11, text: '你抓住绳子荡过缺口，心跳到嗓子眼，落地那一刻特别爽。' },
          { label: '找别的路绕', points: 8, text: '你绕了远路，却在岩缝里发现一小簇水晶。' }
        ]
      },
      {
        scene: '🌫 浓雾',
        text: '雾太浓了，三步之外什么都看不见，连来路也模糊了。',
        options: [
          { label: '跟着风声走', points: 9, text: '风声把你带出迷雾，出口正好有个补给点，还有热水。' },
          { label: '原地等雾散', points: 6, text: '雾散时你发现脚边有一条被人踩出来的小路。' }
        ]
      },
      {
        scene: '🦅 山顶的鹰',
        text: '一只鹰在高处盘旋，它掉了一根羽毛，正好落在你脚边。',
        options: [
          { label: '把羽毛举高还它', points: 10, text: '鹰俯冲下来叼走羽毛，绕着你飞了一圈，像在道谢。' },
          { label: '收进背包当纪念', points: 5, text: '羽毛很漂亮，但总觉得少了点什么。' }
        ]
      }
    ]
  },
  {
    id: 'crystal', name: '水晶遗迹', icon: '💎', color: '#b79cf0', risk: '解谜',
    desc: '珍奇最爱的废墟，每一块石头都藏着机关。',
    events: [
      {
        scene: '💎 宝石之门',
        text: '门上有三个凹槽，形状分别是圆、三角、菱形，没有说明。',
        options: [
          { label: '直接按形状摆上去', points: 10, text: '门缓缓打开，里面是一整面会唱歌的水晶墙。' },
          { label: '先看门缝里的光', points: 12, text: '光的方向提示了另一种顺序。门开得极轻，像怕吵醒谁。' }
        ]
      },
      {
        scene: '🕯 回声走廊',
        text: '走廊把你的脚步声放大成一串回音，一遍一遍地叠上来。',
        options: [
          { label: '跟着回音打节拍', points: 11, text: '回音慢慢变成旋律，墙壁亮起一排小灯，替你把路照开。' },
          { label: '踮脚安静走过', points: 7, text: '你几乎没发出声音，走廊尽头的水晶没有被惊动。' }
        ]
      },
      {
        scene: '🧩 松动的石板',
        text: '地板中央有一块石板明显是松的，下面传来齿轮转动的声音。',
        options: [
          { label: '踩上去试试', points: 12, text: '石板弹开，露出一枚旧徽章，边缘还刻着某位探险家的名字。' },
          { label: '谨慎绕过去', points: 6, text: '你绕开了机关，安全走到下一间，只是忍不住回头看它一眼。' }
        ]
      },
      {
        scene: '✨ 裂了缝的水晶',
        text: '一块水晶裂了缝，里面似乎封着什么东西，微微透出暖色。',
        options: [
          { label: '小心地敲开', points: 10, text: '裂缝里躺着一小段琥珀色的光，被珍奇郑重地收进了设计图。' },
          { label: '原样放回去', points: 8, text: '你把它摆回原位，裂缝在光里慢慢合上了。' }
        ]
      }
    ]
  },
  {
    id: 'candy', name: '糖果溪谷', icon: '🍬', color: '#f7a8c4', risk: '嘴馋',
    desc: '碧琪的主场，河是糖霜做的，屋子是蛋糕盖的。',
    events: [
      {
        scene: '🍬 糖霜河',
        text: '河水是粉色的糖霜，河面上只有几块软糖当落脚点。',
        options: [
          { label: '踩着软糖跳过去', points: 11, text: '软糖弹力惊人，你连跳三下过了河，落地时还舔了舔鞋底。' },
          { label: '去上游找石桥', points: 8, text: '石桥很稳，桥头有个卖棒棒糖的小摊，摊主请你吃了一根。' }
        ]
      },
      {
        scene: '🎂 蛋糕屋',
        text: '整间屋子都是蛋糕做的，屋檐正往下滴奶油。',
        options: [
          { label: '先敲门', points: 10, text: '开门的是纸杯蛋糕夫人，她请你尝了一块刚出炉的试作品。' },
          { label: '先舔一口墙', points: 7, text: '被夫人抓个正着。她笑着递给你一把勺子，说：至少用勺子。' }
        ]
      },
      {
        scene: '🎈 气球风暴',
        text: '一大群气球不知被谁放了出来，满天乱飞，绳头缠成一团。',
        options: [
          { label: '跳起来抓最漂亮的那只', points: 9, text: '抓住时发现下面系着一张派对邀请函，写着你的名字。' },
          { label: '把气球一只只拉回架子', points: 8, text: '你忙了半天，碧琪扑过来给了你一个大大的拥抱。' }
        ]
      },
      {
        scene: '🍭 蹲在路边的糖匠',
        text: '一位糖匠蹲在路边发愁，说他的配方被风吹跑了。',
        options: [
          { label: '帮他一起找', points: 12, text: '配方卡在树梢上，你爬上去取回来，他送了你一罐手工糖。' },
          { label: '陪他重想一份', points: 9, text: '你们一起试了七次，第八次的酸角软糖意外地好吃。' }
        ]
      }
    ]
  }
]

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function defaultState() {
  return { last: '', trips: [], totalPoints: 0 }
}

function loadState() {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return defaultState()
    return Object.assign(defaultState(), JSON.parse(raw))
  } catch (e) {
    return defaultState()
  }
}

/** 洗牌：随机抽 3 段奇遇 */
function pickEvents(list, n) {
  const arr = list.slice()
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    const t = arr[i]; arr[i] = arr[j]; arr[j] = t
  }
  return arr.slice(0, n)
}

export default {
  name: 'AdventureView',
  inject: ['toast'],
  data() {
    return {
      routes: ROUTES,
      state: loadState(),
      phase: 'pick',      // pick / event / result
      route: null,
      events: [],
      step: 0,
      gained: 0,
      picks: []
    }
  },
  computed: {
    today() {
      return todayStr()
    },
    doneToday() {
      return this.state.last === this.today
    },
    todayTrip() {
      return this.state.trips.find((t) => t.date === this.today) || null
    },
    recentTrips() {
      return this.state.trips.slice(0, 5)
    },
    currentEvent() {
      return this.events[this.step] || null
    },
    rank() {
      return RANKS.find((r) => this.state.trips.length >= r.min) || RANKS[RANKS.length - 1]
    },
    rankNext() {
      const i = RANKS.indexOf(this.rank)
      return i > 0 ? RANKS[i - 1] : null
    },
    rankPct() {
      const next = this.rankNext
      if (!next) return 100
      const cur = this.rank.min
      const span = next.min - cur
      return Math.max(0, Math.min(100, Math.round(((this.state.trips.length - cur) / span) * 100)))
    }
  },
  methods: {
    click() {
      try { sound && sound.click && sound.click() } catch (e) {}
    },
    save() {
      try { localStorage.setItem(KEY, JSON.stringify(this.state)) } catch (e) {}
    },
    start(r) {
      // 今天已经走过就不再出发，防止刷新页面重置进度重复拿分
      if (this.doneToday) return
      this.click()
      this.route = r
      this.events = pickEvents(r.events, STEPS)
      this.step = 0
      this.gained = 0
      this.picks = []
      this.phase = 'event'
    },
    choose(opt) {
      this.click()
      const ev = this.currentEvent
      this.picks.push({ scene: ev.scene, label: opt.label, text: opt.text, points: opt.points })
      this.gained += opt.points
      if (this.step < this.events.length - 1) {
        this.step += 1
      } else {
        this.finish()
      }
    },
    finish() {
      const st = this.state
      st.last = this.today
      st.trips.unshift({
        date: this.today,
        route: this.route.id,
        name: this.route.name,
        icon: this.route.icon,
        points: this.gained,
        picks: this.picks.map((p) => ({ scene: p.scene, points: p.points }))
      })
      st.trips = st.trips.slice(0, 20)
      st.totalPoints += this.gained
      this.save()
      addPoints(this.gained)
      this.phase = 'result'
      this.toast(`远征归来，+${this.gained} 积分！`, 'ok')
    },
    backToCamp() {
      this.phase = 'pick'
      this.route = null
      this.events = []
      this.step = 0
      this.gained = 0
      this.picks = []
    }
  }
}
</script>

<style scoped>
.adventure { padding: 20px 18px 26px; }

/* ===== 顶部 ===== */
.adv-head { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.adv-back {
  width: 34px; height: 34px; border-radius: 50%; flex: 0 0 auto;
  background: var(--card); border: 1.5px solid var(--border);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: var(--muted); text-decoration: none;
  transition: all .15s;
}
.adv-back:hover { color: var(--primary); border-color: var(--primary); }
.adv-title b { font-size: 17px; }
.adv-title p { margin: 2px 0 0; font-size: 12px; color: var(--muted); line-height: 1.5; }

/* ===== 探险等级 ===== */
.adv-rank {
  border: 1.5px solid var(--border); border-radius: 16px;
  padding: 13px 14px 11px; margin-bottom: 18px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--purple) 14%, #fff), #fff 70%);
}
.rank-main { display: flex; align-items: center; gap: 10px; }
.rank-icon {
  width: 38px; height: 38px; border-radius: 50%; flex: 0 0 auto;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; background: #fff; box-shadow: 0 2px 8px rgba(160,108,213,.16);
}
.rank-txt b { font-size: 14.5px; color: var(--text); }
.rank-txt em { display: block; font-style: normal; font-size: 11.5px; color: var(--muted); margin-top: 2px; }
.rank-bar {
  height: 6px; border-radius: 999px; background: #ece6f3;
  margin: 11px 0 7px; overflow: hidden;
}
.rank-bar i {
  display: block; height: 100%; border-radius: 999px;
  background: linear-gradient(90deg, var(--pink), var(--purple));
  transition: width .4s ease;
}
.rank-next { margin: 0; font-size: 11px; color: var(--muted); }

/* ===== 选路线 ===== */
.adv-sub { font-size: 13px; font-weight: 800; color: var(--text); margin-bottom: 10px; }
.route-list { display: flex; flex-direction: column; gap: 10px; }
.route-card {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border-radius: 16px;
  background: var(--card); border: 1.5px solid var(--border);
  cursor: pointer; text-align: left; transition: all .16s;
}
.route-card:hover {
  border-color: var(--rc, var(--primary));
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(160,108,213,.14);
}
.route-face {
  width: 44px; height: 44px; border-radius: 14px; flex: 0 0 auto;
  display: flex; align-items: center; justify-content: center; font-size: 22px;
  background: color-mix(in srgb, var(--rc, var(--primary)) 20%, #fff);
}
.route-main { flex: 1; min-width: 0; }
.route-main b { font-size: 14px; color: var(--text); }
.route-main em {
  display: block; font-style: normal; font-size: 11.5px;
  color: var(--muted); line-height: 1.5; margin-top: 2px;
}
.route-meta {
  flex: 0 0 auto; font-size: 11px; font-weight: 800;
  color: var(--rc, var(--primary));
  background: color-mix(in srgb, var(--rc, var(--primary)) 14%, #fff);
  padding: 3px 9px; border-radius: 999px;
}
.adv-hint { font-size: 11px; color: var(--muted); line-height: 1.6; margin: 12px 2px 0; }

/* ===== 今日战报 ===== */
.adv-report {
  border: 1.5px dashed var(--border); border-radius: 16px;
  padding: 14px; background: var(--panel);
}
.report-top { display: flex; align-items: center; gap: 8px; }
.report-icon { font-size: 18px; }
.report-top b { font-size: 13.5px; color: var(--text); }
.report-top em {
  margin-left: auto; font-style: normal; font-size: 13px;
  font-weight: 800; color: var(--primary);
}
.report-list { list-style: none; margin: 10px 0 0; padding: 0; }
.report-list li {
  display: flex; align-items: center; gap: 8px;
  font-size: 11.5px; color: var(--muted); padding: 4px 0;
}
.report-list li i { margin-left: auto; font-style: normal; color: var(--primary); font-weight: 700; }
.report-tip { margin: 10px 0 0; font-size: 11px; color: var(--muted); line-height: 1.6; }

/* ===== 奇遇 ===== */
.ev-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.ev-route { font-size: 12px; font-weight: 800; color: var(--text); }
.ev-dots { display: flex; gap: 5px; margin-left: auto; }
.ev-dots i {
  width: 7px; height: 7px; border-radius: 50%;
  background: var(--border); transition: all .2s;
}
.ev-dots i.on { background: var(--purple); transform: scale(1.15); }
.ev-gain { font-size: 12px; font-weight: 800; color: var(--primary); }
.ev-card {
  border-radius: 16px; padding: 15px 16px;
  border: 1.5px solid color-mix(in srgb, var(--rc, var(--primary)) 32%, #fff);
  background: linear-gradient(150deg, color-mix(in srgb, var(--rc, var(--primary)) 12%, #fff), #fff 72%);
}
.ev-scene { font-size: 15px; font-weight: 800; color: var(--text); }
.ev-text { margin: 8px 0 0; font-size: 12.5px; line-height: 1.8; color: #6b6180; }
.opt-list { display: flex; flex-direction: column; gap: 9px; margin-top: 14px; }
.opt {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 14px; border-radius: 13px; cursor: pointer;
  background: var(--card); border: 1.5px solid var(--border);
  font-size: 13px; color: var(--text); text-align: left; transition: all .15s;
}
.opt:hover { border-color: var(--primary); background: var(--panel); transform: translateX(3px); }
.opt i { margin-left: auto; font-style: normal; color: var(--primary); }

/* ===== 结算 ===== */
.res-card {
  border: 1.5px solid var(--border); border-radius: 18px;
  padding: 18px 16px; text-align: center; background: var(--card);
}
.res-icon { font-size: 30px; }
.res-title { display: block; font-size: 15px; color: var(--text); margin-top: 6px; }
.res-gain { margin: 6px 0 0; font-size: 12px; color: var(--muted); }
.res-gain span { font-size: 15px; font-weight: 800; color: var(--primary); }
.res-list { list-style: none; margin: 14px 0 0; padding: 0; text-align: left; }
.res-list li {
  display: flex; flex-wrap: wrap; align-items: baseline; gap: 6px;
  padding: 9px 0; border-top: 1px dashed var(--border);
  font-size: 11.5px; color: var(--muted); line-height: 1.65;
}
.res-scene { font-weight: 800; color: var(--text); flex: 0 0 auto; }
.res-text { flex: 1 1 100%; }
.res-list li i { font-style: normal; color: var(--primary); font-weight: 700; margin-left: auto; }
.res-btn {
  margin-top: 16px; width: 100%; padding: 11px; border: 0; border-radius: 12px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: #fff; font-size: 13px; font-weight: 800; cursor: pointer;
  transition: transform .15s, box-shadow .15s;
}
.res-btn:hover { transform: translateY(-1px); box-shadow: 0 8px 18px rgba(160,108,213,.28); }

/* ===== 探险日志 ===== */
.trip-log { margin-top: 20px; }
.log-title { font-size: 12px; font-weight: 800; color: var(--text); margin-bottom: 8px; }
.log-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 11.5px; color: var(--muted); padding: 6px 0;
  border-bottom: 1px dashed var(--border);
}
.log-item:last-child { border-bottom: 0; }
.log-date { flex: 0 0 auto; color: #b0a8bf; }
.log-route { flex: 1; min-width: 0; color: var(--text); font-weight: 700; }
.log-points { color: var(--primary); font-weight: 800; }

.adv-link {
  display: block; margin-top: 18px; text-align: center;
  font-size: 11.5px; color: var(--muted); text-decoration: none;
}
.adv-link:hover { color: var(--primary); }
</style>
