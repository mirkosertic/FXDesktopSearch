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
    </head>
    <body>
        <form method="post" action="/search">
            <div class="titlebar">
                <input class="submitbutton" type="submit" value="">
                <div class="querydiv">
                    <input class="query" name="querystring" type="text" value="${querystring}"/>
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

                    <b><a class="searchResultAreaFileName" onclick="desktop.openFile('${queryResult.getEscapedFileName(document.fileName)}')">${queryResult.getSimpleFileName(document.fileName)}</a></b><br/>

                    <a class="searchResultAreaFileNameComplete" onclick="desktop.openFile('${queryResult.getEscapedFileName(document.fileName)}')">${document.fileName}</a>

                    <div class="searchResultAreaContentHighlighted">${document.highlightedSearchResult}</div>

                    <#list document.similarFiles as similarFile>
                        <div><a onclick="desktop.openFile('${queryResult.getEscapedFileName(similarFile)}')" class="searchResultAreaResultSimilar">${queryResult.getSimpleFileName(similarFile)}</a></div>
                    </#list>
                    <br/>
                </#list>
                </div>
            <#else>
                <div class="logoDiv"><img src="logo.png" class="logo"/>
            </#if>
        </div>
    </body>
</html>