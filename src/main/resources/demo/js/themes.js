document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('themeToggleBtn');
    const body = document.body;

    // Aplicar modo guardado en localStorage
    if (localStorage.getItem('theme') === 'dark') {
        body.classList.add('dark-mode');
        btn.textContent = '☀️'; // icono sol para modo oscuro activo
    } else {
        btn.textContent = '🌙'; // icono luna para modo claro activo
    }

    btn.addEventListener('click', () => {
        if (body.classList.contains('dark-mode')) {
            body.classList.remove('dark-mode');
            btn.textContent = '🌙';
            localStorage.setItem('theme', 'light');
        } else {
            body.classList.add('dark-mode');
            btn.textContent = '☀️';
            localStorage.setItem('theme', 'dark');
        }
    });
});
