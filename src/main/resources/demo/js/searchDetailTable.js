document.addEventListener('DOMContentLoaded', () => {
  const table = document.getElementById('redoTable');
  const tbody = table.tBodies[0];

  const searchInput = document.getElementById('searchInput');
  searchInput.addEventListener('input', function() {
    const filter = this.value.toLowerCase();

    for (const row of tbody.rows) {
      const text = row.textContent.toLowerCase();
      row.style.display = text.includes(filter) ? '' : 'none';
    }
  });
});
