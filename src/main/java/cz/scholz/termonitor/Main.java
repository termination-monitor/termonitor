/*
 * Copyright Termonitor authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package cz.scholz.termonitor;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.Quarkus;
import picocli.CommandLine;

import javax.inject.Inject;

@CommandLine.Command
public class Main implements Runnable {
    @Inject
    KubernetesClient client;

    @CommandLine.Option(names = {"-t", "--threshold"}, description = "The threshold (in % or grace period) at which an alert should be raised", defaultValue = "75")
    int thresholdPercentage;

    @Override
    public void run() {
        new Termonitor(client, thresholdPercentage);
        Quarkus.waitForExit();
    }
}
