// View 2 · Recommendations: the ranked programme, with why each film is on it.

import { api } from './api.js';
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
	let sequence = 0;

	const note = h('p', { class: 'method-note', text: METHOD_NOTES[method] });
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
			note),
		list);

	async function load() {
		const mine = ++sequence;
		list.replaceChildren(loading('Ranking 9,742 films'));
		try {
			const result = await api.recommendations(method, k);
			if (mine === sequence) {
				list.replaceChildren(programme(result, method));
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

function programme(result, method) {
	const fallback = result.fallback === 'POPULARITY';
	return h('div', {},
		fallback && h('div', { class: 'banner', role: 'note' },
			h('strong', { text: 'Intermission: not enough to go on yet' }),
			h('span', { text: 'Grade a few films first (five is a good start). Until then, this is simply what '
				+ 'MovieLens viewers rated most.' })),
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
