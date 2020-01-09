package ca.coglinc.gradle.plugins.javacc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.tasks.SourceSetContainer;

public class JavaccPlugin implements Plugin<Project> {
    public static final String GROUP = "JavaCC";

    @Override
    public void apply(Project project) {
        Configuration configuration = createJavaccConfiguration(project);
        configureDefaultJavaccDependency(project, configuration);

        addCompileJavaccTaskToProject(project, configuration);

        configureTaskDependencies(project);
    }

    private void addSourceToIdes(final Project project, final AbstractJavaccTask task) {
        // Make the proto source dirs known to IDEs
        project.getPlugins().withType(JavaPlugin.class, new Action<JavaPlugin>() {
            @Override
            public void execute(JavaPlugin javaPlugin) {
                SourceSetContainer sourceSetContainer = (SourceSetContainer) project.getProperties().get("sourceSets");
                final File inputDirectory = task.getInputDirectory();
                sourceSetContainer.getByName("main").getAllJava().srcDir(inputDirectory);
                final File outputDirectory = task.getOutputDirectory();
                sourceSetContainer.getByName("main").getAllJava().srcDir(outputDirectory);
            }
        });
    }

    private Configuration createJavaccConfiguration(Project project) {
        Configuration configuration = project.getConfigurations().create("javacc");
        configuration.setVisible(false);
        configuration.setTransitive(true);
        configuration.setDescription("The javacc dependencies to be used.");
        return configuration;
    }

    private void configureDefaultJavaccDependency(final Project project, Configuration configuration) {
        configuration.defaultDependencies(new Action<DependencySet>() {
            @Override
            public void execute(DependencySet dependencies) {
                dependencies.add(project.getDependencies().create("net.java.dev.javacc:javacc:6.1.2"));
            }
        });
    }

    private void addCompileJavaccTaskToProject(Project project, Configuration configuration) {
        final CompileJavaccTask compileJavaccTask = addTaskToProject(project, CompileJavaccTask.class, CompileJavaccTask.TASK_NAME_VALUE, CompileJavaccTask.TASK_DESCRIPTION_VALUE,
            JavaccPlugin.GROUP, configuration);
        addSourceToIdes(project, compileJavaccTask);
    }

    private <T extends AbstractJavaccTask> T addTaskToProject(Project project, Class<T> type, String name, String description, String group, final Configuration configuration) {
        Map<String, Object> options = new HashMap<String, Object>(3);

        options.put(Task.TASK_TYPE, type);
        options.put(Task.TASK_DESCRIPTION, description);
        options.put(Task.TASK_GROUP, group);

        final Task task = project.task(options, name);
        final T abstractJavaccTask = type.cast(task);
        abstractJavaccTask.getConventionMapping().map("classpath", returning(configuration));

        return abstractJavaccTask;
    }

    private void configureTaskDependencies(Project project) {
        JavaToJavaccDependencyAction compileJavaDependsOnCompileJavacc = new JavaToJavaccDependencyAction();
        project.afterEvaluate(compileJavaDependsOnCompileJavacc);
    }

    private static <T> Callable<T> returning(final T value) {
        return new Callable<T>() {
            @Override
            public T call() {
                return value;
            }
        };
    }

}
