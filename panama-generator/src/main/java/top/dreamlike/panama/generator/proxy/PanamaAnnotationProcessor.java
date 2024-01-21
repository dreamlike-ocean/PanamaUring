package top.dreamlike.panama.generator.proxy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import top.dreamlike.panama.generator.annotation.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.util.*;


@SupportedAnnotationTypes("top.dreamlike.panama.generator.annotation.CompileTimeGenerate")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions(PanamaAnnotationProcessor.indy_enable_option)
public class PanamaAnnotationProcessor extends AbstractProcessor {
    final static String indy_enable_option = "indy.enable";
    MethodHandles.Lookup lookup;
    private ProcessingEnvironment env;
    private boolean enableIndy = false;
    private ByteBuddy byteBuddy = new ByteBuddy();
    private StructProxyGenerator structProxyGenerator;
    private NativeCallGenerator nativeCallGenerator;
    private Map<String, Class> classMap = new HashMap<>(
            Map.of(
                    MemorySegment.class.getCanonicalName(), MemorySegment.class,
                    NativeArray.class.getCanonicalName(), NativeArray.class
            )
    );

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.env = processingEnv;
        this.enableIndy = Boolean.parseBoolean(env.getOptions().getOrDefault(indy_enable_option, "false"));
        this.structProxyGenerator = new StructProxyGenerator();
        this.structProxyGenerator.beforeGenerateCallBack = unloaded -> {
            String className = unloaded.getTypeDescription().getName();
            try (var ouputStream = this.env.getFiler().createClassFile(className).openOutputStream()) {
                byte[] bytes = unloaded.getBytes();
                ouputStream.write(bytes);
                ouputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        this.nativeCallGenerator = new NativeCallGenerator(structProxyGenerator);
        if (enableIndy) {
            nativeCallGenerator.indyMode();
        } else {
            nativeCallGenerator.plainMode();
        }

        try {
            this.lookup = MethodHandles.privateLookupIn(PanamaAnnotationProcessor.class, MethodHandles.lookup());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(CompileTimeGenerate.class);
            for (Element element : elements) {
                if (!(element instanceof TypeElement)) {
                    continue;
                }
                TypeElement typeElement = (TypeElement) element;
                boolean isInterface = typeElement.getKind().isInterface();
                Class runtimeClass = toRuntiumeClass(typeElement.asType());
                if (isInterface) {
                    nativeCallGenerator.generate(runtimeClass);
                } else {
                    structProxyGenerator.enhance(runtimeClass);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return false;
    }


    private Class toRuntiumeClass(TypeMirror mirror) {
        if (mirror.getKind() == TypeKind.VOID) {
            return void.class;
        }

        if (mirror.getKind().isPrimitive()) {
            return switch (mirror.getKind()) {
                case BOOLEAN -> boolean.class;
                case BYTE -> byte.class;
                case SHORT -> short.class;
                case INT -> int.class;
                case LONG -> long.class;
                case CHAR -> char.class;
                case FLOAT -> float.class;
                case DOUBLE -> double.class;
                default -> null;
            };
        }
        TypeElement currentTypeElement = (TypeElement) env.getTypeUtils().asElement(mirror);
        if (mirror.getKind() != TypeKind.DECLARED) {
            throw new IllegalStateException("dont support other type!" + "current kind is: " + mirror.getKind() + ", in " + currentTypeElement);
        }

        Class aClass = classMap.get(currentTypeElement.getQualifiedName().toString());
        if (aClass != null) {
            return aClass;
        }


        List<VariableElement> fields = ElementFilter.fieldsIn(currentTypeElement.getEnclosedElements());
        //这里不在乎什么继承啥的 只需要解析内部结构数据就行了
        boolean isInterface = currentTypeElement.getKind().isInterface();
        DynamicType.Builder builder;
        if (isInterface) {
            builder = byteBuddy.makeInterface()
                    .name(currentTypeElement.getQualifiedName().toString());
        } else {
            builder = byteBuddy
                    .subclass(Object.class)
                    .name(currentTypeElement.getQualifiedName().toString());
        }
        builder = Optional.ofNullable(currentTypeElement.getAnnotation(Alignment.class))
                .map(builder::annotateType)
                .orElse(builder);

        builder = Optional.ofNullable(currentTypeElement.getAnnotation(CLib.class))
                .map(builder::annotateType)
                .orElse(builder);

        builder = Optional.ofNullable(currentTypeElement.getAnnotation(Union.class))
                .map(builder::annotateType)
                .orElse(builder);
        for (VariableElement field : fields) {
            var fieldDefinition = builder.defineField(field.getSimpleName().toString(), toRuntiumeClass(field.asType()), calModifier(field.getModifiers()));
            Collection<AnnotationDescription> annotationDescriptions = new ArrayList<>();
            NativeArrayMark nativeArrayMark = field.getAnnotation(NativeArrayMark.class);
            if (nativeArrayMark != null) {
                Class size;
                try {
                    size = nativeArrayMark.size();
                } catch (MirroredTypeException exception) {
                    TypeMirror typeMirror = exception.getTypeMirror();
                    size = toRuntiumeClass(typeMirror);
                }
                TypeMirror sizeOfType = env.getElementUtils().getTypeElement(size.getCanonicalName()).asType();
                annotationDescriptions.add(AnnotationDescription.Builder.ofType(NativeArrayMark.class)
                        .define("size", toRuntiumeClass(sizeOfType))
                        .define("length", nativeArrayMark.length())
                        .define("asPointer", nativeArrayMark.asPointer())
                        .build());
            }
            Optional.ofNullable(field.getAnnotation(Pointer.class))
                    .map(p -> AnnotationDescription.Builder.ofType(Pointer.class).build())
                    .ifPresent(annotationDescriptions::add);
            Optional.ofNullable(field.getAnnotation(Union.class))
                    .map(p -> AnnotationDescription.Builder.ofType(Union.class).build())
                    .ifPresent(annotationDescriptions::add);
            builder = fieldDefinition.annotateField(annotationDescriptions)
                    .annotateType(new Annotation[0]);
        }

        if (isInterface) {
            List<ExecutableElement> executableElements = ElementFilter.methodsIn(currentTypeElement.getEnclosedElements());
            for (ExecutableElement executableElement : executableElements) {
                //不需要解析default直接跳过
                if (executableElement.getModifiers().contains(Modifier.DEFAULT)) {
                    continue;
                }
                Class returnClass = toRuntiumeClass(executableElement.getReturnType());
                DynamicType.Builder.MethodDefinition.ParameterDefinition methodBuilder = builder
                        .defineMethod(executableElement.getSimpleName().toString(), returnClass, calModifier(executableElement.getModifiers()));
                List<? extends VariableElement> parameters = executableElement.getParameters();

                for (VariableElement parameter : parameters) {
                    Pointer pointer = parameter.getAnnotation(Pointer.class);
                    if (pointer != null) {
                        methodBuilder = methodBuilder
                                .withParameter(toRuntiumeClass(parameter.asType()), parameter.getSimpleName().toString())
                                .annotateParameter(pointer);
                    } else {
                        methodBuilder = methodBuilder
                                .withParameter(toRuntiumeClass(parameter.asType()), parameter.getSimpleName().toString());
                    }
                }

                NativeFunction nativeFunction = executableElement.getAnnotation(NativeFunction.class);
                if (nativeFunction != null) {
                    builder = methodBuilder.withoutCode().annotateMethod(nativeFunction);
                } else {
                    builder = methodBuilder.withoutCode();
                }
            }
        }
        try {
            DynamicType.Unloaded unloaded = builder.make();
            unloaded.saveIn(new File("apt-generator"));
            var c = unloaded.load(PanamaAnnotationProcessor.class.getClassLoader(), new ClassLoadingStrategy.ForUnsafeInjection(this.getClass().getProtectionDomain()))
                    .getLoaded();
            classMap.put(currentTypeElement.getQualifiedName().toString(), c);
            return c;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private int calModifier(Set<Modifier> modifiers) {
        int modifier = 0;
        for (Modifier m : modifiers) {
            modifier |=
                    switch (m) {
                        case PUBLIC -> java.lang.reflect.Modifier.PUBLIC;
                        case FINAL -> java.lang.reflect.Modifier.FINAL;
                        case PRIVATE -> java.lang.reflect.Modifier.PRIVATE;
                        case PROTECTED -> java.lang.reflect.Modifier.PROTECTED;

                        case STATIC -> java.lang.reflect.Modifier.STATIC;
                        case SYNCHRONIZED -> java.lang.reflect.Modifier.SYNCHRONIZED;
                        case VOLATILE -> java.lang.reflect.Modifier.VOLATILE;
                        case TRANSIENT -> java.lang.reflect.Modifier.TRANSIENT;
                        case NATIVE -> java.lang.reflect.Modifier.NATIVE;
                        case ABSTRACT -> java.lang.reflect.Modifier.ABSTRACT;
                        case STRICTFP -> java.lang.reflect.Modifier.STRICT;
                        //这有点复杂 所以忽略直接返回0就好了 不影响我解析
                        case DEFAULT, NON_SEALED, SEALED -> 0;
                    };
        }
        return modifier;
    }


}
