// Sign-in and registration page. Spring Security's POST /login takes a form; registration is JSON.

import { ApiError, api } from './api.js';

const next = new URLSearchParams(location.search).get('next') || '/#grade';

function showError(form, message) {
	const box = form.querySelector('.auth__error');
	box.textContent = message;
	box.hidden = false;
}

async function signIn(username, password) {
	await api.signIn(username, password);
	// Only same-site paths, never an arbitrary URL from the query string.
	location.href = next.startsWith('/') && !next.startsWith('//') ? next : '/#grade';
}

document.getElementById('sign-in').addEventListener('submit', async (event) => {
	event.preventDefault();
	const form = event.currentTarget;
	const data = new FormData(form);
	try {
		await signIn(String(data.get('username')).trim(), String(data.get('password')));
	} catch (error) {
		showError(form, error instanceof ApiError ? error.message : 'Could not reach the server.');
	}
});

document.getElementById('register').addEventListener('submit', async (event) => {
	event.preventDefault();
	const form = event.currentTarget;
	const data = new FormData(form);
	const username = String(data.get('username')).trim();
	const password = String(data.get('password'));
	try {
		await api.register(username, password);
		await signIn(username, password);
	} catch (error) {
		showError(form, error instanceof ApiError ? error.message : 'Could not reach the server.');
	}
});

// Any request makes the server set the XSRF-TOKEN cookie that the sign-in POST must echo back.
api.genres().catch(() => {});
