<template>
  <!--
    通用用户头像。三级回退：
      1. 真实图片（data: / http 开头的自定义上传头像）→ <img>
      2. emoji（用户在「我的账号」里挑的）→ 直接显示
      3. 都没有 → 按 userId 稳定分配一张 Q 版小马脸（同一个人永远是同一张）
    用法：<UserAvatar :user="u" :size="30" />
  -->
  <span class="ua" :class="{ emoji: !isImage && !!avatarValue }" :style="{ width: size + 'px', height: size + 'px' }" :title="title">
    <img v-if="isImage" class="ua-img" :src="avatarValue" alt="头像" />
    <span v-else-if="avatarValue" class="ua-emoji" :style="{ fontSize: emojiSize + 'px' }">{{ avatarValue }}</span>
    <ChibiHead v-else :id="ponyId" class="ua-pony" />
  </span>
</template>

<script>
import ChibiHead from './ChibiHead.vue'

/** 可分配的小马脸（跟 ChibiHead 的角色表一致） */
const PONY_IDS = [
  'pinkie', 'twilight', 'rainbow', 'applejack', 'fluttershy',
  'rarity', 'spike', 'luna', 'celestia'
]

export default {
  name: 'UserAvatar',
  components: { ChibiHead },
  props: {
    /** 用户对象：{ id, name, avatar } */
    user: { type: Object, default: null },
    /** 直接给 userId 也行 */
    userId: { type: [Number, String], default: null },
    /** 直接给头像值也行（emoji 或图片 dataURL） */
    avatar: { type: String, default: '' },
    name: { type: String, default: '' },
    /** 头像直径（px） */
    size: { type: Number, default: 32 }
  },
  computed: {
    /** 最终头像值：优先用传进来的 avatar，否则用 user.avatar */
    avatarValue() {
      if (this.avatar) return this.avatar
      return (this.user && this.user.avatar) || ''
    },
    /** 图片型头像：data: 或 http(s): */
    isImage() {
      const v = this.avatarValue
      return typeof v === 'string' && (v.startsWith('data:') || v.startsWith('http'))
    },
    /** emoji 的字号随头像大小走，但别太大 */
    emojiSize() {
      return Math.max(11, Math.round(this.size * 0.52))
    },
    /** 没有头像时，按 userId 稳定挑一张小马脸 */
    ponyId() {
      const id = this.uid
      let h = 0
      const s = String(id == null ? 'pony' : id)
      for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) % 100003
      return PONY_IDS[h % PONY_IDS.length]
    },
    uid() {
      if (this.userId != null) return this.userId
      return this.user ? this.user.id : null
    },
    title() {
      return this.name || (this.user && this.user.name) || '小马'
    }
  }
}
</script>

<style scoped>
.ua {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--panel);
  vertical-align: middle;
  line-height: 1;
}
/* emoji 头像：给个渐变圆底，跟 Q 版小马脸风格统一 */
.ua.emoji {
  background: linear-gradient(135deg, var(--pink), var(--purple));
}
.ua-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.ua-emoji {
  line-height: 1;
}
/* 小马脸：ChibiHead 的脸偏下（viewBox 里脸心在 y≈56），上移一点让脸居中 */
.ua-pony {
  width: 100%;
  height: 100%;
  display: block;
  transform: scale(1.12) translateY(-6%);
}
</style>
