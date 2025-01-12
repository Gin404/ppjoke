package com.example.libnavcompiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPObject;
import com.example.libnavannotation.ActivityDestination;
import com.example.libnavannotation.FragmentDestination;
import com.google.auto.service.AutoService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"com.example.libnavannotation.FragmentDestination",
        "com.example.libnavannotation.ActivityDestination"})
public class NavProcessor extends AbstractProcessor {
    private static final String OUTPUT_FILE_NAME = "destination.json";

    private Messager messager;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        messager.printMessage(Diagnostic.Kind.NOTE, "init  ");
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> fragmentElements = roundEnvironment.getElementsAnnotatedWith(FragmentDestination.class);
        Set<? extends Element> activityElements = roundEnvironment.getElementsAnnotatedWith(ActivityDestination.class);
        if (!fragmentElements.isEmpty() || !activityElements.isEmpty()) {
            HashMap<String, JSONObject> destMap = new HashMap<>();
            handleDestination(fragmentElements, FragmentDestination.class, destMap);
            handleDestination(activityElements, ActivityDestination.class, destMap);

            FileOutputStream fos = null;
            OutputStreamWriter writer = null;
            //app/src/main/assets
            try {
                FileObject resource
                        = filer.createResource(StandardLocation.CLASS_OUTPUT, "", OUTPUT_FILE_NAME);
                String resourcePath = resource.toUri().getPath();
                messager.printMessage(Diagnostic.Kind.NOTE, "resource path: " + resourcePath);
                String appPath = resourcePath.substring(0, resourcePath.indexOf("app") + 4);
                String assetsPath = appPath + "src/main/assets/";
                File file = new File(assetsPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                File outputFile = new File(file, OUTPUT_FILE_NAME);
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                outputFile.createNewFile();
                String content = JSON.toJSONString(destMap);

                fos =  new FileOutputStream(outputFile);
                writer = new OutputStreamWriter(fos, "UTF-8");
                writer.write(content);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (writer!=null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return true;
    }

    private void handleDestination(Set<? extends Element> fragmentElements,
                                   Class<? extends Annotation> annotationClazz,
                                   HashMap<String, JSONObject> destMap) {
        for (Element element: fragmentElements) {
            TypeElement typeElement = (TypeElement) element;
            String pageUrl = null;
            String clazzName = typeElement.getQualifiedName().toString();
            messager.printMessage(Diagnostic.Kind.NOTE, "process: " + clazzName + "  \n\n");
            int id = Math.abs(clazzName.hashCode());
            boolean needLogin = false;
            boolean asStarter = false;
            boolean isFragment = false;

            Annotation annotation = typeElement.getAnnotation(annotationClazz);
            if (annotation instanceof FragmentDestination) {
                FragmentDestination dest = (FragmentDestination) annotation;
                pageUrl = dest.pageUrl();
                asStarter = dest.asStarter();
                needLogin = dest.needLogin();
                isFragment = true;
            } else if (annotation instanceof ActivityDestination) {
                ActivityDestination dest = (ActivityDestination) annotation;
                pageUrl = dest.pageUrl();
                asStarter = dest.asStarter();
                needLogin = dest.needLogin();
                isFragment = false;
            }

            if (destMap.containsKey(pageUrl)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "不同的页面不允许使用相同的pageUrl");
            } else {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", id);
                jsonObject.put("asStarter", asStarter);
                jsonObject.put("needLogin", needLogin);
                jsonObject.put("className", clazzName);
                jsonObject.put("pageUrl", pageUrl);
                jsonObject.put("isFragment", isFragment);
                destMap.put(pageUrl, jsonObject);
            }
        }
    }
}
