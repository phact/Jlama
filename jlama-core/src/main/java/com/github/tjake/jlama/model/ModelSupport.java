package com.github.tjake.jlama.model;

import com.github.tjake.jlama.model.bert.BertConfig;
import com.github.tjake.jlama.model.bert.BertModel;
import com.github.tjake.jlama.model.bert.BertTokenizer;
import com.github.tjake.jlama.model.gemma.GemmaConfig;
import com.github.tjake.jlama.model.gemma.GemmaModel;
import com.github.tjake.jlama.model.gemma.GemmaTokenizer;
import com.github.tjake.jlama.model.gpt2.GPT2Config;
import com.github.tjake.jlama.model.gpt2.GPT2Model;
import com.github.tjake.jlama.model.gpt2.GPT2Tokenizer;
import com.github.tjake.jlama.model.llama.LlamaConfig;
import com.github.tjake.jlama.model.llama.LlamaModel;
import com.github.tjake.jlama.model.llama.LlamaTokenizer;
import com.github.tjake.jlama.model.mistral.MistralConfig;
import com.github.tjake.jlama.model.mistral.MistralModel;
import com.github.tjake.jlama.model.mixtral.MixtralConfig;
import com.github.tjake.jlama.model.mixtral.MixtralModel;
import com.github.tjake.jlama.safetensors.Config;
import com.github.tjake.jlama.safetensors.DType;
import com.github.tjake.jlama.safetensors.SafeTensorSupport;
import com.github.tjake.jlama.safetensors.WeightLoader;
import com.github.tjake.jlama.safetensors.tokenizer.Tokenizer;
import com.github.tjake.jlama.util.Pair;
import com.github.tjake.jlama.util.PhysicalCoreExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;

import static com.github.tjake.jlama.util.JsonSupport.om;

public class ModelSupport {
    private static final Logger logger = LoggerFactory.getLogger(ModelSupport.class);


    public enum ModelType {
        GEMMA(GemmaModel.class, GemmaConfig.class, GemmaTokenizer.class),
        MISTRAL(MistralModel.class, MistralConfig.class, LlamaTokenizer.class),
        MIXTRAL(MixtralModel.class, MixtralConfig.class, LlamaTokenizer.class),
        LLAMA(LlamaModel.class, LlamaConfig.class, LlamaTokenizer.class),
        GPT2(GPT2Model.class, GPT2Config.class, GPT2Tokenizer.class),
        BERT(BertModel.class, BertConfig.class, BertTokenizer.class);

        public final Class<? extends AbstractModel> modelClass;
        public final  Class<? extends Config> configClass;
        public final Class<? extends Tokenizer> tokenizerClass;

        ModelType(Class<? extends AbstractModel> modelClass,
             Class<? extends Config> configClass,
             Class<? extends Tokenizer> tokenizerClass) {

            this.modelClass = modelClass;
            this.configClass = configClass;
            this.tokenizerClass = tokenizerClass;
        }
    }

    public static AbstractModel loadModel(File model, File workingDirectory, DType workingMemoryType, DType workingQuantizationType, Optional<DType> modelQuantization, Optional<Integer> threadCount) {
        return loadModel(AbstractModel.InferenceType.FULL_GENERATION, model, workingDirectory, workingMemoryType, workingQuantizationType, modelQuantization, threadCount, Optional.empty());
    }

    public static AbstractModel loadModel(AbstractModel.InferenceType inferenceType, File model, File workingDirectory, DType workingMemoryType, DType workingQuantizationType, Optional<DType> modelQuantization, Optional<Integer> threadCount, Optional<Pair<Integer, Integer>> offset) {

        if (!model.exists()) {
           throw new IllegalArgumentException("Model location does not exist: " + model);
        }

        File baseDir = model.isFile() ? model.getParentFile() : model;

        //Find config
        if (!baseDir.isDirectory()) {
            throw new IllegalArgumentException("Model directory does not exist: " + baseDir);
        }

        File configFile = null;
        for (File f : Objects.requireNonNull(baseDir.listFiles())) {
            if (f.getName().equals("config.json")) {
                configFile = f;
                break;
            }
        }

        if (configFile == null) {
            throw new IllegalArgumentException("config.json in model directory does not exist: " + baseDir);
        }

        try {
            threadCount.ifPresent(PhysicalCoreExecutor::overrideThreadCount);

            ModelSupport.ModelType modelType = SafeTensorSupport.detectModel(configFile);
            Config c = om.readValue(configFile, modelType.configClass);
            offset.ifPresent(c::setOffset);

            c.setWorkingDirectory(workingDirectory);

            Tokenizer t = modelType.tokenizerClass.getConstructor(Path.class).newInstance(baseDir.toPath());
            WeightLoader wl = SafeTensorSupport.loadWeights(baseDir);

            return modelType.modelClass.getConstructor(AbstractModel.InferenceType.class, Config.class, WeightLoader.class, Tokenizer.class, DType.class, DType.class, Optional.class)
                    .newInstance(inferenceType, c, wl, t, workingMemoryType, workingQuantizationType, modelQuantization);

        } catch (IOException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
