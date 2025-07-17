document.addEventListener('DOMContentLoaded', () => {
    function isNumeric(n) {
        return /^-?\d+(\.\d+)?$/.test(n);
    }

    function isDateString(s) {
        return /^\d{4}-\d{2}-\d{2}( \d{2}:\d{2}:\d{2}(\.\d+)?)?$/.test(s);
    }

    const tables = document.querySelectorAll('table');

    tables.forEach(table => {
        const headers = table.querySelectorAll('thead th');
        let sortDirection = Array(headers.length).fill(null);

        headers.forEach((header, index) => {
            header.addEventListener('click', () => {
                const tbody = table.querySelector('tbody');
                if (!tbody) return;

                let rows = Array.from(tbody.querySelectorAll('tr')).filter(row => !row.querySelector('td[colspan]'));

                const firstCell = rows[0]?.querySelectorAll('td')[index];
                let useDataBytes = false;
                let isNum = false;
                let isDate = false;

                if (firstCell) {
                    const text = firstCell.textContent.trim().replaceAll(',', '');
                    useDataBytes = firstCell.hasAttribute('data-bytes'); // Check if the cell has a data-bytes attribute to use its value for sorting
                    isDate = isDateString(text);
                    if (!isDate) {
                        isNum = isNumeric(text);
                    }
                }

                if (sortDirection[index] === null || sortDirection[index] === false) {
                    sortDirection.fill(null);
                    sortDirection[index] = true;
                } else {
                    sortDirection[index] = false;
                }

                rows.sort((a, b) => {
                    const aCell = a.querySelectorAll('td')[index];
                    const bCell = b.querySelectorAll('td')[index];

                    let aVal = aCell.textContent.trim();
                    let bVal = bCell.textContent.trim();

                    if (useDataBytes) {
                        aVal = parseFloat(aCell.getAttribute('data-bytes'));
                        bVal = parseFloat(bCell.getAttribute('data-bytes'));
                    } else if (isDate) {
                        aVal = new Date(aVal);
                        bVal = new Date(bVal);
                    } else if (isNum) {
                        aVal = parseFloat(aVal.replaceAll(',', ''));
                        bVal = parseFloat(bVal.replaceAll(',', ''));
                    }

                    if (aVal < bVal) return sortDirection[index] ? -1 : 1;
                    if (aVal > bVal) return sortDirection[index] ? 1 : -1;
                    return 0;
                });

                tbody.innerHTML = '';
                rows.forEach(row => tbody.appendChild(row));

                headers.forEach(h => h.classList.remove('sorted-asc', 'sorted-desc'));
                header.classList.add(sortDirection[index] ? 'sorted-asc' : 'sorted-desc');
            });
        });
    });
});
