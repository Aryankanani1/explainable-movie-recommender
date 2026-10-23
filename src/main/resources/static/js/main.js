// Hash router for the four views. Each view renders into <main> and may return a cleanup function.

import { api } from './api.js';
import { renderExample } from './example.js';
import { renderGrade } from './grade.js';
import { renderMath } from './math.js';
import { renderRecommendations } from './recommendations.js';
import { store } from './store.js';
import { empty, h, showError } from './ui.js';

const views = {
	grade: renderGrade,
	recommendations: renderRecommendations,
	math: renderMath,
	example: renderExample,
};

const titles = {
	grade: 'Grade',
	recommendations: 'Recommendations',
	math: 'Show the math',
	example: 'Lecture example',
};

let cleanup = null;

function route() {
	const name = views[location.hash.slice(1)] ? location.hash.slice(1) : 'grade';
	for (const tab of document.querySelectorAll('.tab')) {
		if (tab.dataset.view === name) {
			tab.setAttribute('aria-current', 'page');
		} else {
			tab.removeAttribute('aria-current');
		}
	}
	document.title = `${titles[name]} · The Recommender`;
	if (cleanup) {
		cleanup();
	}
	const main = document.getElementById('view');
	main.replaceChildren();
	cleanup = null;
	// Grades are personal; only the lecture example works signed out.
	store.ready.catch(() => {}).then(() => {
		if (location.hash.slice(1) !== name && !(name === 'grade' && !views[location.hash.slice(1)])) {
			return;
		}
		if (!store.user && name !== 'example') {
			main.replaceChildren(signInPrompt(name));
			return;
		}
		cleanup = views[name](main) || null;
	});
	main.focus({ preventScroll: true });
}

function signInPrompt(name) {
	return h('div', { class: 'view-head' },
		h('p', { class: 'eyebrow', text: 'Signed out' }),
		empty('Sign in to grade films', 'Your grades and recommendations are kept per account. Sign in, or create '
			+ 'one in a few seconds. The lecture example works without signing in.',
		h('div', { class: 'after', style: { justifyContent: 'center' } },
			h('a', { class: 'button button--primary', href: `/login.html?next=${encodeURIComponent(`/#${name}`)}` }, 'Sign in'),
			h('a', { class: 'button', href: '#example' }, 'See the lecture example'))));
}

function renderSession() {
	const box = document.getElementById('session');
	if (!box) {
		return;
	}
	box.replaceChildren(store.user
		? h('span', {}, 'Signed in as ', h('b', { text: store.user }), ' · ',
			h('button', { class: 'linklike', type: 'button', onclick: signOut, text: 'Sign out' }))
		: h('a', { href: '/login.html', text: 'Sign in' }));
}

async function signOut() {
	try {
		await api.signOut();
	} catch (error) {
		showError(error, 'Could not sign out');
		return;
	}
	location.href = '/login.html';
}

function updateBadge() {
	const badge = document.getElementById('grade-count');
	badge.textContent = String(store.grades.size);
	badge.hidden = store.grades.size === 0;
}

store.subscribe(updateBadge);
store.subscribe(renderSession);
window.addEventListener('hashchange', route);
store.load().catch((error) => showError(error, 'Could not load your grades'));
route();

// For the console and the end-to-end check: the same client the views use.
window.recsys = { api, store };
