// View 2 · Recommendations: the ranked programme, with why each film is on it.

import { api } from './api.js';
import { MIN_GRADES, store } from './store.js';
import { empty, fixed, genreChips, grade, h, loading, remember, reveal, segmented, select, showError, signed } from './ui.js';

export const METHODS = [
	['lecture', 'Lecture'],
	['improved', 'Improved'],
];

export const METHOD_NOTES = {
	lecture: 'The professor\'s method: each graded film adds its grade to its genres; a film\'s score is the sum of '
		+ 'your normalized genre weights.',
	improved: 'Grades centred on your own average (a low grade counts against its genres), genres weighted by rarity '
		+ '(IDF), and cosine similarity, so films with many genres get no free advantage.',
};

export function renderRecommendations(root) {
	let method = METHOD_NOTES[remember.get('recsys.method')] ? remember.get('recsys.method') : 'improved';
	let k = [5, 10, 20].includes(Number(remember.get('recsys.k'))) ? Number(remember.get('recsys.k')) : 10;
	let lambda = clampLambda(Number(remember.get('recsys.lambda', '1')));
	let sequence = 0;

	const note = h('p', { class: 'method-note', text: METHOD_NOTES[method] });
	const diversity = diversityField(lambda, (value) => {
		lambda = value;
		remember.set('recsys.lambda', String(value));
		load();
	});
	const list = h('div');

	root.append(
		h('section', { class: 'view-head' },
			h('p', { class: 'eyebrow', text: 'Step two' }),
			h('h2', { class: 'view-title' }, 'Tonight\'s ', h('em', { text: 'programme' })),
			h('p', { class: 'lede', text: 'Films you have not graded, ranked for you. Switch methods to see how the '
				+ 'professor\'s method and the improved one read the same grades differently.' })),
		h('div', { class: 'controls' },
			segmented('Method', METHODS, method, (value) => {
				method = value;
				remember.set('recsys.method', value);
				note.textContent = METHOD_NOTES[value];
				load();
			}),
			select('Films', [5, 10, 20], k, (value) => {
				k = value;
				remember.set('recsys.k', String(value));
				load();
			}),
			diversity,
			note),
		list);

	async function load() {
		const mine = ++sequence;
		list.replaceChildren(loading('Ranking 9,742 films'));
		try {
			const result = await api.recommendations(method, k, lambda);
			if (mine === sequence) {
				list.replaceChildren(programme(result, method, lambda));
			}
		} catch (error) {
			if (mine === sequence) {
				list.replaceChildren(empty('Could not rank films', error.message));
				showError(error, 'Recommendations failed');
			}
		}
	}

	load();
	return () => {
		sequence++;
	};
}

/**
 * The diversity slider runs from 0 (ranked by fit only) to 1 (as varied as possible); MMR's λ is its complement, so
 * λ = 1 means no re-ranking.
 */
function diversityField(lambda, onChange) {
	const value = h('output', { class: 'slider__value', for: 'diversity' });
	const describe = (l) => {
		const text = l >= 1 ? 'off, ranked by fit only' : l >= 0.7 ? 'a little variety' : l >= 0.4 ? 'more variety'
			: 'lots of variety';
		value.textContent = `λ = ${l.toFixed(1)} · ${text}`;
	};
	const slider = h('input', {
		type: 'range',
		id: 'diversity',
		class: 'slider',
		min: '0',
		max: '1',
		step: '0.1',
		value: String(1 - lambda),
		'aria-describedby': 'diversity-value',
		oninput: () => describe(clampLambda(1 - Number(slider.value))),
		onchange: () => onChange(clampLambda(1 - Number(slider.value))),
	});
	value.id = 'diversity-value';
	describe(lambda);
	return h('div', { class: 'field' }, h('label', { class: 'field__label', for: 'diversity', text: 'Diversity' }), slider,
		value);
}

function clampLambda(value) {
	return Number.isFinite(value) ? Math.round(Math.min(1, Math.max(0, value)) * 10) / 10 : 1;
}

function programme(result, method, lambda) {
	const fallback = result.fallback === 'POPULARITY';
	const graded = store.grades.size;
	return h('div', {},
		fallback && h('div', { class: 'banner', role: 'note' },
			h('strong', { text: 'Intermission: not enough to go on yet' }),
			h('span', { text: `You have graded ${graded} of the ${MIN_GRADES} films needed for a personal list. Until `
				+ 'then, this is simply what MovieLens viewers rated most.' }),
			h('a', { href: '#grade', text: 'Grade a few films →' })),
		lambda < 1 && result.items.length > 1 && h('p', { class: 'mmr-note' },
			h('strong', { text: `Re-ranked for variety (MMR, λ = ${lambda.toFixed(1)}). ` }),
			'Each pick is penalized for sharing genres with the films above it, so scores are no longer in order: '
			+ 'they still show how well each film fits your taste.'),
		result.items.length === 0
			? empty('Nothing left to recommend', 'You have graded every film that qualifies.')
			: h('ol', { class: 'programme', 'aria-label': `Recommendations, ${method} method` },
				result.items.map((item, i) => reveal(entry(item, method, fallback), i))),
		h('div', { class: 'after' },
			h('a', { class: 'button', href: '#math' }, 'Show how these scores were computed →'),
			h('a', { class: 'button', href: '#grade' }, 'Grade more films')));
}

function entry(item, method, fallback) {
	const explanation = item.explanation;
	const features = explanation?.topFeatures ?? [];
	const matching = new Set(features.filter((f) => f.kind === 'genre').map((f) => f.feature));
	const because = explanation?.becauseYouRated ?? [];
	return h('li', { class: 'entry', 'data-movie-id': item.movieId },
		h('span', { class: 'entry__rank', 'aria-hidden': 'true', text: item.rank }),
		h('div', { class: 'entry__body' },
			h('h3', { class: 'entry__title' }, item.title, item.year && h('span', { class: 'film__year', text: item.year })),
			item.genres.length > 0 && genreChips(item.genres, matching),
			explanation && h('p', { class: 'entry__sentence', text: explanation.sentence }),
			features.length > 0 && h('ul', { class: 'chips', 'aria-label': 'What counted most' },
				features.map((f) => h('li', { class: f.kind === 'tag' ? 'chip chip--tag chip--match' : 'chip chip--match' },
					f.feature, h('span', { class: 'chip__value', text: signed(f.contribution) })))),
			because.length > 0 && h('p', { class: 'entry__because' }, 'Because you rated ',
				because.flatMap((b, i) => [i > 0 ? ' and ' : '', h('b', { text: b.title }), ` (${grade(b.grade)}/10)`]))),
		h('div', { class: 'entry__score' },
			h('span', { class: item.score < 0 ? 'entry__score-value neg' : 'entry__score-value', text: fixed(item.score) }),
			h('span', { class: 'entry__score-label', text: fallback ? 'popularity' : 'score' }),
			method === 'lecture' && !fallback && h('span', { class: 'entry__score-x10', text: `×10 = ${fixed(item.score * 10, 2)}` })));
}
