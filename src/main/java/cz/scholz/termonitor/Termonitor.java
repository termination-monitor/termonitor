/*
 * Copyright Termonitor authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package cz.scholz.termonitor;

import io.fabric8.kubernetes.api.model.MicroTime;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.events.v1.Event;
import io.fabric8.kubernetes.api.model.events.v1.EventBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Termonitor {
    private static final Logger LOG = LoggerFactory.getLogger(Termonitor.class);

    private static final DateTimeFormatter K8S_MICROTIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSSSSSXXX").withZone(ZoneId.from(ZoneOffset.UTC));

    private final Map<String, PodTermination> tracking = new HashMap<>();
    private final int thresholdPercentage;
    private final SharedIndexInformer<Pod> podInformer;
    private final KubernetesClient client;

    public Termonitor(KubernetesClient client, int thresholdPercentage) {
        this.client = client;
        this.thresholdPercentage = thresholdPercentage;

        this.podInformer = this.client.pods().inAnyNamespace().inform();
        this.podInformer.addEventHandlerWithResyncPeriod(new ResourceEventHandler<>() {
            @Override
            public void onAdd(Pod pod) {
                handlePodUpdate(pod);
            }

            @Override
            public void onUpdate(Pod oldPod, Pod newPod) {
                handlePodUpdate(newPod);
            }

            @Override
            public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {
                handlePodDeleted(pod);
            }
        }, 10 * 60 * 1000);
    }

    void handlePodUpdate(Pod pod) {
        LOG.debug("Pod {}/{} updated", pod.getMetadata().getNamespace(), pod.getMetadata().getName());

        if (pod.getMetadata().getDeletionTimestamp() != null)   {
            registerOngoingDeletion(pod);
        }
    }

    void handlePodDeleted(Pod pod) {
        PodTermination pt = tracking.get(pod.getMetadata().getUid());

        if (pt != null)   {
            long elapsedPercentage = pt.elapsedPercentage();
            long elapsedSeconds = pt.elapsedSeconds();

            if (elapsedPercentage > thresholdPercentage) {
                LOG.warn("Pod {}/{} deleted after {} seconds ({}% out of {} second grace period)", pt.namespace, pt.name, elapsedSeconds, elapsedPercentage, pt.deletionGracePeriodSeconds);
                raiseEvent(pod, pt, elapsedPercentage, elapsedSeconds);
            } else {
                LOG.info("Pod {}/{} deleted after {} seconds ({}% out of {} second grace period)", pt.namespace, pt.name, elapsedSeconds, elapsedPercentage, pt.deletionGracePeriodSeconds);
            }

            tracking.remove(pt.uuid);
        } else {
            LOG.error("Pod {}/{} was deleted, but the deletion was not tracked", pod.getMetadata().getNamespace(), pod.getMetadata().getName());
        }
    }

    void registerOngoingDeletion(Pod pod)   {
        PodTermination pt = tracking.get(pod.getMetadata().getUid());

        if (pt != null)   {
            LOG.debug("Pod {}/{} still deleting after {} seconds ({}% out of {} second grace period)", pt.namespace, pt.name, pt.elapsedSeconds(), pt.elapsedPercentage(), pt.deletionGracePeriodSeconds);
        } else {
            LOG.info("Pod {}/{} deletion started at {} with termination grace period of {} seconds", pod.getMetadata().getNamespace(), pod.getMetadata().getName(), pod.getMetadata().getDeletionTimestamp(), pod.getMetadata().getDeletionGracePeriodSeconds());
            tracking.put(pod.getMetadata().getUid(), new PodTermination(pod));
        }
    }

    void raiseEvent(Pod pod, PodTermination pt, long elapsedPercentage, long elapsedSeconds)    {
        Event event = new EventBuilder()
                .withNewMetadata()
                    .withNamespace(pt.namespace)
                    .withGenerateName("termonitor")
                .endMetadata()
                .withEventTime(new MicroTime(K8S_MICROTIME.format(Instant.now())))
                .withReportingController("scholz.cz/termonitor")
                .withReportingInstance("termonitor")
                .withAction("PodTerminationWarning")
                .withReason("PodTerminationTookTooLong")
                .withType("Normal")
                .withRegarding(new ObjectReferenceBuilder()
                        .withApiVersion("v1")
                        .withKind("Pod")
                        .withNamespace(pt.namespace)
                        .withName(pt.name)
                        .build())
                .withNote(String.format("Pod %s/%s deleted after %d seconds (%d%% out of %d second grace period)", pt.namespace, pt.name, elapsedSeconds, elapsedPercentage, pt.deletionGracePeriodSeconds))
                .build();

        client.events().v1().events().create(event);
    }
}
