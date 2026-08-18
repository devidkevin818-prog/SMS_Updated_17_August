document.addEventListener('DOMContentLoaded', () => {
    const maxImageSize = 5 * 1024 * 1024;
    document.querySelectorAll('.image-preview-input').forEach((input) => {
        const preview = document.getElementById(input.dataset.previewTarget);
        const previewImage = preview?.querySelector('img');
        const error = document.getElementById(input.dataset.errorTarget);
        let previewUrl;

        input.addEventListener('change', () => {
            if (previewUrl) URL.revokeObjectURL(previewUrl);
            previewUrl = undefined;
            preview.hidden = true;
            previewImage.removeAttribute('src');
            error.textContent = '';
            input.setCustomValidity('');

            const file = input.files[0];
            if (!file) return;

            if (!['image/jpeg', 'image/png'].includes(file.type)) {
                input.setCustomValidity('Only JPG and PNG images are allowed.');
                error.textContent = 'Only JPG and PNG images are allowed.';
                input.reportValidity();
                return;
            }
            if (file.size > maxImageSize) {
                input.setCustomValidity('The image must be 5 MB or smaller.');
                error.textContent = 'The image must be 5 MB or smaller.';
                input.reportValidity();
                return;
            }

            previewUrl = URL.createObjectURL(file);
            previewImage.src = previewUrl;
            preview.hidden = false;
        });
    });

    const passwordToggle = document.getElementById('passwordToggle');
    const passwordInput = document.getElementById('password');
    passwordToggle?.addEventListener('click', () => {
        const visible = passwordInput.type === 'text';
        passwordInput.type = visible ? 'password' : 'text';
        passwordToggle.innerHTML = visible ? '<i class="bi bi-eye"></i>' : '<i class="bi bi-eye-slash"></i>';
        passwordToggle.setAttribute('aria-label', visible ? 'Show password' : 'Hide password');
    });

    const toggle = document.getElementById('sidebarToggle');
    const sidebar = document.getElementById('appSidebar');
    const backdrop = document.getElementById('sidebarBackdrop');

    if (!toggle || !sidebar) return;

    const closeMobileSidebar = () => document.body.classList.remove('sidebar-mobile-open');
    toggle.addEventListener('click', () => {
        if (window.innerWidth < 992) {
            document.body.classList.toggle('sidebar-mobile-open');
        } else {
            document.body.classList.toggle('sidebar-collapsed');
        }
    });
    backdrop?.addEventListener('click', closeMobileSidebar);
    window.addEventListener('resize', () => {
        if (window.innerWidth >= 992) closeMobileSidebar();
    });
});
