document.addEventListener('DOMContentLoaded', () => {
    const fromDateInput = document.getElementById('fromDate');
    const toDateInput = document.getElementById('toDate');

    function trySubmitForm() {
        // Check if both date inputs have values before submitting the form
        if (fromDateInput.value && toDateInput.value) {
            fromDateInput.form.submit();
        }
    }

    fromDateInput.addEventListener('change', trySubmitForm);
    toDateInput.addEventListener('change', trySubmitForm);
});
