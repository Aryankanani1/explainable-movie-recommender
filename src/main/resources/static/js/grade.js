// View 1 · Grade: search MovieLens titles, grade 1–10, keep a ledger of what you graded.

import { api } from './api.js';
import { MIN_GRADES, store } from './store.js';
import { empty, genreChips, grade as gradeText, h, loading, reveal, showError, toast } from './ui.js';

const PAGE_SIZE = 12;
const LOW = 5;

export function renderGrade(root) {
	let query = '';
	let page = 0;
	let sequence = 0;
	let timer = null;

	const input = h('input', {
		class: 'search',
		type: 'search',
		id: 'movie-search',
		placeholder: 'Search a title, e.g. Matrix, Toy Story, Notebook',
		autocomplete: 'off',
		'aria-label': 'Search movies by title',
		oninput: () => {
			clearTimeout(timer);
			timer = setTimeout(() => search(input.value.trim(), 0), 220);
		},
	});
	const results = h('div', { class: 'results', 'aria-live': 'polite' });
	const meta = h('p', { class: 'results__meta' });
	const more = h('button', { class: 'button', type: 'button', hidden: true, onclick: () => search(query, page + 1) },
		'Show more');
	const ledger = h('aside', { class: 'ledger', 'aria-label': 'Your grades' });

	root.append(
		h('section', { class: 'view-head' },
			h('p', { class: 'eyebrow', text: 'Step one' }),
			h('h2', { class: 'view-title' }, 'Grade the films ', h('em', { text: 'you know' })),
			h('p', { class: 'lede', text: '1 means you disliked it, 10 means you loved it. Grade at least five, and include '
				+ 'one you disliked: the improved method uses low grades to steer away from a genre.' })),
		h('div', { class: 'grade-layout' },
			h('section', { 'aria-label': 'Search results' }, input, meta, results, h('div', { class: 'after' }, more)),
			ledger));

	async function search(text, nextPage) {
		const mine = ++sequence;
		query = text;
		page = nextPage;
		if (nextPage === 0) {
			results.replaceChildren(loading('Searching'));
		}
		try {
			if (!text && nextPage === 0 && store.grades.size < MIN_GRADES) {
				await showStarterSet(mine);
				return;
			}
			const result = await api.searchMovies(text, nextPage, PAGE_SIZE);
			if (mine !== sequence) {
				return;
			}
			if (nextPage === 0) {
				results.replaceChildren();
				cards.clear();
			}
			result.content.forEach((movie, i) => results.append(reveal(filmCard(movie), i)));
			const total = result.page.totalElements;
			meta.textContent = text
				? `${total.toLocaleString()} ${total === 1 ? 'film matches' : 'films match'} "${text}", most-rated first.`
				: 'The most-rated films on MovieLens. Search for anything else above.';
			more.hidden = result.page.number + 1 >= result.page.totalPages;
			if (total === 0) {
				results.replaceChildren(empty('No film by that name', 'Try part of the title, e.g. "lord of the" or "bourne".'));
			}
		} catch (error) {
			if (mine === sequence) {
				results.replaceChildren();
				showError(error, 'Search failed');
			}
		}
	}

	/** New users: well-known films spread across genres, so the first grades say a lot about taste. */
	async function showStarterSet(mine) {
		const starter = await api.starterSet(15);
		if (mine !== sequence) {
			return;
		}
		results.replaceChildren();
		cards.clear();
		starter.forEach((movie, i) => results.append(reveal(filmCard(movie), i)));
		const genres = new Set(starter.flatMap((movie) => movie.genres));
		meta.textContent = `New here? Start with these ${starter.length} well-known films, picked to cover `
			+ `${genres.size} genres. Grade the ones you remember, or search for any other title.`;
		more.hidden = true;
	}

	function filmCard(movie) {
		const buttons = [];
		const card = h('article', { class: 'film', 'data-movie-id': movie.id },
			h('div', { class: 'film__head' },
				h('h3', { class: 'film__title' }, movie.title, ' ', movie.year && h('span', { class: 'film__year', text: movie.year })),
				h('span', { class: 'film__stats',
					text: movie.ratingCount
						? `${movie.ratingCount} ratings · mean ${movie.ratingMean.toFixed(2)}★`
						: 'not rated on MovieLens' })),
			movie.genres.length ? genreChips(movie.genres) : h('p', { class: 'film__stats', text: 'no genres listed' }),
			h('div', { class: 'grade-row' },
				h('span', { class: 'grade-row__label', text: 'Your grade' }),
				h('div', { class: 'grades', role: 'group', 'aria-label': `Grade ${movie.title} from 1 to 10` },
					Array.from({ length: 10 }, (_, i) => {
						const score = i + 1;
						const button = h('button', {
							type: 'button',
							class: score < LOW ? 'grade-button low' : 'grade-button',
							'aria-label': `${score} out of 10`,
							'data-score': score,
							onclick: () => setGrade(movie, score),
							text: score,
						});
						buttons.push(button);
						return button;
					}))));
		const paint = () => {
			const current = store.grades.get(movie.id)?.score;
			card.classList.toggle('film--graded', current != null);
			for (const button of buttons) {
				button.setAttribute('aria-pressed', String(Number(button.dataset.score) === current));
			}
		};
		paint();
		cards.set(movie.id, paint);
		return card;
	}

	async function setGrade(movie, score) {
		try {
			await store.grade(movie.id, score);
			toast(`${movie.title}: ${score}/10`, 'info', 'Graded');
		} catch (error) {
			showError(error, 'Could not save that grade');
		}
	}

	const cards = new Map();

	function renderLedger() {
		for (const paint of cards.values()) {
			paint();
		}
		const grades = [...store.grades.values()];
		const ready = grades.length >= 5;
		ledger.replaceChildren(
			h('h3', { class: 'ledger__title' }, 'Your grades', h('span', { class: 'ledger__count', text: grades.length })),
			grades.length === 0
				? h('p', { class: 'ledger__hint', text: 'Nothing graded yet. Pick a few films you remember well.' })
				: h('ul', { class: 'ledger__list' }, grades.map((g) => h('li', { class: 'ledger__item' },
					h('span', { class: g.score < LOW ? 'ledger__score low' : 'ledger__score', text: gradeText(g.score) }),
					h('span', { class: 'ledger__name', title: g.title }, g.title, g.year ? ` (${g.year})` : ''),
					h('button', {
						class: 'ledger__remove',
						type: 'button',
						'aria-label': `Remove the grade for ${g.title}`,
						onclick: () => store.remove(g.movieId).catch((e) => showError(e, 'Could not remove that grade')),
						text: '×',
					})))),
			h('p', { class: 'ledger__hint', text: ready
				? 'Enough to go on. See what the two methods make of your taste.'
				: `${5 - grades.length} more for a reliable profile; until then you'll see popular films.` }),
			h('a', { class: ready ? 'button button--primary' : 'button', href: '#recommendations' }, 'See recommendations →'));
	}

	const unsubscribe = store.subscribe(renderLedger);
	renderLedger();
	(store.ready ?? Promise.resolve()).finally(() => search('', 0));
	return () => {
		unsubscribe();
		clearTimeout(timer);
		sequence++;
	};
}
