document.addEventListener('DOMContentLoaded', () => {
  const table = document.getElementById('redoTable');
  const tbody = table.tBodies[0];
  const searchInput = document.getElementById('searchInput');
  const exportCsvBtn = document.getElementById('exportCsvBtn');

  searchInput.addEventListener('input', function() {
    const filter = this.value.toLowerCase();

    for (const row of tbody.rows) {
      const text = row.textContent.toLowerCase();
      row.style.display = text.includes(filter) ? '' : 'none';
    }
  });

  exportCsvBtn.addEventListener('click', () => {
    let csvContent = "";
    const rows = table.querySelectorAll('thead tr, tbody tr');

    rows.forEach(row => {
      if (row.style.display === 'none') return; // Ignore filtered rows

      const cols = row.querySelectorAll('th, td');
      const rowData = [];
      cols.forEach(col => {
        let text;
        // Check if the column has a data-bytes attribute to use its value
        if (col.hasAttribute('data-bytes')) {
          text = col.getAttribute('data-bytes');
        } else {
          text = col.textContent.trim();
        }
        // Escapar comillas
        text = text.replace(/"/g, '""');
        rowData.push(`"${text}"`);
      });

      // Depending on the language of navigator, use the common separator
      const userLang = navigator.language || navigator.userLanguage;
      const usesSemicolon = ['es', 'fr', 'de', 'pt'].some(lang => userLang.startsWith(lang));
      const separator = usesSemicolon ? ';' : ',';

      csvContent += rowData.join(separator) + "\r\n";
    });

    // Create a Blob and download it
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `redo_usage_${new Date().toISOString().slice(0,10)}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  });
});
