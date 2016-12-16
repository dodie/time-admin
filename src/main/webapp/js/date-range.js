(function() {
    function DateRangePopup(input) {
        this._input = input;
        var popup = document.querySelector(".date-range-input-popup-template").cloneNode(true);
        popup.classList.remove("date-range-input-popup-template");
        var months = Array.from(popup.querySelectorAll("[data-month]")).map(function(m) {
            return {
                element: m,
                value: parseInt(m.getAttribute("data-month"), 10)
            };
        });
        var view = {
            popup: popup,
            nextYear: popup.querySelector(".next-year"),
            previousYear: popup.querySelector(".previous-year"),
            currentYear: popup.querySelector("[data-year]"),
            months: months,
            monthSelector: popup.querySelector(".month-selector"),
            input: input.querySelector(".date-range-input-field"),
            displayFrom: input.querySelector(".date-range-input-display-from"),
            displayTo: input.querySelector(".date-range-input-display-to")
        };
        this._popup = popup;
        this._view = view;
        this._displayedYear = new Date().getFullYear();

        this._selectedRange = {
            from: undefined,
            to: undefined
        };

        this._hoveredMonth = undefined;
        this._destroyFunctions = [];

        this._initYearSelector();
        this._initMonthSelector();
        this._loadDefaultValue(this._view.input.value);
        this._updateView();
        this._showPopup();
    }

    DateRangePopup.prototype._initYearSelector = function() {
        var self = this;
        this._view.previousYear.addEventListener("click", function() {
            self.setDisplayedYear(self.getDisplayedYear() - 1);
        });
        this._view.nextYear.addEventListener("click", function() {
            self.setDisplayedYear(self.getDisplayedYear() + 1);
        });
    };

    DateRangePopup.prototype._initMonthSelector = function() {
        var self = this;
        this._view.months.forEach(function(month) {
            month.element.addEventListener("click", function() {
                self._doMonthSelect(month.value);
            });
            month.element.addEventListener("mouseenter", function() {
                self._doMonthHover(month.value);
            });
            month.element.addEventListener("mouseleave", function() {
                self._doMonthUnHover(month.value);
            });
        });
    };

    DateRangePopup.prototype.reposition = function() {
        var inputBCR = this._input.getBoundingClientRect();
        this._popup.style.top = inputBCR.bottom + "px";
        var popupWidth = this._popup.getBoundingClientRect().width;
        var posX = inputBCR.left + inputBCR.width / 2 - popupWidth / 2;
        if (posX < 0) posX = 0;
        if (posX + popupWidth > document.documentElement.offsetWidth) posX = document.documentElement.offsetWidth - popupWidth;
        this._popup.style.left = Math.round(posX) + "px";
    };

    DateRangePopup.prototype._showPopup = function() {
        var self = this;

        this._destroyFunctions.push(function() {
            document.body.removeChild(self._popup);
        });
        document.body.appendChild(this._popup);
        this.reposition();

        function outsideMouseDownDetect(e) {
            var popupBCR = self._popup.getBoundingClientRect();
            if (e.clientX < popupBCR.left || e.clientX > popupBCR.right || e.clientY < popupBCR.top || e.clientY > popupBCR.bottom) {
                self.destroy();
            }
        }

        this._destroyFunctions.push(function() {
            document.removeEventListener("mousedown", outsideMouseDownDetect);
        });
        document.addEventListener("mousedown", outsideMouseDownDetect);

        function repositionBound() {
            self.reposition();
        }

        this._destroyFunctions.push(function() {
            window.removeEventListener("resize", repositionBound);
        });
        window.addEventListener("resize", repositionBound);
    };

    DateRangePopup.prototype._commitValue = function() {
        function pad(length, val) {
            val = val + "";
            while (val.length < length) {
                val = "0" + val;
            }
            return val;
        }
        var fromYear = pad(4, this._selectedRange.from.year);
        var fromMonth = pad(2, this._selectedRange.from.month);
        if (this.monthCompare(this._selectedRange.from, this._selectedRange.to) === 0) {
            this._view.input.value = fromYear + "-" + fromMonth;
            this._view.displayFrom.setAttribute("data-value", fromYear + ". " + fromMonth + ".");
            this._view.displayTo.setAttribute("data-value", "");
        } else {
            var toYear = pad(4, this._selectedRange.to.year);
            var toMonth = pad(2, this._selectedRange.to.month);
            this._view.input.value = fromYear + "-" + fromMonth + ";" + toYear + "-" + toMonth;
            this._view.displayFrom.setAttribute("data-value", fromYear + ". " + fromMonth + ".");
            this._view.displayTo.setAttribute("data-value", toYear + ". " + toMonth + ".");
        }
    };

    DateRangePopup.prototype._loadDefaultValue = function(value) {
        function partToMonth(p) {
            var datePart = /^([0-9]{4})-([0-9]{2})$/.exec(p);
            return {
                year: parseInt(datePart[1], 10),
                month: parseInt(datePart[2], 10)
            };
        }

        var parts = value.split(";");
        if (parts.length === 1) { //Single date
            this._selectedRange.from = partToMonth(parts[0]);
            this._selectedRange.to = this._selectedRange.from;
            this._displayedYear = this._selectedRange.from.year;
        } else if (parts.length === 2) { //Date range
            this._selectedRange.from = partToMonth(parts[0]);
            this._selectedRange.to = partToMonth(parts[1]);
            this._displayedYear = this._selectedRange.from.year;
        } else {
            this._commitValue(); //Error, remove the current value
        }
    };

    DateRangePopup.prototype.destroy = function() {
        while (this._destroyFunctions.length > 0) {
            this._destroyFunctions.pop()();
        }
    };

    DateRangePopup.prototype.monthCompare = function(a, b) {
        if (a.year < b.year) return -1;
        if (a.year > b.year) return 1;
        if (a.month < b.month) return -1;
        if (a.month > b.month) return 1;
        return 0;
    };

    DateRangePopup.prototype.monthBetween = function(a, b, query) {
        var forward = this.monthCompare(a, b) < 0;
        if (!forward) {
            var c = a;
            a = b;
            b = c;
        }
        return this.monthCompare(a, query) <= 0 && this.monthCompare(b, query) >= 0;
    };

    DateRangePopup.prototype._doMonthUnHover = function(month) {
        this._hoveredMonth = undefined;
        this._updateView();
    };

    DateRangePopup.prototype._doMonthHover = function(month) {
        var current = {
            year: this.getDisplayedYear(),
            month: month
        };
        this._hoveredMonth = current;
        this._updateView();
    };

    DateRangePopup.prototype._doMonthSelect = function(month) {
        var current = {
            year: this.getDisplayedYear(),
            month: month
        };
        if (this._selectedRange.from === undefined || this._selectedRange.to !== undefined) { //First time OR reselect
            this._selectedRange.from = current;
            this._selectedRange.to = undefined;
        } else if (this._selectedRange.from !== undefined) { //Selecting the end point
            var from = this._selectedRange.from;
            var to = current;
            if (this.monthCompare(from, to) > 0) {
                var c = from;
                from = to;
                to = c;
            }
            this._selectedRange.from = from;
            this._selectedRange.to = to;
            this._commitValue();
        }
        this._updateView();
    };

    DateRangePopup.prototype.getDisplayedYear = function() {
        return this._displayedYear;
    };

    DateRangePopup.prototype.setDisplayedYear = function(newValue) {
        if (this._displayedYear !== newValue) {
            var oldYear = this._displayedYear;
            this._displayedYear = newValue;
            this._view.monthSelector.style.display = "none";
            this._view.monthSelector.setAttribute("data-direction", (oldYear < newValue ? "bottom" : "top"));
            document.body.offsetWidth; //Force JS to yield (apply css values)
            this._view.monthSelector.style.display = "";
            this._updateView();
        }
    };

    DateRangePopup.prototype._updateView = function() {
        var self = this;
        this._view.currentYear.setAttribute("data-year", this.getDisplayedYear());

        //Reset months
        this._view.months.forEach(function(month) {
            month.element.classList.remove("selection-hard");
            month.element.classList.remove("selection-begin");
            month.element.classList.remove("selection-end");
            month.element.classList.remove("selection-cont");
        });

        if (this._selectedRange.from !== undefined) {
            var from = this._selectedRange.from;
            this._view.months.forEach(function(month) {
                var current = {
                    year: self.getDisplayedYear(),
                    month: month.value
                };
                if (self.monthCompare(current, from) === 0) {
                    month.element.classList.add("selection-hard");
                }
            });
        }

        if (this._selectedRange.to !== undefined) {
            var to = this._selectedRange.to;
            this._view.months.forEach(function(month) {
                var current = {
                    year: self.getDisplayedYear(),
                    month: month.value
                };
                if (self.monthCompare(current, to) === 0) {
                    month.element.classList.add("selection-hard");
                }
            });
        }

        if (this._selectedRange.from === undefined || this._selectedRange.to === undefined) {
            this._view.popup.classList.add("selection-hovering");
        } else {
            this._view.popup.classList.remove("selection-hovering");
        }

        if (this._selectedRange.from !== undefined && this._selectedRange.to === undefined && this._hoveredMonth !== undefined) {
            var from = this._selectedRange.from;
            var to = this._hoveredMonth;
            var same = this.monthCompare(from, to) === 0;
            if (!same) {
                var forward = this.monthCompare(from, to) < 0;
                this._view.months.forEach(function(month) {
                    var current = {
                        year: self.getDisplayedYear(),
                        month: month.value
                    };
                    if (self.monthCompare(current, from) === 0) {
                        if (forward) {
                            month.element.classList.add("selection-begin");
                        } else {
                            month.element.classList.add("selection-end");
                        }
                    } else if (self.monthBetween(from, to, current)) {
                        month.element.classList.add("selection-cont");
                    }
                });
            }
        }
        if (this._selectedRange.from !== undefined && this._selectedRange.to !== undefined) {
            var from = this._selectedRange.from;
            var to = this._selectedRange.to;
            this._view.months.forEach(function(month) {
                var current = {
                    year: self.getDisplayedYear(),
                    month: month.value
                };
                const isFrom = self.monthCompare(current, from) === 0;
                const isTo = self.monthCompare(current, to) === 0;
                if (isFrom) {
                    month.element.classList.add("selection-begin");
                }
                if (isTo) {
                    month.element.classList.add("selection-end");
                }
                if (!isFrom && !isTo && self.monthBetween(from, to, current)) {
                    month.element.classList.add("selection-cont");
                }
            });
        }
    };

    $(document).on("click", ".date-range-input", function() {
        new DateRangePopup(this);
    });
})();