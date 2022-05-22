/*
 * Copyright Termonitor authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package cz.scholz.termonitor;

import io.fabric8.kubernetes.api.model.Pod;

import java.time.Duration;
import java.time.Instant;

class PodTermination  {
    String namespace;
    String name;
    String uuid;
    String deletionTimestamp;
    long deletionGracePeriodSeconds;
    Instant deletionInstant;

    public PodTermination(Pod pod) {
        this.namespace = pod.getMetadata().getNamespace();
        this.name = pod.getMetadata().getName();
        this.uuid = pod.getMetadata().getUid();
        this.deletionTimestamp = pod.getMetadata().getDeletionTimestamp();
        //this.deletionInstant = Instant.parse(deletionTimestamp);
        this.deletionInstant = Instant.now();
        this.deletionGracePeriodSeconds = pod.getMetadata().getDeletionGracePeriodSeconds();
    }

    public long elapsedPercentage()  {
        return Math.round((double) elapsedSeconds() / deletionGracePeriodSeconds * 100);
    }

    public long elapsedSeconds() {
        return Duration.between(deletionInstant, Instant.now()).getSeconds();
    }
}
