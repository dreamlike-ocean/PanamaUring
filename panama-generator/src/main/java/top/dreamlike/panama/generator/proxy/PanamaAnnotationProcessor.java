
package top.dreamlike.panama.generator.proxy;

import top.dreamlike.panama.generator.annotation.*;
import top.dreamlike.panama.generator.exception.StructException;
import top.dreamlike.panama.generator.helper.ClassFileHelper;
import top.dreamlike.panama.generator.helper.NativeGeneratorHelper;
import top.dreamlike.panama.generator.helper.NativeStructEnhanceMark;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.*;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleParameterAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AccessFlag;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@SupportedAnnotationTypes("top.dreamlike.panama.generator.annotation.CompileTimeGenerate")
@SupportedSourceVersion(SourceVersion.RELEASE_22)
@SupportedOptions({PanamaAnnotationProcessor.INDY_ENABLE, PanamaAnnotationProcessor.GENERATE_GRAAL_FEATURE_ENABLE, PanamaAnnotationProcessor.GENERATE_GRAAL_FEATURE_NAME})
public class PanamaAnnotationProcessor extends AbstractProcessor {
    final static String INDY_ENABLE = "indy.enable";

    final static String GENERATE_GRAAL_FEATURE_ENABLE = "generate.graal.feature.enable";

    final static String GENERATE_GRAAL_FEATURE_NAME = "generate.graal.feature.name";

    private ProcessingEnvironment env;
    private boolean enableIndy;

    private boolean enableGraalFeature;

    private String packageName;

    private String className;

    private String featureName;

    private StructProxyGenerator structProxyGenerator;
    private NativeCallGenerator nativeCallGenerator;
    private ArrayList<ProxyPair> proxyPair = new ArrayList<>();


    private ClassFile classFile = ClassFile.of();

    private ProxyClassLoader classLoader = new ProxyClassLoader();

    private Map<String, Class> classMap = new HashMap<>(
            Map.of(
                    MemorySegment.class.getCanonicalName(), MemorySegment.class,
                    NativeArray.class.getCanonicalName(), NativeArray.class
            )
    );

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.env = processingEnv;
        this.enableIndy = Boolean.parseBoolean(env.getOptions().getOrDefault(INDY_ENABLE, "false"));
        this.enableGraalFeature = Boolean.parseBoolean(env.getOptions().getOrDefault(GENERATE_GRAAL_FEATURE_ENABLE, "false"));
        this.featureName = env.getOptions().getOrDefault(GENERATE_GRAAL_FEATURE_NAME, "PanamaGeneratorFeature");
        this.packageName = null;
        this.className = featureName;
        int lastIndexOf = featureName.lastIndexOf(".");
        if (lastIndexOf != -1) {
            packageName = featureName.substring(0, lastIndexOf);
            className = featureName.substring(lastIndexOf + 1);
        }

