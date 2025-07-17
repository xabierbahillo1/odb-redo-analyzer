window.addEventListener('DOMContentLoaded', () => {
    const dynamicDateSpan = document.querySelector('.dynamic-date');
    if (!dynamicDateSpan) return;

    const dynamicDate = dynamicDateSpan.textContent.trim();
    if (!dynamicDate) return;

    const table = document.getElementById('redoTable');
    if (!table) return;

    const rows = table.querySelectorAll('tbody tr');

    rows.forEach(row => {
        const lastUpdatedCell = row.cells[6];
        if (!lastUpdatedCell) return;

        const originalText = lastUpdatedCell.textContent.trim();
        const newText = originalText.replace(/^\d{4}-\d{2}-\d{2}/, dynamicDate);
        lastUpdatedCell.textContent = newText;
    });
});
