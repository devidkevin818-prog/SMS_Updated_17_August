document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.dashboard-counter').forEach((counter) => {
        counter.addEventListener('click', (event) => {
            const targetHref = counter.getAttribute('href');
            const title = counter.dataset.sectionTitle;
            if (!title) return;
            const heading = Array.from(document.querySelectorAll('.surface-card h2')).find((item) => item.textContent.trim() === title);
            if (!heading) return;
            event.preventDefault();
            const section = heading.closest('.surface-card');
            section.scrollIntoView({behavior: 'smooth', block: 'start'});
            section.classList.add('dashboard-section-focus');
            window.setTimeout(() => section.classList.remove('dashboard-section-focus'), 1400);
            if (targetHref?.startsWith('#')) history.replaceState(null, '', targetHref);
        });
    });

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

    document.querySelectorAll('.sidebar-link').forEach((link) => {
        if (!link.title) link.title = link.querySelector('span')?.textContent?.trim() || '';
    });

    if (!toggle || !sidebar) return;

    const closeMobileSidebar = () => document.body.classList.remove('sidebar-mobile-open');
    backdrop?.addEventListener('click', closeMobileSidebar);
    window.addEventListener('resize', () => {
        if (window.innerWidth >= 992) closeMobileSidebar();
    });
});
