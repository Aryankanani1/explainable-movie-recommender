// End-to-end check of the web UI through headless Chrome (Chrome DevTools Protocol), no npm dependencies.
// It clicks through the four views like a person and, with a second argument, saves the docs/screenshots.
//
//   ./mvnw -q spring-boot:run              (in another terminal; add -Dspring-boot.run.arguments=--server.port=8090
//                                           if 8080 is taken)
//   node scripts/ui-e2e.mjs http://localhost:8080 docs/screenshots
//
// Needs Node 22+ (built-in WebSocket) and Google Chrome. WARNING: it deletes the current user's grades and grades six
// films instead (The Matrix, Toy Story, Pulp Fiction, Inception, Titanic, The Notebook).
import { spawn } from 'node:child_process';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const BASE = process.argv[2] ?? 'http://localhost:8080';
const SHOTS = process.argv[3];
const PORT = 9333;
const chrome = spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', [
	'--headless=new', `--remote-debugging-port=${PORT}`, `--user-data-dir=${mkdtempSync(join(tmpdir(), 'e2e-'))}`,
	'--window-size=1280,900', '--no-first-run', '--no-default-browser-check', 'about:blank'], { stdio: 'ignore' });

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const results = [];
const check = (name, ok, detail = '') => { results.push({ name, ok, detail }); console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  — ' + detail : ''}`); };

let ws; let nextId = 1; const pending = new Map();
async function connect() {
	for (let i = 0; i < 50; i++) {
		try {
			const targets = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json();
			const page = targets.find((t) => t.type === 'page');
			if (page) { ws = new WebSocket(page.webSocketDebuggerUrl); break; }
		} catch { /* not up yet */ }
		await sleep(200);
	}
	await new Promise((r) => ws.addEventListener('open', r, { once: true }));
	ws.addEventListener('message', (e) => {
		const msg = JSON.parse(e.data);
		if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); }
	});
}
function send(method, params = {}) {
	const id = nextId++;
	ws.send(JSON.stringify({ id, method, params }));
	return new Promise((resolve, reject) => pending.set(id, (m) => (m.error ? reject(new Error(m.error.message)) : resolve(m.result))));
}
async function js(expression) {
	const r = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
	if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description ?? r.exceptionDetails.text);
	return r.result.value;
}
async function waitFor(expression, timeout = 8000) {
	const start = Date.now();
	while (Date.now() - start < timeout) {
		if (await js(`!!(${expression})`)) return true;
		await sleep(100);
	}
	throw new Error(`Timed out waiting for: ${expression}`);
}
async function go(hash) {
	await js(`location.hash = '${hash}'`);
	await sleep(150);
}
async function shot(name, fullHeight = 2000) {
	if (!SHOTS) return;
	await js(`(document.getElementById('toasts').replaceChildren(), true)`);
	await js('document.fonts.ready.then(() => true)');
	await sleep(700);
	const { cssContentSize } = await send('Page.getLayoutMetrics');
	const height = Math.min(Math.ceil(cssContentSize.height), fullHeight);
	const { data } = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true,
		clip: { x: 0, y: 0, width: Math.ceil(cssContentSize.width), height, scale: 1 } });
	writeFileSync(join(SHOTS, `${name}.png`), Buffer.from(data, 'base64'));
	console.log(`      screenshot ${name}.png (${Math.ceil(cssContentSize.width)}×${height})`);
}

