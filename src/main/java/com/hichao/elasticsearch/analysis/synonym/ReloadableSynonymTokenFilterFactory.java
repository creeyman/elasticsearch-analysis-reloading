package com.hichao.elasticsearch.analysis.synonym;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.io.FastStringReader;
import org.elasticsearch.common.lucene.Lucene;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.*;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;

import com.hichao.elasticsearch.analysis.reloading.Reloadables;

import java.io.Reader;
import java.util.List;
import java.util.Map;

@AnalysisSettingsRequired
public class ReloadableSynonymTokenFilterFactory extends AbstractTokenFilterFactory {
    
    private boolean ignoreCase;
    private SynonymMapBuilder builder;
    private SynonymMap synonymMap;

    @Inject
    public ReloadableSynonymTokenFilterFactory(Index index, @IndexSettings Settings indexSettings, Environment env, IndicesAnalysisService indicesAnalysisService, Map<String, TokenizerFactoryFactory> tokenizerFactories,
                                     @Assisted String name, @Assisted Settings settings) {
        super(index, indexSettings, name, settings);
        
        this.ignoreCase = settings.getAsBoolean("ignore_case", false);

        SynonymMapBuilder builder = new SynonymMapBuilder();
        builder.setSettings(settings);
        builder.setEnvironment(env);
        builder.setIndicesAnalysisService(indicesAnalysisService);
        builder.setTokenizerFactoryFactories(tokenizerFactories);
        builder.setIgnoreCase(this.ignoreCase);
        this.builder = builder;
        this.synonymMap = builder.build();
    }
   
    public static class SynonymMapBuilder{
        
        private Settings settings;
        private Environment env;
        private IndicesAnalysisService indicesAnalysisService;
        private Map<String, TokenizerFactoryFactory> tokenizerFactories;
        private boolean ignoreCase;

        public SynonymMapBuilder(){
            
        }
        
        public void setIgnoreCase(boolean ignoreCase) {
            this.ignoreCase = ignoreCase;
            
        }

        public void setTokenizerFactoryFactories(
                Map<String, TokenizerFactoryFactory> tokenizerFactories) {
            this.tokenizerFactories = tokenizerFactories;
            
        }

        public void setIndicesAnalysisService(
                IndicesAnalysisService indicesAnalysisService) {
            this.indicesAnalysisService = indicesAnalysisService;
            
        }

        public void setEnvironment(Environment env) {
            this.env = env;
        }

        public void setSettings(Settings settings) {
            this.settings = settings;
        }

        public SynonymMap build(){
            
            SynonymMap synonymMap = null;
            
            Reader rulesReader = null;
            if (settings.getAsArray("synonyms", null) != null) {
                List<String> rules = Analysis.getWordList(env, settings, "synonyms");
                StringBuilder sb = new StringBuilder();
                for (String line : rules) {
                    sb.append(line).append(System.getProperty("line.separator"));
                }
                rulesReader = new FastStringReader(sb.toString());
            } else if (settings.get("synonyms_path") != null) {
                rulesReader = Analysis.getReaderFromFile(env, settings, "synonyms_path");
            } else {
                throw new ElasticsearchIllegalArgumentException("synonym requires either `synonyms` or `synonyms_path` to be configured");
            }
            
            boolean expand = settings.getAsBoolean("expand", true);

            String tokenizerName = settings.get("tokenizer", "whitespace");

            TokenizerFactoryFactory tokenizerFactoryFactory = tokenizerFactories.get(tokenizerName);
            if (tokenizerFactoryFactory == null) {
                tokenizerFactoryFactory = indicesAnalysisService.tokenizerFactoryFactory(tokenizerName);
            }
            if (tokenizerFactoryFactory == null) {
                throw new ElasticsearchIllegalArgumentException("failed to find tokenizer [" + tokenizerName + "] for synonym token filter");
            }
            final TokenizerFactory tokenizerFactory = tokenizerFactoryFactory.create(tokenizerName, settings);

            Analyzer analyzer = new Analyzer() {
                @Override
                protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
                    Tokenizer tokenizer = tokenizerFactory == null ? new WhitespaceTokenizer(Lucene.ANALYZER_VERSION, reader) : tokenizerFactory.create(reader);
                    @SuppressWarnings("resource")
                    TokenStream stream = ignoreCase ? new LowerCaseFilter(Lucene.ANALYZER_VERSION, tokenizer) : tokenizer;
                    return new TokenStreamComponents(tokenizer, stream);
                }
            };

            try {
                SynonymMap.Builder parser = null;

                if ("wordnet".equalsIgnoreCase(settings.get("format"))) {
                    parser = new WordnetSynonymParser(true, expand, analyzer);
                    ((WordnetSynonymParser) parser).parse(rulesReader);
                } else {
                    parser = new SolrSynonymParser(true, expand, analyzer);
                    ((SolrSynonymParser) parser).parse(rulesReader);
                }

                synonymMap = parser.build();
                
            } catch (Exception e) {
                throw new ElasticsearchIllegalArgumentException("failed to build synonyms", e);
            }
            
            return synonymMap;
        }
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        ReloadableSynonymTokenFilter tokenFilter =  new ReloadableSynonymTokenFilter(tokenStream, synonymMap, builder, ignoreCase);
        Reloadables.register(tokenFilter);
        return tokenFilter;
    }
}
