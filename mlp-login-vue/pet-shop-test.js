/**
 * 领养 + 食物 + 装扮 端到端测试
 */
const http = require('http');

const ME = 1;      // 已有宠物：小火龙·焰焰
const ME2 = 2;     // 无宠物，用来测手动领养

function api(path, method = 'GET', body = null) {
  return new Promise((resolve) => {
    const data = body ? JSON.stringify(body) : null;
    const req = http.request({
      host: 'localhost', port: 8081, path,
      method,
      headers: data ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data) } : {}
    }, (res) => {
      let text = '';
      res.on('data', (c) => text += c);
      res.on('end', () => {
        try { resolve(JSON.parse(text)); } catch (e) { resolve({ code: -1, msg: text.slice(0, 200) }); }
      });
    });
    req.on('error', (e) => resolve({ code: -1, msg: e.message }));
    if (data) req.write(data);
    req.end();
  });
}

const out = [];
const log = (s) => { out.push(s); console.log(s); };

(async () => {
  // ===== A. 手动领养（账号2）=====
  log('===== A. 手动领养 =====');
  // 无宠物时装备装扮应被拦
  let r = await api('/api/pet/cosmetic/equip', 'POST', { userId: ME2, cosmeticId: 'cosmetic-hat' });
  log('[A1] 无宠物equip拦截=' + JSON.stringify(r));

  r = await api('/api/pet/adopt', 'POST', { userId: ME2, kind: 'bird', name: '云雀·小曲' });
  log('[A2] 领养云雀=' + JSON.stringify(r));

  r = await api('/api/pet/mine?userId=' + ME2);
  log('[A3] 账号2宠物=' + JSON.stringify(r.data));

  // 重复领养应被拦
  r = await api('/api/pet/adopt', 'POST', { userId: ME2, kind: 'cat', name: '二猫' });
  log('[A4] 重复领养拦截=' + JSON.stringify(r));

  // ===== B. 食物三兄弟（账号1）=====
  log('===== B. 食物 =====');
  r = await api('/api/shop/buy', 'POST', { userId: ME, itemId: 'pet-food' });
  log('[B1] 兑口粮=' + (r.code === 200 ? 'ok' : JSON.stringify(r)));
  let before = await api('/api/pet/mine?userId=' + ME);
  r = await api('/api/shop/use', 'POST', { userId: ME, itemId: 'pet-food' });
  let after = await api('/api/pet/mine?userId=' + ME);
  log('[B2] 口粮: 饱食 ' + before.data.hunger + ' → ' + after.data.hunger + ' | ' + JSON.stringify(r.data));

  r = await api('/api/shop/buy', 'POST', { userId: ME, itemId: 'pet-cake' });
  before = await api('/api/pet/mine?userId=' + ME);
  r = await api('/api/shop/use', 'POST', { userId: ME, itemId: 'pet-cake' });
  after = await api('/api/pet/mine?userId=' + ME);
  log('[B3] 蛋糕: 饱食 ' + before.data.hunger + ' → ' + after.data.hunger + ' | ' + JSON.stringify(r.data));

  r = await api('/api/shop/buy', 'POST', { userId: ME, itemId: 'pet-milk' });
  before = await api('/api/pet/mine?userId=' + ME);
  r = await api('/api/shop/use', 'POST', { userId: ME, itemId: 'pet-milk' });
  after = await api('/api/pet/mine?userId=' + ME);
  log('[B4] 牛奶: 心情 ' + before.data.mood + ' → ' + after.data.mood + ' | ' + JSON.stringify(r.data));

  // ===== C. 装扮（账号1）=====
  log('===== C. 装扮 =====');
  r = await api('/api/shop/items');
  const cosmetics = (r.data || []).filter(i => i.kind === 'cosmetic');
  log('[C1] 装扮商品=' + cosmetics.map(c => c.name + '💎' + c.price).join(' / '));

  r = await api('/api/shop/buy', 'POST', { userId: ME, itemId: 'cosmetic-hat' });
  log('[C2] 兑草帽=' + JSON.stringify(r));

  r = await api('/api/shop/bag?userId=' + ME);
  log('[C3] 背包(装扮不该在)=' + JSON.stringify((r.data || []).map(i => i.name)));

  r = await api('/api/pet/cosmetics?userId=' + ME);
  log('[C4] 衣橱=' + JSON.stringify(r.data));

  r = await api('/api/pet/cosmetic/equip', 'POST', { userId: ME, cosmeticId: 'cosmetic-hat' });
  log('[C5] 装备草帽=' + JSON.stringify(r));

  r = await api('/api/pet/mine?userId=' + ME);
  log('[C6] 宠物头顶=' + JSON.stringify(r.data.cosmetic));

  // 买蝴蝶结并装备（草帽应自动卸下）
  r = await api('/api/shop/buy', 'POST', { userId: ME, itemId: 'cosmetic-bow' });
  r = await api('/api/pet/cosmetic/equip', 'POST', { userId: ME, cosmeticId: 'cosmetic-bow' });
  log('[C7] 换蝴蝶结=' + JSON.stringify(r));
  r = await api('/api/pet/cosmetics?userId=' + ME);
  log('[C8] 衣橱状态=' + JSON.stringify((r.data || []).map(c => c.name + ':' + (c.equipped ? '戴' : '挂'))));
  r = await api('/api/pet/mine?userId=' + ME);
  log('[C9] 头顶=' + (r.data.cosmetic || {}).name);

  // 卸下
  r = await api('/api/pet/cosmetic/unequip', 'POST', { userId: ME });
  log('[C10] 卸下=' + JSON.stringify(r));
  r = await api('/api/pet/mine?userId=' + ME);
  log('[C11] 头顶(应null)=' + JSON.stringify(r.data.cosmetic));

  // 不拥有的装扮装备应被拦
  r = await api('/api/pet/cosmetic/equip', 'POST', { userId: ME, cosmeticId: 'cosmetic-crown' });
  log('[C12] 未拥有拦截=' + JSON.stringify(r));

  // 重复购买同一个 → count=2
  r = await api('/api/shop/buy', 'POST', { userId: ME, itemId: 'cosmetic-hat' });
  r = await api('/api/pet/cosmetics?userId=' + ME);
  const hat = (r.data || []).find(c => c.id === 'cosmetic-hat');
  log('[C13] 草帽数量=' + (hat ? hat.count : '?'));

  // ===== D. 商品总数 =====
  r = await api('/api/shop/items');
  log('[D1] 商品总数=' + (r.data || []).length);

  require('fs').writeFileSync(__dirname + '/pet-shop-test.txt', out.join('\n'), 'utf8');
})().catch((e) => {
  require('fs').writeFileSync(__dirname + '/pet-shop-test.txt', out.join('\n') + '\nFATAL: ' + e.message, 'utf8');
});
