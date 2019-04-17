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
    }
}

function prepareSubmit() {
    $("body").toggleClass("wait");
    return true;
}