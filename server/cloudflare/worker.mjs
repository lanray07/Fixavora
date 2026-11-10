export const SKU = 'fixavora.premium.lifetime';
const reply = (status, body) => Response.json(body, {status, headers: {'Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});

export function entitlement(receipt, userId, receiptId) {
  return {sku: SKU, userId, receiptId, active: receipt.productId === SKU &&
    receipt.productType === 'ENTITLED' && receipt.receiptId === receiptId &&
    !receipt.cancelDate, testTransaction:receipt.testTransaction === true};
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === '/health' && request.method === 'GET') {
      return reply(env.AMAZON_SHARED_SECRET && env.RECEIPT_LIMITER ? 200 : 503,
        {service:'fixavora-receipts', configured:!!(env.AMAZON_SHARED_SECRET && env.RECEIPT_LIMITER), environment:env.AMAZON_RVS_ENV || 'production'});
    }
    if (url.pathname !== '/verify' || url.search) return reply(404, {error:'not_found'});
    if (request.method !== 'POST') return reply(405, {error:'method_not_allowed'});
    if (!env.AMAZON_SHARED_SECRET || !env.RECEIPT_LIMITER) return reply(503, {error:'not_configured'});
    // Platform binding is distributed and approximate per location. Free-plan daily caps
    // bound total service usage; no paid database or in-memory-only limiter is required.
    const limited = await env.RECEIPT_LIMITER.limit({key:request.headers.get('CF-Connecting-IP') || 'unknown'});
    if (!limited.success) return reply(429, {error:'rate_limit'});
    if (!(request.headers.get('Content-Type') || '').toLowerCase().startsWith('application/json')) return reply(415, {error:'json_required'});
    let data;
    try {
      const reader = request.body?.getReader();
      if (!reader) return reply(400, {error:'invalid_request'});
      const chunks = []; let length = 0;
      while (true) {
        const {done, value} = await reader.read(); if (done) break;
        length += value.byteLength;
        if (length > 8192) { await reader.cancel(); return reply(413, {error:'request_too_large'}); }
        chunks.push(value);
      }
      const bytes = new Uint8Array(length); let offset = 0;
      for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
      data = JSON.parse(new TextDecoder('utf-8', {fatal:true}).decode(bytes));
      if (!data || Array.isArray(data) || !['userId','receiptId'].every(k => typeof data[k] === 'string' && data[k].length >= 1 && data[k].length <= 2048)) throw Error();
    } catch { return reply(400, {error:'invalid_request'}); }
    const {userId, receiptId} = data;
    const mode = env.AMAZON_RVS_ENV || 'production';
    if (!['production','sandbox'].includes(mode)) return reply(503, {error:'not_configured'});
    const prefix = mode === 'sandbox' ? 'sandbox/' : '';
    const target = 'https://appstore-sdk.amazon.com/' + prefix + 'version/1.0/verifyReceiptId/developer/' +
      encodeURIComponent(env.AMAZON_SHARED_SECRET) + '/user/' + encodeURIComponent(userId) + '/receiptId/' + encodeURIComponent(receiptId);
    try {
      // Workers does not implement redirect:'error'. Manual mode plus the !ok
      // check below rejects redirects without forwarding a secret to another host.
      const response = await fetch(target, {redirect:'manual', signal:AbortSignal.timeout(15000), headers:{'Accept':'application/json','User-Agent':'FixavoraReceiptVerifier/1.0'}});
      if ([400,410].includes(response.status)) return reply(200, {sku:SKU,userId,receiptId,active:false});
      if (!response.ok) return reply(502, {error:'verification_unavailable', upstreamStatus:response.status});
      const receipt = await response.json();
      if (!receipt || typeof receipt !== 'object') throw Error();
      // Amazon's production RVS also verifies genuine LAT/reviewer purchases. The
      // verified test marker describes billing; it does not make the receipt invalid.
      return reply(200, entitlement(receipt,userId,receiptId));
    } catch { return reply(502, {error:'verification_unavailable'}); }
  }
};
