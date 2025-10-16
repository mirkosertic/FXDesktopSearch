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
                                a.href = "/search/" + s.value;
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
    var active = false;

    var lazyLoad = function() {
        if (active === false) {
            active =true;

            setTimeout(function() {
                lazyImages.forEach(function(lazyImage) {
                    if ((lazyImage.getBoundingClientRect().top <= window.innerHeight && lazyImage.getBoundingClientRect().bottom >= 0) && getComputedStyle(lazyImage).display !== "none") {
                        lazyImage.src = lazyImage.dataset.src;
                        lazyImage.classList.remove("lazy");

                        lazyImages = lazyImages.filter(function(image) {
                            return image !== lazyImage;
                        });

                        if (lazyImages.length === 0) {
                            document.removeEventListener("scroll", lazyLoad);
                            window.removeEventListener("resize", lazyLoad);
                            window.removeEventListener("orientationchange", lazyLoad);
                        }
                    }
                });

                active = false;
            }, 200);
        }
    };

    document.addEventListener("scroll", lazyLoad);
    window.addEventListener("resize", lazyLoad);
    window.addEventListener("orientationchange", lazyLoad);

    lazyLoad();
});

function prepareSubmit() {
    $("body").toggleClass("wait");
    return true;
}