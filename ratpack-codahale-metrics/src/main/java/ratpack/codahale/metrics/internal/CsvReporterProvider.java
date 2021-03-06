/*
 * Copyright 2013 the original author or authors.
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

package ratpack.codahale.metrics.internal;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import ratpack.launch.LaunchConfig;

import javax.inject.Inject;
import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * A Provider implementation that sets up a {@link CsvReporter} for a {@link MetricRegistry}.
 */
public class CsvReporterProvider implements Provider<CsvReporter> {

  public static final String CSV_REPORT_DIRECTORY = "ratpack.codahale.metrics.internal.CsvReporterProvider.csvReportDirectory";

  private final MetricRegistry metricRegistry;
  private final File csvReportDirectory;
  private final LaunchConfig launchConfig;

  @Inject
  public CsvReporterProvider(MetricRegistry metricRegistry, @Named(CsvReporterProvider.CSV_REPORT_DIRECTORY) File csvReportDirectory, LaunchConfig launchConfig) {
    this.metricRegistry = metricRegistry;
    this.csvReportDirectory = csvReportDirectory;
    this.launchConfig = launchConfig;
  }

  @Override
  public CsvReporter get() {
    CsvReporter reporter = CsvReporter.forRegistry(metricRegistry).build(csvReportDirectory);
    String interval = launchConfig.getOther("metrics.scheduledreporter.interval", "30");
    reporter.start(Long.parseLong(interval), TimeUnit.SECONDS);
    return reporter;
  }
}

