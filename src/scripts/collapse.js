
    var triggers = document.querySelectorAll("[data-collapse-target]");
    var collapses = document.querySelectorAll("[data-collapse]");
    if (triggers && collapses) {
        Array.from(triggers).forEach(function (trigger) {
            return Array.from(collapses).forEach(function (collapse) {
                if (trigger.dataset.collapseTarget === collapse.dataset.collapse) {
                    trigger.addEventListener("click", function () {
                        console.log("click")
                        if (collapse.style.height && collapse.style.height !== "0px") {
                            collapse.style.height = 0;
                            collapse.style.overflow = "hidden";
                            trigger.removeAttribute("open")
                        } else {
                            collapse.style.height = "".concat(collapse.children[0].clientHeight, "px");
                            collapse.style.overflow = "visible";
                            trigger.setAttribute("open", "")
                        }
                    })
                }
            })
        })
    }