var desktopsearch = {
    configure: function() {
        desktop.configure();
    },
    completecrawl: function() {
        desktop.completecrawl();
    },
    close: function() {
        desktop.close();
    },
    openFile: function(event) {
        desktop.openFile(event.target.dataset.filename);
    },
    registerSuggest: function() {
        document.getElementById("querystring").addEventListener("keyup", function(e) {
            var value = this.value;
            if (value.length > 2) {
                fetch('/suggestion?term=' + encodeURI(value))
                    .then(function(response) {
                        return response.json();
                    })
                    .then(function(myJson) {
                        var datalist = document.getElementById("suggestion");
                        while (datalist.firstChild) {
                            datalist.removeChild(datalist.firstChild);
                        }

                        if (myJson.length > 0) {
                            for (var i = 0; i < myJson.length; i++) {
                                var s = myJson[i];

                                var a = document.createElement('a');
                                a.innerHTML = s.label;
                                a.href = "/search?querystring=" + encodeURIComponent(s.value);
                                datalist.appendChild(a);
                            }
                            datalist.classList.remove("hidden")
                        } else {
                            datalist.classList.add("hidden")
                        }
                    });
            }
        });
    }
}


document.addEventListener("DOMContentLoaded", function() {
    var lazyImages = [].slice.call(document.querySelectorAll("img.lazy"));
    var lazyHighlights = [].slice.call(document.querySelectorAll("div.lazyhighlight"));

    var lazyImageactive = false;
    var lazyHighlightactive = false;

    var lazyImageLoad = function() {
        if (lazyImageactive === false) {
            lazyImageactive = true;

            setTimeout(function() {
                lazyImages.forEach(function(lazyImage) {
                    if ((lazyImage.getBoundingClientRect().top <= window.innerHeight && lazyImage.getBoundingClientRect().bottom >= 0) && getComputedStyle(lazyImage).display !== "none") {
                        var resourceToLoad = "/thumbnail/preview/" + lazyImage.dataset.filename + ".png";
                        console.info("Loading lazy resource " + resourceToLoad);
                        lazyImage.src = resourceToLoad;
                        lazyImage.classList.remove("lazy");

                        lazyImages = lazyImages.filter(function(image) {
                            return image !== lazyImage;
                        });

                        if (lazyImages.length === 0) {
                            document.removeEventListener("scroll", lazyImageLoad);
                            window.removeEventListener("resize", lazyImageLoad);
                            window.removeEventListener("orientationchange", lazyImageLoad);
                        }
                    }
                });

                lazyImageactive = false;
            }, 200);
        }
    };

    var lazyHighlightLoad = function() {
        if (lazyHighlightactive === false) {
            lazyHighlightactive = true;

            setTimeout(function() {
                lazyHighlights.forEach(function(lazyHighlight) {
                    if ((lazyHighlight.getBoundingClientRect().top <= window.innerHeight && lazyHighlight.getBoundingClientRect().bottom >= 0) && getComputedStyle(lazyHighlight).display !== "none") {
                        var resourceToLoad = "/highlight" +
                            "?query=" + encodeURIComponent(lazyHighlight.dataset.query) +
                            "&docId=" + encodeURIComponent(lazyHighlight.dataset.docid);

                        console.info("Loading lazy highlight resource " + resourceToLoad);

                        fetch(resourceToLoad)
                            .then(function(response) {
                                console.info("Loaded lazy highlight resource " + resourceToLoad);
                                return response.text();
                            })
                            .then(function(text) {
                                console.info("Replacing content of lazy highlight " + lazyHighlight.id + " with " + text);
                                lazyHighlight.innerHTML = text;
                            });

                        lazyHighlights = lazyHighlights.filter(function(hl) {
                            return hl !== lazyHighlight;
                        });

                        if (lazyHighlights.length === 0) {
                            document.removeEventListener("scroll", lazyHighlightLoad);
                            window.removeEventListener("resize", lazyHighlightLoad);
                            window.removeEventListener("orientationchange", lazyHighlightLoad);
                        }
                    }
                });

                lazyHighlightactive = false;
            }, 200);
        }
    };

    document.addEventListener("scroll", lazyImageLoad);
    window.addEventListener("resize", lazyImageLoad);
    window.addEventListener("orientationchange", lazyImageLoad);

    document.addEventListener("scroll", lazyHighlightLoad);
    window.addEventListener("resize", lazyHighlightLoad);
    window.addEventListener("orientationchange", lazyHighlightLoad);

    lazyImageLoad();
    lazyHighlightLoad();
});

function prepareSubmit() {
    $("body").toggleClass("wait");
    return true;
}