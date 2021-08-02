package io.best.tool.service;

import org.apache.commons.io.FileUtils;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class TwigService {

    private static void moveKlineAndRefresh(final Long exchangeId, final String source, final String target) throws IOException {
        refreshKlineCache(exchangeId, new HashSet<>(Collections.singletonList(target)));
        moveKline(exchangeId, source, target);
    }

    private static void moveKline(final long exchangeId, final String source, final String target) throws IOException {
        final JtwigTemplate template = JtwigTemplate.fileTemplate("/Users/fish/tool/tool/src/main/resources/move_kline.twig");
        final JtwigModel model = JtwigModel.newModel();
        final Long now = System.currentTimeMillis();
        final File outputFile = new File("/Users/fish/tool/move_kline_" + System.currentTimeMillis() + ".txt");
        model.with("exchangeId", exchangeId);
        model.with("source", source);
        model.with("target", target);
        FileUtils.write(outputFile, template.render(model), StandardCharsets.UTF_8, true);
    }
    
    private static void refreshKlineCache(final Long exchangeId, final Set<String> symbolSet) throws IOException {
        final JtwigTemplate template = JtwigTemplate.fileTemplate("/Users/fish/tool/tool/src/main/resources/refresh_kline_cache.twig");
        final JtwigModel model = JtwigModel.newModel();
        final Long now = System.currentTimeMillis();
        final File outputFile = new File("/Users/fish/tool/refresh_kline_cache_" + System.currentTimeMillis() + ".txt");
        for (final String symbol : symbolSet) {
            for (final KlineTypes value : KlineTypes.values()) {
                model.with("exchangeId", exchangeId);
                model.with("symbol", symbol);
                model.with("interval", value.getInterval());
                model.with("from", 0);
                model.with("to", now);
                FileUtils.write(outputFile, template.render(model), Charset.forName("UTF-8"), true);
            }
        }
    }

    public static void main(String[] args) throws Exception{
        moveKlineAndRefresh(301l,"MDX8USDT","MDXUSDT");
    }
}