try {
	await connect();
	await send('Page.enable');
	await send('Runtime.enable');
	await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 900, deviceScaleFactor: 1, mobile: false });

	// Start from no grades, through the same API the UI uses.
	await send('Page.navigate', { url: `${BASE}/#grade` });
	await waitFor(`window.recsys && document.querySelectorAll('.film').length > 0`);
	await js(`(async () => { for (const g of await recsys.api.grades()) await recsys.store.remove(g.movieId); return true; })()`);
	await waitFor(`document.querySelector('.ledger__count')?.textContent === '0'`);
	check('grade view lists the most-rated films before any search', await js(`document.querySelectorAll('.film').length`) >= 10);

	// Grade 6 films by typing in the search box and clicking a grade button, like a person.
	const persona = [['matrix', 'The Matrix', 9], ['toy story', 'Toy Story', 8], ['pulp fiction', 'Pulp Fiction', 10],
		['inception', 'Inception', 9], ['titanic', 'Titanic', 3], ['notebook', 'The Notebook', 2]];
	for (const [query, title, score] of persona) {
		await js(`(() => { const i = document.getElementById('movie-search'); i.value = ${JSON.stringify(query)}; i.dispatchEvent(new Event('input')); return true; })()`);
		const card = `[...document.querySelectorAll('.film')].find(c => c.querySelector('.film__title').firstChild.textContent.trim() === ${JSON.stringify(title)})`;
		await waitFor(card);
		await js(`${card}.querySelector('[data-score="${score}"]').click()`);
		await waitFor(`${card}.querySelector('[data-score="${score}"]').getAttribute('aria-pressed') === 'true'`);
	}
	await waitFor(`document.querySelector('.ledger__count').textContent === '6'`);
	check('graded 6 films through the UI', true, (await js(`[...document.querySelectorAll('.ledger__item')].map(li => li.querySelector('.ledger__name').title + ' ' + li.querySelector('.ledger__score').textContent).join(', ')`)));
	check('tab badge shows 6', (await js(`document.getElementById('grade-count').textContent`)) === '6');
	await js(`(() => { const i = document.getElementById('movie-search'); i.value = ''; i.dispatchEvent(new Event('input')); return true; })()`);
	await sleep(600);
	await shot('1-grade', 1700);

	// Recommendations: both methods, lists differ, explanations show, never a graded film.
	await go('#recommendations');
	const listTitles = `[...document.querySelectorAll('.programme .entry__title')].map(t => t.firstChild.textContent)`;
	await js(`document.querySelector('.segmented [data-value="lecture"]').click()`);
	await waitFor(`document.querySelector('[aria-label="Recommendations, lecture method"]')`);
	const lecture = await js(listTitles);
	await js(`document.querySelector('.segmented [data-value="improved"]').click()`);
	await waitFor(`document.querySelector('[aria-label="Recommendations, improved method"]')`);
	const improved = await js(listTitles);
	check('lecture list has 10 films', lecture.length === 10, lecture.slice(0, 3).join(' | '));
	check('improved list has 10 films', improved.length === 10, improved.slice(0, 3).join(' | '));
	check('the two lists differ', JSON.stringify(lecture) !== JSON.stringify(improved),
		`${lecture.filter((t) => !improved.includes(t)).length} of 10 differ`);
	const graded = persona.map((p) => p[1]);
	check('no graded film is recommended', ![...lecture, ...improved].some((t) => graded.includes(t)));
	const sentences = await js(`[...document.querySelectorAll('.entry__sentence')].map(s => s.textContent)`);
	check('every improved pick has an explanation sentence', sentences.length === 10 && sentences.every((s) => s.length > 10), sentences[0]);
	check('"because you rated" line shows', (await js(`document.querySelectorAll('.entry__because').length`)) > 0,
		await js(`document.querySelector('.entry__because')?.textContent`));
	check('no fallback banner with 6 grades', !(await js(`!!document.querySelector('.banner')`)));
	await shot('2-recommendations', 1900);

	// Show the math (lecture): the rendered P row and scores match the API exactly.
	await go('#math');
	await js(`document.querySelector('.segmented [data-value="lecture"]').click()`);
	await waitFor(`[...document.querySelectorAll('tfoot th')].some(t => t.textContent.startsWith('P '))`);
	const api = await js(`recsys.api.math('lecture', 5)`);
	const domP = await js(`[...[...document.querySelectorAll('tfoot tr')].find(r => r.querySelector('th').textContent.startsWith('P ')).querySelectorAll('td')].map(td => td.firstChild.textContent)`);
	check('math view P row equals the API', JSON.stringify(domP) === JSON.stringify(api.lecture.profile.P.map((v) => (Math.abs(v) < 1e-1 ? '0' : v.toFixed(0)))),
		`P = [${domP.join(', ')}], T = ${api.lecture.profile.T}`);
	const domScores = await js(`[...document.querySelectorAll('td.total:not(:last-child)')].map(td => td.textContent)`);
	check('math view scores equal the API', JSON.stringify(domScores) === JSON.stringify(api.lecture.candidates.map((c) => c.score.toFixed(4))),
		domScores.join(', '));
	await shot('3-show-the-math-lecture', 2300);
	await js(`document.querySelector('.segmented [data-value="improved"]').click()`);
	await waitFor(`[...document.querySelectorAll('tfoot th')].some(t => t.textContent.startsWith('p = '))`);
	const apiImproved = await js(`recsys.api.math('improved', 5)`);
	const domImprovedScores = await js(`[...document.querySelectorAll('td.total')].map(td => td.textContent)`);
	check('improved math scores equal the API', JSON.stringify(domImprovedScores) === JSON.stringify(apiImproved.contentBased.candidates.map((c) => c.score.toFixed(4))),
		`r̄ = ${apiImproved.contentBased.rBar.toFixed(2)}, ${domImprovedScores.join(', ')}`);
	await shot('3-show-the-math-improved', 2600);

	// Lecture example: 0.667 / 0.633 / 0.333 / 0 and ×10.
	await go('#example');
	await waitFor(`document.querySelector('[aria-label="Lecture example results"]')`);
	const example = await js(`[...document.querySelectorAll('[aria-label="Lecture example results"] .entry')].map(e => [e.querySelector('.entry__title').firstChild.textContent, e.querySelector('.entry__score-value').textContent, e.querySelector('.entry__score-x10').textContent])`);
	check('lecture example scores', JSON.stringify(example.map((e) => e[1])) === JSON.stringify(['0.6667', '0.6333', '0.3333', '0.0000']),
		example.map((e) => `${e[0]} ${e[1]} (${e[2]})`).join('; '));
	check('lecture example ×10 values', JSON.stringify(example.map((e) => e[2])) === JSON.stringify(['×10 = 6.67', '×10 = 6.33', '×10 = 3.33', '×10 = 0.00']));
	const tRow = await js(`document.querySelector('.figure__value').textContent`);
	check('lecture example T = 60', tRow === '60');
	await shot('4-lecture-example', 2400);

	// An invalid grade, through the UI's own error path: the toast shows the server's readable detail.
	await go('#grade');
	await waitFor(`document.querySelectorAll('.film').length > 0`);
	await js(`import('/js/ui.js').then(ui => recsys.store.grade(2571, 11).catch(e => ui.showError(e, 'Could not save that grade'))).then(() => true)`);
	await waitFor(`document.querySelector('.toast--error')`);
	const toastText = await js(`document.querySelector('.toast--error').textContent`);
	check('invalid grade shows a readable error', toastText.includes('score must be a whole number from 1 to 10'), toastText);
	check('the invalid grade was not saved', (await js(`recsys.api.grades()`)).find((g) => g.movieId === 2571)?.score === 9);

	// Phone width.
	await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 2, mobile: true });
	await go('#recommendations');
	await waitFor(`document.querySelector('.programme')`);
	const overflow = await js(`document.documentElement.scrollWidth - document.documentElement.clientWidth`);
	check('phone width: no horizontal page scroll', overflow <= 0, `overflow ${overflow}px`);
	await shot('5-recommendations-phone', 2400);
	await go('#grade');
	await waitFor(`document.querySelectorAll('.film').length > 0`);
	const gradeOverflow = await js(`document.documentElement.scrollWidth - document.documentElement.clientWidth`);
	check('phone width: grade view fits', gradeOverflow <= 0, `overflow ${gradeOverflow}px`);
} catch (error) {
	check('script error', false, error.message);
} finally {
	chrome.kill();
	const failed = results.filter((r) => !r.ok).length;
	console.log(`\n${results.length - failed}/${results.length} checks passed`);
	process.exit(failed ? 1 : 0);
}
