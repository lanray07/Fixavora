import {test} from 'node:test';
import assert from 'node:assert/strict';
import worker, {SKU} from './worker.mjs';
const env = {AMAZON_SHARED_SECRET:'unit-test-only', RECEIPT_LIMITER:{limit:async()=>({success:true})}};
const req = (data={userId:'user',receiptId:'receipt'}) => new Request('https://example.test/verify', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});
const receipt = {productId:SKU,productType:'ENTITLED',receiptId:'receipt'};
test('valid Amazon receipt grants only matching entitlement', async t=> {
  t.mock.method(globalThis,'fetch',async()=>Response.json(receipt));
  const result = await worker.fetch(req(),env);
  assert.equal(result.status,200); assert.equal((await result.json()).active,true);
});
test('canceled, wrong SKU and wrong receipt never grant access', async t=> {
  for (const invalid of [{cancelDate:123},{productId:'wrong'},{receiptId:'wrong'},{productType:'CONSUMABLE'}]) {
    const mock = t.mock.method(globalThis,'fetch',async()=>Response.json({...receipt,...invalid}));
    assert.equal((await (await worker.fetch(req(),env)).json()).active,false); mock.mock.restore();
  }
});
test('LAT receipts must come from authenticated production RVS',async t=> {
  t.mock.method(globalThis,'fetch',async target=> {
    assert.ok(!new URL(target).pathname.startsWith('/sandbox/'));
    return Response.json({...receipt,testTransaction:true});
  });
  const result=await (await worker.fetch(req(),env)).json();
  assert.equal(result.active,true); assert.equal(result.testTransaction,true);
});
test('sandbox accepts Amazon test receipt', async t=> {
  t.mock.method(globalThis,'fetch',async()=>Response.json({...receipt,testTransaction:true}));
  assert.equal((await (await worker.fetch(req(),{...env,AMAZON_RVS_ENV:'sandbox'})).json()).active,true);
});
test('Amazon revocation is distinct from temporary unavailability', async t=> {
  for (const code of [400,410,429,500]) {
    const mock=t.mock.method(globalThis,'fetch',async()=>new Response('',{status:code}));
    const result=await worker.fetch(req(),env);
    assert.equal(result.status,code < 420 ? 200 : 502); mock.mock.restore();
  }
});
test('missing configuration, bad input and oversized body fail closed',async()=> {
  assert.equal((await worker.fetch(req(),{})).status,503);
  assert.equal((await worker.fetch(req({}),env)).status,400);
  assert.equal((await worker.fetch(req({userId:1,receiptId:'r'}),env)).status,400);
  assert.equal((await worker.fetch(req({userId:'x'.repeat(9000),receiptId:'r'}),env)).status,413);
  assert.equal((await worker.fetch(req(),{...env,RECEIPT_LIMITER:{limit:async()=>({success:false})}})).status,429);
});
test('health reveals configuration status without secrets',async()=> {
  const result=await worker.fetch(new Request('https://example.test/health'),env);
  assert.equal(result.status,200); assert.ok(!(await result.text()).includes(env.AMAZON_SHARED_SECRET));
});
test('Workers-compatible redirect mode refuses upstream redirects',async t=> {
  let calls=0,redirectMode;
  t.mock.method(globalThis,'fetch',async (target,init)=> {
    calls++;
    redirectMode=init.redirect;
    return new Response('',{status:302,headers:{Location:'https://unexpected.example/'}});
  });
  assert.equal((await worker.fetch(req(),env)).status,502);
  assert.equal(calls,1);
  assert.equal(redirectMode,'manual');
});
