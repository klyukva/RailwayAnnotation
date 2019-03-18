package com.example.annotation;

import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import structure.RailwayRoutes;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

@SupportedAnnotationTypes(value = {RailwayAnnotationProcessor.ANNOTATION_TYPE})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({RailwayAnnotationProcessor.ENABLE_OPTIONS_NAME})
public class RailwayAnnotationProcessor extends AbstractProcessor {
    public static final String ANNOTATION_TYPE = "com.example.annotation.Railway";
    public static final String ENABLE_OPTIONS_NAME = "Railway";
    private static final String SUPPORT_FIELD_TYPE = "String";
    private JavacProcessingEnvironment javacProcessingEnv;
    private TreeMaker maker;

    private boolean enable = true;

    @Override
    public void init(ProcessingEnvironment procEnv) {
        super.init(procEnv);
        this.javacProcessingEnv = (JavacProcessingEnvironment) procEnv;
        this.maker = TreeMaker.instance(javacProcessingEnv.getContext());
        java.util.Map<String, String> opt = javacProcessingEnv.getOptions();
        if (opt.containsKey(ENABLE_OPTIONS_NAME) && opt.get(ENABLE_OPTIONS_NAME).equals("disable")){
            enable = false;
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!enable){
            javacProcessingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "Annotation Railway is disable");
            return false;
        }
        if ( annotations == null || annotations.isEmpty()) {
            return false;
        }
        //Получаем вспомогательные инструменты.
        JavacElements utils = javacProcessingEnv.getElementUtils();
        for (TypeElement annotation : annotations)
        {
            //Проверяем тип аннотации
            if (ANNOTATION_TYPE.equals(annotation.asType().toString())){
                // Выбираем все элементы, у которых стоит наша аннотация
                final Set<? extends Element> fields = roundEnv.getElementsAnnotatedWith(annotation);
                for (final Element field : fields) {
                    //Получаем саму аннотацию, чтоб достать из неё исходные данные для задачи с поездами
                    Railway railway = field.getAnnotation(Railway.class);
                    //Проверка корректности объекта аннотации
                    if (railway == null){
                        javacProcessingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                "Bad annotate field: System error - annotation is null", field);
                        continue;
                    }
                    //Преобразовываем аннотированный элемент в дерево
                    JCTree blockNode = utils.getTree(field);
                    //Получаем описания полей
                    if (blockNode instanceof JCTree.JCVariableDecl) {
                        JCTree.JCVariableDecl var = (JCTree.JCVariableDecl) blockNode;
                        if (!SUPPORT_FIELD_TYPE.equals(var.vartype.toString())){
                            javacProcessingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                    String.format("Unsupported field type \"%s\" for railway. Supported only String.",
                                            var.vartype.toString()), field);
                            continue;
                        }
                        JCTree.JCExpression initializer = var.getInitializer();
                        //Проверка отсечёт поля с инициализацией в конструкторе, а так же конструкции вида:
                        // "" + 1
                        // new String("new string")
                        if ((initializer != null) && (var.getInitializer() instanceof JCTree.JCLiteral)){
                            //Берём строку инициализации.
                            JCTree.JCLiteral lit = (JCTree.JCLiteral) var.getInitializer();
                            //Получаем строку
                            String value = lit.getValue().toString();
                                //Вызываем проверку проезда поездов
                                value = RailwayRoutes.railwayCheck(value);
                                //Выводим результат во время компиляции
                                javacProcessingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                        value, field);
                                //Меняем строку с исходными данными на результат
                                lit = maker.Literal(value);
                                var.init = lit;

                        }else{
                            javacProcessingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                    "Bad annotation: supported only literal string. Example: \"Good string\" ",
                                    field);
                        }

                    }else{
                        javacProcessingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "Bad annotate. Supported only class variable field.",
                                field);
                    }
                }
            }
        }
        return true;
    }
}
