package com.hichao.elasticsearch.rest;

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

import com.hichao.elasticsearch.analysis.reloading.Reloadable;
import com.hichao.elasticsearch.analysis.reloading.Reloadables;

public class ListReloadablesHandlerRestAction extends BaseRestHandler {

    @Inject
    protected ListReloadablesHandlerRestAction(Settings settings, Client client, RestController restController, IndicesService indicesService) {
        super(settings, client);

        restController.registerHandler(RestRequest.Method.GET, "/_reloadables/list", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel,
            Client client) throws Exception {

        try {

            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            builder.field("ok", true);
            builder.startArray("reloadables");
            for (Reloadable reloadable : Reloadables.getReloadables()) {
                builder.startObject();
                builder.field("type", reloadable.getClass().getName());
                builder.endObject();
            }
            builder.endArray();
            builder.endObject();

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

    //    private List<Analyzer> getAnalyzers(String analyzerName){
    //        List<Analyzer> analyzers = new ArrayList<Analyzer>();
    //        Iterator<IndexService> a = indicesService.iterator();
    //        while (a.hasNext()) {
    //            IndexService indexService = (IndexService) a.next();
    //            Analyzer analyzer = indexService.analysisService().analyzer(analyzerName).analyzer();
    //            if (!analyzers.contains(analyzer)){
    //                analyzers.add(analyzer);
    //            }
    //        }
    //        return analyzers;
    //    }


}
