// Thin client for the REST API. Failed requests throw ApiError carrying the RFC 9457 problem detail, so every view
// can show the server's own readable message.

export class ApiError extends Error {
	constructor(problem, status) {
		super(problem.detail || problem.title || `Request failed (HTTP ${status})`);
		this.problem = problem;
		this.status = status;
	}
}

function xsrfToken() {
	// Spring Security sets this cookie on every response; writes must echo it back as the X-XSRF-TOKEN header.
	const cookie = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='));
	return cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : null;
}

async function request(path, { method = 'GET', body, form } = {}) {
	const headers = { Accept: 'application/json' };
	if (form !== undefined) {
		headers['Content-Type'] = 'application/x-www-form-urlencoded';
	} else if (body !== undefined) {
		headers['Content-Type'] = 'application/json';
	}
	const token = xsrfToken();
	if (token && method !== 'GET') {
		headers['X-XSRF-TOKEN'] = token;
	}
	const response = await fetch(path, {
		method,
		headers,
		credentials: 'same-origin',
		body: form !== undefined ? new URLSearchParams(form) : body === undefined ? undefined : JSON.stringify(body),
	});
	if (response.status === 204) {
		return null;
	}
	const type = response.headers.get('Content-Type') || '';
	const data = type.includes('json') ? await response.json() : null;
	if (!response.ok) {
		throw new ApiError(data || {}, response.status);
	}
	return data;
}

const query = (params) => new URLSearchParams(params).toString();

export const api = {
	searchMovies: (q, page = 0, size = 20) => request(`/api/movies?${query({ q, page, size })}`),
	movie: (id) => request(`/api/movies/${id}`),
	genres: () => request('/api/genres'),
	grades: () => request('/api/me/grades'),
	putGrade: (movieId, score) => request(`/api/me/grades/${movieId}`, { method: 'PUT', body: { score } }),
	deleteGrade: (movieId) => request(`/api/me/grades/${movieId}`, { method: 'DELETE' }),
	recommendations: (method, k, lambda = 1) => request(`/api/me/recommendations?${query({ method, k, lambda })}`),
	starterSet: (size = 15) => request(`/api/onboarding/starter-set?${query({ size })}`),
	math: (method, k) => request(`/api/me/recommendations/math?${query({ method, k })}`),
	lectureExample: () => request('/api/demo/lecture-example'),
	me: () => request('/api/me'),
	register: (username, password) => request('/api/auth/register', { method: 'POST', body: { username, password } }),
	signIn: (username, password) => request('/login', { method: 'POST', form: { username, password } }),
	signOut: () => request('/logout', { method: 'POST' }),
};
