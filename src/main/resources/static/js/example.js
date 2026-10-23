// View 4 · Lecture example: the professor's worked example, computed live by the engine.

import { api } from './api.js';
import { lectureTables } from './math.js';
import { empty, fixed, genreChips, grade, h, loading, reveal, showError } from './ui.js';

export function renderExample(root) {
	const body = h('div', {}, loading('Computing the slide example'));
	root.append(
		h('section', { class: 'view-head' },
			h('p', { class: 'eyebrow', text: 'From the lecture' }),
			h('h2', { class: 'view-title' }, 'The worked example, ', h('em', { text: 'live' })),
			h('p', { class: 'lede', text: 'One viewer grades three super-hero films. Nothing on this page is typed in: '
				+ 'the engine computes it, the same code that ranks the 9,742 MovieLens films.' })),
		body);

	let active = true;
	api.lectureExample().then((example) => {
		if (!active) {
			return;
		}
		const { math, recommendations } = example;
		body.replaceChildren(
			h('div', { class: 'stubs', 'aria-label': 'The grades' },
				math.lecture.rows.map((r, i) => reveal(h('div', { class: 'stub' },
					h('span', { class: r.grade < 5 ? 'stub__grade low' : 'stub__grade', text: grade(r.grade) }),
					h('span', {}, h('span', { class: 'stub__title', text: r.title }), h('br'),
						h('span', { class: 'stub__genres', text: math.columns.filter((_, j) => r.x[j]).join(' · ') }))), i))),
			lectureTables(math),
			h('h3', { class: 'section-title' }, h('span', { class: 'section-title__step', text: '04' }), 'Final results'),
			h('ol', { class: 'programme', 'aria-label': 'Lecture example results' },
				recommendations.items.map((item, i) => reveal(h('li', { class: 'entry', 'data-movie-id': item.movieId },
					h('span', { class: 'entry__rank', 'aria-hidden': 'true', text: item.rank }),
					h('div', { class: 'entry__body' },
						h('h3', { class: 'entry__title' }, item.title, h('span', { class: 'film__year', text: item.year })),
						genreChips(item.genres),
						h('p', { class: 'entry__sentence', text: item.explanation.sentence })),
					h('div', { class: 'entry__score' },
						h('span', { class: 'entry__score-value', text: fixed(item.score) }),
						h('span', { class: 'entry__score-label', text: 'score' }),
						h('span', { class: 'entry__score-x10', text: `×10 = ${fixed(item.score * 10, 2)}` }))), i))),
			h('div', { class: 'callout' },
				h('p', {}, h('strong', { text: 'Why Lady Bird scores 0. ' }),
					'It is a Drama, and none of the graded films is, so Drama has no weight in the profile. However '
					+ 'good it is, this method can never recommend it: over-specialization, or the filter bubble.')),
			h('div', { class: 'after' }, h('a', { class: 'button button--primary', href: '#grade' }, 'Try it with your own grades →')));
	}).catch((error) => {
		if (active) {
			body.replaceChildren(empty('Could not compute the example', error.message));
			showError(error);
		}
	});
	return () => {
		active = false;
	};
}
