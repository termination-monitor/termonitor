/*
 * Copyright Termonitor authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package cz.scholz.termonitor;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.quarkus.runtime.Quarkus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@CommandLine.Command
public class Termonitor implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Termonitor.class);

    @CommandLine.Option(names = {"-t", "--threshold"}, description = "The threshold (in % or grace period) at which an alert should be raised", defaultValue = "90")
    int thresholdPercentage;

    private final Map<String, PodTermination> tracking = new HashMap<>();

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public void run() {
        kubernetesClient.pods().inAnyNamespace().inform().addEventHandlerWithResyncPeriod(new ResourceEventHandler<>() {
            @Override
            public void onAdd(Pod pod) {
                handlePod(pod);
            }

            @Override
            public void onUpdate(Pod oldPod, Pod newPod) {
                handlePod(newPod);
            }

            @Override
            public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {
                handleDeletedPod(pod);
            }
        }, 10 * 60 * 1000);

        Quarkus.waitForExit();
    }

    void handlePod(Pod pod) {
        LOG.debug("Pod {}/{} updated", pod.getMetadata().getNamespace(), pod.getMetadata().getName());

        if (pod.getMetadata().getDeletionTimestamp() != null)   {
            registerPodDeleting(pod);
        }
    }

    void handleDeletedPod(Pod pod) {
        if (tracking.containsKey(pod.getMetadata().getUid()))   {
            Duration elapsed = Duration.between(tracking.get(pod.getMetadata().getUid()).deletionInstant, Instant.now());
            LOG.info("Pod {}/{} deleted after {} seconds (out of {})", pod.getMetadata().getNamespace(), pod.getMetadata().getName(), elapsed.getSeconds(), tracking.get(pod.getMetadata().getUid()).deletionGracePeriodSeconds);
            tracking.remove(pod.getMetadata().getUid());
        } else {
            LOG.warn("Pod {}/{} was deleted, but the deletion was not tracked", pod.getMetadata().getNamespace(), pod.getMetadata().getName());
            tracking.put(pod.getMetadata().getUid(), PodTermination.fromPod(pod));
        }
    }

    void registerPodDeleting(Pod pod)   {
        if (tracking.containsKey(pod.getMetadata().getUid()))   {
            Duration elapsed = Duration.between(tracking.get(pod.getMetadata().getUid()).deletionInstant, Instant.now());
            LOG.info("Pod {}/{} deletion is ongoing for {} seconds (out of {})", pod.getMetadata().getNamespace(), pod.getMetadata().getName(), elapsed.getSeconds(), tracking.get(pod.getMetadata().getUid()).deletionGracePeriodSeconds);
        } else {
            LOG.warn("Pod {}/{} deletion started at {} with termination grace period of {} seconds", pod.getMetadata().getNamespace(), pod.getMetadata().getName(), pod.getMetadata().getDeletionTimestamp(), pod.getMetadata().getDeletionGracePeriodSeconds());
            tracking.put(pod.getMetadata().getUid(), PodTermination.fromPod(pod));
        }
    }
}

class PodTermination  {
    String namespace;
    String name;
    String deletionTimestamp;
    long deletionGracePeriodSeconds;
    Instant deletionInstant;

    public PodTermination(String namespace, String name, String deletionTimestamp, long deletionGracePeriodSeconds) {
        this.namespace = namespace;
        this.name = name;
        this.deletionTimestamp = deletionTimestamp;
        //this.deletionInstant = Instant.parse(deletionTimestamp);
        this.deletionInstant = Instant.now();
        this.deletionGracePeriodSeconds = deletionGracePeriodSeconds;
    }

    public static PodTermination fromPod(Pod pod)  {
        return new PodTermination(pod.getMetadata().getNamespace(), pod.getMetadata().getName(), pod.getMetadata().getDeletionTimestamp(), pod.getMetadata().getDeletionGracePeriodSeconds());
    }
}
