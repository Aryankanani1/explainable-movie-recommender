// API smoke test of a running app, local or deployed: register a fresh account, sign in, grade six films from the
// starter set and fetch both recommendation lists and the math. Node 20+, no npm dependencies.
//
//   node scripts/smoke-test.mjs http://localhost:8080
//   node scripts/smoke-test.mjs https://<your-app>.onrender.com     (a sleeping free instance takes about a minute)
//
// It leaves one throwaway account (smoke-<timestamp>) with six grades behind.

const BASE = (process.argv[2] ?? 'http://localhost:8080').replace(/\/$/, '');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let failures = 0;
const check = (name, ok, detail = '') => {
	if (!ok) failures++;
	console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  — ' + detail : ''}`);
};

// A minimal cookie jar: the session cookie and the XSRF-TOKEN cookie that every write must echo as a header.
const cookies = new Map();
async function call(method, path, { json, form } = {}) {
	const headers = { Accept: 'application/json' };
	if (cookies.size) headers.Cookie = [...cookies].map(([k, v]) => `${k}=${v}`).join('; ');
	if (cookies.has('XSRF-TOKEN')) headers['X-XSRF-TOKEN'] = decodeURIComponent(cookies.get('XSRF-TOKEN'));
	let body;
	if (json !== undefined) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(json); }
	if (form) { headers['Content-Type'] = 'application/x-www-form-urlencoded'; body = new URLSearchParams(form).toString(); }
	const response = await fetch(BASE + path, { method, headers, body, redirect: 'manual' });
	for (const line of response.headers.getSetCookie()) {
		const pair = line.split(';')[0];
		const name = pair.slice(0, pair.indexOf('='));
		const value = pair.slice(pair.indexOf('=') + 1);
		if (value === '' || /max-age=0/i.test(line)) cookies.delete(name); else cookies.set(name, value);
	}
	const text = await response.text();
	const isJson = (response.headers.get('content-type') ?? '').includes('json');
	return { status: response.status, body: isJson && text ? JSON.parse(text) : text };
}

// Retries while the host wakes up (connection errors, 502/503) or the catalog is still loading (503, or `ready` false).
async function until(method, path, seconds, ready = () => true) {
	const deadline = Date.now() + seconds * 1000;
	let last = 'no response';
	while (Date.now() < deadline) {
		try {
			const res = await call(method, path);
			if (res.status !== 502 && res.status !== 503 && res.status !== 504 && ready(res)) return res;
			last = `HTTP ${res.status}`;
		} catch (e) {
			last = e.cause?.code ?? e.message;
		}
		await sleep(2000);
	}
	return { status: 0, body: last };
}

console.log(`Smoke test against ${BASE}`);
// The web server is up before the first-boot import has finished; until then the genre list is empty.
const genres = await until('GET', '/api/genres', 240, (res) => res.status !== 200 || res.body.length > 0);
check('the app answers and lists genres', genres.status === 200 && genres.body.length > 0, `HTTP ${genres.status}`);
if (genres.status !== 200 || genres.body.length === 0) process.exit(1);

const lecture = await call('GET', '/api/demo/lecture-example');
check('the lecture example is served', lecture.status === 200, `HTTP ${lecture.status}`);

const username = `smoke-${Date.now()}`;
const password = `pw-${Math.random().toString(36).slice(2)}-${Date.now()}`;
const registered = await call('POST', '/api/auth/register', { json: { username, password } });
check('register a new account', registered.status === 201, `HTTP ${registered.status}`);

const login = await call('POST', '/login', { form: { username, password } });
check('sign in', login.status === 200 && login.body.username === username, `HTTP ${login.status}`);
await call('GET', '/api/me'); // picks up the XSRF-TOKEN rotated by the sign-in

const starter = await until('GET', '/api/onboarding/starter-set?size=15', 120);
check('the starter set has 15 films', starter.status === 200 && starter.body.length === 15, `HTTP ${starter.status}`);
const graded = (starter.body ?? []).slice(0, 6);
let gradesOk = graded.length === 6;
for (let i = 0; i < graded.length; i++) {
	const res = await call('PUT', `/api/me/grades/${graded[i].id}`, { json: { score: i < 3 ? 9 : 3 } });
	gradesOk &&= res.status === 200 || res.status === 204;
}
check('grade six films', gradesOk);

const gradedIds = new Set(graded.map((m) => m.id));
for (const method of ['improved', 'lecture']) {
	const recs = await until('GET', `/api/me/recommendations?method=${method}&k=10`, 120);
	const items = recs.body?.items ?? [];
	check(`${method}: ten recommendations, none of them graded`,
		recs.status === 200 && items.length === 10 && items.every((r) => !gradedIds.has(r.movieId)),
		`HTTP ${recs.status}, ${items.length} items`);
	check(`${method}: built from the grades, each with an explanation`,
		recs.body?.fallback === 'NONE' && items.every((r) => r.explanation?.sentence),
		`fallback ${recs.body?.fallback}`);
}

const math = await call('GET', '/api/me/recommendations/math?method=lecture&k=5');
check('show the math', math.status === 200, `HTTP ${math.status}`);

const logout = await call('POST', '/logout');
const after = await call('GET', '/api/me');
check('sign out', logout.status === 204 && after.status === 401, `HTTP ${logout.status}, then ${after.status}`);

console.log(failures === 0 ? 'All checks passed.' : `${failures} check(s) failed.`);
process.exit(failures === 0 ? 0 : 1);
