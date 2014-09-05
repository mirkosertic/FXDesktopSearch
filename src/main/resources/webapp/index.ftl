<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="cache-control" content="max-age=0" />
        <meta http-equiv="cache-control" content="no-cache" />
        <meta http-equiv="expires" content="0" />
        <meta http-equiv="expires" content="Tue, 01 Jan 1980 1:00:00 GMT" />
        <meta http-equiv="pragma" content="no-cache" />
        <base href="${serverBase}"/>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <link rel="stylesheet" href="webapp.css"/>
        <link rel="stylesheet" href="jquery-ui-1.11.1.custom/jquery-ui.min.css"/>
        <link rel="stylesheet" href="jquery-ui-1.11.1.custom/jquery-ui.theme.min.css"/>
        <style>
            .ui-autocomplete-loading {
                background: white url("ui-anim_basic_16x16.gif") right center no-repeat;
            }
        </style>
    </head>
    <body>
        <form method="post" action="/search">
            <div class="titlebar">
                <input class="submitbutton" type="submit" value="">
                <div class="querydiv">
                    <input placeholder="Please enter your search query here" class="query" id="querystring" name="querystring" type="text" value="${querystring?html}"/>
                </div>
            </div>
        </form>
        <div class="searchResult">

            <#if queryResult?has_content>

                The search was processed in ${queryResult.elapsedTime}ms., searched in ${queryResult.totalDocuments} documents.<br/><br/>

                <div class="afterSearchNavigationArea">
                    <#if queryResult.backLink?has_content>
                        <a class="dimensionGoBackLink" href="${queryResult.backLink}">&lt;&lt; Go back</a>
                    </#if>
                    <#list queryResult.facetDimensions as dimension>
                    <div class="dimension">
                        <div class="dimensionTitle">${dimension.name}</div>
                        <div class="dimensionValues">
                            <#list dimension.facets as facet>
                            <div class="dimensionValue"><a href="${facet.link}" class="dimensionValueLink">${facet.name}</a> (${facet.number})</div>
                            </#list>
                        </div>
                    </div>
                    </#list>
                </div>
                <div class="searchResultArea">

                <#list queryResult.documents as document>

                    <#list document.fileNames as filename>
                        <#if filename_index == 0>
                            <b><span>
                                <#list 0..5 as index>
                                    <#if document.normalizedScore &gt;= index>
                                        <img class="ratingStar" src="star_light.png"/>
                                    <#else>
                                        <img class="ratingStar" src="star_dark.png"/>
                                    </#if>
                                </#list>
                            </span><a class="searchResultAreaFileName" onclick="desktop.openFile('${queryResult.getEscapedFileName(filename)}')">${queryResult.getSimpleFileName(filename)}</a></b><br/>
                            <a class="searchResultAreaFileNameComplete" onclick="desktop.openFile('${queryResult.getEscapedFileName(filename)}')">${filename}</a>
                        <#else>
                            <br/><a class="searchResultAreaFileNameComplete" onclick="desktop.openFile('${queryResult.getEscapedFileName(filename)}')">${filename}</a>
                        </#if>
                    </#list>

                    <div class="searchResultAreaContentHighlighted">${document.highlightedSearchResult}</div>

                    <#list document.similarFiles as similarFile>
                        <div><a onclick="desktop.openFile('${queryResult.getEscapedFileName(similarFile)}')" class="searchResultAreaResultSimilar">${queryResult.getSimpleFileName(similarFile)}</a></div>
                    </#list>
                    <br/>
                </#list>
                </div>
            <#else>
                <div class="logoDiv"><img src="logo.png" class="logo"/>
                <div class="introduction">
                    <div>Search examples:</div>
                    <div><span>scott adams</span> searches for all documents containing the phrase "scott adams" or at least the words "scott" and "adams" in any order.</div>
                    <div><span>scott adams -dogbert</span> searches for all documents containing the text "scot adams", but not "dogbert".</div>
                    <div><span>scott ad*</span> searches for all documents containing the word "scott" and any words matching the wildcard "ad*", where * stands for any number of characters.</div>
                    <div><span>scott ad?ms</span> searches for all documents containing the word "scott" and any words matching the wildcard "ad?ms", where ? stands for one character.</div>
                    <div><span>scott ~adams</span> searches for all documents containing the word "scott" and any words matching the fuzzy term "adams".</div>
                </div>
            </#if>
        </div>

        <script src="jquery-ui-1.11.1.custom/external/jquery/jquery.js"></script>
        <script src="jquery-ui-1.11.1.custom/jquery-ui.min.js"></script>
        <script>
            $(function() {
                function split( val ) {
                    return val.split( / \s*/ );
                }
                function extractLast( term ) {
                    return split( term ).pop();
                }
                $( "#querystring" )
                        // don't navigate away from the field on tab when selecting an item
                        .bind( "keydown", function( event ) {
                            if ( event.keyCode === $.ui.keyCode.TAB &&
                                    $( this ).autocomplete( "instance" ).menu.active ) {
                                event.preventDefault();
                            }
                        })
                        .autocomplete({
                            source: function( request, response ) {
                                $.getJSON( "suggestion", {
                                    term: extractLast( request.term )
                                }, response );
                            },
                            search: function() {
                                // custom minLength
                                var term = extractLast( this.value );
                                if ( term.length < 2 ) {
                                    return false;
                                }
                            },
                            focus: function() {
                                // prevent value inserted on focus
                                return false;
                            },
                            select: function( event, ui ) {
                                var terms = split( this.value );
                                // remove the current input
                                terms.pop();
                                // add the selected item
                                terms.push( ui.item.value );
                                terms.push( "" );
                                this.value = terms.join( " " );
                                return false;
                            }
                        });
            });
        </script>
    </body>
</html>