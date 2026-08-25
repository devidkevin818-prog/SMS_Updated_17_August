function copyPassword(inputId, button) {
    const input = document.getElementById(inputId);
    if (!input) return;
    const markCopied = () => {
        const original = button.innerHTML;
        button.innerHTML = '<i class="bi bi-check-lg me-1"></i>Copied';
        window.setTimeout(() => { button.innerHTML = original; }, 1800);
    };
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(input.value).then(markCopied);
    } else {
        input.select();
        document.execCommand('copy');
        input.setSelectionRange(0, 0);
        markCopied();
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('passwordResetForm');
    if (!form) return;
    const generatedPanel = document.getElementById('generatedPasswordPanel');
    const manualPanel = document.getElementById('manualPasswordPanel');
    const generatedPassword = document.getElementById('generatedPassword');
    const newPassword = document.getElementById('newPassword');
    const confirmPassword = document.getElementById('confirmPassword');
    const openConfirmation = document.getElementById('openResetConfirmation');
    const confirmReset = document.getElementById('confirmPasswordReset');
    const modalElement = document.getElementById('resetConfirmationModal');
    const modal = bootstrap.Modal.getOrCreateInstance(modalElement);

    function selectedMethod() {
        return form.querySelector('input[name="resetMethod"]:checked')?.value || 'generate';
    }

    function updateMethodPanels() {
        const manual = selectedMethod() === 'manual';
        generatedPanel.hidden = manual;
        manualPanel.hidden = !manual;
        generatedPassword.required = !manual;
        newPassword.required = manual;
        confirmPassword.required = manual;
        updateSubmitAvailability();
    }

    function updateSubmitAvailability() {
        const manualPasswordsMatch = selectedMethod() !== 'manual'
            || newPassword.value === confirmPassword.value;
        openConfirmation.disabled = !form.checkValidity() || !manualPasswordsMatch;
    }

    form.querySelectorAll('input[name="resetMethod"]').forEach(option =>
        option.addEventListener('change', updateMethodPanels));
    document.getElementById('copyGeneratedPassword')?.addEventListener('click', event =>
        copyPassword('generatedPassword', event.currentTarget));

    openConfirmation.addEventListener('click', () => {
        if (!form.reportValidity()) return;
        if (selectedMethod() === 'manual' && newPassword.value !== confirmPassword.value) {
            confirmPassword.setCustomValidity('Password confirmation does not match.');
            confirmPassword.reportValidity();
            return;
        }
        confirmPassword.setCustomValidity('');
        modal.show();
    });

    form.addEventListener('input', updateSubmitAvailability);
    confirmPassword.addEventListener('input', () => {
        confirmPassword.setCustomValidity('');
        updateSubmitAvailability();
    });
    confirmReset.addEventListener('click', () => {
        confirmReset.disabled = true;
        openConfirmation.disabled = true;
        confirmReset.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Resetting...';
        form.submit();
    });
    updateMethodPanels();
});
