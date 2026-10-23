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
	// Ticket 08 adds Spring Security with a cookie-based CSRF token; sending it now costs nothing.
	const cookie = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='));
	return cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : null;
}

async function request(path, { method = 'GET', body } = {}) {
	const headers = { Accept: 'application/json' };
	if (body !== undefined) {
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
		body: body === undefined ? undefined : JSON.stringify(body),
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
	recommendations: (method, k) => request(`/api/me/recommendations?${query({ method, k })}`),
	math: (method, k) => request(`/api/me/recommendations/math?${query({ method, k })}`),
	lectureExample: () => request('/api/demo/lecture-example'),
};
