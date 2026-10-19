/**
 * 商店端到端测试：兑换 → 背包 → 使用（孵化/喂食/红包/改名卡）→ 积分扣减
 * 前置：先清理测试数据、发足积分
 */
const http = require('http');

const ME = 1;
const FRIEND = 2;

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
  // 0. 先看看商品列表
  let r = await api('/api/shop/items');
  log('[0] 商品数=' + (r.data || []).length + ' | ' + (r.data || []).map(i => i.name + '💎' + i.price).join(' / '));

  // 1. 余额查询（账号1）
  r = await api('/api/account/profile?userId=' + ME);
  log('[1] ME 资料=' + JSON.stringify(r.data));

  // 2. 兑换一口粮（60 分）
  r = await api('/api/shop/buy', 'POST', { userId: ME, itemId: 'pet-food' });
  log('[2] 兑口粮=' + JSON.stringify(r));

  // 3. 兑换宠物蛋（200 分）
  r = await api('/api/shop/buy', 'POST', { userId: ME, itemId: 'pet-egg-dragon' });
  log('[3] 兑龙蛋=' + JSON.stringify(r));

  // 4. 背包
  r = await api('/api/shop/bag?userId=' + ME);
  log('[4] 背包=' + JSON.stringify((r.data || []).map(i => i.name + 'x' + i.count)));

  // 5. 孵化宠物蛋（起名）
  r = await api('/api/shop/use', 'POST', { userId: ME, itemId: 'pet-egg-dragon', petName: '小火龙·焰焰' });
  log('[5] 孵化=' + JSON.stringify(r));

  // 6. 宠物信息
  r = await api('/api/pet/mine?userId=' + ME);
  log('[6] 宠物=' + JSON.stringify(r.data));

  // 7. 喂食（用口粮）
  r = await api('/api/shop/use', 'POST', { userId: ME, itemId: 'pet-food' });
  log('[7] 喂食=' + JSON.stringify(r));
  r = await api('/api/pet/mine?userId=' + ME);
  log('[7b] 喂后饱食=' + (r.data || {}).hunger);

  // 8. 兑换红包 + 送给好友2
  r = await api('/api/shop/buy', 'POST', { userId: ME, itemId: 'point-redpacket' });
  log('[8] 兑红包=' + JSON.stringify(r));
  r = await api('/api/shop/use', 'POST', { userId: ME, itemId: 'point-redpacket', toId: FRIEND });
  log('[8b] 送红包=' + JSON.stringify(r));

  // 9. 好友2到账验证
  r = await api('/api/account/profile?userId=' + FRIEND);
  log('[9] 好友2积分=' + (r.data || {}).points);

  // 10. 兑换改名卡 + 改名（特殊符号）
  r = await api('/api/shop/buy', 'POST', { userId: ME, itemId: 'rename-card' });
  log('[10] 兑改名卡=' + JSON.stringify(r));
  r = await api('/api/shop/use', 'POST', { userId: ME, itemId: 'rename-card', newName: '焰焰的铲屎官·No.1' });
  log('[10b] 用改名卡=' + JSON.stringify(r));
  r = await api('/api/account/profile?userId=' + ME);
  log('[10c] 改名后=' + (r.data || {}).name);

  // 11. 背包核对（改名卡、红包应已消耗，口粮剩 0 不计）
  r = await api('/api/shop/bag?userId=' + ME);
  log('[11] 用后背包=' + JSON.stringify((r.data || []).map(i => i.name + 'x' + i.count)));

  // 12. 重复孵化应被拦（已有宠物）
  r = await api('/api/shop/buy', 'POST', { userId: ME, itemId: 'pet-egg-cat' });
  r = await api('/api/shop/use', 'POST', { userId: ME, itemId: 'pet-egg-cat', petName: '二猫' });
  log('[12] 二胎拦截=' + JSON.stringify(r));

  // 13. 积分花超了应被拦
  r = await api('/api/shop/buy', 'POST', { userId: ME, itemId: 'title-collector' });
  log('[13] 余额不足=' + JSON.stringify(r));

  require('fs').writeFileSync(__dirname + '/shop-test.txt', out.join('\n'), 'utf8');
})().catch((e) => {
  require('fs').writeFileSync(__dirname + '/shop-test.txt', out.join('\n') + '\nFATAL: ' + e.message, 'utf8');
});
