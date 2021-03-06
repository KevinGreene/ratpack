/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ratpack.groovy.markuptemplates;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import ratpack.groovy.markuptemplates.internal.MarkupTemplateRenderer;
import ratpack.launch.LaunchConfig;

import javax.inject.Singleton;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An extension module that provides support for the Groovy markup template engine.
 * <p>
 * To use it one has to register the module and then render {@link MarkupTemplate} instances.
 * Instances of {@code ratpack.groovy.markuptemplates.MarkupTemplate} can be created using one of the
 * {@link ratpack.groovy.Groovy#groovyMarkupTemplate(java.util.Map, String, String)}
 * static methods.
 * </p>
 * <p>
 * By default templates are looked up in the {@code templates} directory of the application root.
 * So {@code groovyMarkupTemplate("my/template/path.gtpl")} maps to {@code tempaltes/my/template/path.gtpl} in the application root directory.
 * <p>
 * The template engine can be configured using the {@link #getTemplateConfiguration() template configuration}. In particular, it is possible to configure
 * things like automatic indentation.
 * </p>
 * <p>
 * Response content type can be manually specified, i.e. {@code groovyMarkupTemplate("template.gtpl", model, "text/html")} if
 * not specified will default to {@code text/html}.
 * </p>
 *
 * Example usage: (Java DSL)
 * <pre class="tested">
 * import ratpack.handling.*;
 * import ratpack.guice.*;
 * import ratpack.func.Action;
 * import ratpack.launch.*;
 * import ratpack.groovy.markuptemplates.MarkupTemplatingModule;
 * import static ratpack.groovy.Groovy.groovyMarkupTemplate;
 *
 * class MyHandler implements Handler {
 *   void handle(final Context context) {
 *     context.render(groovyMarkupTemplate("my/template/path.gtpl", key: "it works!"));
 *   }
 * }
 *
 * class Bindings implements Action&lt;BindingsSpec&gt; {
 *   public void execute(BindingsSpec bindings) {
 *     bindings.add(new MarkupTemplatingModule());
 *   }
 * }
 *
 * LaunchConfig launchConfig = LaunchConfigBuilder.baseDir(new File("appRoot"))
 *     .build(new HandlerFactory() {
 *   public Handler create(LaunchConfig launchConfig) {
 *     return Guice.handler(launchConfig, new Bindings(), new ChainAction() {
 *       protected void execute() {
 *         handler(chain.getRegistry().get(MyHandler.class));
 *       }
 *     });
 *   }
 * });
 * </pre>
 *
 * Example usage: (Groovy DSL)
 * <pre class="groovy-ratpack-dsl">
 * import ratpack.groovy.markuptemplates.MarkupTemplatingModule
 * import static ratpack.groovy.Groovy.groovyMarkupTemplate
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     add new MarkupTemplatingModule()
 *   }
 *   handlers {
 *     get {
 *       render groovyMarkupTemplate('my/template/path.gtpl', key: 'it works!')
 *     }
 *   }
 * }
 * </pre>
 *
 * @see <a href="http://beta.groovy-lang.org/docs/latest/html/documentation/markup-template-engine.html" target="_blank">Groovy Markup Template Engine</a>
 */
public class MarkupTemplatingModule extends AbstractModule {

  private final TemplateConfiguration templateConfiguration;
  private String templatesDirectory = "templates";

  public MarkupTemplatingModule() {
    templateConfiguration = new TemplateConfiguration();
  }

  public TemplateConfiguration getTemplateConfiguration() {
    return templateConfiguration;
  }

  public String getTemplatesDirectory() {
    return templatesDirectory;
  }

  public void setTemplatesDirectory(String templatesDirectory) {
    this.templatesDirectory = templatesDirectory;
  }

  @Override
  protected void configure() {
    bind(MarkupTemplateRenderer.class).in(Singleton.class);
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  MarkupTemplateEngine provideTemplateEngine(LaunchConfig launchConfig) {
    if (launchConfig.isReloadable()) {
      templateConfiguration.setCacheTemplates(false);
    }
    ClassLoader parent = getClass().getClassLoader();
    try {
      parent = new URLClassLoader(new URL[]{launchConfig.getBaseDir().file(templatesDirectory).toFile().toURI().toURL()}, parent);
    } catch (MalformedURLException e) {
      parent = getClass().getClassLoader();
    }
    return new MarkupTemplateEngine(parent, templateConfiguration, new CachingTemplateResolver());
  }

  /**
   * A template resolver which avoids calling {@link ClassLoader#getResource(String)} if a template path already has
   * been queried before. This improves performance if caching is enabled in the configuration.
   */
  private static class CachingTemplateResolver extends MarkupTemplateEngine.DefaultTemplateResolver {
    private final Map<String, URL> cachedResources = new ConcurrentHashMap<>();
    private boolean cache;

    @Override
    public void configure(ClassLoader templateClassLoader, TemplateConfiguration configuration) {
      super.configure(templateClassLoader, configuration);
      cache = configuration.isCacheTemplates();
    }

    @Override
    public URL resolveTemplate(String templatePath) throws IOException {
      if (cache) {
        URL url = cachedResources.get(templatePath);
        if (url!=null) {
          return url;
        }
      }
      URL url = super.resolveTemplate(templatePath);
      if (cache) {
        cachedResources.put(templatePath, url);
      }
      return url;
    }
  }
}
