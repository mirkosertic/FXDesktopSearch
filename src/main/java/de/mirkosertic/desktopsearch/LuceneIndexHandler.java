/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2013 Mirko Sertic
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.mirkosertic.desktopsearch;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.utils.DateUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

class LuceneIndexHandler {

    private static final Logger LOGGER = Logger.getLogger(LuceneIndexHandler.class);

    private static final int NUMBER_OF_FRAGMENTS = 5;

    private final Configuration configuration;
    private final PreviewProcessor previewProcessor;
    private final SolrEmbedded solrEmbedded;
    private final SolrClient solrClient;

    public LuceneIndexHandler(Configuration aConfiguration, PreviewProcessor aPreviewProcessor) throws IOException {
        previewProcessor = aPreviewProcessor;
        configuration = aConfiguration;

        File theIndexDirectory = new File(aConfiguration.getConfigDirectory(), "index");
        theIndexDirectory.mkdirs();

        solrEmbedded = new SolrEmbedded(new SolrEmbedded.Config(theIndexDirectory));
        solrClient = solrEmbedded.solrClient();
    }

    public void crawlingStarts() throws IOException {
    }

    public void addToIndex(String aLocationId, Content aContent) throws IOException {

        SupportedLanguage theLanguage = aContent.getLanguage();

        SolrInputDocument theDocument = new SolrInputDocument();
        theDocument.setField(IndexFields.UNIQUEID, aContent.getFileName());
        theDocument.setField(IndexFields.LOCATIONID, aLocationId);
        theDocument.setField(IndexFields.CONTENTMD5, DigestUtils.md5Hex(aContent.getFileContent()));
        theDocument.setField(IndexFields.LOCATIONID, aLocationId);
        theDocument.setField(IndexFields.FILESIZE, Long.toString(aContent.getFileSize()));
        theDocument.setField(IndexFields.LASTMODIFIED, Long.toString(aContent.getLastModified()));
        theDocument.setField(IndexFields.LANGUAGE, theLanguage.name());

        StringBuilder theContentAsString = new StringBuilder(aContent.getFileContent());

        aContent.getMetadata().forEach(theEntry -> {
            if (!StringUtils.isEmpty(theEntry.key)) {
                Object theValue = theEntry.value;
                if (theValue instanceof String) {
                    String theStringValue = (String) theValue;
                    if (!StringUtils.isEmpty(theStringValue)) {
                        theDocument.setField("attr_" + theEntry.key, theStringValue);
                    }
                }
                if (theValue instanceof Date) {
                    Date theDateValue = (Date) theValue;
                    Calendar theCalendar = GregorianCalendar.getInstance(DateUtils.UTC, Locale.US);
                    theCalendar.setTime(theDateValue);

                    // Full-Path
                    {
                        String thePathInfo = String.format(
                                "%04d/%02d/%02d",
                                theCalendar.get(Calendar.YEAR),
                                theCalendar.get(Calendar.MONTH) + 1,
                                theCalendar.get(Calendar.DAY_OF_MONTH));

                        theDocument.setField("attr_" + theEntry.key+"-year-month-day", thePathInfo);
                    }
                    // Year
                    {
                        String thePathInfo = String.format(
                                "%04d",
                                theCalendar.get(Calendar.YEAR));

                        theDocument.setField("attr_" + theEntry.key+"-year", thePathInfo);
                    }
                    // Year-month
                    {
                        String thePathInfo = String.format(
                                "%04d/%02d",
                                theCalendar.get(Calendar.YEAR),
                                theCalendar.get(Calendar.MONTH) + 1);

                        theDocument.setField("attr_" + theEntry.key+"-year-month", thePathInfo);
                    }

                }
            }
        });

        theDocument.setField(IndexFields.CONTENT, theContentAsString.toString());

        try {
            solrClient.add(theDocument);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void removeFromIndex(String aFileName) throws IOException {
        try {
            solrClient.deleteById(aFileName);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void shutdown() {
        try {
            solrEmbedded.shutdown();
        } catch (Exception e) {
            LOGGER.error("Error while closing IndexWriter", e);
        }
    }

    public UpdateCheckResult checkIfModified(String aFilename, long aLastModified) throws IOException {

        Map<String, Object> theParams = new HashMap<>();
        theParams.put("q", IndexFields.UNIQUEID + ":" + ClientUtils.escapeQueryChars(aFilename));

        try {
            QueryResponse theQueryResponse = solrClient.query(new SearchMapParams(theParams));
            if (theQueryResponse.getResults().isEmpty()) {
                // Nothing in Index, hence mark it as updated
                return UpdateCheckResult.UPDATED;
            }
            SolrDocument theDocument = theQueryResponse.getResults().get(0);

            long theStoredLastModified = Long.valueOf((String) theDocument.getFieldValue(IndexFields.LASTMODIFIED));
            if (theStoredLastModified != aLastModified) {
                return UpdateCheckResult.UPDATED;
            }
            return UpdateCheckResult.UNMODIFIED;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private String encode(String aValue) {
        URLCodec theURLCodec = new URLCodec();
        try {
            return theURLCodec.encode(aValue);
        } catch (EncoderException e) {
            return null;
        }
    }

    private long indexSize() throws IOException, SolrServerException {
        SolrQuery q = new SolrQuery("*:*");
        q.setRows(0);  // don't actually request any data
        return solrClient.query(q).getResults().getNumFound();
    }

    public QueryResult performQuery(String aQueryString, String aBacklink, String aBasePath, Configuration aConfiguration, Map<String, String> aDrilldownFields) throws IOException {

        Map<String, Object> theParams = new HashMap<>();
        theParams.put("q", IndexFields.CONTENT + ":" + ClientUtils.escapeQueryChars(aQueryString));
        theParams.put("rows", "" + configuration.getNumberOfSearchResults());
        theParams.put("stats", "true");
        theParams.put("stats.field", IndexFields.UNIQUEID);
        theParams.put("facet", "true");
        theParams.put("facet.field", new String[] {IndexFields.LANGUAGE, "attr_author", "attr_last-modified-year", "attr_" + IndexFields.EXTENSION});
        theParams.put("hl", "true");
        theParams.put("hl.fl", IndexFields.CONTENT);
        theParams.put("hl.snippets", "" + NUMBER_OF_FRAGMENTS);
        theParams.put("hl.fragsize", "100");

        if (aDrilldownFields != null) {
            List<String> theFilters = new ArrayList<>();
            for (Map.Entry<String, String> theField : aDrilldownFields.entrySet()) {
                theFilters.add(theField.getKey()+":"+ClientUtils.escapeQueryChars(theField.getValue()));
            }
            if (!theFilters.isEmpty()) {
                theParams.put("fq", theFilters.toArray(new String[theFilters.size()]));
            }
        }

        if (aConfiguration.isShowSimilarDocuments()) {
            theParams.put("mlt", "true");
            theParams.put("mlt.count", "5");
            theParams.put("mlt.fl", IndexFields.CONTENT);
        }

        try {
            long theStartTime = System.currentTimeMillis();
            QueryResponse theQueryResponse = solrClient.query(new SearchMapParams(theParams));

            List<QueryResultDocument> theDocuments = new ArrayList<>();
            for (int i=0;i<theQueryResponse.getResults().size();i++) {
                SolrDocument theSolrDocument = theQueryResponse.getResults().get(i);

                String theFileName  = (String) theSolrDocument.getFieldValue(IndexFields.UNIQUEID);
                long theStoredLastModified = Long.valueOf((String) theSolrDocument.getFieldValue(IndexFields.LASTMODIFIED));

                int theNormalizedScore = 5;
                StringBuffer theHighlight = new StringBuffer();
                Map<String, List<String>> theHighlightPhrases = theQueryResponse.getHighlighting().get(theFileName);
                if (theHighlightPhrases != null) {
                    List<String> theContentSpans = theHighlightPhrases.get(IndexFields.CONTENT);
                    if (theContentSpans != null) {
                        for (String thePhrase : theContentSpans) {
                            if (theHighlight.length() > 0) {
                                theHighlight.append(" ... ");
                            }
                            theHighlight.append(thePhrase);
                        }
                    } else {
                        LOGGER.warn("No highligting for " + theFileName);
                    }
                }

                File theFileOnDisk = new File(theFileName);
                if (theFileOnDisk.exists()) {

                    boolean thePreviewAvailable = previewProcessor.previewAvailableFor(theFileOnDisk);

                    QueryResultDocument theDocument = new QueryResultDocument(i, theFileName, theHighlight.toString(),
                            theStoredLastModified, theNormalizedScore, theFileName, thePreviewAvailable);

                    if (configuration.isShowSimilarDocuments()) {
                        SolrDocumentList theMoreLikeThisDocuments = theQueryResponse.getMoreLikeThis().get(theFileName);
                        if (theMoreLikeThisDocuments != null) {
                            for (int j = 0; j < theMoreLikeThisDocuments.size(); j++) {
                                SolrDocument theMLt = theMoreLikeThisDocuments.get(j);
                                theDocument.addSimilarFile(((String) theMLt.getFieldValue(IndexFields.UNIQUEID)));
                            }
                        }
                    }

                    theDocuments.add(theDocument);

                } else {

                    // Document can be deleted, as it is no longer on the hard drive
                    solrClient.deleteById(theFileName);
                }
            }

            long theIndexSize = indexSize();

            long theDuration = System.currentTimeMillis() - theStartTime;

            List<FacetDimension> theDimensions = new ArrayList<>();
            fillFacet(IndexFields.LANGUAGE, "Language", aBasePath, theQueryResponse, theDimensions, t -> SupportedLanguage.valueOf(t).toLocale().getDisplayName());
            fillFacet("attr_author", "Author", aBasePath, theQueryResponse, theDimensions, t -> t);
            fillFacet("attr_last-modified-yea", "Last modified", aBasePath, theQueryResponse, theDimensions, t -> t);
            fillFacet("attr_" + IndexFields.EXTENSION, "File type", aBasePath, theQueryResponse, theDimensions, t -> t);

            return new QueryResult(theDuration, theDocuments, theDimensions, theIndexSize, aBacklink);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void fillFacet(String aFacetField, String aFacetDisplayLabel, String aBacklink, QueryResponse aQueryResponse, List<FacetDimension> aDimensions,
            Function<String, String> aConverter) {
        FacetField theFacet = aQueryResponse.getFacetField(aFacetField);
        if (theFacet != null) {
            List<Facet> theFacets = new ArrayList<>();
            for (FacetField.Count theCount : theFacet.getValues()) {
                if (theCount.getCount() > 0) {
                    String theName = theCount.getName().trim();
                    if (theName.length() > 0) {
                        theFacets.add(new Facet(aConverter.apply(theName), theCount.getCount(),
                                aBacklink + "/" + encode(
                                        FacetSearchUtils.encode(aFacetField, theCount.getName()))));
                    }
                }
            }
            if (!theFacets.isEmpty()) {
                aDimensions.add(new FacetDimension(aFacetDisplayLabel, theFacets));
            }
        }
    }

    public Suggestion[] findSuggestionTermsFor(String aTerm) throws IOException {
/*
        searcherManager.maybeRefreshBlocking();
        IndexSearcher theSearcher = searcherManager.acquire();

        try {
            SearchPhraseSuggester theSuggester = new SearchPhraseSuggester(theSearcher, analyzer, configuration);
            List<Suggestion> theResult = theSuggester.suggestSearchPhrase(IndexFields.CONTENT_NOT_STEMMED, aTerm);

            return theResult.toArray(new Suggestion[theResult.size()]);

        } finally {
            searcherManager.release(theSearcher);
        }*/
        return new Suggestion[0];
    }

    public File getFileOnDiskForDocument(String aUniqueID) throws IOException {
        return new File(aUniqueID);
    }
}