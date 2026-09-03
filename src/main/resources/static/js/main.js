// Client-side helpers only. No business rules live here — quantity limits,
// stock checks, and totals are always (re)validated on the server.

document.addEventListener('DOMContentLoaded', function () {
    // Product details page: keep the quantity input within [1, stock] and
    // disable Add to Cart when stock is zero. The server re-validates
    // regardless, so this is purely a UX nicety.
    var qtyInput = document.querySelector('[data-qty-input]');
    if (qtyInput) {
        var max = parseInt(qtyInput.getAttribute('max'), 10);
        qtyInput.addEventListener('change', function () {
            var value = parseInt(qtyInput.value, 10);
            if (isNaN(value) || value < 1) {
                qtyInput.value = 1;
            } else if (max && value > max) {
                qtyInput.value = max;
            }
        });
    }

    // Cart page: submit the quantity-update form automatically when the
    // number input changes, so users don't have to hunt for an "Update" button.
    document.querySelectorAll('[data-auto-submit]').forEach(function (input) {
        input.addEventListener('change', function () {
            input.form.requestSubmit();
        });
    });
});
