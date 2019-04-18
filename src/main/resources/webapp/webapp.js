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
    openFile: function(f) {
        desktop.openFile(f);
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
                        var str = '';
                        for (var i = 0;i<myJson.length;i++) {
                            var s = myJson[i];
                            str+= '<option>' + s.value;
                        }
                        datalist.innerHTML = str;
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