        this.structProxyGenerator = new StructProxyGenerator();
        structProxyGenerator.skipInit = false;
        this.structProxyGenerator.classDataPeek = (className, bytecode) -> {
            try (var ouputStream = this.env.getFiler().createClassFile(className).openOutputStream()) {
                ouputStream.write(bytecode);
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
                CompileTimeGenerate.GenerateType generateType = element.getAnnotation(CompileTimeGenerate.class).value();

                if (!isInterface) {
                    structProxyGenerator.enhance(runtimeClass);
                    proxyPair.add(new ProxyPair(runtimeClass.getName(), structProxyGenerator.generateProxyClassName(runtimeClass), CompileTimeGenerate.GenerateType.STRUCT_PROXY));
                } else {
                    switch (generateType) {
                        case STRUCT_PROXY -> {
                            structProxyGenerator.enhance(runtimeClass);
                            proxyPair.add(new ProxyPair(runtimeClass.getName(), structProxyGenerator.generateProxyClassName(runtimeClass), CompileTimeGenerate.GenerateType.STRUCT_PROXY));
                        }
                        case SHORTCUT -> {
                            structProxyGenerator.generateShortcut(runtimeClass);
                            proxyPair.add(new ProxyPair(runtimeClass.getName(), structProxyGenerator.generatorShortcutProxyName(runtimeClass), CompileTimeGenerate.GenerateType.SHORTCUT));
                        }
                        case NATIVE_CALL -> {
                            nativeCallGenerator.generateSupplier(runtimeClass);
                            proxyPair.add(new ProxyPair(runtimeClass.getName(), nativeCallGenerator.generateProxyClassName(runtimeClass), CompileTimeGenerate.GenerateType.NATIVE_CALL));
                        }
                    }
                }

            }
            if (roundEnv.processingOver() && enableGraalFeature) {
                String generatedFeatureJsonFile = generateFeature();
                try (OutputStream outputStream = env.getFiler().createSourceFile(featureName).openOutputStream()) {
                    outputStream.write(generatedFeatureJsonFile.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
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
                case VOID -> void.class;
                default -> throw new IllegalArgumentException("should not reach here! " + mirror);
            };
        }
        
        if (mirror.getKind() == TypeKind.ARRAY) {
            ArrayType type = (ArrayType) mirror;
            TypeMirror typeComponentType = type.getComponentType();
            if (!typeComponentType.getKind().isPrimitive()) {
                throw new IllegalArgumentException("array must be primitive type");
            }
            return switch (mirror.getKind()) {
                case BYTE -> byte[].class;
                case SHORT -> short[].class;
                case INT -> int[].class;
                case LONG -> long[].class;
                case CHAR -> char[].class;
                case FLOAT -> float[].class;
                case DOUBLE -> double[].class;
                default -> throw new IllegalArgumentException("should not reach here! " + mirror);
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

        //这里不在乎什么继承啥的 只需要解析内部结构数据就行了
        boolean isInterface = currentTypeElement.getKind().isInterface();
        CompileTimeGenerate compileTimeGenerate = currentTypeElement.getAnnotation(CompileTimeGenerate.class);
        CompileTimeGenerate.GenerateType type = compileTimeGenerate.value();
        try {
            Class c = null;
            if (!isInterface) {
                c = toRuntimeClassForNativeStruct(currentTypeElement);
            } else {
                c = switch (type) {
                    case STRUCT_PROXY -> toRuntimeClassForNativeStruct(currentTypeElement);
                    case SHORTCUT -> toRuntimeClassForShortcutInterface(currentTypeElement);
                    case NATIVE_CALL -> toRuntimeClassForNativeCallInterface(currentTypeElement);
                };
            }

            return c;
        } catch (Throwable t) {
            throw new StructException("should not reach here!", t);
        }
    }

    public Class toRuntimeClassForNativeCallInterface(TypeElement currentTypeElement) throws Throwable {
        String thisClassName = getRealName(currentTypeElement);
        String thisClassDescStr = thisClassName.replace(".", "/");
        thisClassDescStr = "L" + thisClassDescStr + ";";
        ClassDesc thisClassDesc = ClassDesc.ofDescriptor(thisClassDescStr);
        byte[] classByteCode = classFile.build(thisClassDesc, classBuilder -> {
            classBuilder.withFlags(AccessFlag.PUBLIC, AccessFlag.INTERFACE, AccessFlag.ABSTRACT);
            CLib cLib = currentTypeElement.getAnnotation(CLib.class);
            if (cLib != null) {
                Annotation annotation = Annotation.of(
                        ClassFileHelper.toDesc(CLib.class),
                        AnnotationElement.of("value", AnnotationValue.ofString(cLib.value())),
                        AnnotationElement.of("inClassPath", AnnotationValue.ofBoolean(cLib.inClassPath())),
                        AnnotationElement.of("isLib", AnnotationValue.ofBoolean(cLib.isLib()))
                );
                classBuilder.with(RuntimeVisibleAnnotationsAttribute.of(annotation));
            }

            List<ExecutableElement> executableElements = ElementFilter.methodsIn(currentTypeElement.getEnclosedElements());
            for (ExecutableElement executableElement : executableElements) {
                //不需要解析default直接跳过
                if (executableElement.getModifiers().contains(Modifier.DEFAULT)) {
                    continue;
                }
                Class returnClass = toRuntiumeClass(executableElement.getReturnType());
                List<? extends VariableElement> parameters = executableElement.getParameters();
                String parametersSignature = parameters.stream()
                        .map(VariableElement::asType)
                        .map(this::toRuntiumeClass)
                        .map(ClassFileHelper::toSignature)
                        .collect(Collectors.joining());
                String returnSignature = ClassFileHelper.toSignature(returnClass);
                MethodTypeDesc methodTypeDesc = MethodTypeDesc.ofDescriptor("(" + parametersSignature + ")" + returnSignature);
                classBuilder.withMethod(executableElement.getSimpleName().toString(), methodTypeDesc, AccessFlags.ofMethod(AccessFlag.PUBLIC, AccessFlag.ABSTRACT).flagsMask(), it -> {
                    //对参数做处理
                    List<List<Annotation>> parameterAnnotation = parameters.stream()
                            .map(v -> v.getAnnotation(Pointer.class) == null ? List.<Annotation>of() : List.of(Annotation.of(ClassFileHelper.toDesc(Pointer.class))))
                            .toList();

                    it.with(RuntimeVisibleParameterAnnotationsAttribute.of(parameterAnnotation));

                    //NativeFunction处理
                    NativeFunction nativeFunction = executableElement.getAnnotation(NativeFunction.class);
                    if (nativeFunction != null) {
                        Annotation annotation = Annotation.of(
                                ClassFileHelper.toDesc(NativeFunction.class),
                                AnnotationElement.of("value", AnnotationValue.ofString(nativeFunction.value())),
                                AnnotationElement.of("fast", AnnotationValue.ofBoolean(nativeFunction.fast())),
                                AnnotationElement.of("allowPassHeap", AnnotationValue.ofBoolean(nativeFunction.allowPassHeap())),
                                AnnotationElement.of("returnIsPointer", AnnotationValue.ofBoolean(nativeFunction.returnIsPointer())),
                                AnnotationElement.of("needErrorNo", AnnotationValue.ofBoolean(nativeFunction.needErrorNo()))
                        );
                        it.with(RuntimeVisibleAnnotationsAttribute.of(annotation));
                    }

                });
            }
        });
        return define(thisClassName, classByteCode);
    }

    public Class toRuntimeClassForShortcutInterface(TypeElement currentTypeElement) throws Throwable {
        String thisClassName = getRealName(currentTypeElement);
        ClassDesc thisClassDesc = ClassFileHelper.toDesc(thisClassName);
        var bytecode = classFile.build(thisClassDesc, cb -> {
            cb.withFlags(AccessFlag.PUBLIC, AccessFlag.INTERFACE, AccessFlag.ABSTRACT);
            List<ExecutableElement> executableElements = ElementFilter.methodsIn(currentTypeElement.getEnclosedElements());
            for (ExecutableElement executableElement : executableElements) {
                if (executableElement.getModifiers().contains(Modifier.DEFAULT)) {
                    continue;
                }
                //不需要解析default直接跳过
                if (executableElement.getModifiers().contains(Modifier.DEFAULT)) {
                    continue;
                }
                Class returnClass = toRuntiumeClass(executableElement.getReturnType());
                List<? extends VariableElement> parameters = executableElement.getParameters();
                String parametersSignature = parameters.stream()
                        .map(VariableElement::asType)
                        .map(this::toRuntiumeClass)
                        .map(ClassFileHelper::toSignature)
                        .collect(Collectors.joining());
                String returnSignature = ClassFileHelper.toSignature(returnClass);
                MethodTypeDesc methodTypeDesc = MethodTypeDesc.ofDescriptor("(" + parametersSignature + ")" + returnSignature);
                cb.withMethod(executableElement.getSimpleName().toString(), methodTypeDesc, AccessFlags.ofMethod(AccessFlag.PUBLIC, AccessFlag.ABSTRACT).flagsMask(), it -> {
                    //NativeFunction处理
                    ShortcutOption option = executableElement.getAnnotation(ShortcutOption.class);
                    if (option != null) {
                        var valued = Stream.of(option.value()).map(AnnotationValue::ofString).map(c -> ((AnnotationValue) c)).toList();
                        Annotation annotation = Annotation.of(
                                ClassFileHelper.toDesc(ShortcutOption.class),
                                AnnotationElement.of("value", AnnotationValue.ofArray(valued)),
                                AnnotationElement.of("mode", AnnotationValue.ofEnum(ClassFileHelper.toDesc(VarHandle.AccessMode.class), option.mode().name())),
                                AnnotationElement.ofClass("owner", ClassFileHelper.toDesc(getAnnotationAttributeClassValue(option::owner)))
                        );
                        it.with(RuntimeVisibleAnnotationsAttribute.of(annotation));
                    }

                });


            }
        });

        return define(thisClassName, bytecode);
    }

    private Class getAnnotationAttributeClassValue(Supplier<Class> supplier) {
        try {
            return supplier.get();
        } catch (MirroredTypeException exception) {
            TypeMirror typeMirror = exception.getTypeMirror();
            return toRuntiumeClass(typeMirror);
        }
    }

    public Class toRuntimeClassForNativeStruct(TypeElement currentTypeElement) throws Throwable {
        String thisClassName = getRealName(currentTypeElement);
        String thisClassDescStr = thisClassName.replace(".", "/");
        thisClassDescStr = "L" + thisClassDescStr + ";";
        ClassDesc thisClassDesc = ClassDesc.ofDescriptor(thisClassDescStr);
        byte[] byteCode = classFile.build(thisClassDesc, classBuilder -> {
            classBuilder.withFlags(AccessFlag.PUBLIC);
            Alignment alignment = currentTypeElement.getAnnotation(Alignment.class);
            if (alignment != null) {
                Annotation annotation = Annotation.of(
                        ClassFileHelper.toDesc(Alignment.class),
                        AnnotationElement.of("byteSize", AnnotationValue.ofInt(alignment.byteSize()))
                );
                classBuilder.with(RuntimeVisibleAnnotationsAttribute.of(annotation));
            }

            Union union = currentTypeElement.getAnnotation(Union.class);
            if (union != null) {
                Annotation annotation = Annotation.of(
                        ClassFileHelper.toDesc(Union.class)
                );
                classBuilder.with(RuntimeVisibleAnnotationsAttribute.of(annotation));
            }
            List<VariableElement> fields = ElementFilter.fieldsIn(currentTypeElement.getEnclosedElements());
            for (VariableElement field : fields) {
                classBuilder.withField(field.getSimpleName().toString(), ClassFileHelper.toDesc(toRuntiumeClass(field.asType())), it -> {
                    it.withFlags(calModifier(field.getModifiers()));
                    ArrayList<Annotation> annotations = new ArrayList<>();
                    Pointer pointer = field.getAnnotation(Pointer.class);
                    if (pointer != null) {
                        Class layout;
                        try {
                            layout = pointer.targetLayout();
                        } catch (MirroredTypeException exception) {
                            TypeMirror typeMirror = exception.getTypeMirror();
                            layout = toRuntiumeClass(typeMirror);
                        }
                        annotations.add(
                                Annotation.of(
                                        ClassFileHelper.toDesc(Pointer.class),
                                        AnnotationElement.of("targetLayout", AnnotationValue.ofClass(ClassFileHelper.toDesc(layout)))
                                )
                        );
                    }
                    NativeArrayMark nativeArrayMark = field.getAnnotation(NativeArrayMark.class);
                    if (nativeArrayMark != null) {
                        Class size;
                        try {
                            size = nativeArrayMark.size();
                        } catch (MirroredTypeException exception) {
                            TypeMirror typeMirror = exception.getTypeMirror();
                            size = toRuntiumeClass(typeMirror);
                        }
                        annotations.add(
                                Annotation.of(
                                        ClassFileHelper.toDesc(NativeArrayMark.class),
                                        AnnotationElement.of("size", AnnotationValue.ofClass(ClassFileHelper.toDesc(size))),
                                        AnnotationElement.of("length", AnnotationValue.ofInt(nativeArrayMark.length())),
                                        AnnotationElement.of("asPointer", AnnotationValue.ofBoolean(nativeArrayMark.asPointer()))
                                )
                        );
                    }

                    if (field.getAnnotation(Skip.class) != null) {
                        annotations.add(
                                Annotation.of(
                                        ClassFileHelper.toDesc(Skip.class)
                                )
                        );
                    }


                    it.with(RuntimeVisibleAnnotationsAttribute.of(annotations));
                });
            }

        });
        return define(thisClassName, byteCode);
    }

    public Class define(String name, byte[] bytecode) throws Throwable {
        ClassLoader classLoader = this.classLoader;
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException _) {
        }

        dump(name, bytecode);
        return this.classLoader.defindClass(name, bytecode);
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

    private String generateDuringSetupBlock() {
        ArrayList<ProxyPair> callInterfaceNames = this.proxyPair;
        return callInterfaceNames.stream()
                .filter(p -> p.generateType == CompileTimeGenerate.GenerateType.NATIVE_CALL)
                .map(proxyPair -> "NativeImageHelper.initPanamaFeature(" + proxyPair.origin + ".class);")
                .collect(Collectors.joining("\n"));
    }

    private String generateBeforeAnalysisBlock() {
        var interfaceStream = this.proxyPair
                .stream()
                .filter(p -> p.generateType == CompileTimeGenerate.GenerateType.NATIVE_CALL || p.generateType == CompileTimeGenerate.GenerateType.SHORTCUT)
                .map(p -> String.format("""
                                needRegisterClass = Class.forName("%s", false, getClass().getClassLoader());
                                RuntimeReflection.registerFieldLookup(needRegisterClass, "_generator");
                                RuntimeReflection.registerConstructorLookup(needRegisterClass);
                                RuntimeReflection.registerMethodLookup(needRegisterClass,"<init>");
                                RuntimeReflection.register(needRegisterClass);
                                RuntimeReflection.registerForReflectiveInstantiation(needRegisterClass);
                                needRegisterClass = Class.forName("%s", false, getClass().getClassLoader());
                                RuntimeReflection.registerAllMethods(needRegisterClass);

                                RuntimeReflection.registerAllDeclaredClasses(needRegisterClass);
                              //  RuntimeClassInitialization.initializeAtBuildTime(needRegisterClass);
                        """, p.proxy, p.origin));

        var structProxyStream = this.proxyPair
                .stream()
                .filter(p -> p.generateType == CompileTimeGenerate.GenerateType.STRUCT_PROXY)
                .map(p -> String.format("""
                                needRegisterClass = Class.forName("%s", false, getClass().getClassLoader());
                              //  RuntimeReflection.registerConstructorLookup(needRegisterClass, MemorySegment.class);
                                RuntimeReflection.register(needRegisterClass);
                                RuntimeReflection.registerAllDeclaredFields(needRegisterClass);
                                needRegisterClass = Class.forName("%s", false, getClass().getClassLoader());
                                constructor = needRegisterClass.getDeclaredConstructor(MemorySegment.class);
                                RuntimeReflection.register(constructor);
                                RuntimeReflection.register(needRegisterClass);
                                RuntimeReflection.registerAllDeclaredConstructors(needRegisterClass);
                              //  RuntimeClassInitialization.initializeAtBuildTime(needRegisterClass);
                        """, p.origin, p.proxy));

        var baseStream = Stream.of(NativeStructEnhanceMark.class, NativeGeneratorHelper.class)
                .map(Class::getName)
                .map(name -> String.format("""
                        needRegisterClass = Class.forName("%s", false, getClass().getClassLoader());
                         RuntimeReflection.registerAllMethods(needRegisterClass);
                        """, name));
        return Stream.of(baseStream, interfaceStream, structProxyStream)
                .flatMap(Function.identity())
                .collect(Collectors.joining("\n"));
    }

    private String generateFeature() {
        ArrayList<String> statements = new ArrayList<>();
        if (packageName != null) {
            statements.add("package " + packageName + ";");
        }
        statements.add("""
                import org.graalvm.nativeimage.hosted.Feature;
                import org.graalvm.nativeimage.hosted.RuntimeReflection;
                import java.lang.foreign.MemorySegment;
                import top.dreamlike.panama.generator.proxy.NativeImageHelper;
                import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
                import java.lang.reflect.Constructor;
                """);
        statements.add(String.format("""
                public class %s implements Feature {
                    @Override
                    public void duringSetup(DuringSetupAccess access) {
                       Class needRegisterClass = null;

                       try {
                         %s
                       } catch(Throwable t) {
                          t.printStackTrace();
                       }
                    }

                    @Override
                    public void beforeAnalysis(BeforeAnalysisAccess access) {
                        Class needRegisterClass = null;
                         Constructor constructor;
                        try {
                         %s
                        } catch(Throwable t) {
                          t.printStackTrace();
                       }
                    }
                }
                """, className, generateDuringSetupBlock(), generateBeforeAnalysisBlock()));
        return String.join("\n", statements);
    }

    public String getRealName(TypeElement typeElement) {
        String qualifiedName = typeElement.getQualifiedName().toString();
        if (!typeElement.getNestingKind().isNested()) {
            return qualifiedName;
        }
        ArrayDeque<String> realNameDeque = new ArrayDeque<>();

        while (true) {
            int lastIndexOf = qualifiedName.lastIndexOf(".");
            String mayHost = qualifiedName.substring(0, lastIndexOf);
            TypeElement element = env.getElementUtils().getTypeElement(mayHost);
            realNameDeque.addFirst(qualifiedName.substring(lastIndexOf + 1));
            realNameDeque.addFirst("$");
            if (element == null || !element.getNestingKind().isNested()) {
                realNameDeque.addFirst(mayHost);
                break;
            }
            qualifiedName = mayHost;
        }
        return String.join("", realNameDeque);
    }

    private record ProxyPair(String origin, String proxy, CompileTimeGenerate.GenerateType generateType) {
    }

    private void dump(String className, byte[] bytes) {
        if (System.getProperty("panama.generator.debug") == null) {
            return;
        }
        try {
            String fileName = className + ".class";
            Path path = Path.of("apt-generator");
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
            Files.write(Path.of(path.toFile().getAbsolutePath(), fileName), bytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        } catch (Throwable r) {
            throw new RuntimeException(r);
        }
    }

    static class ProxyClassLoader extends ClassLoader {

        private final Map<String, Class> loadedClassed = new HashMap<>();

        private static final ClassLoader parent = PanamaAnnotationProcessor.class.getClassLoader();

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
           try {
               return Class.forName(name, false, parent);
           }catch (ClassNotFoundException _) {

           }
           name = name.replace('.', '/');
            Class aClass = loadedClassed.get(name);
            if (aClass == null) {
                throw new ClassNotFoundException(name);
            }
            return aClass;
        }

        public Class<?> defindClass(String name, byte[] bytes) {
            Class<?> aClass = defineClass(null, bytes, 0, bytes.length);
            loadedClassed.put(name, aClass);
            return aClass;
        }
    }


}

