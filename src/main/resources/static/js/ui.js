// Small DOM helpers. Text always goes through textContent, never innerHTML.

export function h(tag, props = {}, ...children) {
	const el = document.createElement(tag);
	for (const [key, value] of Object.entries(props ?? {})) {
		if (value == null || value === false) {
			continue;
		}
		if (key === 'class') {
			el.className = value;
		} else if (key === 'text') {
			el.textContent = value;
		} else if (key === 'style') {
			for (const [name, v] of Object.entries(value)) {
				if (name.startsWith('--')) {
					el.style.setProperty(name, v);
				} else {
					el.style[name] = v;
				}
			}
		} else if (key.startsWith('on')) {
			el.addEventListener(key.slice(2).toLowerCase(), value);
		} else {
			el.setAttribute(key, value === true ? '' : value);
		}
	}
	for (const child of children.flat(Infinity)) {
		if (child == null || child === false) {
			continue;
		}
		el.append(child instanceof Node ? child : document.createTextNode(String(child)));
	}
	return el;
}

/** Fixed decimals, never "-0.0000". */
export function fixed(value, digits = 4) {
	const text = Number(value).toFixed(digits);
	return /^-0\.?0*$/.test(text) ? text.slice(1) : text;
}

/** 10 → "10", 4.5 → "4.5". */
export function grade(value) {
	return Number.isInteger(value) ? String(value) : value.toFixed(1);
}

export function signed(value, digits = 3) {
	const text = fixed(value, digits);
	return value > 0 && !/^0\.?0*$/.test(text) ? `+${text}` : text;
}

export function genreChips(genres, matches = new Set()) {
	return h('ul', { class: 'chips', 'aria-label': 'Genres' },
		genres.map((genre) => h('li', { class: matches.has(genre) ? 'chip chip--match' : 'chip', text: genre })));
}

export function reveal(el, index) {
	el.classList.add('reveal');
	el.style.setProperty('--i', String(Math.min(index, 14)));
	return el;
}

export function loading(text = 'Loading') {
	return h('p', { class: 'loading', text });
}

export function empty(title, text, action) {
	return h('div', { class: 'empty' }, h('h3', { text: title }), h('p', { text }), action);
}

const MAX_TOASTS = 2;

export function toast(message, kind = 'info', title) {
	const box = document.getElementById('toasts');
	while (box.children.length >= MAX_TOASTS) {
		box.firstElementChild.remove();
	}
	const el = h('div', { class: `toast toast--${kind}` }, title && h('span', { class: 'toast__title', text: title }),
		message);
	box.append(el);
	setTimeout(() => el.remove(), kind === 'error' ? 6000 : 3200);
}

export function showError(error, title = 'Something went wrong') {
	toast(error.message || String(error), 'error', title);
}

/** Reads and writes a remembered choice; storage can be unavailable (private windows), so it never throws. */
export const remember = {
	get(key, fallback) {
		try {
			return localStorage.getItem(key) ?? fallback;
		} catch {
			return fallback;
		}
	},
	set(key, value) {
		try {
			localStorage.setItem(key, value);
		} catch {
			// Not remembered; the default is used next time.
		}
	},
};

/** A two-option segmented control; calls onChange with the chosen value. */
export function segmented(label, options, current, onChange) {
	const buttons = options.map(([value, text]) => h('button', {
		type: 'button',
		'aria-pressed': String(value === current),
		onclick: () => {
			for (const b of buttons) {
				b.setAttribute('aria-pressed', String(b === buttonFor(value)));
			}
			onChange(value);
		},
		'data-value': value,
		text,
	}));
	const buttonFor = (value) => buttons.find((b) => b.dataset.value === value);
	return h('div', { class: 'field' }, h('span', { class: 'field__label', text: label }),
		h('div', { class: 'segmented', role: 'group', 'aria-label': label }, buttons));
}

export function select(label, values, current, onChange) {
	const id = `select-${label.replace(/\W+/g, '-').toLowerCase()}`;
	const el = h('select', { class: 'select', id, onchange: (e) => onChange(Number(e.target.value)) },
		values.map((v) => h('option', { value: v, selected: v === current, text: v })));
	return h('div', { class: 'field' }, h('label', { class: 'field__label', for: id, text: label }), el);
}
