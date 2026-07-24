function validateDateFilters() {
    var startDateInput = document.getElementById("startDate");
    var endDateInput = document.getElementById("endDate");
    var errorContainer = document.getElementById("inlineDateError");

    if (startDateInput && endDateInput && errorContainer) {
        var startDateVal = startDateInput.value;
        var endDateVal = endDateInput.value;

        errorContainer.style.display = "none";
        errorContainer.innerText = "";

        if (startDateVal && endDateVal) {
            var start = new Date(startDateVal);
            var end = new Date(endDateVal);

            if (start > end) {
                errorContainer.innerText = "Filter error: The start date can't be later than the end date.";
                errorContainer.style.display = "block";

                startDateInput.value = "";
                endDateInput.value = "";

                return false;
            }
        }
    }
    return true;
}