// The current user's grades, shared by every view. Views subscribe and unsubscribe when they leave.

import { api } from './api.js';

const listeners = new Set();

function emit() {
	for (const listener of listeners) {
		listener(store);
	}
}

export const store = {
	/** movieId → { movieId, title, year, score, updatedAt } */
	grades: new Map(),

	async load() {
		const list = await api.grades();
		this.grades = new Map(list.map((g) => [g.movieId, g]));
		emit();
	},

	async grade(movieId, score) {
		const saved = await api.putGrade(movieId, score);
		this.grades.delete(movieId);
		this.grades = new Map([[movieId, saved], ...this.grades]);
		emit();
		return saved;
	},

	async remove(movieId) {
		await api.deleteGrade(movieId);
		this.grades.delete(movieId);
		emit();
	},

	subscribe(listener) {
		listeners.add(listener);
		return () => listeners.delete(listener);
	},
};
