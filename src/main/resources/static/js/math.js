// View 3 · Show the math: the slides' matrices for your actual grades. Each table tints a cell by its value:
// warm cells push a film up, cool cells push it down, and the tint deepens with size.

import { api } from './api.js';
import { METHOD_NOTES, METHODS } from './recommendations.js';
import { empty, fixed, grade, h, loading, remember, segmented, select, showError } from './ui.js';

export function renderMath(root) {
	let method = METHOD_NOTES[remember.get('recsys.method')] ? remember.get('recsys.method') : 'improved';
	let k = 5;
	let sequence = 0;
	const body = h('div');

	root.append(
		h('section', { class: 'view-head' },
			h('p', { class: 'eyebrow', text: 'Step three' }),
			h('h2', { class: 'view-title' }, 'Show the ', h('em', { text: 'math' })),
			h('p', { class: 'lede', text: 'Every number behind your recommendations, laid out like the lecture slides '
				+ 'and computed from your current grades by the same engine that ranks the films.' })),
		h('div', { class: 'controls' },
			segmented('Method', METHODS, method, (value) => {
				method = value;
				remember.set('recsys.method', value);
				load();
			}),
			select('Candidates shown', [3, 5, 10], k, (value) => {
				k = value;
				load();
			})),
		body);

	async function load() {
		const mine = ++sequence;
		body.replaceChildren(loading('Computing'));
		try {
			const trace = await api.math(method, k);
			if (mine !== sequence) {
				return;
			}
			if (trace.fallback === 'POPULARITY') {
				body.replaceChildren(empty('No profile to show yet', 'Grade a few films first: with no usable grades '
					+ 'the recommendations fall back to popularity, which has no matrices.',
					h('a', { class: 'button button--primary', href: '#grade' }, 'Grade films')));
				return;
			}
			body.replaceChildren(trace.lecture ? lectureTables(trace) : contentTables(trace));
		} catch (error) {
			if (mine === sequence) {
				body.replaceChildren(empty('Could not compute the trace', error.message));
				showError(error, 'Show the math failed');
			}
		}
	}

	load();
	return () => {
		sequence++;
	};
}

/** The lecture method, step by step: weighted genre matrix → P → T → p̂ → scores (×10, as on the slides). */
export function lectureTables(trace) {
	const { columns } = trace;
	const { rows, profile, candidates } = trace.lecture;
	const weightedMax = maxAbs(rows.flatMap((r) => r.weighted).concat(profile.P));
	const pHatMax = maxAbs(profile.pHat);
	const contribMax = maxAbs(candidates.flatMap((c) => c.contrib));

	return h('div', {},
		h('p', { class: 'note' }, METHOD_NOTES.lecture),
		legend(),
		section('1', 'Genres of the films you graded', h('span', {},
			'Each film is a row of 0s and 1s (', formula('x'), '), one column per genre.')),
		table(['Film', 'Grade r', ...columns],
			rows.map((r) => [th(r.title), cell(grade(r.grade), 'divider'), ...r.x.map((v) => binary(v))])),

		section('2', 'Weighted genre matrix', h('span', {},
			'Multiply each row by its grade: ', formula('r · x'), '. Column sums give ', formula('P'),
			'; their total is ', formula('T'), '; dividing gives the profile ', formula('p̂ = P / T'), '.')),
		table(['Film', ...columns],
			rows.map((r) => [th(r.title), ...r.weighted.map((v) => heat(v, weightedMax, 0))]),
			[
				[th('P (column sums)'), ...profile.P.map((v) => heat(v, weightedMax, 0))],
				[th('p̂ = P / T'), ...profile.pHat.map((v) => heat(v, pHatMax, 3))],
			]),
		h('div', { class: 'figure' }, figure(fixed(profile.T, 0), 'T = Σ P'),
			figure(fixed(profile.pHat.reduce((a, b) => a + b, 0), 3), 'Σ p̂ (always 1)')),

		section('3', 'Scores for films you have not seen', h('span', {},
			formula('score(c) = Σ p̂_g · x_cg'), ': add up your profile weights over the film\'s genres. ',
			'The slides also show it ×10.')),
		table(['Film', ...columns, 'Score', '×10'],
			candidates.map((c) => [th(c.title), ...c.contrib.map((v, j) => (c.x[j] ? heat(v, contribMax, 3) : binary(0))),
				cell(fixed(c.score), 'divider total'), cell(fixed(c.scoreTimes10, 2), 'total')])));
}

