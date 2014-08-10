<html>
    <head>
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

                <#list queryResult.documents as document>

                    <b><a class="searchResultFileName" onclick="desktop.openFile('${queryResult.getEscapedFileName(document.fileName)}')">${queryResult.getSimpleFileName(document.fileName)}</a></b><br/>

                    <a class="searchResultFileNameComplete" onclick="desktop.openFile('${queryResult.getEscapedFileName(document.fileName)}')" class="location1">${document.fileName}</a>

                    <div class="searchResultContentHighlighted">${document.highlightedSearchResult}</div>

                    <#list document.similarFiles as similarFile>
                        <div><a onclick="desktop.openFile('${queryResult.getEscapedFileName(similarFile)}')" class="searchResultSimilar">${queryResult.getSimpleFileName(similarFile)}</a></div>
                    </#list>
                    <br/>
                </#list>
            <#else>
                <div class="logoDiv"><img src="logo.png" class="logo"/>
            </#if>
        </div>
    </body>
</html>