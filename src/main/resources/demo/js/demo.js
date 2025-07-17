document.addEventListener('DOMContentLoaded', () => {
  // Rellenar fechas en tabla
  document.querySelectorAll('.dynamic-date').forEach(cell => {
    const offset = parseInt(cell.dataset.offset, 10);
    const date = new Date();
    date.setDate(date.getDate() - (7 - offset));
    cell.textContent = date.toISOString().split('T')[0];
  });

  // Calcular fechas y asignar inputs
  const calculateDateByOffset = (offset) => {
    const d = new Date();
    d.setDate(d.getDate() - (7 - offset));
    return d.toISOString().split('T')[0];
  };

  const fromDateInput = document.getElementById('fromDate');
  const toDateInput = document.getElementById('toDate');

  if (fromDateInput && toDateInput) {
    fromDateInput.value = calculateDateByOffset(1);
    toDateInput.value = calculateDateByOffset(6);
  }

  // Modal y boton cerrar
  const modal = document.getElementById('demoModal');
  const closeBtn = document.getElementById('closeDemoModal');
  const demoSeenKey = 'demoNoticeSeen';

  // Usar clase para mostrar/ocultar modal en vez de hidden
  const showModal = () => modal.classList.add('show');
  const hideModal = () => modal.classList.remove('show');

  if (!localStorage.getItem(demoSeenKey)) {
    if (modal) {
      showModal();
    }
  }

  if (closeBtn) {
    closeBtn.addEventListener('click', () => {
      if (modal) {
        hideModal();
      }
      localStorage.setItem(demoSeenKey, 'true');
    });
  }
});
