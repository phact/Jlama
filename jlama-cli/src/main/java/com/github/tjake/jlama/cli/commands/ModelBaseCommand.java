package com.github.tjake.jlama.cli.commands;


import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import picocli.CommandLine.*;

public class ModelBaseCommand extends BaseCommand {
    @Option(names = {"-p", "--prompt"}, description = "Text to complete", required = true)
    protected String prompt;

    @Option(names={"-t", "--temperature"}, description = "Temperature of response [0,1] (default: ${DEFAULT-VALUE})", defaultValue = "0.6")
    protected Float temperature;

    @Option(names={"--top-p"}, description = "Controls how many different words the model considers per token [0,1] (default: ${DEFAULT-VALUE})", defaultValue = ".9")
    protected Float topp;

    @Option(names={"-n", "--tokens"}, description = "Number of tokens to generate (default: ${DEFAULT-VALUE})", defaultValue = "256")
    protected Integer tokens;

    protected BiConsumer<String, Float> makeOutHandler() {
        PrintWriter out;
        Charset utf8 = Charset.forName("UTF-8");
        BiConsumer<String, Float> outCallback;
        if (System.console() == null) {
            AtomicInteger i = new AtomicInteger(0);
            StringBuilder b = new StringBuilder();
            out = new PrintWriter(System.out, true, utf8);
            outCallback = (w,t) ->  {
                b.append(w);
                out.println(String.format("%d: %s [took %.2fms])", i.getAndIncrement(), b, t));
                out.flush();
            };
        } else {
            out = System.console().writer();
            outCallback = (w,t) -> {
                out.print(w);
                out.flush();
            };
        }

        return outCallback;
    }
}
