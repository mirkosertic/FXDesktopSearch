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
        <link rel="stylesheet" href="style.css"/>
        <script src="webapp.js"></script>
    </head>
    <body>
        <form method="post" action="/search">
            <input type="checkbox" id="drawer">
            <label id="drawer-toggle" for="drawer"></label>
            <aside>
                <div class="title">FX Desktop Search</div>
                <a class="item settings" onclick="desktopsearch.configure()">Configure</a>
                <a class="item search" onclick="desktopsearch.completecrawl()">Complete crawl</a>
                <a class="item close" onclick="desktopsearch.close()">Quit / Shutdown</a>
            </aside>
            <#if queryResult?has_content>
                <nav class="search">
                    <a href="/search"><img src="logo.png"/></a>
                    <div class="search-input-wrapper">
                        <input value="${queryResult.searchTerm}" autocomplete="off" id="querystring" name="querystring" placeholder="Enter searchphrase here...">
                        <div id="suggestion" class="hidden">
                        </div>
                    </div>
                    <button type="submit">Search!</button>
                </nav>
            </#if>

            <#if queryResult?has_content>
                <nav role="chips">
                    <div class="chips">
                        <#list queryResult.activeFilters as filter>
                            <a href="${filter.deleteLink}">${filter.name}</a>
                        </#list>
                    </div>
                </nav>
            </#if>

            <#if queryResult?has_content>
                <nav role="facet">
                    <ul>
                        <#list queryResult.facetDimensions as dimension>
                            <li>
                                <input type="checkbox" id="facet_${dimension?index}">
                                <label for="facet_${dimension?index}"><a>${dimension.name}</a></label>
                                <div class="menu"><#list dimension.facets as facet><a href="${facet.link}">${facet.name} (${facet.number})</a></#list></div>
                            </li>
                        </#list>
                    </ul>
                </nav>
            </#if>

            <#if queryResult?has_content>
                <div class="summarytext">The search was processed in ${queryResult.elapsedTime}ms., searched in ${queryResult.totalDocuments} documents.</div>

                <#list queryResult.documents as document>
                    <div class="resultentry">
                        <div class="image">
                            <img class="lazy" src="loading.gif" data-src="/thumbnail/preview/${document.uniqueID}.png"/>
                        </div>
                        <div class="text">
                            <#list document.fileNames as filename>
                                <a class="entrytitle" onclick="desktopsearch.openFile('${queryResult.getEscapedFileName(filename)}')">${filename}</a>
                            </#list>
                            <#list document.similarFiles as similarFile>
                                <a class="filename" onclick="desktopsearch.openFile('${queryResult.getEscapedFileName(similarFile)}')">${queryResult.getSimpleFileName(similarFile)}</a>
                            </#list>
                            <div class="starsouter">
                                <#list 1..5 as index>
                                    <#if document.normalizedScore &gt;= index>
                                        <span class="stars-full"></span>
                                    <#else>
                                        <span class="stars-empty"></span>
                                    </#if>
                                </#list>
                            </div>
                            <div class="entrytext">${document.highlightedSearchResult}</div>
                        </div>
                    </div>
                </#list>
            <#else>
                <div class="welcome">
                    <div class="logo">
                        <img src="logo.png"/>
                    </div>
                    <div class="search">
                        <div class="search-input-wrapper">
                            <input autocomplete="off" id="querystring" name="querystring" placeholder="Enter searchphrase here...">
                            <div id="suggestion" class="hidden">
                            </div>
                        </div>
                        <button type="submit">Search!</button>
                    </div>
                    <div class="introduction">
                        <div>Search examples:</div>
                        <div><mark>scott adams</mark> searches for all documents containing the phrase "scott adams" or at least the words "scott" and "adams" in any order.</div>
                        <div><mark>scott adams -dogbert</mark> searches for all documents containing the text "scot adams", but not "dogbert".</div>
                        <div><mark>scott ad*</mark> searches for all documents containing the word "scott" and any words matching the wildcard "ad*", where * stands for any number of characters.</div>
                        <div><mark>scott ad?ms</mark> searches for all documents containing the word "scott" and any words matching the wildcard "ad?ms", where ? stands for one character.</div>
                        <div><mark>scott ~adams</mark> searches for all documents containing the word "scott" and any words matching the fuzzy term "adams".</div>
                    </div>
                </div>
            </#if>
        </form>
        <script>
            desktopsearch.registerSuggest();
        </script>
    </body>
</html>