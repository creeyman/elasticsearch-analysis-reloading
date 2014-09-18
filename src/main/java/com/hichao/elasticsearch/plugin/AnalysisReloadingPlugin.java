package com.hichao.elasticsearch.plugin;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;

import com.hichao.elasticsearch.analysis.ik.IkAnalysisBinderProcessor;
import com.hichao.elasticsearch.analysis.synonym.ReloadableSynonymTokenFilterFactory;
import com.hichao.elasticsearch.rest.ListReloadablesHandlerRestAction;
import com.hichao.elasticsearch.rest.ReloadReloadablesHandlerRestAction;

public class AnalysisReloadingPlugin extends AbstractPlugin {
    public String name() {  
        return "analysis-reloading";  
      }  
      
      public String description() {
          return "analysis-reloading";
      }
      
      @Override public void processModule(Module module) {  
        if (module instanceof AnalysisModule) {  
           
            ((AnalysisModule) module).addTokenFilter("reloadable_synonym", ReloadableSynonymTokenFilterFactory.class); 
            ((AnalysisModule) module).addProcessor(new IkAnalysisBinderProcessor());

        }  
        
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(ListReloadablesHandlerRestAction.class);
            ((RestModule) module).addRestAction(ReloadReloadablesHandlerRestAction.class);
        }
      } 
}
