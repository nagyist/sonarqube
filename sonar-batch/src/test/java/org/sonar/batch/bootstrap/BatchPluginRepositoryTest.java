/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.util.FileUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.core.plugin.JpaPlugin;
import org.sonar.core.plugin.JpaPluginFile;

import java.util.Arrays;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchPluginRepositoryTest {

  @Test
  public void shouldLoadPlugin() {
    ExtensionDownloader extensionDownloader = mock(ExtensionDownloader.class);
    when(extensionDownloader.downloadExtension(any(JpaPluginFile.class))).thenReturn(
        FileUtils.toFile(getClass().getResource("/org/sonar/batch/bootstrap/BatchPluginRepositoryTest/sonar-artifact-size-plugin-0.2.jar")));
    BatchPluginRepository repository = new BatchPluginRepository(null, extensionDownloader);

    JpaPlugin plugin = new JpaPlugin("artifactsize");
    plugin.setPluginClass("org.sonar.plugins.artifactsize.ArtifactSizePlugin");
    plugin.createFile("sonar-artifact-size-plugin-0.2.jar");
    repository.doStart(Arrays.asList(plugin));

    Plugin entryPoint = repository.getPlugin("artifactsize");
    assertThat(entryPoint, not(nullValue()));
    ClassRealm classloader = (ClassRealm) entryPoint.getClass().getClassLoader();
    assertThat(classloader.getId(), is("artifactsize"));
  }

  /**
   * Of course clirr does not extend artifact-size plugin in real life !
   */
  @Test
  public void shouldPluginExtensionInTheSameClassloader() {
    ExtensionDownloader extensionDownloader = mock(ExtensionDownloader.class);
    prepareDownloader(extensionDownloader, "artifactsize", "/org/sonar/batch/bootstrap/BatchPluginRepositoryTest/sonar-artifact-size-plugin-0.2.jar");
    prepareDownloader(extensionDownloader, "clirr", "/org/sonar/batch/bootstrap/BatchPluginRepositoryTest/sonar-clirr-plugin-1.1.jar");
    BatchPluginRepository repository = new BatchPluginRepository(null, extensionDownloader);

    JpaPlugin pluginBase = new JpaPlugin("artifactsize");
    pluginBase.setPluginClass("org.sonar.plugins.artifactsize.ArtifactSizePlugin");
    pluginBase.createFile("sonar-artifact-size-plugin-0.2.jar");

    JpaPlugin pluginExtension = new JpaPlugin("clirr");
    pluginExtension.setBasePlugin("artifactsize");
    pluginExtension.setPluginClass("org.sonar.plugins.clirr.ClirrPlugin");
    pluginExtension.createFile("sonar-clirr-plugin-1.1.jar");

    repository.doStart(Arrays.asList(pluginBase, pluginExtension));

    Plugin entryPointBase = repository.getPlugin("artifactsize");
    Plugin entryPointExtension = repository.getPlugin("clirr");
    assertThat(entryPointBase.getClass().getClassLoader(), is(entryPointExtension.getClass().getClassLoader()));
  }

  private void prepareDownloader(ExtensionDownloader extensionDownloader, final String pluginKey, final String filename) {
    when(extensionDownloader.downloadExtension(argThat(new BaseMatcher<JpaPluginFile>() {
      public boolean matches(Object o) {
        return o!=null && ((JpaPluginFile) o).getPluginKey().equals(pluginKey);
      }

      public void describeTo(Description description) {

      }
    }))).thenReturn(FileUtils.toFile(getClass().getResource(filename)));
  }

//  @Test
//  public void shouldRegisterBatchExtension() {
//    MutablePicoContainer pico = IocContainer.buildPicoContainer();
//    pico.addComponent(new PropertiesConfiguration());
//    BatchPluginRepository repository = new BatchPluginRepository();
//
//    // check classes
//    assertThat(repository.shouldRegisterExtension(pico, "foo", FakeBatchExtension.class), is(true));
//    assertThat(repository.shouldRegisterExtension(pico, "foo", FakeServerExtension.class), is(false));
//    assertThat(repository.shouldRegisterExtension(pico, "foo", String.class), is(false));
//
//    // check objects
//    assertThat(repository.shouldRegisterExtension(pico, "foo", new FakeBatchExtension()), is(true));
//    assertThat(repository.shouldRegisterExtension(pico, "foo", new FakeServerExtension()), is(false));
//    assertThat(repository.shouldRegisterExtension(pico, "foo", "bar"), is(false));
//  }
//
//  @Test
//  public void shouldRegisterOnlyCoberturaExtensionByDefault() {
//    BatchPluginRepository repository = new BatchPluginRepository();
//    PropertiesConfiguration conf = new PropertiesConfiguration();
//    assertThat(repository.shouldRegisterCoverageExtension("cobertura", newJavaProject(), conf), is(true));
//    assertThat(repository.shouldRegisterCoverageExtension("clover", newJavaProject(), conf), is(false));
//  }
//
//  @Test
//  public void shouldRegisterCustomCoverageExtension() {
//    Configuration conf = new PropertiesConfiguration();
//    conf.setProperty(AbstractCoverageExtension.PARAM_PLUGIN, "clover,phpunit");
//    BatchPluginRepository repository = new BatchPluginRepository();
//    assertThat(repository.shouldRegisterCoverageExtension("cobertura", newJavaProject(), conf), is(false));
//    assertThat(repository.shouldRegisterCoverageExtension("clover", newJavaProject(), conf), is(true));
//    assertThat(repository.shouldRegisterCoverageExtension("phpunit", newJavaProject(), conf), is(true));
//    assertThat(repository.shouldRegisterCoverageExtension("other", newJavaProject(), conf), is(false));
//  }
//
//  @Test
//  public void shouldNotCheckCoverageExtensionsOnNonJavaProjects() {
//    Configuration conf = new PropertiesConfiguration();
//    conf.setProperty(AbstractCoverageExtension.PARAM_PLUGIN, "cobertura");
//    BatchPluginRepository repository = new BatchPluginRepository();
//
//    assertThat(repository.shouldRegisterCoverageExtension("groovy", newGroovyProject(), conf), is(true));
//    assertThat(repository.shouldRegisterCoverageExtension("groovy", newJavaProject(), conf), is(false));
//  }
//
//  private static Project newJavaProject() {
//    return new Project("foo").setLanguageKey(Java.KEY).setAnalysisType(AnalysisType.DYNAMIC);
//  }
//
//  private static Project newGroovyProject() {
//    return new Project("foo").setLanguageKey("grvy").setAnalysisType(AnalysisType.DYNAMIC);
//  }
//
//  public static class FakeBatchExtension implements BatchExtension {
//
//  }
//
//  public static class FakeServerExtension implements ServerExtension {
//
//  }
}
