function processAdd(cmd) {

    doc = cmd.solrDoc;  // org.apache.solr.common.SolrInputDocument

    id = doc.getFieldValue("id");
    logger.info("update-script#processAdd: id=" + id);

}

function processDelete(cmd) {
    // no-op
}

function processMergeIndexes(cmd) {
    // no-op
}

function processCommit(cmd) {
    // no-op
}

function processRollback(cmd) {
    // no-op
}

function finish() {
    // no-op
}