/** The improved method: centred weights, IDF, normalized vectors, profile p, cosine scores. */
export function contentTables(trace) {
	const { columns } = trace;
	const c = trace.contentBased;
	const wMax = maxAbs(c.rows.map((r) => r.w));
	const wgMax = maxAbs(c.rows.flatMap((r) => r.wGHat).concat(c.p));
	const contribMax = maxAbs(c.candidates.flatMap((x) => x.contrib));
	const idfMax = c.idf ? maxAbs(c.idf) : 1;

	return h('div', {},
		h('p', { class: 'note' }, METHOD_NOTES.improved),
		h('ul', { class: 'config chips', 'aria-label': 'Configuration' },
			h('li', { class: 'chip', text: c.config.centerRatings ? 'centred grades ✓' : 'raw grades' }),
			h('li', { class: 'chip', text: c.config.useIdf ? 'IDF weighting ✓' : 'no IDF' }),
			h('li', { class: 'chip', text: `tag weight β = ${c.config.tagWeight}` }),
			c.config.popularityPrior > 0 && h('li', { class: 'chip', text: `popularity prior ${c.config.popularityPrior}` })),
		legend(),
		section('1', 'Centre the grades on your own average', h('span', {},
			formula(`r̄ = ${fixed(c.rBar, 2)}`), ', and each film\'s weight is ', formula('w = r − r̄'),
			'. A grade below your average becomes negative and pulls its genres down.')),
		table(['Film', 'Grade r', 'Weight w'],
			c.rows.map((r) => [th(r.title), cell(grade(r.grade)), heat(r.w, wMax, 2)])),

		c.idf && section('2', 'Rare genres count more', h('span', {},
			formula('idf_g = ln(N / n_g)'), ': a genre on few films tells you more than one on thousands.')),
		c.idf && table(['', ...columns], [[th('idf'), ...c.idf.map((v) => heat(v, idfMax, 3))]]),

		section(c.idf ? '3' : '2', 'Your taste profile p', h('span', {},
			'Each graded film becomes a vector of length 1 (', formula('ĝ'), '), scaled by its weight; ',
			'the sum is your profile ', formula('p = Σ w · ĝ'), '. Small print shows ĝ.')),
		table(['Film', ...columns],
			c.rows.map((r) => [th(r.title), ...r.wGHat.map((v, j) => heat(v, wgMax, 2, r.x[j] ? fixed(r.gHat[j], 3) : null))]),
			[[th('p = Σ w · ĝ'), ...c.p.map((v) => heat(v, wgMax, 2))]]),
		c.topTags.length > 0
			? h('div', {}, h('p', { class: 'note', text: 'Strongest tags in your profile (tags are summarized, not drawn as columns):' }),
				h('ul', { class: 'chips' }, c.topTags.map((t) => h('li', { class: 'chip chip--tag' }, t.tag,
					h('span', { class: 'chip__value', text: fixed(t.weight, 2) })))))
			: h('p', { class: 'note', text: `Tags are switched off (β = ${c.config.tagWeight}): the offline evaluation `
				+ 'found that they lowered accuracy on MovieLens, so the profile uses genres only.' }),

		section(c.idf ? '4' : '3', 'Scores: cosine similarity', h('span', {},
			formula('score(c) = cos(p, v_c)'), ': how closely the film points the same way as your taste, from −1 to 1. ',
			'Each cell is that genre\'s share of the score; together they add up to it.')),
		table(['Film', ...columns, 'Genres', 'Tags', 'Score'],
			c.candidates.map((x) => [th(x.title), ...x.contrib.map((v) => heat(v, contribMax, 3)),
				cell(fixed(x.genrePart), 'divider'), cell(fixed(x.tagPart)), cell(fixed(x.score), 'total')])));
}

function section(step, title, text) {
	return h('div', {}, h('h3', { class: 'section-title' }, h('span', { class: 'section-title__step', text: `0${step}` }),
		title), h('p', { class: 'note' }, text));
}

function table(headers, bodyRows, footRows = []) {
	return h('div', { class: 'table-wrap' }, h('table', { class: 'matrix' },
		h('thead', {}, h('tr', {}, headers.map((text, i) => h('th', { scope: 'col', style: i === 0 ? { textAlign: 'left' } : null, text })))),
		h('tbody', {}, bodyRows.map((cells) => h('tr', {}, cells))),
		footRows.length > 0 && h('tfoot', {}, footRows.map((cells) => h('tr', {}, cells)))));
}

function th(text) {
	return h('th', { scope: 'row', title: text, text });
}

function cell(text, cls) {
	return h('td', { class: cls, text });
}

function binary(value) {
	return h('td', { class: value ? null : 'zero', text: value ? '1' : '·' });
}

/** A value cell tinted by |value| / max: warm when positive, cool when negative. */
function heat(value, max, digits, small) {
	const strength = max > 0 ? Math.min(1, Math.abs(value) / max) : 0;
	const isZero = Math.abs(value) < 10 ** -(digits + 1);
	const classes = ['heat', value < 0 && !isZero ? 'neg' : '', strength > 0.62 ? 'strong' : '', isZero ? 'zero' : '']
		.filter(Boolean)
		.join(' ');
	return h('td', { class: classes, style: { '--heat': isZero ? '0' : strength.toFixed(3) } },
		isZero ? '0' : fixed(value, digits), small && h('small', { text: `ĝ ${small}` }));
}

function legend() {
	return h('p', { class: 'legend' },
		h('span', {}, h('span', { class: 'legend__swatch', style: { background: 'var(--red)' } }), 'pushes a film up'),
		h('span', {}, h('span', { class: 'legend__swatch', style: { background: 'var(--cool)' } }), 'pushes a film down'),
		h('span', { text: 'Deeper tint = larger effect within the table.' }));
}

function figure(value, label) {
	return h('div', { class: 'figure__item' }, h('span', { class: 'figure__value', text: value }),
		h('span', { class: 'figure__label', text: label }));
}

function formula(text) {
	return h('code', { class: 'formula', text });
}

function maxAbs(values) {
	return values.reduce((max, v) => Math.max(max, Math.abs(v)), 0);
}
