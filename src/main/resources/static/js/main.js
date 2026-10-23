// Hash router for the four views. Each view renders into <main> and may return a cleanup function.

import { api } from './api.js';
import { renderExample } from './example.js';
import { renderGrade } from './grade.js';
import { renderMath } from './math.js';
import { renderRecommendations } from './recommendations.js';
import { store } from './store.js';
import { showError } from './ui.js';

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
	cleanup = views[name](main) || null;
	main.focus({ preventScroll: true });
}

function updateBadge() {
	const badge = document.getElementById('grade-count');
	badge.textContent = String(store.grades.size);
	badge.hidden = store.grades.size === 0;
}

store.subscribe(updateBadge);
window.addEventListener('hashchange', route);
store.load().catch((error) => showError(error, 'Could not load your grades'));
route();

// For the console and the end-to-end check: the same client the views use.
window.recsys = { api, store };
