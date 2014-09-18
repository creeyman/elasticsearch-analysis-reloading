package com.hichao.elasticsearch.rest;

import java.util.List;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import com.hichao.elasticsearch.analysis.synonym.ReloadableSynonymTokenFilter;

public class ReloadReloadableSynonymTokenFilterHandlerRestAction extends BaseRestHandler {

    @Inject
    protected ReloadReloadableSynonymTokenFilterHandlerRestAction(Settings settings, Client client, RestController restController, IndicesService indicesService) {
        super(settings, client);

        restController.registerHandler(RestRequest.Method.GET, "/_synonyms/reload", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel,
            Client client) throws Exception {

        try {

            List<ReloadableSynonymTokenFilter> reloadableSynonymTokenFilters = ReloadableSynonymTokenFilter.getInstances();
            for (ReloadableSynonymTokenFilter synonymFilter : reloadableSynonymTokenFilters) {
                synonymFilter.reloadSynonymMap();
            }

            XContentBuilder builder = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("ok", true)
                    .endObject();

            channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));

        } catch (Exception e) {

            XContentBuilder builder = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("ok", false)
                    .field("reason", e.getMessage())
                    .endObject();

            channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, builder));
        }

    }
}
