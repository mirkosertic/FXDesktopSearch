<!DOCTYPE html>
<html class="no-js" lang="en">
<head>
    <meta http-equiv="cache-control" content="max-age=0" />
    <meta http-equiv="cache-control" content="no-cache" />
    <meta http-equiv="expires" content="0" />
    <meta http-equiv="expires" content="Tue, 01 Jan 1980 1:00:00 GMT" />
    <meta http-equiv="pragma" content="no-cache" />
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <base href="${serverBase}"/>
    <link rel="stylesheet" href="css/foundation.css" />
    <script src="js/vendor/modernizr.js"></script>
    <link rel="stylesheet" href="webapp.css"/>
    <script src="webapp.js"></script>
</head>
<body>
    <form data-abide>
        <div class="row collapse">
            <h2>Global settings</h2>
        </div>
        <div class="row collapse">
            <div class="small-3 medium-3 large-3 columns">
                <label>Show similar search results</label>
            </div>
            <div class="small-3 medium-3 large-3 columns">
                <div class="switch">
                    <input id="showsimilarresults" type="checkbox">
                    <label for="showsimilarresults"></label>
                </div>
            </div>
            <div class="small-3 medium-3 large-3 columns">
                <label>Limit search results to</label>
            </div>
            <div class="small-3 medium-3 large-3 columns end">
                <div class="range-slider" data-slider data-options="start: 10; end: 100; display_selector: #searchresultshandle; step: 5;">
                    <span id="searchresultshandle" class="range-slider-handle text-center" role="slider" tabindex="0"></span>
                    <span class="range-slider-active-segment"></span>
                    <input type="hidden" id="limitsearchresultsto">
                </div>
            </div>
        </div>
        <div class="row collapse">
            <div class="small-3 medium-3 large-3 columns">
                <label>Perform recrawl after application startup</label>
            </div>
            <div class="small-3 medium-3 large-3 columns end">
                <div class="switch">
                    <input id="crawlonstartup" type="checkbox">
                    <label for="crawlonstartup"></label>
                </div>
            </div>
        </div>
        <div class="row collapse">
            <h2>Suggestions</h2>
        </div>
        <div class="row collapse">
            <div class="small-3 medium-3 large-3 columns">
                <label>Require suggestions to be in order</label>
            </div>
            <div class="small-3 medium-3 large-3 columns">
                <div class="switch">
                    <input id="requiresuggestionstobeinorder" type="checkbox">
                    <label for="requiresuggestionstobeinorder"></label>
                </div>
            </div>
            <div class="small-3 medium-3 large-3 columns">
                <label>Number of suggestions</label>
            </div>
            <div class="small-3 medium-3 large-3 columns end">
                <div class="range-slider" data-slider data-options="start: 5; end: 20; display_selector: #numberofsuggestionshandle; step: 1;">
                    <span id="numberofsuggestionshandle" class="range-slider-handle text-center" role="slider" tabindex="0"></span>
                    <span class="range-slider-active-segment"></span>
                    <input type="hidden" id="numberofsuggestions">
                </div>
            </div>
        </div>
        <div class="row collapse">
            <div class="small-3 medium-3 large-3 columns">
                <label>Number of words before suggestion span</label>
            </div>
            <div class="small-3 medium-3 large-3 columns">
                <div class="range-slider" data-slider data-options="start: 0; end: 10; display_selector: #numberofworddsbeforesuggestionspanhandle; step: 1;">
                    <span id="numberofworddsbeforesuggestionspanhandle" class="range-slider-handle text-center" role="slider" tabindex="0"></span>
                    <span class="range-slider-active-segment"></span>
                    <input type="hidden" id="numberofworddsbeforesuggestionspan">
                </div>
            </div>
            <div class="small-3 medium-3 large-3 columns">
                <label>Number of words after suggestion span</label>
            </div>
            <div class="small-3 medium-3 large-3 columns end">
                <div class="range-slider" data-slider data-options="start: 0; end: 10; display_selector: #numberofworddsaftersuggestionspanhandle; step: 1;">
                    <span id="numberofworddsaftersuggestionspanhandle" class="range-slider-handle text-center" role="slider" tabindex="0"></span>
                    <span class="range-slider-active-segment"></span>
                    <input type="hidden" id="numberofworddsaftersuggestionspan">
                </div>
            </div>
        </div>
        <div class="row collapse">
            <div class="small-3 medium-3 large-3 columns">
                <label>Slop for suggestion spans</label>
            </div>
            <div class="small-3 medium-3 large-3 columns end">
                <div class="range-slider" data-slider data-options="start: 0; end: 10; display_selector: #slopforsuggestionspanhandle; step: 1;">
                    <span id="slopforsuggestionspanhandle" class="range-slider-handle text-center" role="slider" tabindex="0"></span>
                    <span class="range-slider-active-segment"></span>
                    <input type="hidden" id="slopforsuggestionspan">
                </div>
            </div>
        </div>

        <div class="row collapse">
            <div class="small-3 medium-3 large-3 columns">
                <a href="#" class="button [radius round]">Save configuration</a>
            </div>
        </div>
    </form>

    <script src="js/vendor/jquery.js"></script>
    <script src="js/foundation.min.js"></script>
    <script>
        $(document).foundation();
    </script>
</body>
